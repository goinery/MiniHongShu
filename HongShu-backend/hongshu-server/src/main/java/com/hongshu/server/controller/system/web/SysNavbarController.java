package com.hongshu.server.controller.system.web;

import org.springdoc.core.annotations.ParameterObject;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import cn.hutool.json.JSONUtil;
import com.hongshu.common.annotation.Log;
import com.hongshu.common.core.controller.BaseController;
import com.hongshu.common.core.domain.AjaxResult;
import com.hongshu.common.enums.BusinessType;
import com.hongshu.web.domain.entity.WebNavbar;
import com.hongshu.web.service.ISysNavbarService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 导航栏信息
 *
 * @author hongshu
 */
@Tag(name = "运营·分类导航", description = "内容分类树、分类封面、排序与状态管理")
@RestController
@RequestMapping("/navbar")
public class SysNavbarController extends BaseController {

    private final ISysNavbarService navbarService;

    public SysNavbarController(ISysNavbarService navbarService) {
        this.navbarService = navbarService;
    }

    /**
     * 获取导航栏列表
     */
    @Operation(summary = "查询内容分类树",
            description = "按标题和状态筛选分类，并返回包含父子关系的分类树。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('web:navbar:list')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @PreAuthorize("@ss.hasPermi('web:navbar:list')")
    @GetMapping("/list")
    public AjaxResult list(@ParameterObject @Parameter(description = "分类筛选条件，具体字段见关联模型；分页使用 pageNum 和 pageSize") WebNavbar category) {
        List<WebNavbar> categoryList = navbarService.selectNavbarList(category);
        return success(categoryList);
    }

    /**
     * 根据导航栏编号获取详细信息
     */
    @Operation(summary = "获取分类详情",
            description = "根据路径中的主键 ID 查询指定分类的详细信息。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('web:navbar:query')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @PreAuthorize("@ss.hasPermi('web:navbar:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@Parameter(description = "分类 ID") @PathVariable Long id) {
        return success(navbarService.selectById(id));
    }

    /**
     * 加载导航栏列表树
     */
    @Operation(summary = "获取分类树及选中项",
            description = "返回分类选择树与指定条件下的分类 ID，兼容管理端分类选择器。 需携带管理端 Bearer JWT。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @GetMapping(value = "/navbarTreeSelect")
    public AjaxResult roleMenuTreeSelect(@ParameterObject @Parameter(description = "分类筛选条件，具体字段见关联模型；分页使用 pageNum 和 pageSize") WebNavbar category) {
        List<WebNavbar> categoryList = navbarService.selectNavbarList(category);
        AjaxResult ajax = AjaxResult.success();
        ajax.put("categoryList", navbarService.buildNavbarTreeSelect(categoryList));
        return ajax;
    }

    /**
     * 获取菜单下拉树列表
     */
    @Operation(summary = "获取分类下拉树",
            description = "查询分类并组装适用于下拉树选择器的数据。 需携带管理端 Bearer JWT。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @GetMapping("/treeSelect")
    public AjaxResult treeSelect(@ParameterObject @Parameter(description = "分类筛选条件，具体字段见关联模型；分页使用 pageNum 和 pageSize") WebNavbar category) {
        List<WebNavbar> categoryList = navbarService.selectNavbarList(category);
        return success(navbarService.buildNavbarTreeSelect(categoryList));
    }

    /**
     * 新增导航栏
     */
    @Operation(summary = "新增内容分类",
            description = "以 multipart/form-data 提交 category JSON 字符串及分类封面 file，保存父分类、名称、排序和状态。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('web:navbar:add')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @PreAuthorize("@ss.hasPermi('web:navbar:add')")
    @Log(title = "导航栏管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(
            @Parameter(description = "分类数据 JSON 字符串，包含 title、pid、sort、status；修改须包含 id") @RequestParam("category") String category,
            @Parameter(description = "上传文件，使用 multipart/form-data 提交；原始字段名以接口参数名为准") @RequestParam("file") MultipartFile file
    ) {
        WebNavbar webNavbar = JSONUtil.toBean(category, WebNavbar.class);
        return toAjax(navbarService.insertNavbar(webNavbar, file));
    }

    /**
     * 修改导航栏
     */
    @Operation(summary = "修改内容分类",
            description = "以 multipart/form-data 提交 category JSON 字符串及可选封面 file；提交后检查旧分类封面引用。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('web:navbar:edit')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @PreAuthorize("@ss.hasPermi('web:navbar:edit')")
    @Log(title = "导航栏管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(
            @Parameter(description = "分类数据 JSON 字符串，包含 title、pid、sort、status；修改须包含 id") @RequestParam("category") String category,
            @Parameter(description = "上传文件，使用 multipart/form-data 提交；原始字段名以接口参数名为准") @RequestParam(value = "file", required = false) MultipartFile file
    ) {
        WebNavbar webNavbar = JSONUtil.toBean(category, WebNavbar.class);
        return toAjax(navbarService.updateNavbar(webNavbar, file));
    }

    /**
     * 删除导航栏
     */
    @Operation(summary = "删除内容分类",
            description = "删除指定分类；存在子分类时拒绝删除，提交后检查旧封面是否需要回收。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('web:navbar:remove')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @PreAuthorize("@ss.hasPermi('web:navbar:remove')")
    @Log(title = "导航栏管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@Parameter(description = "分类 ID") @PathVariable("id") Long id) {
        if (navbarService.hasChildById(id)) {
            return warn("存在子导航栏,不允许删除");
        }
        return toAjax(navbarService.deleteById(id));
    }
}
