package com.hongshu.system.domain.vo;

import lombok.Getter;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * 路由配置信息
 *
 * @author hongshu
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class RouterVo {

    /**
     * 路由名字
     */
    @Getter
    @Setter
    private String name;

    /**
     * 路由地址
     */
    @Getter
    @Setter
    private String path;

    /**
     * 是否隐藏路由，当设置 true 的时候该路由不会再侧边栏出现
     */
    @Setter
    private boolean hidden;

    public boolean getHidden() {
        return hidden;
    }

    /**
     * 重定向地址，当设置 noRedirect 的时候该路由在面包屑导航中不可被点击
     */
    @Getter
    @Setter
    private String redirect;

    /**
     * 组件地址
     */
    @Getter
    @Setter
    private String component;

    /**
     * 路由参数：如 {"id": 1, "name": "ry"}
     */
    @Getter
    @Setter
    private String query;

    /**
     * 当你一个路由下面的 children 声明的路由大于1个时，自动会变成嵌套的模式--如组件页面
     */
    @Getter
    @Setter
    private Boolean alwaysShow;

    /**
     * 其他元素
     */
    @Getter
    @Setter
    private MetaVo meta;

    /**
     * 子路由
     */
    @Getter
    @Setter
    private List<RouterVo> children;

}
