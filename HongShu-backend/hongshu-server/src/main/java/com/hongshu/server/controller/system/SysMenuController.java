package com.hongshu.server.controller.system;

import org.springdoc.core.annotations.ParameterObject;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import com.hongshu.common.annotation.Log;
import com.hongshu.common.constant.UserConstants;
import com.hongshu.common.core.controller.BaseController;
import com.hongshu.common.core.domain.AjaxResult;
import com.hongshu.common.core.domain.entity.SysMenu;
import com.hongshu.common.enums.BusinessType;
import com.hongshu.common.utils.StringUtils;
import com.hongshu.system.service.ISysMenuService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单信息
 *
 * @author hongshu
 */
@Tag(name = "系统·菜单权限", description = "目录、菜单、按钮权限及角色菜单树")
@RestController
@RequestMapping("/system/menu")
public class SysMenuController extends BaseController {

    private final ISysMenuService menuService;

    public SysMenuController(ISysMenuService menuService) {
        this.menuService = menuService;
    }

    /**
     * 获取菜单列表
     */
    @Operation(summary = "查询菜单和按钮权限",
            description = "按菜单名称、状态等条件返回当前账号可管理的目录、菜单和按钮数据。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('system:menu:list')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @PreAuthorize("@ss.hasPermi('system:menu:list')")
    @GetMapping("/list")
    public AjaxResult list(@ParameterObject @Parameter(description = "菜单筛选条件，具体字段见关联模型；分页使用 pageNum 和 pageSize") SysMenu menu) {
        List<SysMenu> menus = menuService.selectMenuList(menu, getUserId());
        return success(menus);
    }

    /**
     * 根据菜单编号获取详细信息
     */
    @Operation(summary = "获取菜单详情",
            description = "根据路径中的主键 ID 查询指定菜单的详细信息。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('system:menu:query')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @PreAuthorize("@ss.hasPermi('system:menu:query')")
    @GetMapping(value = "/{menuId}")
    public AjaxResult getInfo(@Parameter(description = "菜单 ID") @PathVariable Long menuId) {
        return success(menuService.selectMenuById(menuId));
    }

    /**
     * 获取菜单下拉树列表
     */
    @Operation(summary = "获取菜单下拉树",
            description = "将可访问菜单组装为前端树形选择器数据。 需携带管理端 Bearer JWT。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @GetMapping("/treeselect")
    public AjaxResult treeselect(@ParameterObject @Parameter(description = "菜单筛选条件，具体字段见关联模型；分页使用 pageNum 和 pageSize") SysMenu menu) {
        List<SysMenu> menus = menuService.selectMenuList(menu, getUserId());
        return success(menuService.buildMenuTreeSelect(menus));
    }

    /**
     * 加载对应角色菜单列表树
     */
    @Operation(summary = "获取角色菜单授权树",
            description = "返回菜单树及角色已勾选的菜单 ID，用于编辑角色菜单和按钮权限。 需携带管理端 Bearer JWT。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @GetMapping(value = "/roleMenuTreeselect/{roleId}")
    public AjaxResult roleMenuTreeselect(@Parameter(description = "角色 ID") @PathVariable("roleId") Long roleId) {
        List<SysMenu> menus = menuService.selectMenuList(getUserId());
        AjaxResult ajax = AjaxResult.success();
        ajax.put("checkedKeys", menuService.selectMenuListByRoleId(roleId));
        ajax.put("menus", menuService.buildMenuTreeSelect(menus));
        return ajax;
    }

    /**
     * 新增菜单
     */
    @Operation(summary = "新增菜单",
            description = "提交菜单数据，校验业务约束后保存。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('system:menu:add')。",
            security = @SecurityRequirement(name = "adminBearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "请求数据，字段含义见下方模型；具体取值与操作约束见接口说明"))
    @PreAuthorize("@ss.hasPermi('system:menu:add')")
    @Log(title = "菜单管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysMenu menu) {
        if (!menuService.checkMenuNameUnique(menu)) {
            return error("新增菜单'" + menu.getMenuName() + "'失败，菜单名称已存在");
        } else if (UserConstants.YES_FRAME.equals(menu.getIsFrame()) && !StringUtils.ishttp(menu.getPath())) {
            return error("新增菜单'" + menu.getMenuName() + "'失败，地址必须以http(s)://开头");
        }
        menu.setCreateBy(getUsername());
        return toAjax(menuService.insertMenu(menu));
    }

    /**
     * 修改菜单
     */
    @Operation(summary = "修改菜单",
            description = "提交包含主键 ID 的菜单数据，校验操作权限和业务约束后更新。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('system:menu:edit')。",
            security = @SecurityRequirement(name = "adminBearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "请求数据，字段含义见下方模型；具体取值与操作约束见接口说明"))
    @PreAuthorize("@ss.hasPermi('system:menu:edit')")
    @Log(title = "菜单管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SysMenu menu) {
        if (!menuService.checkMenuNameUnique(menu)) {
            return error("修改菜单'" + menu.getMenuName() + "'失败，菜单名称已存在");
        } else if (UserConstants.YES_FRAME.equals(menu.getIsFrame()) && !StringUtils.ishttp(menu.getPath())) {
            return error("修改菜单'" + menu.getMenuName() + "'失败，地址必须以http(s)://开头");
        } else if (menu.getMenuId().equals(menu.getParentId())) {
            return error("修改菜单'" + menu.getMenuName() + "'失败，上级菜单不能选择自己");
        }
        menu.setUpdateBy(getUsername());
        return toAjax(menuService.updateMenu(menu));
    }

    /**
     * 删除菜单
     */
    @Operation(summary = "删除菜单",
            description = "按路径中的 ID 删除菜单；数组形式的 ID 以英文逗号分隔，具体业务约束由服务端校验。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('system:menu:remove')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @PreAuthorize("@ss.hasPermi('system:menu:remove')")
    @Log(title = "菜单管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{menuId}")
    public AjaxResult remove(@Parameter(description = "菜单 ID") @PathVariable("menuId") Long menuId) {
        if (menuService.hasChildByMenuId(menuId)) {
            return warn("存在子菜单,不允许删除");
        }
        if (menuService.checkMenuExistRole(menuId)) {
            return warn("菜单已分配,不允许删除");
        }
        return toAjax(menuService.deleteMenuById(menuId));
    }
}
