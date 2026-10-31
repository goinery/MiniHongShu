package com.hongshu.system.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;

import lombok.Getter;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.hongshu.common.annotation.Excel;
import com.hongshu.common.annotation.Excel.ColumnType;
import com.hongshu.common.core.domain.BaseEntity;

import java.util.Date;

/**
 * 系统访问记录表 sys_logininfor
 *
 * @author hongshu
 */
@Schema(description = "管理端登录日志")
public class SysLogininfor extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @Excel(name = "序号", cellType = ColumnType.NUMERIC)
    @Getter
    @Setter
    @Schema(description = "登录日志 ID")
    private Long infoId;

    /**
     * 用户账号
     */
    @Excel(name = "用户账号")
    @Getter
    @Setter
    @Schema(description = "用户账号")
    private String userName;

    /**
     * 登录状态 0成功 1失败
     */
    @Excel(name = "登录状态", readConverterExp = "0=成功,1=失败")
    @Getter
    @Setter
    @Schema(description = "登录状态 0成功 1失败")
    private String status;

    /**
     * 登录IP地址
     */
    @Excel(name = "登录地址")
    @Getter
    @Setter
    @Schema(description = "登录IP地址")
    private String ipaddr;

    /**
     * 登录地点
     */
    @Excel(name = "登录地点")
    @Getter
    @Setter
    @Schema(description = "登录地点")
    private String loginLocation;

    /**
     * 浏览器类型
     */
    @Excel(name = "浏览器")
    @Getter
    @Setter
    @Schema(description = "浏览器类型")
    private String browser;

    /**
     * 操作系统
     */
    @Excel(name = "操作系统")
    @Getter
    @Setter
    @Schema(description = "操作系统")
    private String os;

    /**
     * 提示消息
     */
    @Excel(name = "提示消息")
    @Getter
    @Setter
    @Schema(description = "提示消息")
    private String msg;

    /**
     * 访问时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "访问时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @Getter
    @Setter
    @Schema(description = "访问时间")
    private Date loginTime;

}
