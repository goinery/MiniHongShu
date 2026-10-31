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
import com.hongshu.system.domain.SysOperLog;
import com.hongshu.system.service.ISysOperLogService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

/**
 * 操作日志记录
 *
 * @author hongshu
 */
@Tag(name = "监控·操作审计日志", description = "后台业务操作记录的查询、导出与清理")
@RestController
@RequestMapping("/monitor/operlog")
public class SysOperlogController extends BaseController {

    private final ISysOperLogService operLogService;

    public SysOperlogController(ISysOperLogService operLogService) {
        this.operLogService = operLogService;
    }

    @Parameters({
            @Parameter(name = "pageNum", in = ParameterIn.QUERY, description = "当前页码，从 1 开始，默认 1", schema = @Schema(type = "integer", defaultValue = "1")),
            @Parameter(name = "pageSize", in = ParameterIn.QUERY, description = "每页条数，默认 10", schema = @Schema(type = "integer", defaultValue = "10"))
    })
    @Operation(summary = "分页查询操作日志",
            description = "按请求中的筛选条件查询操作日志，分页接口使用 pageNum 和 pageSize。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('monitor:operlog:list')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @PreAuthorize("@ss.hasPermi('monitor:operlog:list')")
    @GetMapping("/list")
    public TableDataInfo list(@ParameterObject @Parameter(description = "操作日志筛选条件，具体字段见关联模型；分页使用 pageNum 和 pageSize") SysOperLog operLog) {
        startPage();
        List<SysOperLog> list = operLogService.selectOperLogList(operLog);
        return getDataTable(list);
    }

    @Operation(summary = "导出操作日志为 Excel",
            description = "按筛选条件导出操作日志，响应为 Excel 文件，不使用普通 JSON 列表格式。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('monitor:operlog:export')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @Log(title = "操作日志", businessType = BusinessType.EXPORT)
    @PreAuthorize("@ss.hasPermi('monitor:operlog:export')")
    @PostMapping("/export")
    public void export(
            HttpServletResponse response,
            @ParameterObject @Parameter(description = "操作日志筛选条件，具体字段见关联模型；分页使用 pageNum 和 pageSize") SysOperLog operLog
    ) {
        List<SysOperLog> list = operLogService.selectOperLogList(operLog);
        ExcelUtil<SysOperLog> util = new ExcelUtil<>(SysOperLog.class);
        util.exportExcel(response, list, "操作日志");
    }

    @Operation(summary = "删除操作日志",
            description = "按路径中的 ID 删除操作日志；数组形式的 ID 以英文逗号分隔，具体业务约束由服务端校验。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('monitor:operlog:remove')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @Log(title = "操作日志", businessType = BusinessType.DELETE)
    @PreAuthorize("@ss.hasPermi('monitor:operlog:remove')")
    @DeleteMapping("/{operIds}")
    public AjaxResult remove(@Parameter(description = "操作日志 ID 列表，多个值以英文逗号分隔") @PathVariable Long[] operIds) {
        return toAjax(operLogService.deleteOperLogByIds(operIds));
    }

    @Operation(summary = "清空操作日志",
            description = "删除全部操作日志记录，执行后无法通过本接口恢复，请谨慎调用。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('monitor:operlog:remove')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @Log(title = "操作日志", businessType = BusinessType.CLEAN)
    @PreAuthorize("@ss.hasPermi('monitor:operlog:remove')")
    @DeleteMapping("/clean")
    public AjaxResult clean() {
        operLogService.cleanOperLog();
        return success();
    }
}
