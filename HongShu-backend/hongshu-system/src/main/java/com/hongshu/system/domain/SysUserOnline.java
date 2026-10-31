package com.hongshu.system.domain;

import lombok.Getter;
import lombok.Setter;

/**
 * 当前在线会话
 *
 * @author hongshu
 */
public class SysUserOnline {

    /**
     * 会话编号
     */
    @Getter
    @Setter
    private String tokenId;

    /**
     * 部门名称
     */
    @Getter
    @Setter
    private String deptName;

    /**
     * 用户名称
     */
    @Getter
    @Setter
    private String userName;

    /**
     * 登录IP地址
     */
    @Getter
    @Setter
    private String ipaddr;

    /**
     * 登录地址
     */
    @Getter
    @Setter
    private String loginLocation;

    /**
     * 浏览器类型
     */
    @Getter
    @Setter
    private String browser;

    /**
     * 操作系统
     */
    @Getter
    @Setter
    private String os;

    /**
     * 登录时间
     */
    @Getter
    @Setter
    private Long loginTime;

}
