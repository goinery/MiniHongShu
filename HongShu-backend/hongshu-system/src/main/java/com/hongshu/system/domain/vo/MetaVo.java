package com.hongshu.system.domain.vo;

import lombok.Getter;
import lombok.Setter;

import com.hongshu.common.utils.StringUtils;

/**
 * 路由显示信息
 *
 * @author hongshu
 */
public class MetaVo {

    /**
     * 设置该路由在侧边栏和面包屑中展示的名字
     */
    @Getter
    @Setter
    private String title;

    /**
     * 设置该路由的图标，对应路径src/assets/icons/svg
     */
    @Getter
    @Setter
    private String icon;

    /**
     * 设置为true，则不会被 <keep-alive>缓存
     */
    @Getter
    @Setter
    private boolean noCache;

    /**
     * 内链地址（http(s)://开头）
     */
    @Getter
    @Setter
    private String link;

    public MetaVo() {
    }

    public MetaVo(String title, String icon) {
        this.title = title;
        this.icon = icon;
    }

    public MetaVo(String title, String icon, boolean noCache) {
        this.title = title;
        this.icon = icon;
        this.noCache = noCache;
    }

    public MetaVo(String title, String icon, String link) {
        this.title = title;
        this.icon = icon;
        this.link = link;
    }

    public MetaVo(String title, String icon, boolean noCache, String link) {
        this.title = title;
        this.icon = icon;
        this.noCache = noCache;
        if (StringUtils.ishttp(link)) {
            this.link = link;
        }
    }

}
