package com.hongshu.common.constant;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author hongshu
 */
@Schema(description = "权限管理常量")
public interface AuthConstant {

    @Schema(description = "accessToken过期时间")
    long ACCESS_TOKEN_EXPIRATION_TIME = 1000 * 60 * 60 * 24;
    @Schema(description = "refreshToken过期时间")
    long REFRESH_TOKEN_EXPIRATION_TIME = ACCESS_TOKEN_EXPIRATION_TIME * 2;
    @Schema(description = "refreshToken保留时间")
    String REFRESH_TOKEN_START_TIME = "refreshTokenStartTime:";
    @Schema(description = "用户key")
    String USER_KEY = "userKey:";
    @Schema(description = "用户信息")
    String USER_INFO = "userInfo";
    @Schema(description = "验证码key")
    String CODE = "code:";
    @Schema(description = "默认头像")
    String DEFAULT_AVATAR = "/default-avatar.png";
    @Schema(description = "默认密码")
    String DEFAULT_PASSWORD = "123456";
    @Schema(description = "登录失败")
    String LOGIN_FAIL = "登录失败";
}
