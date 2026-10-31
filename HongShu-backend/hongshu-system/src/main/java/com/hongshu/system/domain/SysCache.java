package com.hongshu.system.domain;

import lombok.Getter;
import lombok.Setter;

import com.hongshu.common.utils.StringUtils;

/**
 * 缓存信息
 *
 * @author hongshu
 */
public class SysCache {

    /**
     * 缓存名称
     */
    @Getter
    @Setter
    private String cacheName = "";

    /**
     * 缓存键名
     */
    @Getter
    @Setter
    private String cacheKey = "";

    /**
     * 缓存内容
     */
    @Getter
    @Setter
    private String cacheValue = "";

    /**
     * 备注
     */
    @Getter
    @Setter
    private String remark = "";

    public SysCache() {

    }

    public SysCache(String cacheName, String remark) {
        this.cacheName = cacheName;
        this.remark = remark;
    }

    public SysCache(String cacheName, String cacheKey, String cacheValue) {
        this.cacheName = StringUtils.replace(cacheName, ":", "");
        this.cacheKey = StringUtils.replace(cacheKey, cacheName, "");
        this.cacheValue = cacheValue;
    }

}
