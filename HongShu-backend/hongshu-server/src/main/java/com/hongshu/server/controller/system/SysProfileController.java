package com.hongshu.server.controller.system;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import com.hongshu.common.annotation.Log;
import com.hongshu.common.core.controller.BaseController;
import com.hongshu.common.core.domain.AjaxResult;
import com.hongshu.common.core.domain.entity.SysUser;
import com.hongshu.common.core.domain.model.LoginUser;
import com.hongshu.common.enums.BusinessType;
import com.hongshu.common.utils.SecurityUtils;
import com.hongshu.common.utils.StringUtils;
import com.hongshu.web.service.IWebOssService;
import com.hongshu.web.service.FileAssetService;
import com.hongshu.common.exception.web.HongshuException;
import com.hongshu.framework.web.service.TokenService;
import com.hongshu.system.service.ISysUserService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * 个人信息 业务处理
 *
 * @author hongshu
 */
@Tag(name = "系统·个人中心", description = "当前管理员资料、密码与头像维护")
@RestController
@RequestMapping("/system/user/profile")
public class SysProfileController extends BaseController {

    private final IWebOssService ossService;
    private final FileAssetService assets;
    private final JdbcTemplate jdbc;

    private final ISysUserService userService;
    private final TokenService tokenService;

    public SysProfileController(IWebOssService ossService, FileAssetService assets, JdbcTemplate jdbc, ISysUserService userService, TokenService tokenService) {
        this.ossService = ossService;
        this.assets = assets;
        this.jdbc = jdbc;
        this.userService = userService;
        this.tokenService = tokenService;
    }

    /**
     * 个人信息
     */
    @Operation(summary = "查看当前管理员个人资料",
            description = "返回当前登录用户资料以及角色组、岗位组名称。 需携带管理端 Bearer JWT。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @GetMapping
    public AjaxResult profile() {
        LoginUser loginUser = getLoginUser();
        SysUser user = loginUser.getUser();
        AjaxResult ajax = AjaxResult.success(user);
        ajax.put("roleGroup", userService.selectUserRoleGroup(loginUser.getUsername()));
        ajax.put("postGroup", userService.selectUserPostGroup(loginUser.getUsername()));
        return ajax;
    }

    /**
     * 修改用户
     */
    @Operation(summary = "修改当前管理员个人资料",
            description = "修改昵称、邮箱、手机号和性别，校验联系方式唯一性并刷新登录缓存。 需携带管理端 Bearer JWT。",
            security = @SecurityRequirement(name = "adminBearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "请求数据，字段含义见下方模型；具体取值与操作约束见接口说明"))
    @Log(title = "个人信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult updateProfile(@RequestBody SysUser user) {
        LoginUser loginUser = getLoginUser();
        SysUser currentUser = loginUser.getUser();
        currentUser.setNickName(user.getNickName());
        currentUser.setEmail(user.getEmail());
        currentUser.setPhonenumber(user.getPhonenumber());
        currentUser.setSex(user.getSex());
        if (StringUtils.isNotEmpty(user.getPhonenumber()) && !userService.checkPhoneUnique(currentUser)) {
            return error("修改用户'" + loginUser.getUsername() + "'失败，手机号码已存在");
        }
        if (StringUtils.isNotEmpty(user.getEmail()) && !userService.checkEmailUnique(currentUser)) {
            return error("修改用户'" + loginUser.getUsername() + "'失败，邮箱账号已存在");
        }
        if (userService.updateUserProfile(currentUser) > 0) {
            // 更新缓存用户信息
            tokenService.setLoginUser(loginUser);
            return success();
        }
        return error("修改个人信息异常，请联系管理员");
    }

    /**
     * 重置密码
     */
    @Operation(summary = "修改当前管理员密码",
            description = "验证旧密码且要求新旧密码不同；使用 BCrypt 更新密码并刷新当前会话缓存。 需携带管理端 Bearer JWT。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @Log(title = "个人信息", businessType = BusinessType.UPDATE)
    @PutMapping("/updatePwd")
    public AjaxResult updatePwd(
            @Parameter(description = "当前账号的原密码") String oldPassword,
            @Parameter(description = "准备设置的新密码，不能与原密码相同") String newPassword
    ) {
        LoginUser loginUser = getLoginUser();
        String userName = loginUser.getUsername();
        String password = loginUser.getPassword();
        if (!SecurityUtils.matchesPassword(oldPassword, password)) {
            return error("修改密码失败，旧密码错误");
        }
        if (SecurityUtils.matchesPassword(newPassword, password)) {
            return error("新密码不能与旧密码相同");
        }
        newPassword = SecurityUtils.encryptPassword(newPassword);
        if (userService.resetUserPwd(userName, newPassword) > 0) {
            // 更新缓存用户密码
            loginUser.getUser().setPassword(newPassword);
            tokenService.setLoginUser(loginUser);
            return success();
        }
        return error("修改密码异常，请联系管理员");
    }

    /**
     * 头像上传
     */
    @Operation(summary = "上传当前管理员头像",
            description = "以 multipart/form-data 提交 avatarfile 图片，保存新头像并在事务提交后检查旧头像是否需要回收。 需携带管理端 Bearer JWT。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @Log(title = "用户头像", businessType = BusinessType.UPDATE)
    @PostMapping("/avatar")
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult avatar(@Parameter(description = "上传文件，使用 multipart/form-data 提交；原始字段名以接口参数名为准") @RequestParam("avatarfile") MultipartFile file) {
        if (!file.isEmpty()) {
            LoginUser loginUser = getLoginUser();
            List<String> previous = jdbc.queryForList("SELECT avatar FROM sys_user WHERE user_id = ? FOR UPDATE", String.class, loginUser.getUserId());
            if (previous.isEmpty()) throw new HongshuException("用户不存在");
            String avatar = ossService.save(file);
            if (userService.updateUserAvatar(loginUser.getUsername(), avatar)) {
                cleanPreviousAvatar(previous.getFirst());
                AjaxResult ajax = AjaxResult.success();
                ajax.put("imgUrl", avatar);
                // 更新缓存用户头像
                loginUser.getUser().setAvatar(avatar);
                tokenService.setLoginUser(loginUser);
                return ajax;
            }
            throw new HongshuException("头像保存失败，请重试");
        }
        return error("上传图片异常，请联系管理员");
    }

    /**
     * 保存当前用户的外链头像，仅保存地址，不在服务端请求远程图片。
     */
    @Operation(summary = "设置当前管理员外链头像",
            description = "保存有效 HTTP 或 HTTPS 图片地址，最长 512 字符；后端不请求远程图片，提交后检查旧受管头像引用。 需携带管理端 Bearer JWT。",
            security = @SecurityRequirement(name = "adminBearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "请求数据，字段含义见下方模型；具体取值与操作约束见接口说明"))
    @Log(title = "用户头像", businessType = BusinessType.UPDATE)
    @PutMapping("/avatar")
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult avatarUrl(@RequestBody AvatarUrlRequest request) {
        String avatar = request == null || request.avatarUrl() == null ? "" : request.avatarUrl().trim();
        if (avatar.isEmpty() || avatar.length() > 512) {
            return error("头像地址不能为空且不能超过 512 个字符");
        }
        try {
            URI uri = new URI(avatar);
            if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null || uri.getHost().isBlank() || uri.getUserInfo() != null
                    || uri.getPort() == 0 || uri.getPort() > 65535) {
                return error("请输入有效的 HTTP 或 HTTPS 图片地址，且不能包含用户名或密码");
            }
        } catch (URISyntaxException exception) {
            return error("请输入有效的 HTTP 或 HTTPS 图片地址");
        }
        LoginUser loginUser = getLoginUser();
        List<String> previous = jdbc.queryForList("SELECT avatar FROM sys_user WHERE user_id = ? FOR UPDATE",
                String.class, loginUser.getUserId());
        if (previous.isEmpty()) throw new HongshuException("用户不存在");
        if (!userService.updateUserAvatar(loginUser.getUsername(), avatar)) {
            throw new HongshuException("头像保存失败，请重试");
        }
        cleanPreviousAvatar(previous.getFirst());
        loginUser.getUser().setAvatar(avatar);
        tokenService.setLoginUser(loginUser);
        AjaxResult ajax = AjaxResult.success();
        ajax.put("imgUrl", avatar);
        return ajax;
    }

    @Schema(description = "外链头像参数")
    public record AvatarUrlRequest(
            @Schema(description = "HTTP 或 HTTPS 图片地址，最长 512 字符，不允许包含用户名或密码") String avatarUrl) {
    }

    /**
     * 外链不参与文件回收，仅处理上传登记表中精确匹配的历史头像。
     */
    private void cleanPreviousAvatar(String previous) {
        if (StringUtils.isEmpty(previous)) return;
        Long managed = jdbc.queryForObject("SELECT COUNT(*) FROM web_upload WHERE path = ?", Long.class, previous);
        if (managed != null && managed > 0) assets.cleanStoredReferencesAfterCommit(previous);
    }
}
