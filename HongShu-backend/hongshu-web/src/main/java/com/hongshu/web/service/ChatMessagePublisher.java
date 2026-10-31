package com.hongshu.web.service;

import com.hongshu.web.websocket.WebSocketSessionRegistry;
import com.hongshu.web.websocket.im.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 聊天实时投递入口，业务提交后查询会话视图并分别投递。
 */
@Slf4j
@Component
public class ChatMessagePublisher {

    private static final int CONVERSATION_MESSAGE_TYPE = 5;

    private final WebSocketSessionRegistry connections;
    private final ChatConversationService conversations;

    public ChatMessagePublisher(WebSocketSessionRegistry connections, ChatConversationService conversations) {
        this.connections = connections;
        this.conversations = conversations;
    }

    public void publishAfterCommit(Message message) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                sendMessage(message);
                sendConversation(message.getSendUid());
                if (!message.getSendUid().equals(message.getAcceptUid())) {
                    sendConversation(message.getAcceptUid());
                }
            }
        });
    }

    public void closeConnection(String userId) {
        connections.closeUser(userId);
    }

    private void sendMessage(Message message) {
        try {
            connections.sendInfo(message);
        } catch (RuntimeException exception) {
            log.error("聊天消息推送失败: userId={}", message.getAcceptUid(), exception);
        }
    }

    private void sendConversation(String userId) {
        try {
            Message message = new Message();
            message.setAcceptUid(userId);
            message.setContent(conversations.listForUser(userId));
            message.setMsgType(CONVERSATION_MESSAGE_TYPE);
            connections.sendInfo(message);
        } catch (RuntimeException exception) {
            log.error("聊天会话推送失败: userId={}", userId, exception);
        }
    }
}
