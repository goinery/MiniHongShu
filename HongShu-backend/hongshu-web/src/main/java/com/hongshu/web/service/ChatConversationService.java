package com.hongshu.web.service;

import com.hongshu.common.utils.ConvertUtils;
import com.hongshu.web.domain.entity.WebChatUserRelation;
import com.hongshu.web.domain.entity.WebUser;
import com.hongshu.web.domain.vo.ChatUserRelationVO;
import com.hongshu.web.mapper.WebUserMapper;
import com.hongshu.web.repository.ChatRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 组装聊天会话查询视图，供 HTTP 和实时投递复用，不负责持久化或发送消息。
 */
@Service
public class ChatConversationService {

    private final ChatRepository chats;
    private final WebUserMapper userMapper;

    public ChatConversationService(ChatRepository chats, WebUserMapper userMapper) {
        this.chats = chats;
        this.userMapper = userMapper;
    }

    public List<ChatUserRelationVO> listForUser(String userId) {
        List<WebChatUserRelation> relations = chats.findConversations(userId);
        if (relations.isEmpty()) {
            return new ArrayList<>();
        }
        Set<String> userIds = relations.stream().map(WebChatUserRelation::getSendUid).collect(Collectors.toSet());
        Map<String, WebUser> users = userMapper.selectByIds(userIds).stream()
                .collect(Collectors.toMap(WebUser::getId, user -> user));
        List<ChatUserRelationVO> result = new ArrayList<>(relations.size());
        for (WebChatUserRelation relation : relations) {
            WebUser user = users.get(relation.getSendUid());
            if (user == null) {
                continue;
            }
            ChatUserRelationVO view = ConvertUtils.sourceToTarget(relation, ChatUserRelationVO.class);
            view.setUid(user.getId());
            view.setUsername(user.getUsername());
            view.setAvatar(user.getAvatar());
            result.add(view);
        }
        return result;
    }
}
