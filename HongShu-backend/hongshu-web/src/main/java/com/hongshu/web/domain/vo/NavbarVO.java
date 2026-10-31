package com.hongshu.web.domain.vo;

import com.hongshu.common.utils.TreeNode;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 分类
 *
 * @author hongshu
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class NavbarVO extends TreeNode<NavbarVO> implements Serializable {

    private String title;
    private long likeCount;
}
