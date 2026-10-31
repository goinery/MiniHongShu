package com.hongshu.framework.manager.factory;

import com.hongshu.common.constant.Constants;
import com.hongshu.common.utils.spring.SpringUtils;
import com.hongshu.system.domain.SysLogininfor;
import com.hongshu.system.service.ISysLogininforService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.TimerTask;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

/**
 * 退出发生在安全过滤器阶段，不能依赖 MVC 绑定的请求上下文。
 */
class AsyncFactoryTest {

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void logoutLogCapturesRequestBeforeAsyncExecutionWithoutThreadLocal() {
        RequestContextHolder.resetRequestAttributes();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.20.30.40");
        request.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0");

        TimerTask task = assertDoesNotThrow(() -> AsyncFactory.recordLogininfor(
                request, "test-admin", Constants.LOGOUT, "退出成功"));
        // 请求稍后即使变化或销毁，日志也使用创建任务时捕获的值。
        request.setRemoteAddr("10.99.99.99");
        request.removeHeader("User-Agent");
        ISysLogininforService service = mock(ISysLogininforService.class);
        try (MockedStatic<SpringUtils> spring = mockStatic(SpringUtils.class)) {
            spring.when(() -> SpringUtils.getBean(ISysLogininforService.class)).thenReturn(service);
            assertDoesNotThrow(task::run);
        }
        ArgumentCaptor<SysLogininfor> captor = ArgumentCaptor.forClass(SysLogininfor.class);
        verify(service).insertLogininfor(captor.capture());
        assertEquals("10.20.30.40", captor.getValue().getIpaddr());
        assertEquals("test-admin", captor.getValue().getUserName());
        assertEquals(Constants.SUCCESS, captor.getValue().getStatus());
        assertEquals("退出成功", captor.getValue().getMsg());
        assertNull(RequestContextHolder.getRequestAttributes());
    }

    @Test
    void existingLoginCallStillCapturesTheMvcRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.1.2.3");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        TimerTask task = AsyncFactory.recordLogininfor("test-user", Constants.LOGIN_SUCCESS, "登录成功");
        RequestContextHolder.resetRequestAttributes();

        ISysLogininforService service = mock(ISysLogininforService.class);
        try (MockedStatic<SpringUtils> spring = mockStatic(SpringUtils.class)) {
            spring.when(() -> SpringUtils.getBean(ISysLogininforService.class)).thenReturn(service);
            assertDoesNotThrow(task::run);
        }
        ArgumentCaptor<SysLogininfor> captor = ArgumentCaptor.forClass(SysLogininfor.class);
        verify(service).insertLogininfor(captor.capture());
        assertEquals("10.1.2.3", captor.getValue().getIpaddr());
        assertEquals("test-user", captor.getValue().getUserName());
    }
}
