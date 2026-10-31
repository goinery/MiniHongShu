package com.hongshu.framework.web.exception;

import com.hongshu.common.enums.Result;
import com.hongshu.common.exception.web.HongshuException;
import com.hongshu.common.utils.JwtUtils;
import com.hongshu.common.utils.RedisUtils;
import com.hongshu.common.validator.myVaildator.noLogin.NoLoginIntercept;
import com.hongshu.web.auth.UserLoginInterceptor;
import com.hongshu.web.auth.WebSessionService;
import com.hongshu.web.domain.entity.WebUser;
import com.hongshu.web.mapper.WebUserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static com.hongshu.framework.web.exception.JsonResponseMatchers.jsonField;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 验证真实 JWT 校验、登录拦截器和异常响应之间的契约。 */
class WebSessionExceptionTest {
    private final Object originalSecret = ReflectionTestUtils.getField(JwtUtils.class, "appSecret");
    private RedisUtils redis;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        JwtUtils.setAppSecret(Base64.getEncoder().encodeToString(
                "session-regression-only-signing-secret".getBytes(StandardCharsets.UTF_8)));
        redis = mock(RedisUtils.class);
        WebUserMapper users = mock(WebUserMapper.class);
        WebUser user = new WebUser();
        user.setId("42");
        user.setStatus("0");
        when(users.selectById("42")).thenReturn(user);
        when(redis.get("web:session:42")).thenReturn("session-test");
        WebSessionService sessions = new WebSessionService(redis, users);
        mvc = MockMvcBuilders.standaloneSetup(new SessionController())
                .addInterceptors(new UserLoginInterceptor(sessions))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @AfterEach
    void tearDown() {
        ReflectionTestUtils.setField(JwtUtils.class, "appSecret", originalSecret);
    }

    @Test
    void invalidTokenReturns401InsteadOfGeneric500() throws Exception {
        mvc.perform(get("/web/session").servletPath("/web/session").header("accessToken", "invalid"))
                .andExpect(status().isOk()).andExpect(jsonField("code", 401))
                .andExpect(jsonField("message", "token异常"));
    }

    @Test
    void missingTokenReturns401() throws Exception {
        mvc.perform(get("/web/session").servletPath("/web/session"))
                .andExpect(jsonField("code", 401));
    }

    @Test
    void lostRedisSessionAfterRestartReturns401() throws Exception {
        when(redis.get("web:session:42")).thenReturn(null);
        mvc.perform(get("/web/session").servletPath("/web/session")
                        .header("accessToken", token(60000)))
                .andExpect(jsonField("code", 401));
    }

    @Test
    void expiredAccessTokenKeeps501SoClientCanRefresh() throws Exception {
        mvc.perform(get("/web/session").servletPath("/web/session")
                        .header("accessToken", token(-60000)))
                .andExpect(jsonField("code", 501));
    }

    @Test
    void validPersistedSessionSurvivesApplicationRestart() throws Exception {
        mvc.perform(get("/web/session").servletPath("/web/session")
                        .header("accessToken", token(60000)))
                .andExpect(jsonField("code", 200));
    }

    @Test
    void publicBrowsingRemainsAvailableWithInvalidToken() throws Exception {
        mvc.perform(get("/web/public").servletPath("/web/public").header("accessToken", "invalid"))
                .andExpect(jsonField("code", 200));
    }

    @Test
    void ordinaryWebBusinessErrorRetainsItsOwnCode() throws Exception {
        mvc.perform(get("/web/business-error").servletPath("/web/business-error")
                        .header("accessToken", token(60000)))
                .andExpect(jsonField("code", 409));
    }

    private String token(long lifetime) {
        return JwtUtils.getJwtToken("42", lifetime, "session-test", "access");
    }

    @RestController
    static class SessionController {
        @GetMapping("/web/session")
        public Result<?> session() { return Result.ok(); }

        @NoLoginIntercept
        @GetMapping("/web/public")
        public Result<?> publicPage() { return Result.ok(); }

        @GetMapping("/web/business-error")
        public Result<?> businessError() { throw new HongshuException("业务冲突", 409); }
    }
}
