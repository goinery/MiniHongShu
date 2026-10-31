package com.hongshu.framework.security.handle;

import org.jspecify.annotations.NonNull;

import com.alibaba.fastjson2.JSON;
import com.hongshu.common.constant.Constants;
import com.hongshu.common.core.domain.AjaxResult;
import com.hongshu.common.core.domain.model.LoginUser;
import com.hongshu.common.utils.MessageUtils;
import com.hongshu.common.utils.ServletUtils;
import com.hongshu.common.utils.StringUtils;
import com.hongshu.framework.manager.AsyncManager;
import com.hongshu.framework.manager.factory.AsyncFactory;
import com.hongshu.framework.web.service.TokenService;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


/**
 * 自定义退出处理类 返回成功
 *
 * @author hongshu
 */
@Configuration
public class LogoutSuccessHandlerImpl implements LogoutSuccessHandler {

    private final TokenService tokenService;

    public LogoutSuccessHandlerImpl(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    /**
     * 退出处理
     *
     */
    @Override
    public void onLogoutSuccess(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, Authentication authentication)
            {
        LoginUser loginUser = tokenService.getLoginUser(request);
        if (StringUtils.isNotNull(loginUser)) {
            String userName = loginUser.getUsername();
            // 删除用户缓存记录
            tokenService.delLoginUser(loginUser.getToken());
            // 记录用户退出日志
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(request, userName, Constants.LOGOUT,
                    MessageUtils.message("user.logout.success")));
        }
        ServletUtils.renderString(response, JSON.toJSONString(AjaxResult.success(MessageUtils.message("user.logout.success"))));
    }
}
