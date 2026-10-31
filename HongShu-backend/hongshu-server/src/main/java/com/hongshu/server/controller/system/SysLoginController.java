package com.hongshu.server.controller.system;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import com.hongshu.common.constant.Constants;
import com.hongshu.common.core.domain.AjaxResult;
import com.hongshu.common.core.domain.entity.SysMenu;
import com.hongshu.common.core.domain.entity.SysUser;
import com.hongshu.common.core.domain.model.LoginBody;
import com.hongshu.common.utils.SecurityUtils;
import com.hongshu.framework.web.service.SysLoginService;
import com.hongshu.framework.web.service.SysPermissionService;
import com.hongshu.system.service.ISysMenuService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

/**
 * 登录验证
 *
 * @author hongshu
 */
@Tag(name = "系统·管理员认证", description = "后台登录、当前账号信息与动态菜单路由")
@RestController
public class SysLoginController {

    private final SysLoginService loginService;
    private final ISysMenuService menuService;
    private final SysPermissionService permissionService;

    public SysLoginController(SysLoginService loginService, ISysMenuService menuService, SysPermissionService permissionService) {
        this.loginService = loginService;
        this.menuService = menuService;
        this.permissionService = permissionService;
    }

    /**
     * 登录方法
     *
     * @param loginBody 登录信息
     * @return 结果
     */
    @Operation(summary = "管理员账号登录",
            description = "提交用户名、密码及按配置启用的验证码，成功后返回管理端 JWT；后续通过 Authorization: Bearer 请求头携带。 无需登录。",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "请求数据，字段含义见下方模型；具体取值与操作约束见接口说明"))
    @PostMapping("/login")
    public AjaxResult login(@RequestBody LoginBody loginBody) {
        AjaxResult ajax = AjaxResult.success();
        // 生成令牌
        String token = loginService.login(loginBody.getUsername(), loginBody.getPassword(), loginBody.getCode(),
                loginBody.getUuid());
        ajax.put(Constants.TOKEN, token);
        return ajax;
    }

    /**
     * 获取用户信息
     *
     * @return 用户信息
     */
    @Operation(summary = "获取当前管理员信息",
            description = "返回当前登录用户、角色、权限集合和密码提示信息，用于恢复管理端登录状态。 需携带管理端 Bearer JWT。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @GetMapping("getInfo")
    public AjaxResult getInfo() {
        SysUser user = SecurityUtils.getLoginUser().getUser();
        // 角色集合
        Set<String> roles = permissionService.getRolePermission(user);
        // 权限集合
        Set<String> permissions = permissionService.getMenuPermission(user);
        AjaxResult ajax = AjaxResult.success();
        ajax.put("user", user);
        ajax.put("roles", roles);
        ajax.put("permissions", permissions);
        return ajax;
    }

    /**
     * 获取路由信息
     *
     * @return 路由信息
     */
    @Operation(summary = "获取当前管理员菜单路由",
            description = "按当前用户的菜单权限构建动态路由树，供管理端生成侧边菜单和页面路由。 需携带管理端 Bearer JWT。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @GetMapping("getRouters")
    public AjaxResult getRouters() {
        Long userId = SecurityUtils.getUserId();
        List<SysMenu> menus = menuService.selectMenuTreeByUserId(userId);
        return AjaxResult.success(menuService.buildMenus(menus));
    }
}
