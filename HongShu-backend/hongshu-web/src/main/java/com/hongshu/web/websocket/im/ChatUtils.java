package com.hongshu.web.websocket.im;

import com.hongshu.web.service.ChatNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 社交业务的通知入口，只有业务事务提交后才增加计数和推送。
 */
@Slf4j
@Component
public class ChatUtils {

    private final ChatNotificationService notifications;

    public ChatUtils(ChatNotificationService notifications) {
        this.notifications = notifications;
    }

    /**
     * @param userId 接收通知的用户 ID
     * @param type 通知类型：0 点赞或收藏，1 评论，2 关注
     */
    public void sendMessage(String userId, Integer type) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deliver(userId, type);
                }
            });
            return;
        }
        deliver(userId, type);
    }

    private void deliver(String userId, Integer type) {
        try {
            notifications.incrementAndPush(userId, type);
        } catch (RuntimeException exception) {
            // 通知失败不能把已经提交的点赞、评论或关注呈现为业务失败。
            log.error("社交通知发送失败: userId={}, type={}", userId, type, exception);
        }
    }
}
