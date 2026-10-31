package com.hongshu.quartz.controller;

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
import com.hongshu.quartz.domain.SysJobLog;
import com.hongshu.quartz.service.ISysJobLogService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

/**
 * 调度日志操作处理
 *
 * @author hongshu
 */
@Tag(name = "调度·执行日志", description = "定时任务执行结果、异常与耗时记录")
@RestController
@RequestMapping("/monitor/jobLog")
public class SysJobLogController extends BaseController {

    private final ISysJobLogService jobLogService;

    public SysJobLogController(ISysJobLogService jobLogService) {
        this.jobLogService = jobLogService;
    }

    /**
     * 查询定时任务调度日志列表
     */
    @Parameters({
            @Parameter(name = "pageNum", in = ParameterIn.QUERY, description = "当前页码，从 1 开始，默认 1", schema = @Schema(type = "integer", defaultValue = "1")),
            @Parameter(name = "pageSize", in = ParameterIn.QUERY, description = "每页条数，默认 10", schema = @Schema(type = "integer", defaultValue = "10"))
    })
    @Operation(summary = "分页查询任务执行日志",
            description = "按请求中的筛选条件查询任务执行日志，分页接口使用 pageNum 和 pageSize。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('monitor:job:list')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @PreAuthorize("@ss.hasPermi('monitor:job:list')")
    @GetMapping("/list")
    public TableDataInfo list(@ParameterObject @Parameter(description = "任务执行日志筛选条件，具体字段见关联模型；分页使用 pageNum 和 pageSize") SysJobLog sysJobLog) {
        startPage();
        List<SysJobLog> list = jobLogService.selectJobLogList(sysJobLog);
        return getDataTable(list);
    }

    /**
     * 导出定时任务调度日志列表
     */
    @Operation(summary = "导出任务执行日志为 Excel",
            description = "按筛选条件导出任务执行日志，响应为 Excel 文件，不使用普通 JSON 列表格式。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('monitor:job:export')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @PreAuthorize("@ss.hasPermi('monitor:job:export')")
    @Log(title = "任务调度日志", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(
            HttpServletResponse response,
            @ParameterObject @Parameter(description = "任务执行日志筛选条件，具体字段见关联模型；分页使用 pageNum 和 pageSize") SysJobLog sysJobLog
    ) {
        List<SysJobLog> list = jobLogService.selectJobLogList(sysJobLog);
        ExcelUtil<SysJobLog> util = new ExcelUtil<>(SysJobLog.class);
        util.exportExcel(response, list, "调度日志");
    }

    /**
     * 根据调度编号获取详细信息
     */
    @Operation(summary = "获取任务执行日志详情",
            description = "根据路径中的主键 ID 查询指定任务执行日志的详细信息。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('monitor:job:query')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @PreAuthorize("@ss.hasPermi('monitor:job:query')")
    @GetMapping(value = "/{jobLogId}")
    public AjaxResult getInfo(@Parameter(description = "任务日志 ID") @PathVariable Long jobLogId) {
        return success(jobLogService.selectJobLogById(jobLogId));
    }

    /**
     * 删除定时任务调度日志
     */
    @Operation(summary = "删除任务执行日志",
            description = "按路径中的 ID 删除任务执行日志；数组形式的 ID 以英文逗号分隔，具体业务约束由服务端校验。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('monitor:job:remove')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @PreAuthorize("@ss.hasPermi('monitor:job:remove')")
    @Log(title = "定时任务调度日志", businessType = BusinessType.DELETE)
    @DeleteMapping("/{jobLogIds}")
    public AjaxResult remove(@Parameter(description = "任务日志 ID 列表，多个值以英文逗号分隔") @PathVariable Long[] jobLogIds) {
        return toAjax(jobLogService.deleteJobLogByIds(jobLogIds));
    }

    /**
     * 清空定时任务调度日志
     */
    @Operation(summary = "清空任务执行日志",
            description = "删除全部任务执行日志记录，执行后无法通过本接口恢复，请谨慎调用。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('monitor:job:remove')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @PreAuthorize("@ss.hasPermi('monitor:job:remove')")
    @Log(title = "调度日志", businessType = BusinessType.CLEAN)
    @DeleteMapping("/clean")
    public AjaxResult clean() {
        jobLogService.cleanJobLog();
        return success();
    }
}
