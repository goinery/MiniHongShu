package com.hongshu.web.domain.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hongshu.web.domain.HongshuBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 标签
 *
 * @author hongshu
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("web_tag")
@Schema(description = "内容标签")
public class WebTag extends HongshuBaseEntity {

    /**
     * 使用次数
     */
    @Schema(description = "使用次数")
    private Long likeCount;

    /**
     * 标题
     */
    @Schema(description = "标题")
    private String title;

    /**
     * 排序
     */
    @Schema(description = "排序")
    private Integer sort;
}
