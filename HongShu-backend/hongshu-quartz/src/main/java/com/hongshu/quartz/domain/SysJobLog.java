package com.hongshu.quartz.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;

import lombok.Getter;
import lombok.Setter;

import com.hongshu.common.annotation.Excel;
import com.hongshu.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Date;

/**
 * 定时任务调度日志表 sys_job_log
 *
 * @author hongshu
 */
@Schema(description = "定时任务执行日志")
public class SysJobLog extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @Excel(name = "日志序号")
    @Getter
    @Setter
    @Schema(description = "任务执行日志 ID")
    private Long jobLogId;

    /**
     * 任务名称
     */
    @Excel(name = "任务名称")
    @Getter
    @Setter
    @Schema(description = "任务名称")
    private String jobName;

    /**
     * 任务组名
     */
    @Excel(name = "任务组名")
    @Getter
    @Setter
    @Schema(description = "任务组名")
    private String jobGroup;

    /**
     * 调用目标字符串
     */
    @Excel(name = "调用目标字符串")
    @Getter
    @Setter
    @Schema(description = "调用目标字符串")
    private String invokeTarget;

    /**
     * 日志信息
     */
    @Excel(name = "日志信息")
    @Getter
    @Setter
    @Schema(description = "日志信息")
    private String jobMessage;

    /**
     * 执行状态（0正常 1失败）
     */
    @Excel(name = "执行状态", readConverterExp = "0=正常,1=失败")
    @Getter
    @Setter
    @Schema(description = "执行状态（0正常 1失败）")
    private String status;

    /**
     * 异常信息
     */
    @Excel(name = "异常信息")
    @Getter
    @Setter
    @Schema(description = "异常信息")
    private String exceptionInfo;

    /**
     * 开始时间
     */
    @Getter
    @Setter
    @Schema(description = "开始时间")
    private Date startTime;

    /**
     * 停止时间
     */
    @Getter
    @Setter
    @Schema(description = "停止时间")
    private Date stopTime;

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("jobLogId", getJobLogId())
                .append("jobName", getJobName())
                .append("jobGroup", getJobGroup())
                .append("jobMessage", getJobMessage())
                .append("status", getStatus())
                .append("exceptionInfo", getExceptionInfo())
                .append("startTime", getStartTime())
                .append("stopTime", getStopTime())
                .toString();
    }
}
