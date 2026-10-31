package com.hongshu.server.controller.web;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Parameter;
import com.hongshu.common.enums.Result;
import com.hongshu.common.enums.ResultCodeEnum;
import com.hongshu.common.utils.JwtUtils;
import com.hongshu.common.validator.myVaildator.noLogin.NoLoginIntercept;
import com.hongshu.web.auth.AuthContextHolder;
import com.hongshu.web.domain.dto.AuthUserDTO;
import com.hongshu.web.service.IWebAuthUserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户权限
 *
 * @author hongshu
 */
@Tag(name = "前台·账号认证", description = "手机号登录注册、验证码兼容登录、会话退出与双令牌刷新")
@RestController
@RequestMapping("/web/auth")
@Slf4j
public class WebAuthController {

    private final IWebAuthUserService authUserService;

    public WebAuthController(IWebAuthUserService authUserService) {
        this.authUserService = authUserService;
    }

    /**
     * 用户登录
     *
     * @param authUserDTO 用户
     */
    @Operation(summary = "手机号密码登录或注册",
            description = "手机号首次登录时自动创建会员；已有账号验证密码，兼容旧密码升级 BCrypt，返回 accessToken、refreshToken 和本人资料（不含密码）。无需登录；携带有效令牌时识别当前用户。",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "请求数据，字段含义见下方模型；具体取值与操作约束见接口说明"))
    @PostMapping("login")
    @NoLoginIntercept
    public Result<?> login(@RequestBody AuthUserDTO authUserDTO) {
        Map<String, Object> map = authUserService.login(authUserDTO);
        return Result.ok(map);
    }

    /**
     * 验证码登录
     *
     * @param authUserDTO 用户
     */
    @Operation(summary = "验证码登录兼容入口",
            description = "验证 Redis 中已存在的手机号或邮箱验证码并登录已有账号；当前未提供完整验证码发送链路，不会在此接口发送验证码。 无需登录；公开接口携带有效令牌时可识别当前用户。",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "请求数据，字段含义见下方模型；具体取值与操作约束见接口说明"))
    @PostMapping("loginByCode")
    @NoLoginIntercept
    public Result<?> loginByCode(@RequestBody AuthUserDTO authUserDTO) {
        Map<String, Object> map = authUserService.loginByCode(authUserDTO);
        return Result.ok(map);
    }

    /**
     * 根据token获取当前用户
     *
     * @param accessToken accessToken
     */
    @Operation(summary = "获取当前登录会员资料",
            description = "校验 accessToken 对应的有效会话，返回本人资料，含手机号、邮箱和登录信息，始终隐藏密码及内部备注。需携带前台 accessToken。",
            security = @SecurityRequirement(name = "appAccessToken"))
    @GetMapping("getUserInfoByToken")
    public Result<?> getUserInfoByToken(@Parameter(description = "前台登录返回的访问令牌，通过同名请求头发送") @RequestHeader("accessToken") String accessToken) {
        return Result.ok(authUserService.getUserInfoByToken(accessToken));
    }

    /**
     * 退出登录
     *
     * @param userId 用户ID
     */
    @Operation(summary = "退出当前会员会话",
            description = "撤销当前认证用户的 Redis 会话并关闭其本实例 WebSocket；userId 为兼容参数，不用于决定退出哪个账号。 需携带前台 accessToken。",
            security = @SecurityRequirement(name = "appAccessToken"))
    @GetMapping("loginOut")
    public Result<?> loginOut(@Parameter(description = "兼容参数，可不传；服务端始终退出当前认证会员") String userId) {
        authUserService.loginOut(AuthContextHolder.getUserId());
        return Result.ok();
    }

    /**
     * 刷新token
     *
     * @param refreshToken refreshToken
     */
    @Operation(summary = "刷新前台访问令牌",
            description = "使用有效 refreshToken 签发新的 accessToken，保留原 refreshToken；不会延长原刷新令牌的到期时间。 无需登录；公开接口携带有效令牌时可识别当前用户。")
    @GetMapping("refreshToken")
    @NoLoginIntercept
    public Result<?> refreshToken(@Parameter(description = "前台登录返回的刷新令牌") String refreshToken) {
        boolean checkToken = JwtUtils.checkToken(refreshToken);
        if (!checkToken) {
            //refreshToken过期，跳转登录界面
            return Result.build(ResultCodeEnum.TOKEN_FAIL.getCode(), ResultCodeEnum.TOKEN_FAIL.getMessage());
        }
        Map<String, Object> map = authUserService.refreshToken(refreshToken);
        return Result.ok(map);
    }
}
