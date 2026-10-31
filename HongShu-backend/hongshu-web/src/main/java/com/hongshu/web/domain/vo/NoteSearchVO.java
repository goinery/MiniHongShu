package com.hongshu.web.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * ES-笔记
 *
 * @author hongshu
 */
@Data
@Accessors(chain = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "笔记列表与搜索结果")
public class NoteSearchVO implements Serializable {

    @Schema(description = "主键 ID")
    private String id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "正文内容")
    private String content;

    @Schema(description = "笔记封面图片地址")
    private String noteCover;

    @Schema(description = "笔记类型：1 图文，兼容历史图文值 0；2 视频预留")
    private String noteType;

    @Schema(description = "笔记封面高度")
    private Integer noteCoverHeight;

    @Schema(description = "分类 ID")
    private String cid;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "父分类 ID")
    private String cpid;

    @Schema(description = "父分类名称")
    private String categoryParentName;

    @Schema(description = "用户 ID")
    private String uid;

    @Schema(description = "用户名或昵称")
    private String username;

    @Schema(description = "头像访问地址")
    private String avatar;

    @Schema(description = "图片地址列表的 JSON 字符串")
    private String urls;

    @Schema(description = "标签数据")
    private String tags;

    @Schema(description = "置顶状态：0 未置顶，1 已置顶")
    private String pinned;

    @Schema(description = "审核状态：0 待审核，1 通过，2 拒绝")
    private String auditStatus;

    @Schema(description = "状态，具体枚举按所属业务解释")
    private String status;

    @Schema(description = "当前用户是否已点赞")
    private Boolean isLike;

    @Schema(description = "点赞数量")
    private Long likeCount;

    @Schema(description = "浏览数量")
    private Long viewCount;

    @Schema(description = "时间戳，单位毫秒")
    private Long time;

    @Schema(description = "前端列表加载状态标记")
    private Boolean isLoading;


}
