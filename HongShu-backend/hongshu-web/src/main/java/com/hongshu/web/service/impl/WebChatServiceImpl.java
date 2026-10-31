package com.hongshu.web.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hongshu.common.exception.web.HongshuException;
import com.hongshu.web.auth.AuthContextHolder;
import com.hongshu.web.domain.entity.WebChat;
import com.hongshu.web.domain.entity.WebUser;
import com.hongshu.web.domain.vo.ChatUserRelationVO;
import com.hongshu.web.repository.ChatRepository;
import com.hongshu.web.service.ChatConversationService;
import com.hongshu.web.service.ChatMessagePublisher;
import com.hongshu.web.service.ChatNotificationService;
import com.hongshu.web.service.FileAssetService;
import com.hongshu.web.service.IWebChatService;
import com.hongshu.web.websocket.im.CountMessage;
import com.hongshu.web.websocket.im.Message;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 聊天应用服务，负责权限校验及事务编排。
 * 写事务使用 READ_COMMITTED，结合固定顺序的双方用户行锁协调会话变更。
 */
@Service
public class WebChatServiceImpl implements IWebChatService {

    private static final int TEXT_MESSAGE_TYPE = 1;
    private static final int IMAGE_MESSAGE_TYPE = 2;
    private static final int DIRECT_CHAT_TYPE = 0;
    private static final int CHAT_UNREAD_COUNT_TYPE = 3;
    private static final int MAX_MESSAGE_LENGTH = 5000;
    private static final int MAX_PAGE_SIZE = 100;
    private static final String ENABLED_STATUS = "0";
    private static final String IMAGE_PREVIEW = "[图片]";

    private final ChatRepository chats;
    private final ChatConversationService conversations;
    private final ChatMessagePublisher publisher;
    private final ChatNotificationService notifications;
    private final FileAssetService assets;

    public WebChatServiceImpl(ChatRepository chats, ChatConversationService conversations,
                              ChatMessagePublisher publisher, ChatNotificationService notifications,
                              FileAssetService assets) {
        this.chats = chats;
        this.conversations = conversations;
        this.publisher = publisher;
        this.notifications = notifications;
        this.assets = assets;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public void sendMsg(Message message) {
        validateMessage(message);
        ChatRepository.Participants participants = chats.lockParticipants(message.getSendUid(), message.getAcceptUid());
        requireEnabled(participants.currentUser());
        requireEnabled(participants.otherUser());
        message.setSendUid(participants.currentUserId());
        message.setAcceptUid(participants.otherUserId());
        WebChat chat = toChat(message);
        chats.saveMessage(chat, preview(chat));
        publisher.publishAfterCommit(message);
    }

    @Override
    public Page<WebChat> getAllChatRecord(long currentPage, long pageSize, String acceptUid) {
        if (currentPage < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new HongshuException("分页参数无效，每页最多 100 条");
        }
        if (currentPage - 1 > Long.MAX_VALUE / pageSize) {
            throw new HongshuException("分页参数超出范围");
        }
        return chats.findVisibleMessages(requireCurrentUser(), requireOtherUser(acceptUid), currentPage, pageSize);
    }

    @Override
    public List<ChatUserRelationVO> getChatUserList() {
        return conversations.listForUser(requireCurrentUser());
    }

    @Override
    public CountMessage getCountMessage() {
        return notifications.getCount(requireCurrentUser());
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public void clearMessageCount(String sendUid, Integer type) {
        String currentUserId = requireCurrentUser();
        if (Integer.valueOf(CHAT_UNREAD_COUNT_TYPE).equals(type)) {
            String otherUserId = requireOtherUser(sendUid);
            ChatRepository.Participants participants = chats.lockParticipants(currentUserId, otherUserId);
            chats.clearUnread(participants.currentUserId(), participants.otherUserId());
            return;
        }
        notifications.clearCount(currentUserId, type);
    }

    @Override
    public boolean closeChat(String sendUid) {
        publisher.closeConnection(requireCurrentUser());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public void deleteMessage(String id) {
        String userId = requireCurrentUser();
        WebChat original = chats.findMessage(id);
        if (original == null) {
            return;
        }
        String otherUserId = otherParticipant(original, userId);
        ChatRepository.Participants participants = chats.lockParticipants(userId, otherUserId);
        userId = participants.currentUserId();
        otherUserId = participants.otherUserId();
        WebChat current = chats.findMessageForUpdate(id);
        if (current == null) {
            return;
        }
        if (!otherUserId.equals(otherParticipant(current, userId))) {
            throw new HongshuException("聊天记录已变化，请重试");
        }
        chats.hideMessage(id, userId);
        refreshConversation(userId, otherUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public void deleteConversation(String otherUserId) {
        String userId = requireCurrentUser();
        ChatRepository.Participants participants = chats.lockParticipants(userId, requireOtherUser(otherUserId));
        chats.hideConversation(participants.currentUserId(), participants.otherUserId());
    }

    private void validateMessage(Message message) {
        if (message == null) {
            throw new HongshuException("聊天用户不可用");
        }
        String userId = requireCurrentUser();
        requireOtherUser(message.getAcceptUid());
        int type = message.getMsgType() == null ? TEXT_MESSAGE_TYPE : message.getMsgType();
        if ((type != TEXT_MESSAGE_TYPE && type != IMAGE_MESSAGE_TYPE)
                || (message.getChatType() != null && message.getChatType() != DIRECT_CHAT_TYPE)) {
            throw new HongshuException("不支持的消息类型");
        }
        if (!(message.getContent() instanceof String content)
                || content.isBlank() || content.length() > MAX_MESSAGE_LENGTH) {
            throw new HongshuException("消息需为 1 至 5000 字");
        }
        if (type == IMAGE_MESSAGE_TYPE) {
            String path = assets.normalize(content);
            if (!path.startsWith("/profile/") && !path.startsWith("https://") && !path.startsWith("http://")) {
                throw new HongshuException("图片地址无效");
            }
            assets.requireUsable(content);
            message.setContent(path);
        }
        message.setMsgType(type);
        message.setSendUid(userId);
        message.setChatType(DIRECT_CHAT_TYPE);
    }

    private WebChat toChat(Message message) {
        WebChat chat = new WebChat();
        chat.setSendUid(message.getSendUid());
        chat.setAcceptUid(message.getAcceptUid());
        chat.setChatType(message.getChatType());
        chat.setMsgType(message.getMsgType());
        chat.setContent((String) message.getContent());
        chat.setTimestamp(System.currentTimeMillis());
        return chat;
    }

    private void refreshConversation(String userId, String otherUserId) {
        WebChat latest = chats.findLatestVisibleMessageForUpdate(userId, otherUserId);
        if (latest == null) {
            chats.removeConversation(userId, otherUserId);
            return;
        }
        chats.refreshPreview(userId, otherUserId, preview(latest), latest.getTimestamp());
    }

    private String preview(WebChat chat) {
        return Integer.valueOf(IMAGE_MESSAGE_TYPE).equals(chat.getMsgType()) ? IMAGE_PREVIEW : chat.getContent();
    }

    private String otherParticipant(WebChat chat, String userId) {
        if (userId.equals(chat.getSendUid())) {
            return requireOtherUser(chat.getAcceptUid());
        }
        if (userId.equals(chat.getAcceptUid())) {
            return requireOtherUser(chat.getSendUid());
        }
        throw new HongshuException("无权删除此消息", 403);
    }

    private void requireEnabled(WebUser user) {
        if (user == null || (StringUtils.isNotBlank(user.getStatus()) && !ENABLED_STATUS.equals(user.getStatus()))) {
            throw new HongshuException("聊天用户不可用");
        }
    }

    private String requireOtherUser(String userId) {
        if (StringUtils.isBlank(userId)) {
            throw new HongshuException("聊天用户不可用");
        }
        return userId;
    }

    private String requireCurrentUser() {
        String userId = AuthContextHolder.getUserId();
        if (StringUtils.isBlank(userId)) {
            throw new HongshuException("请先登录", 401);
        }
        return userId;
    }
}
