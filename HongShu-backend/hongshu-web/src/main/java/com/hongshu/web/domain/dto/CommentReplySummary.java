package com.hongshu.web.domain.dto;

import lombok.Data;

/** 根评论的实际回复数量，仅用于兼容历史空计数。 */
@Data
public class CommentReplySummary {

    private String pid;

    private Long replyCount;
}
