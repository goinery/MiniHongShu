package com.hongshu.web.domain.dto;

import com.hongshu.common.validator.group.AddGroup;
import com.hongshu.common.validator.group.UpdateGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.List;

/**
 * 笔记
 *
 * @author hongshu
 */
@Data
@Schema(description = "图文发布与编辑数据（以 noteData JSON 字符串提交）")
public class NoteDTO implements Serializable {

    @Schema(description = "修改时必填的笔记 ID，新增时忽略")
    @NotBlank(message = "标题不能为空", groups = UpdateGroup.class)
    private String id;

    @Schema(description = "笔记标题")
    @NotBlank(message = "标题不能为空", groups = AddGroup.class)
    private String title;

    @Schema(description = "笔记内容")
    @NotBlank(message = "内容不能为空", groups = AddGroup.class)
    private String content;

    @Schema(description = "笔记封面")
    private String noteCover;

    @Schema(description = "笔记封面高度")
    private Integer noteCoverHeight;

    @Schema(description = "笔记分类")
    @NotBlank(message = "二级分类不能为空", groups = AddGroup.class)
    private String cid;

    @Schema(description = "笔记父分类")
    @NotBlank(message = "一级分类不能为空", groups = AddGroup.class)
    private String cpid;

    @Schema(description = "已有图片地址数组，可混合外部链接与本人受管图片")
    private List<String> urls;

    @Schema(description = "笔记标签")
    private List<String> tagList;

    @Schema(description = "笔记图片数量")
    private Integer count;

    @Schema(description = "当前前台发布图文使用 1；2 为视频预留类型")
    private Integer type;

    @Schema(description = "可选专辑 ID，必须归当前作者所有")
    private String albumId;
}
