package com.hongshu.web.domain.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hongshu.web.domain.HongshuBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 专辑
 *
 * @author hongshu
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("web_album")
@Schema(description = "社区专辑")
public class WebAlbum extends HongshuBaseEntity {

    /**
     * 标题
     */
    @Schema(description = "标题")
    private String title;

    /**
     * 用户ID
     */
    @Schema(description = "用户ID")
    private String uid;

    /**
     * 专辑封面图
     */
    @Schema(description = "专辑封面图")
    private String albumCover;

    /**
     * 专辑类型（0：默认）
     */
    @Schema(description = "专辑类型（0：默认）")
    private Integer type;

    /**
     * 排序
     */
    @Schema(description = "排序")
    private Integer sort;

    /**
     * 专辑中笔记数量
     */
    @Schema(description = "专辑中笔记数量")
    private Long imgCount;

    /**
     * 收藏数量
     */
    @Schema(description = "收藏数量")
    private Long collectionCount;

}
