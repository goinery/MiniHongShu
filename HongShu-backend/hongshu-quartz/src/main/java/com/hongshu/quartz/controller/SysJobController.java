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
import com.hongshu.common.constant.Constants;
import com.hongshu.common.core.controller.BaseController;
import com.hongshu.common.core.domain.AjaxResult;
import com.hongshu.common.core.page.TableDataInfo;
import com.hongshu.common.enums.BusinessType;
import com.hongshu.common.exception.job.TaskException;
import com.hongshu.common.utils.StringUtils;
import com.hongshu.common.utils.poi.ExcelUtil;
import com.hongshu.quartz.domain.SysJob;
import com.hongshu.quartz.service.ISysJobService;
import com.hongshu.quartz.util.CronUtils;
import com.hongshu.quartz.util.ScheduleUtils;
import org.quartz.SchedulerException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

/**
 * 调度任务信息操作处理
 *
 * @author hongshu
 */
@Tag(name = "调度·定时任务", description = "Quartz 任务定义、Cron、启停及立即运行")
@RestController
@RequestMapping("/monitor/job")
public class SysJobController extends BaseController {

    private final ISysJobService jobService;

    public SysJobController(ISysJobService jobService) {
        this.jobService = jobService;
    }

    /**
     * 查询定时任务列表
     */
    @Parameters({
            @Parameter(name = "pageNum", in = ParameterIn.QUERY, description = "当前页码，从 1 开始，默认 1", schema = @Schema(type = "integer", defaultValue = "1")),
            @Parameter(name = "pageSize", in = ParameterIn.QUERY, description = "每页条数，默认 10", schema = @Schema(type = "integer", defaultValue = "10"))
    })
    @Operation(summary = "分页查询定时任务",
            description = "按请求中的筛选条件查询定时任务，分页接口使用 pageNum 和 pageSize。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('monitor:job:list')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @PreAuthorize("@ss.hasPermi('monitor:job:list')")
    @GetMapping("/list")
    public TableDataInfo list(@ParameterObject @Parameter(description = "定时任务筛选条件，具体字段见关联模型；分页使用 pageNum 和 pageSize") SysJob sysJob) {
        startPage();
        List<SysJob> list = jobService.selectJobList(sysJob);
        return getDataTable(list);
    }

    /**
     * 导出定时任务列表
     */
    @Operation(summary = "导出定时任务为 Excel",
            description = "按筛选条件导出定时任务，响应为 Excel 文件，不使用普通 JSON 列表格式。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('monitor:job:export')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @PreAuthorize("@ss.hasPermi('monitor:job:export')")
    @Log(title = "定时任务", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(
            HttpServletResponse response,
            @ParameterObject @Parameter(description = "定时任务筛选条件，具体字段见关联模型；分页使用 pageNum 和 pageSize") SysJob sysJob
    ) {
        List<SysJob> list = jobService.selectJobList(sysJob);
        ExcelUtil<SysJob> util = new ExcelUtil<>(SysJob.class);
        util.exportExcel(response, list, "定时任务");
    }

    /**
     * 获取定时任务详细信息
     */
    @Operation(summary = "获取定时任务详情",
            description = "根据路径中的主键 ID 查询指定定时任务的详细信息。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('monitor:job:query')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @PreAuthorize("@ss.hasPermi('monitor:job:query')")
    @GetMapping(value = "/{jobId}")
    public AjaxResult getInfo(@Parameter(description = "定时任务 ID") @PathVariable("jobId") Long jobId) {
        return success(jobService.selectJobById(jobId));
    }

    /**
     * 新增定时任务
     */
    @Operation(summary = "新增定时任务",
            description = "保存任务名称、分组、Cron、调用目标及执行策略，校验表达式和调用白名单后建立 Quartz 调度。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('monitor:job:add')。",
            security = @SecurityRequirement(name = "adminBearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "请求数据，字段含义见下方模型；具体取值与操作约束见接口说明"))
    @PreAuthorize("@ss.hasPermi('monitor:job:add')")
    @Log(title = "定时任务", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysJob job) throws SchedulerException, TaskException {
        if (!CronUtils.isValid(job.getCronExpression())) {
            return error("新增任务'" + job.getJobName() + "'失败，Cron表达式不正确");
        } else if (StringUtils.containsIgnoreCase(job.getInvokeTarget(), Constants.LOOKUP_RMI)) {
            return error("新增任务'" + job.getJobName() + "'失败，目标字符串不允许'rmi'调用");
        } else if (StringUtils.containsAnyIgnoreCase(job.getInvokeTarget(), Constants.LOOKUP_LDAP, Constants.LOOKUP_LDAPS)) {
            return error("新增任务'" + job.getJobName() + "'失败，目标字符串不允许'ldap(s)'调用");
        } else if (StringUtils.containsAnyIgnoreCase(job.getInvokeTarget(), Constants.HTTP, Constants.HTTPS)) {
            return error("新增任务'" + job.getJobName() + "'失败，目标字符串不允许'http(s)'调用");
        } else if (StringUtils.containsAnyIgnoreCase(job.getInvokeTarget(), Constants.JOB_ERROR_STR)) {
            return error("新增任务'" + job.getJobName() + "'失败，目标字符串存在违规");
        } else if (!ScheduleUtils.whiteList(job.getInvokeTarget())) {
            return error("新增任务'" + job.getJobName() + "'失败，目标字符串不在白名单内");
        }
        job.setCreateBy(getUsername());
        return toAjax(jobService.insertJob(job));
    }

    /**
     * 修改定时任务
     */
    @Operation(summary = "修改定时任务",
            description = "更新任务定义并重建调度，支持调整 Cron、调用参数、并发及错过触发策略。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('monitor:job:edit')。",
            security = @SecurityRequirement(name = "adminBearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "请求数据，字段含义见下方模型；具体取值与操作约束见接口说明"))
    @PreAuthorize("@ss.hasPermi('monitor:job:edit')")
    @Log(title = "定时任务", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysJob job) throws SchedulerException, TaskException {
        if (!CronUtils.isValid(job.getCronExpression())) {
            return error("修改任务'" + job.getJobName() + "'失败，Cron表达式不正确");
        } else if (StringUtils.containsIgnoreCase(job.getInvokeTarget(), Constants.LOOKUP_RMI)) {
            return error("修改任务'" + job.getJobName() + "'失败，目标字符串不允许'rmi'调用");
        } else if (StringUtils.containsAnyIgnoreCase(job.getInvokeTarget(), Constants.LOOKUP_LDAP, Constants.LOOKUP_LDAPS)) {
            return error("修改任务'" + job.getJobName() + "'失败，目标字符串不允许'ldap(s)'调用");
        } else if (StringUtils.containsAnyIgnoreCase(job.getInvokeTarget(), Constants.HTTP, Constants.HTTPS)) {
            return error("修改任务'" + job.getJobName() + "'失败，目标字符串不允许'http(s)'调用");
        } else if (StringUtils.containsAnyIgnoreCase(job.getInvokeTarget(), Constants.JOB_ERROR_STR)) {
            return error("修改任务'" + job.getJobName() + "'失败，目标字符串存在违规");
        } else if (!ScheduleUtils.whiteList(job.getInvokeTarget())) {
            return error("修改任务'" + job.getJobName() + "'失败，目标字符串不在白名单内");
        }
        job.setUpdateBy(getUsername());
        return toAjax(jobService.updateJob(job));
    }

    /**
     * 定时任务状态修改
     */
    @Operation(summary = "暂停或恢复定时任务",
            description = "按任务 ID 修改状态：0 正常、1 暂停，并同步对应 Quartz 任务状态。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('monitor:job:changeStatus')。",
            security = @SecurityRequirement(name = "adminBearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "请求数据，字段含义见下方模型；具体取值与操作约束见接口说明"))
    @PreAuthorize("@ss.hasPermi('monitor:job:changeStatus')")
    @Log(title = "定时任务", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@RequestBody SysJob job) throws SchedulerException {
        SysJob newJob = jobService.selectJobById(job.getJobId());
        newJob.setStatus(job.getStatus());
        return toAjax(jobService.changeStatus(newJob));
    }

    /**
     * 定时任务立即执行一次
     */
    @Operation(summary = "立即执行一次定时任务",
            description = "根据已有任务 ID 立即触发一次执行，执行结果写入任务日志。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('monitor:job:changeStatus')。",
            security = @SecurityRequirement(name = "adminBearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "请求数据，字段含义见下方模型；具体取值与操作约束见接口说明"))
    @PreAuthorize("@ss.hasPermi('monitor:job:changeStatus')")
    @Log(title = "定时任务", businessType = BusinessType.UPDATE)
    @PutMapping("/run")
    public AjaxResult run(@RequestBody SysJob job) throws SchedulerException {
        boolean result = jobService.run(job);
        return result ? success() : error("任务不存在或已过期！");
    }

    /**
     * 删除定时任务
     */
    @Operation(summary = "删除定时任务",
            description = "按路径中的 ID 删除定时任务；数组形式的 ID 以英文逗号分隔，具体业务约束由服务端校验。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('monitor:job:remove')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @PreAuthorize("@ss.hasPermi('monitor:job:remove')")
    @Log(title = "定时任务", businessType = BusinessType.DELETE)
    @DeleteMapping("/{jobIds}")
    public AjaxResult remove(@Parameter(description = "定时任务 ID 列表，多个值以英文逗号分隔") @PathVariable Long[] jobIds) throws SchedulerException {
        jobService.deleteJobByIds(jobIds);
        return success();
    }
}
