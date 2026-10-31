package com.hongshu.common.core.domain.entity;

import java.io.Serial;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hongshu.common.core.domain.SuperEntity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统配置表
 *
 * @author hongshu
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_system_config")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SystemConfig extends SuperEntity<SystemConfig> {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 邮箱账号
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String email;

    /**
     * 邮箱发件人用户名
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String emailUserName;

    /**
     * 邮箱密码
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String emailPassword;

    /**
     * SMTP地址
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String smtpAddress;

    /**
     * SMTP端口
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String smtpPort;

    /**
     * 是否开启邮件通知(0:否， 1:是)
     * 当有新的反馈，友链申请时进行通知，首先需要在系统管理处设置接收通知的邮箱
     */
    private String startEmailNotification;

    /**
     * 编辑器模式，(0：富文本编辑器CKEditor，1：markdown编辑器Veditor)
     */
    private String editorModel;

    /**
     * 主题颜色
     */
    private String themeColor;

    /**
     * 仪表盘通知【首次进入时弹出】
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String dashboardNotification;

    /**
     * 是否开启仪表盘通知【0 关闭，1 开启】
     */
    private String openDashboardNotification;

    /**
     * 是否开启用户邮件激活功能【0 关闭，1 开启】
     */
    private String openEmailActivate;

    /**
     * 搜索模式：0:SQL搜索 、1：全文检索
     */
    private String searchModel;
}
