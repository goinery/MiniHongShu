package com.hongshu.web.auth;

import org.jspecify.annotations.NonNull;

import com.hongshu.common.constant.AuthConstant;
import com.hongshu.common.constant.TokenConstant;
import com.hongshu.common.enums.ResultCodeEnum;
import com.hongshu.common.exception.web.HongshuException;
import com.hongshu.common.utils.JwtUtils;
import com.hongshu.common.utils.RedisUtils;
import com.hongshu.web.domain.entity.WebUser;
import com.hongshu.web.mapper.WebUserMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 前台令牌签发、校验及撤销；会话资源清理由事件订阅者处理。
 */
@Service
public class WebSessionService implements ApplicationEventPublisherAware {

    private static final String SESSION_KEY_PREFIX = "web:session:";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";
    private static final String ENABLED_STATUS = "0";
    private static final String USER_ID_CLAIM = "userId";
    private static final String SESSION_ID_CLAIM = "sessionId";
    private static final String TOKEN_TYPE_CLAIM = "tokenType";

    private final RedisUtils redis;
    private final WebUserMapper users;
    private ApplicationEventPublisher eventPublisher;

    public WebSessionService(RedisUtils redis, WebUserMapper users) {
        this.redis = redis;
        this.users = users;
    }

    @Override
    public void setApplicationEventPublisher(@NonNull ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher");
    }

    public void requireEnabled(WebUser user) {
        if (user == null || (user.getStatus() != null && !user.getStatus().isBlank()
                && !ENABLED_STATUS.equals(user.getStatus()))) {
            throw new HongshuException("账号不可用，请联系管理员", 403);
        }
    }

    public WebUser requireUser(String token, String type) {
        return authenticate(token, type).user();
    }

    public Map<String, Object> login(WebUser user) {
        requireEnabled(user);
        ApplicationEventPublisher publisher = requireEventPublisher();
        String sessionId = UUID.randomUUID().toString();
        String accessToken = JwtUtils.getJwtToken(user.getId(), AuthConstant.ACCESS_TOKEN_EXPIRATION_TIME,
                sessionId, ACCESS_TOKEN_TYPE);
        String refreshToken = JwtUtils.getJwtToken(user.getId(), AuthConstant.REFRESH_TOKEN_EXPIRATION_TIME,
                sessionId, REFRESH_TOKEN_TYPE);
        redis.setEx(SESSION_KEY_PREFIX + user.getId(), sessionId,
                AuthConstant.REFRESH_TOKEN_EXPIRATION_TIME, TimeUnit.MILLISECONDS);
        publisher.publishEvent(new WebSessionInvalidatedEvent(user.getId()));
        return Map.of(TokenConstant.ACCESS_TOKEN, accessToken, TokenConstant.REFRESH_TOKEN, refreshToken,
                AuthConstant.USER_INFO, WebUserViews.selfProfile(user));
    }

    public Map<String, Object> refresh(String refreshToken) {
        AuthenticatedSession session = authenticate(refreshToken, REFRESH_TOKEN_TYPE);
        Claims claims = session.claims();
        long remaining = claims.getExpiration().getTime() - System.currentTimeMillis();
        if (remaining <= 0) {
            throw new HongshuException(ResultCodeEnum.TOKEN_FAIL);
        }
        String accessToken = JwtUtils.getJwtToken(session.user().getId(),
                Math.min(AuthConstant.ACCESS_TOKEN_EXPIRATION_TIME, remaining),
                claims.get(SESSION_ID_CLAIM, String.class), ACCESS_TOKEN_TYPE);
        return Map.of(TokenConstant.ACCESS_TOKEN, accessToken, TokenConstant.REFRESH_TOKEN, refreshToken);
    }

    public void revoke(String userId) {
        ApplicationEventPublisher publisher = requireEventPublisher();
        redis.delete(SESSION_KEY_PREFIX + userId);
        redis.delete(AuthConstant.USER_KEY + userId);
        redis.delete(AuthConstant.REFRESH_TOKEN_START_TIME + userId);
        publisher.publishEvent(new WebSessionInvalidatedEvent(userId));
    }

    private ApplicationEventPublisher requireEventPublisher() {
        return Objects.requireNonNull(eventPublisher, "会话签发和撤销需通过已初始化的 Spring 服务执行");
    }

    private AuthenticatedSession authenticate(String token, String type) {
        Claims claims = parseClaims(token, type);
        String userId = claims.get(USER_ID_CLAIM, String.class);
        String sessionId = claims.get(SESSION_ID_CLAIM, String.class);
        if (userId == null || sessionId == null || type == null
                || !type.equals(claims.get(TOKEN_TYPE_CLAIM, String.class))
                || !sessionId.equals(redis.get(SESSION_KEY_PREFIX + userId))) {
            throw new HongshuException(ResultCodeEnum.TOKEN_FAIL);
        }
        WebUser user = users.selectById(userId);
        requireEnabled(user);
        return new AuthenticatedSession(user, claims);
    }

    private Claims parseClaims(String token, String type) {
        try {
            return JwtUtils.getClaims(token);
        } catch (ExpiredJwtException exception) {
            throw new HongshuException(ACCESS_TOKEN_TYPE.equals(type)
                    ? ResultCodeEnum.TOKEN_EXIST : ResultCodeEnum.TOKEN_FAIL);
        } catch (JwtException | IllegalArgumentException exception) {
            throw new HongshuException(ResultCodeEnum.TOKEN_FAIL);
        }
    }

    private record AuthenticatedSession(WebUser user, Claims claims) {
    }
}
