package com.hongshu.web.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 点赞/收藏
 *
 * @author hongshu
 */
@Data
@Schema(description = "点赞收藏操作参数")
public class LikeOrCollectDTO implements Serializable {

    @Schema(description = "互动目标 ID，按 type 分别对应笔记、评论或专辑")
    private String likeOrCollectionId;

    @Schema(description = "兼容字段，目标发布者由后端查询资源后确定")
    private String publishUid;

    @Schema(description = "互动类型：1 笔记点赞，2 评论点赞，3 笔记收藏，4 专辑收藏")
    private Integer type;
}
