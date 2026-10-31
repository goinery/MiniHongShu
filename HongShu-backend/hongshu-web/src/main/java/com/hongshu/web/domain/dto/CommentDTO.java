package com.hongshu.web.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 评论
 *
 * @author hongshu
 */
@Data
@Schema(description = "评论及回复发布参数")
public class CommentDTO implements Serializable {

    @Schema(description = "所属笔记 ID")
    private String nid;

    @Schema(description = "兼容字段，笔记作者由后端根据笔记确定")
    private String noteUid;

    @Schema(description = "一级根评论 ID；为空或 0 时发表一级评论")
    private String pid;

    @Schema(description = "被回复评论 ID；二级回复时可指定同一根评论下的目标")
    private String replyId;

    @Schema(description = "兼容字段，接收用户由后端根据被回复评论确定")
    private String replyUid;

    @Schema(description = "兼容字段，评论层级由后端设置为 1 一级或 2 回复")
    private Integer level;

    @Schema(description = "评论内容，1 至 2000 字")
    private String content;
}
