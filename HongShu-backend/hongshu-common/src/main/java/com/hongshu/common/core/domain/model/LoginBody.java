package com.hongshu.common.core.domain.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 用户登录对象
 *
 * @author hongshu
 */
@Schema(description = "后台登录参数")
public class LoginBody {

    /**
     * 用户名
     */
    @Getter
    @Setter
    @Schema(description = "用户名")
    private String username;

    /**
     * 用户密码
     */
    @Getter
    @Setter
    @Schema(description = "用户密码")
    private String password;

    /**
     * 验证码
     */
    @Getter
    @Setter
    @Schema(description = "图形或算术验证码答案；仅启用验证码时需要")
    private String code;

    /**
     * 唯一标识
     */
    @Getter
    @Setter
    @Schema(description = "获取图形验证码时返回的 UUID；启用验证码时与 code 一起提交")
    private String uuid;

}
