/**
 * Copyright (c) 2018 人人开源 All rights reserved.
 * <p>
 * https://www.renren.io
 * <p>
 * 版权所有，侵权必究！
 */

package com.hongshu.common.utils;

import java.io.Serial;

import lombok.Getter;
import lombok.Setter;

import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 树节点，所有需要实现树节点的，都需要继承该类
 *
 * @author hongshu
 */
@EqualsAndHashCode
public class TreeNode<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @Getter
    @Setter
    private String id;
    /**
     * 上级ID
     */
    @Getter
    @Setter
    private String pid;
    /**
     * 子节点列表
     */
    @Getter
    @Setter
    private List<T> children = new ArrayList<>();

}
