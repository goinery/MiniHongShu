package com.hongshu.common.core.domain;

import java.io.Serial;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Tree基类
 *
 * @author hongshu
 */
public class TreeEntity extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 父菜单名称
     */
    @Getter
    @Setter
    private String parentName;

    /**
     * 父菜单ID
     */
    @Getter
    @Setter
    private Long parentId;

    /**
     * 显示顺序
     */
    @Getter
    @Setter
    private Integer orderNum;

    /**
     * 祖级列表
     */
    @Getter
    @Setter
    private String ancestors;

    /**
     * 子部门
     */
    @Getter
    @Setter
    private List<?> children = new ArrayList<>();

}
