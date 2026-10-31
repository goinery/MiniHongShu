package com.hongshu.web.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hongshu.web.domain.entity.WebChat;
import com.hongshu.web.domain.entity.WebChatUserRelation;
import com.hongshu.web.domain.entity.WebUser;
import com.hongshu.web.mapper.WebChatMapper;
import com.hongshu.web.mapper.WebChatUserRelationMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 聊天数据访问入口，统一可见性条件、会话摘要和双方用户行锁。
 * 所有写操作必须在同一事务中先调用 {@link #lockParticipants(String, String)}。
 */
@Repository
public class ChatRepository {

    private final WebChatMapper chatMapper;
    private final WebChatUserRelationMapper relationMapper;
    private final JdbcTemplate jdbc;

    public ChatRepository(WebChatMapper chatMapper, WebChatUserRelationMapper relationMapper, JdbcTemplate jdbc) {
        this.chatMapper = chatMapper;
        this.relationMapper = relationMapper;
        this.jdbc = jdbc;
    }

    /**
     * 按用户 ID 的字典序逐行锁定，和关注等业务采用相同顺序。
     * 用户记录是首次会话也必定存在的互斥点，避免依赖尚未创建的会话行。
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Participants lockParticipants(String currentUserId, String otherUserId) {
        // 使用数据库实际 ID，避免 "02" 等别名改变同一用户行的加锁顺序。
        String currentId = resolveUserId(currentUserId);
        String otherId = resolveUserId(otherUserId);
        String firstId = currentId.compareTo(otherId) <= 0 ? currentId : otherId;
        String secondId = firstId.equals(currentId) ? otherId : currentId;
        WebUser firstUser = lockUser(firstId);
        WebUser secondUser = firstId.equals(secondId) ? firstUser : lockUser(secondId);
        return firstId.equals(currentId)
                ? new Participants(currentId, otherId, firstUser, secondUser)
                : new Participants(currentId, otherId, secondUser, firstUser);
    }

    public WebChat findMessage(String messageId) {
        return chatMapper.selectById(messageId);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public WebChat findMessageForUpdate(String messageId) {
        return chatMapper.selectList(new QueryWrapper<WebChat>().eq("id", messageId).last("FOR UPDATE"))
                .stream().findFirst().orElse(null);
    }

    public Page<WebChat> findVisibleMessages(String userId, String otherUserId, long pageNumber, long pageSize) {
        return chatMapper.selectPage(new Page<>(pageNumber, pageSize), visibleMessages(userId, otherUserId));
    }

    public List<WebChatUserRelation> findConversations(String userId) {
        return relationMapper.selectList(new QueryWrapper<WebChatUserRelation>()
                .eq("accept_uid", userId).orderByDesc("timestamp", "id"));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void saveMessage(WebChat chat, String preview) {
        chatMapper.insert(chat);
        boolean selfConversation = chat.getSendUid().equals(chat.getAcceptUid());
        updateConversation(chat.getSendUid(), chat.getAcceptUid(), preview, chat.getTimestamp(), !selfConversation);
        if (!selfConversation) {
            updateConversation(chat.getAcceptUid(), chat.getSendUid(), preview, chat.getTimestamp(), false);
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void clearUnread(String userId, String otherUserId) {
        jdbc.update("UPDATE web_chat_user_relation SET count = 0 WHERE accept_uid = ? AND send_uid = ?",
                userId, otherUserId);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void hideMessage(String messageId, String userId) {
        jdbc.update("UPDATE web_chat SET send_deleted = 1 WHERE id = ? AND send_uid = ?", messageId, userId);
        jdbc.update("UPDATE web_chat SET accept_deleted = 1 WHERE id = ? AND accept_uid = ?", messageId, userId);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void hideConversation(String userId, String otherUserId) {
        jdbc.update("UPDATE web_chat SET send_deleted = 1 WHERE send_uid = ? AND accept_uid = ?",
                userId, otherUserId);
        jdbc.update("UPDATE web_chat SET accept_deleted = 1 WHERE accept_uid = ? AND send_uid = ?",
                userId, otherUserId);
        removeConversation(userId, otherUserId);
    }

    /**
     * 删除消息后必须使用当前读，避免事务此前的普通查询留下旧快照。
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public WebChat findLatestVisibleMessageForUpdate(String userId, String otherUserId) {
        return chatMapper.selectList(visibleMessages(userId, otherUserId).last("LIMIT 1 FOR UPDATE"))
                .stream().findFirst().orElse(null);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void refreshPreview(String userId, String otherUserId, String preview, long timestamp) {
        jdbc.update("UPDATE web_chat_user_relation SET content = ?, timestamp = ?, count = 0 "
                + "WHERE accept_uid = ? AND send_uid = ?", preview, timestamp, userId, otherUserId);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void removeConversation(String userId, String otherUserId) {
        relationMapper.delete(new QueryWrapper<WebChatUserRelation>()
                .eq("accept_uid", userId).eq("send_uid", otherUserId));
    }

    private String resolveUserId(String userId) {
        List<String> userIds = jdbc.queryForList("SELECT id FROM web_user WHERE id = ?", String.class, userId);
        // 已删除用户的历史会话仍允许清理，保留原 ID 供可见性条件使用。
        return userIds.isEmpty() ? userId : userIds.getFirst();
    }

    private WebUser lockUser(String userId) {
        List<WebUser> users = jdbc.query("SELECT id, status FROM web_user WHERE id = ? FOR UPDATE",
                (resultSet, rowNumber) -> {
                    WebUser user = new WebUser();
                    user.setId(resultSet.getString("id"));
                    user.setStatus(resultSet.getString("status"));
                    return user;
                }, userId);
        return users.isEmpty() ? null : users.getFirst();
    }

    private QueryWrapper<WebChat> visibleMessages(String userId, String otherUserId) {
        return new QueryWrapper<WebChat>()
                .and(condition -> condition.eq("send_uid", userId).eq("accept_uid", otherUserId)
                        .eq("send_deleted", 0))
                .or(condition -> condition.eq("send_uid", otherUserId).eq("accept_uid", userId)
                        .eq("accept_deleted", 0))
                .orderByDesc("timestamp", "id");
    }

    private void updateConversation(String senderId, String recipientId, String preview,
                                    long timestamp, boolean unread) {
        List<WebChatUserRelation> relations = relationMapper.selectList(new QueryWrapper<WebChatUserRelation>()
                .eq("send_uid", senderId).eq("accept_uid", recipientId).orderByAsc("id").last("FOR UPDATE"));
        if (relations.isEmpty()) {
            WebChatUserRelation relation = new WebChatUserRelation();
            relation.setSendUid(senderId);
            relation.setAcceptUid(recipientId);
            relation.setCount(unread ? 1 : 0);
            relation.setContent(preview);
            relation.setTimestamp(timestamp);
            relationMapper.insert(relation);
            return;
        }
        // 保留旧环境已存在的重复行；同步其摘要，避免 selectOne 因历史数据中断发送。
        for (WebChatUserRelation relation : relations) {
            relationMapper.updatePreview(relation.getId(), preview, timestamp, unread);
        }
    }

    /**
     * 持有双方行锁时读取的用户状态，应用服务据此复核写入权限。
     */
    public record Participants(String currentUserId, String otherUserId, WebUser currentUser, WebUser otherUser) {
    }
}
