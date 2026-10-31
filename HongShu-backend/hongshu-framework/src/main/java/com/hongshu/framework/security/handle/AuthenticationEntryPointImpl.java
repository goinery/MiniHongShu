package com.hongshu.framework.security.handle;

import java.io.Serial;

import org.jspecify.annotations.NonNull;

import com.alibaba.fastjson2.JSON;
import com.hongshu.common.constant.HttpStatus;
import com.hongshu.common.core.domain.AjaxResult;
import com.hongshu.common.utils.ServletUtils;
import com.hongshu.common.utils.StringUtils;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.Serializable;

/**
 * 认证失败处理类 返回未授权
 *
 * @author hongshu
 */
@Component
public class AuthenticationEntryPointImpl implements AuthenticationEntryPoint, Serializable {

    @Serial
    private static final long serialVersionUID = -8970718410437077606L;

    @Override
    public void commence(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull AuthenticationException e)
            {
        int code = HttpStatus.UNAUTHORIZED;
        String msg = StringUtils.format("请求访问：{}，认证失败，无法访问系统资源", request.getRequestURI());
        ServletUtils.renderString(response, JSON.toJSONString(AjaxResult.error(code, msg)));
    }
}
