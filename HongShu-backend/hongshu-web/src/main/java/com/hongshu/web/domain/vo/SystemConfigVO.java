package com.hongshu.web.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 网站配置VO
 *
 * @author hongshu
 * @since 2018年11月11日14:54:12
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "业务系统配置参数")
public class SystemConfigVO extends BaseVO<SystemConfigVO> {

    /**
     * 邮箱账号
     */
    @Schema(description = "邮箱账号")
    private String email;

    /**
     * 邮箱发件人用户名
     */
    @Schema(description = "邮箱发件人用户名")
    private String emailUserName;

    /**
     * 邮箱密码
     */
    @Schema(description = "邮件配置密码字段；当前邮件发送链路未完整接入")
    private String emailPassword;

    /**
     * SMTP地址
     */
    @Schema(description = "SMTP地址")
    private String smtpAddress;

    /**
     * SMTP端口
     */
    @Schema(description = "SMTP端口")
    private String smtpPort;

    /**
     * 是否开启邮件通知(0:否， 1:是)
     * 当有新的反馈，友链申请时进行通知，首先需要在系统管理处设置接收通知的邮箱
     */
    @Schema(description = "是否开启邮件通知(0:否， 1:是)")
    private String startEmailNotification;

    /**
     * 编辑器模式，(0：富文本编辑器CKEditor，1：markdown编辑器Veditor)
     */
    @Schema(description = "编辑器模式，(0：富文本编辑器CKEditor，1：markdown编辑器Veditor)")
    private String editorModel;

    /**
     * 主题颜色
     */
    @Schema(description = "主题颜色")
    private String themeColor;

    /**
     * 仪表盘通知【首次进入时弹出】
     */
    @Schema(description = "仪表盘通知【首次进入时弹出】")
    private String dashboardNotification;

    /**
     * 是否开启仪表盘通知【0 关闭，1 开启】
     */
    @Schema(description = "是否开启仪表盘通知【0 关闭，1 开启】")
    private String openDashboardNotification;

    /**
     * 是否开启用户邮件激活功能【0 关闭，1 开启】
     */
    @Schema(description = "是否开启用户邮件激活功能【0 关闭，1 开启】")
    private String openEmailActivate;

    /**
     * 搜索模式：0:SQL搜索 、1：全文检索
     */
    @Schema(description = "历史搜索模式配置；当前搜索服务固定查询 MySQL")
    private String searchModel;
}
