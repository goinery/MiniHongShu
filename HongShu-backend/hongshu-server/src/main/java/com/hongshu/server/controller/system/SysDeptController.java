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
import com.hongshu.common.core.domain.entity.SysDept;
import com.hongshu.common.enums.BusinessType;
import com.hongshu.common.utils.StringUtils;
import com.hongshu.system.service.ISysDeptService;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 部门信息
 *
 * @author hongshu
 */
@Tag(name = "系统·部门管理", description = "部门树、组织层级及部门状态维护")
@RestController
@RequestMapping("/system/dept")
public class SysDeptController extends BaseController {

    private final ISysDeptService deptService;

    public SysDeptController(ISysDeptService deptService) {
        this.deptService = deptService;
    }

    /**
     * 获取部门列表
     */
    @Operation(summary = "查询部门树数据",
            description = "按部门名称和状态查询部门列表，返回构建组织树所需的父子关系。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('system:dept:list')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @PreAuthorize("@ss.hasPermi('system:dept:list')")
    @GetMapping("/list")
    public AjaxResult list(@ParameterObject @Parameter(description = "部门筛选条件，具体字段见关联模型；分页使用 pageNum 和 pageSize") SysDept dept) {
        List<SysDept> depts = deptService.selectDeptList(dept);
        return success(depts);
    }

    /**
     * 查询部门列表（排除节点）
     */
    @Operation(summary = "查询可选上级部门",
            description = "返回排除当前部门及其子部门后的列表，避免将部门移动到自身层级之下。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('system:dept:list')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @PreAuthorize("@ss.hasPermi('system:dept:list')")
    @GetMapping("/list/exclude/{deptId}")
    public AjaxResult excludeChild(@Parameter(description = "部门 ID") @PathVariable(value = "deptId", required = false) Long deptId) {
        List<SysDept> depts = deptService.selectDeptList(new SysDept());
        depts.removeIf(d -> d.getDeptId().intValue() == deptId || ArrayUtils.contains(StringUtils.split(d.getAncestors(), ","), deptId + ""));
        return success(depts);
    }

    /**
     * 根据部门编号获取详细信息
     */
    @Operation(summary = "获取部门详情",
            description = "根据路径中的主键 ID 查询指定部门的详细信息。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('system:dept:query')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @PreAuthorize("@ss.hasPermi('system:dept:query')")
    @GetMapping(value = "/{deptId}")
    public AjaxResult getInfo(@Parameter(description = "部门 ID") @PathVariable Long deptId) {
        deptService.checkDeptDataScope(deptId);
        return success(deptService.selectDeptById(deptId));
    }

    /**
     * 新增部门
     */
    @Operation(summary = "新增部门",
            description = "提交部门数据，校验业务约束后保存。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('system:dept:add')。",
            security = @SecurityRequirement(name = "adminBearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "请求数据，字段含义见下方模型；具体取值与操作约束见接口说明"))
    @PreAuthorize("@ss.hasPermi('system:dept:add')")
    @Log(title = "部门管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysDept dept) {
        if (!deptService.checkDeptNameUnique(dept)) {
            return error("新增部门'" + dept.getDeptName() + "'失败，部门名称已存在");
        }
        dept.setCreateBy(getUsername());
        return toAjax(deptService.insertDept(dept));
    }

    /**
     * 修改部门
     */
    @Operation(summary = "修改部门",
            description = "提交包含主键 ID 的部门数据，校验操作权限和业务约束后更新。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('system:dept:edit')。",
            security = @SecurityRequirement(name = "adminBearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "请求数据，字段含义见下方模型；具体取值与操作约束见接口说明"))
    @PreAuthorize("@ss.hasPermi('system:dept:edit')")
    @Log(title = "部门管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SysDept dept) {
        Long deptId = dept.getDeptId();
        deptService.checkDeptDataScope(deptId);
        if (!deptService.checkDeptNameUnique(dept)) {
            return error("修改部门'" + dept.getDeptName() + "'失败，部门名称已存在");
        } else if (dept.getParentId().equals(deptId)) {
            return error("修改部门'" + dept.getDeptName() + "'失败，上级部门不能是自己");
        } else if (StringUtils.equals(UserConstants.DEPT_DISABLE, dept.getStatus()) && deptService.selectNormalChildrenDeptById(deptId) > 0) {
            return error("该部门包含未停用的子部门！");
        }
        dept.setUpdateBy(getUsername());
        return toAjax(deptService.updateDept(dept));
    }

    /**
     * 删除部门
     */
    @Operation(summary = "删除部门",
            description = "按路径中的 ID 删除部门；数组形式的 ID 以英文逗号分隔，具体业务约束由服务端校验。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('system:dept:remove')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @PreAuthorize("@ss.hasPermi('system:dept:remove')")
    @Log(title = "部门管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{deptId}")
    public AjaxResult remove(@Parameter(description = "部门 ID") @PathVariable Long deptId) {
        if (deptService.hasChildByDeptId(deptId)) {
            return warn("存在下级部门,不允许删除");
        }
        if (deptService.checkDeptExistUser(deptId)) {
            return warn("部门存在用户,不允许删除");
        }
        deptService.checkDeptDataScope(deptId);
        return toAjax(deptService.deleteDeptById(deptId));
    }
}
