package com.hongshu.web.domain.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hongshu.web.domain.HongshuBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 导航栏分类
 *
 * @author hongshu
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("web_navbar")
@Schema(description = "内容分类导航")
public class WebNavbar extends HongshuBaseEntity {

    /**
     * 标题
     */
    @Schema(description = "标题")
    private String title;

    /**
     * 父级ID
     */
    @Schema(description = "父级ID")
    private String pid;

    /**
     * 分类描述
     */
    @Schema(description = "分类描述")
    private String description;

    /**
     * 喜欢数量
     */
    @Schema(description = "喜欢数量")
    private long likeCount;

    /**
     * 排序
     */
    @Schema(description = "排序")
    private Integer sort;

    /**
     * 状态
     */
    @Schema(description = "状态")
    private String status;

    /**
     * 分类封面
     */
    @Schema(description = "分类封面")
    private String normalCover;

    /**
     * 热门封面
     */
    @Schema(description = "热门封面")
    private String hotCover;

    /**
     * 子导航栏
     */
    @TableField(exist = false)
    @Schema(description = "子导航栏")
    private List<WebNavbar> children = new ArrayList<>();
}
