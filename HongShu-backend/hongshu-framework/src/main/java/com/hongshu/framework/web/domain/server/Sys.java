package com.hongshu.framework.web.domain.server;

import lombok.Getter;
import lombok.Setter;

/**
 * 系统相关信息
 *
 * @author hongshu
 */
public class Sys {

    /**
     * 服务器名称
     */
    @Getter
    @Setter
    private String computerName;

    /**
     * 服务器Ip
     */
    @Getter
    @Setter
    private String computerIp;

    /**
     * 项目路径
     */
    @Getter
    @Setter
    private String userDir;

    /**
     * 操作系统
     */
    @Getter
    @Setter
    private String osName;

    /**
     * 系统架构
     */
    @Getter
    @Setter
    private String osArch;

}
