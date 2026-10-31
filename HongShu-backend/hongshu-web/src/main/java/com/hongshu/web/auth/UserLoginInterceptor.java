package com.hongshu.web.auth;

import org.jspecify.annotations.NonNull;

import com.hongshu.common.constant.TokenConstant;
import com.hongshu.common.enums.ResultCodeEnum;
import com.hongshu.common.exception.web.HongshuException;
import com.hongshu.common.validator.myVaildator.noLogin.NoLoginIntercept;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class UserLoginInterceptor implements HandlerInterceptor {
    private final WebSessionService sessions;

    public UserLoginInterceptor(WebSessionService sessions) {
        this.sessions = sessions;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        AuthContextHolder.removeUserId();
        if (!request.getServletPath().startsWith("/web/") || !(handler instanceof HandlerMethod method)) {
            return true;
        }
        boolean optional = method.hasMethodAnnotation(NoLoginIntercept.class);
        String token = request.getHeader(TokenConstant.ACCESS_TOKEN);
        if (token == null || token.isBlank()) {
            if (optional) {
                return true;
            }
            throw new HongshuException(ResultCodeEnum.TOKEN_FAIL);
        }
        try {
            AuthContextHolder.setUserId(sessions.requireUser(token, "access").getId());
        } catch (HongshuException e) {
            if (!optional) {
                throw e;
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, Exception ex) {
        AuthContextHolder.removeUserId();
    }
}
