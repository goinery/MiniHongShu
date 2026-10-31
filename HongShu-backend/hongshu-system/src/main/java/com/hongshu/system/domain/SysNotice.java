package com.hongshu.system.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;

import lombok.Getter;
import lombok.Setter;

import com.hongshu.common.core.domain.BaseEntity;
import com.hongshu.common.xss.Xss;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 通知公告表 sys_notice
 *
 * @author hongshu
 */
@Schema(description = "通知公告")
public class SysNotice extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 公告ID
     */
    @Getter
    @Setter
    @Schema(description = "公告ID")
    private Long noticeId;

    /**
     * 公告标题
     */
    @Setter
    @Schema(description = "公告标题")
    private String noticeTitle;

    /**
     * 公告类型（1通知 2公告）
     */
    @Getter
    @Setter
    @Schema(description = "公告类型（1通知 2公告）")
    private String noticeType;

    /**
     * 公告内容
     */
    @Getter
    @Setter
    @Schema(description = "公告内容")
    private String noticeContent;

    /**
     * 公告状态（0正常 1关闭）
     */
    @Getter
    @Setter
    @Schema(description = "公告状态（0正常 1关闭）")
    private String status;

    @Xss(message = "公告标题不能包含脚本字符")
    @NotBlank(message = "公告标题不能为空")
    @Size(max = 50, message = "公告标题不能超过50个字符")
    public String getNoticeTitle() {
        return noticeTitle;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("noticeId", getNoticeId())
                .append("noticeTitle", getNoticeTitle())
                .append("noticeType", getNoticeType())
                .append("noticeContent", getNoticeContent())
                .append("status", getStatus())
                .append("createBy", getCreateBy())
                .append("createTime", getCreateTime())
                .append("updateBy", getUpdateBy())
                .append("updateTime", getUpdateTime())
                .append("remark", getRemark())
                .toString();
    }
}
