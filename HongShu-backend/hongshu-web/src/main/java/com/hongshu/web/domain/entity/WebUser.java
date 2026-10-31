package com.hongshu.web.domain.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hongshu.web.domain.HongshuBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 用户
 *
 * @author hongshu
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("web_user")
@Schema(description = "社区会员资料")
public class WebUser extends HongshuBaseEntity {

    /**
     * 红薯ID
     */
    @Schema(description = "红薯ID")
    private Long hsId;

    /**
     * 用户名
     */
    @Schema(description = "用户名")
    private String username;

    /**
     * 密码
     */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Schema(description = "密码")
    private String password;

    /**
     * 头像
     */
    @Schema(description = "头像")
    private String avatar;

    /**
     * 性别
     */
    @Schema(description = "性别")
    private String gender;

    /**
     * 电话
     */
    @Schema(description = "电话")
    private String phone;

    /**
     * email
     */
    @Schema(description = "邮箱地址")
    private String email;

    /**
     * 描述
     */
    @Schema(description = "描述")
    private String description;

    /**
     * 用户状态
     */
    @Schema(description = "账号状态：0 正常，1 停用")
    private String status;

    /**
     * 生日
     */
    @Schema(description = "生日")
    private String birthday;

    /**
     * 地址
     */
    @Schema(description = "IP属地，前台根据请求IP自动识别，只读")
    private String address;

    /**
     * 用户标签
     */
    @Schema(description = "用户标签")
    private String tags;

    /**
     * 笔记数量
     */
    @Schema(description = "笔记数量")
    private Long trendCount;

    /**
     * 关注数量
     */
    @Schema(description = "关注数量")
    private Long followerCount;

    /**
     * 粉丝数量
     */
    @Schema(description = "粉丝数量")
    private Long fanCount;

    /**
     * 最后登录IP
     */
    @Schema(description = "最后登录IP")
    private String loginIp;

    /**
     * 最后登录时间
     */
    @Schema(description = "最后登录时间")
    private Date loginDate;

    /**
     * 备注
     */
    @Schema(description = "备注")
    private String remark;
}
