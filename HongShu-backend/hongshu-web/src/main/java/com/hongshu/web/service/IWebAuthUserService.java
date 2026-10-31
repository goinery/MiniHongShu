package com.hongshu.web.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.hongshu.web.domain.dto.AuthUserDTO;
import com.hongshu.web.domain.entity.WebUser;

import java.util.Map;

/**
 * 权限
 *
 * @author hongshu
 */
public interface IWebAuthUserService extends IService<WebUser> {

    /**
     * 用户登录
     *
     * @param authUserDTO 用户
     */
    Map<String, Object> login(AuthUserDTO authUserDTO);

    /**
     * 验证码登录
     *
     * @param authUserDTO 用户
     */
    Map<String, Object> loginByCode(AuthUserDTO authUserDTO);

    /**
     * 根据token获取当前用户
     *
     * @param accessToken accessToken
     */
    WebUser getUserInfoByToken(String accessToken);

    /**
     * 退出登录
     *
     * @param userId 用户ID
     */
    void loginOut(String userId);

    /**
     * 刷新token
     *
     * @param refreshToken refreshToken
     */
    Map<String, Object> refreshToken(String refreshToken);
}
