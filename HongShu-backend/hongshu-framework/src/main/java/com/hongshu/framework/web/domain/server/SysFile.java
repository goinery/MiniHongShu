package com.hongshu.framework.web.domain.server;

import lombok.Getter;
import lombok.Setter;

/**
 * 系统文件相关信息
 *
 * @author hongshu
 */
public class SysFile {

    /**
     * 盘符路径
     */
    @Getter
    @Setter
    private String dirName;

    /**
     * 盘符类型
     */
    @Getter
    @Setter
    private String sysTypeName;

    /**
     * 文件类型
     */
    @Getter
    @Setter
    private String typeName;

    /**
     * 总大小
     */
    @Getter
    @Setter
    private String total;

    /**
     * 剩余大小
     */
    @Getter
    @Setter
    private String free;

    /**
     * 已经使用量
     */
    @Getter
    @Setter
    private String used;

    /**
     * 资源的使用率
     */
    @Getter
    @Setter
    private double usage;

}
