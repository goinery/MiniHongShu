package com.hongshu.web.websocket;

import com.hongshu.common.utils.spring.SpringUtils;
import com.hongshu.web.websocket.config.ServerEncoder;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Locale;

/**
 * Jakarta WebSocket 生命周期适配器，连接管理交由 Spring 服务处理。
 */
@ServerEndpoint(value = "/web/ws/{uid}", encoders = {ServerEncoder.class})
@Component
@Slf4j
public class WebSocketServer {

    @OnOpen
    public void onOpen(Session session, @PathParam("uid") String userId) {
        registry().register(userId, session);
    }

    @OnClose
    public void onClose(Session session) {
        registry().remove(session);
    }

    @OnMessage
    public void onMessage(String message) {
        // 客户端通过 HTTP 接口发送业务消息，此入口仅接收连接心跳。
        log.debug("收到 WebSocket 心跳");
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        registry().disconnect(session);
        if (isClientDisconnect(throwable)) {
            log.debug("WebSocket 客户端连接中断", throwable);
        } else {
            log.error("WebSocket 发生错误", throwable);
        }
    }

    private WebSocketSessionRegistry registry() {
        // Jakarta 容器创建端点实例，仅在此适配边界查找 Spring Bean。
        return SpringUtils.getBean(WebSocketSessionRegistry.class);
    }

    private boolean isClientDisconnect(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (current instanceof IOException && message != null) {
                String lower = message.toLowerCase(Locale.ROOT);
                if (lower.contains("connection reset") || lower.contains("broken pipe")
                        || lower.contains("closed") || lower.contains("eof")
                        || lower.contains("connection timed out")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }
}
