package com.hongshu.web.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * ES-笔记
 *
 * @author hongshu
 */
@Data
@Schema(description = "笔记搜索条件（当前使用 MySQL）")
public class EsNoteDTO implements Serializable {

    @Schema(description = "搜索关键词")
    private String keyword;

    // 0是指全部 1是指点赞排序 2是指时间排序
    @Schema(description = "排序类型：1 按点赞数；0、2 或不传按时间排序")
    private Integer type;

    @Schema(description = "分类 ID")
    private String cid;

    @Schema(description = "父分类 ID")
    private String cpid;
}
