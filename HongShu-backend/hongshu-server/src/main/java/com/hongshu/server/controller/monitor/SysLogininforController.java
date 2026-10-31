package com.hongshu.server.controller.monitor;

import org.springdoc.core.annotations.ParameterObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import com.hongshu.common.annotation.Log;
import com.hongshu.common.core.controller.BaseController;
import com.hongshu.common.core.domain.AjaxResult;
import com.hongshu.common.core.page.TableDataInfo;
import com.hongshu.common.enums.BusinessType;
import com.hongshu.common.utils.poi.ExcelUtil;
import com.hongshu.framework.web.service.SysPasswordService;
import com.hongshu.system.domain.SysLogininfor;
import com.hongshu.system.service.ISysLogininforService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

/**
 * 系统访问记录
 *
 * @author hongshu
 */
@Tag(name = "监控·后台登录日志", description = "管理账号登录记录、导出、清理及账号解锁")
@RestController
@RequestMapping("/monitor/logininfor")
public class SysLogininforController extends BaseController {

    private final ISysLogininforService logininforService;
    private final SysPasswordService passwordService;

    public SysLogininforController(ISysLogininforService logininforService, SysPasswordService passwordService) {
        this.logininforService = logininforService;
        this.passwordService = passwordService;
    }

    @Parameters({
            @Parameter(name = "pageNum", in = ParameterIn.QUERY, description = "当前页码，从 1 开始，默认 1", schema = @Schema(type = "integer", defaultValue = "1")),
            @Parameter(name = "pageSize", in = ParameterIn.QUERY, description = "每页条数，默认 10", schema = @Schema(type = "integer", defaultValue = "10"))
    })
    @Operation(summary = "分页查询后台登录日志",
            description = "按请求中的筛选条件查询后台登录日志，分页接口使用 pageNum 和 pageSize。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('monitor:logininfor:list')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @PreAuthorize("@ss.hasPermi('monitor:logininfor:list')")
    @GetMapping("/list")
    public TableDataInfo list(@ParameterObject @Parameter(description = "后台登录日志筛选条件，具体字段见关联模型；分页使用 pageNum 和 pageSize") SysLogininfor logininfor) {
        startPage();
        List<SysLogininfor> list = logininforService.selectLogininforList(logininfor);
        return getDataTable(list);
    }

    @Operation(summary = "导出后台登录日志为 Excel",
            description = "按筛选条件导出后台登录日志，响应为 Excel 文件，不使用普通 JSON 列表格式。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('monitor:logininfor:export')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @Log(title = "登录日志", businessType = BusinessType.EXPORT)
    @PreAuthorize("@ss.hasPermi('monitor:logininfor:export')")
    @PostMapping("/export")
    public void export(
            HttpServletResponse response,
            @ParameterObject @Parameter(description = "后台登录日志筛选条件，具体字段见关联模型；分页使用 pageNum 和 pageSize") SysLogininfor logininfor
    ) {
        List<SysLogininfor> list = logininforService.selectLogininforList(logininfor);
        ExcelUtil<SysLogininfor> util = new ExcelUtil<>(SysLogininfor.class);
        util.exportExcel(response, list, "登录日志");
    }

    @Operation(summary = "删除后台登录日志",
            description = "按路径中的 ID 删除后台登录日志；数组形式的 ID 以英文逗号分隔，具体业务约束由服务端校验。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('monitor:logininfor:remove')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @PreAuthorize("@ss.hasPermi('monitor:logininfor:remove')")
    @Log(title = "登录日志", businessType = BusinessType.DELETE)
    @DeleteMapping("/{infoIds}")
    public AjaxResult remove(@Parameter(description = "登录日志 ID 列表，多个值以英文逗号分隔") @PathVariable Long[] infoIds) {
        return toAjax(logininforService.deleteLogininforByIds(infoIds));
    }

    @Operation(summary = "清空后台登录日志",
            description = "删除全部后台登录日志记录，执行后无法通过本接口恢复，请谨慎调用。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('monitor:logininfor:remove')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @PreAuthorize("@ss.hasPermi('monitor:logininfor:remove')")
    @Log(title = "登录日志", businessType = BusinessType.CLEAN)
    @DeleteMapping("/clean")
    public AjaxResult clean() {
        logininforService.cleanLogininfor();
        return success();
    }

    @Operation(summary = "解除管理账号登录锁定",
            description = "清除指定用户名的密码错误次数缓存，允许被临时锁定的账号重新尝试登录。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('monitor:logininfor:unlock')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @PreAuthorize("@ss.hasPermi('monitor:logininfor:unlock')")
    @Log(title = "账户解锁", businessType = BusinessType.OTHER)
    @GetMapping("/unlock/{userName}")
    public AjaxResult unlock(@Parameter(description = "管理账号用户名") @PathVariable("userName") String userName) {
        passwordService.clearLoginRecordCache(userName);
        return success();
    }
}
