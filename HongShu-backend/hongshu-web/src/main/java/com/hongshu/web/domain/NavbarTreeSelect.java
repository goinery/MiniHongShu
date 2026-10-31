package com.hongshu.web.domain;

import java.io.Serial;

import lombok.Getter;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hongshu.web.domain.entity.WebNavbar;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

/**
 * NavbarTreeSelect树结构实体类
 *
 * @author hongshu
 */
public class NavbarTreeSelect implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 节点ID
     */
    @Getter
    @Setter
    private String id;

    /**
     * 节点名称
     */
    @Getter
    @Setter
    private String label;

    /**
     * 子节点
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @Getter
    @Setter
    private List<NavbarTreeSelect> children;

    public NavbarTreeSelect() {

    }

    public NavbarTreeSelect(WebNavbar category) {
        this.id = category.getId();
        this.label = category.getTitle();
        this.children = category.getChildren().stream().map(NavbarTreeSelect::new).collect(Collectors.toList());
    }

}
