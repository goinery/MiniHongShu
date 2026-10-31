package com.hongshu.web.domain.dto;

import com.hongshu.common.validator.group.DefaultGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Pattern;
import java.io.Serializable;

/**
 * 权限用户
 *
 * @author hongshu
 */
@Schema(description = "前台账号登录参数")
@Data
public class AuthUserDTO implements Serializable {

    @Schema(description = "兼容用户 ID 字段，登录身份由后端根据账号确定")
    private String id;
    @Schema(description = "用户名")
    private String username;

    @Pattern(regexp = "^(?![0-9]+$)(?![a-zA-Z]+$)[0-9A-Za-z]{8,20}$", message = "请输入8-20位由字母和数字组成的密码", groups = DefaultGroup.class)
    @Schema(description = "手机号密码登录所需密码")
    private String password;

    @Pattern(regexp = "^1[0-9]{10}$", message = "手机号格式有误", groups = DefaultGroup.class)
    @Schema(description = "11 位手机号，密码登录必填；首次登录自动注册")
    private String phone;

    //@Pattern(regexp = "^([a-zA-Z0-9._-])+@([a-zA-Z0-9_-])+((.[a-zA-Z0-9_-]{2,3}){1,2})$", message = "邮箱输入有误", groups = DefaultGroup.class)
    @Schema(description = "邮箱账号，仅验证码兼容登录使用")
    private String email;

    @Schema(description = "预先保存在 Redis 的登录验证码；当前没有发送验证码接口")
    private String code;

}
