package com.hongshu.web.websocket;

import com.hongshu.common.exception.web.HongshuException;
import com.hongshu.web.auth.WebSessionInvalidatedEvent;
import com.hongshu.web.auth.WebSessionService;
import com.hongshu.web.websocket.im.Message;
import jakarta.websocket.EncodeException;
import jakarta.websocket.Session;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 管理本机 WebSocket 连接及消息投递，不承担聊天记录和通知计数的业务逻辑。
 */
@Slf4j
@Component
public class WebSocketSessionRegistry {

    private static final String ACCESS_TOKEN_PROPERTY = "accessToken";
    private static final String USER_ID_PROPERTY = "userId";
    private static final String ACCESS_TOKEN_TYPE = "access";

    private final ConcurrentMap<String, Session> sessions = new ConcurrentHashMap<>();
    private final WebSessionService webSessionService;

    public WebSocketSessionRegistry(WebSessionService webSessionService) {
        this.webSessionService = webSessionService;
    }

    /**
     * 校验握手令牌并替换同一用户的旧连接。
     */
    public void register(String userId, Session session) {
        String token = firstParameter(session.getRequestParameterMap(), "token");
        if (StringUtils.isBlank(token)) {
            token = firstParameter(session.getRequestParameterMap(), ACCESS_TOKEN_PROPERTY);
        }
        try {
            if (StringUtils.isBlank(userId)
                    || !userId.equals(webSessionService.requireUser(token, ACCESS_TOKEN_TYPE).getId())) {
                closeSession(session);
                return;
            }
        } catch (HongshuException exception) {
            closeSession(session);
            return;
        } catch (RuntimeException exception) {
            closeSession(session);
            log.error("WebSocket 握手校验失败: userId={}", userId, exception);
            return;
        }
        session.getUserProperties().put(ACCESS_TOKEN_PROPERTY, token);
        session.getUserProperties().put(USER_ID_PROPERTY, userId);
        Session previous = sessions.put(userId, session);
        if (previous != null && previous != session) {
            closeSession(previous);
        }
        log.debug("WebSocket 用户连接: userId={}, online={}", userId, sessions.size());
    }

    /**
     * 只移除指定连接，旧连接的关闭回调不会清除新连接。
     */
    public void remove(Session session) {
        if (session == null) {
            return;
        }
        Object userId = session.getUserProperties().get(USER_ID_PROPERTY);
        if (userId instanceof String id) {
            sessions.remove(id, session);
        }
    }

    public void sendInfo(Message message) {
        if (message == null || StringUtils.isBlank(message.getAcceptUid())) {
            return;
        }
        Session session = sessions.get(message.getAcceptUid());
        if (session == null) {
            return;
        }
        // BasicRemote 不允许同一连接并发发送；锁定连接可避免阻塞其他用户。
        synchronized (session) {
            if (!session.isOpen()) {
                remove(session);
                return;
            }
            try {
                String token = (String) session.getUserProperties().get(ACCESS_TOKEN_PROPERTY);
                webSessionService.requireUser(token, ACCESS_TOKEN_TYPE);
                session.getBasicRemote().sendObject(message);
            } catch (HongshuException exception) {
                remove(session);
                closeSession(session);
            } catch (IOException | EncodeException | RuntimeException exception) {
                remove(session);
                closeSession(session);
                log.warn("WebSocket 消息投递失败: userId={}", message.getAcceptUid(), exception);
            }
        }
    }

    public void closeUser(String userId) {
        if (StringUtils.isBlank(userId)) {
            return;
        }
        Session session = sessions.remove(userId);
        if (session != null) {
            closeSession(session);
        }
    }

    public void disconnect(Session session) {
        if (session == null) {
            return;
        }
        remove(session);
        closeSession(session);
    }

    @EventListener
    public void onSessionInvalidated(WebSessionInvalidatedEvent event) {
        closeUser(event.userId());
    }

    private String firstParameter(Map<String, List<String>> parameters, String name) {
        if (parameters == null) {
            return null;
        }
        List<String> values = parameters.get(name);
        return values == null || values.isEmpty() ? null : values.getFirst();
    }

    private void closeSession(Session session) {
        try {
            if (session.isOpen()) {
                session.close();
            }
        } catch (IOException | IllegalStateException exception) {
            log.debug("关闭 WebSocket 连接失败: sessionId={}", session.getId(), exception);
        }
    }
}
