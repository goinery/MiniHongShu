package com.hongshu.system.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;

import lombok.Getter;
import lombok.Setter;

import com.hongshu.common.annotation.Excel;
import com.hongshu.common.annotation.Excel.ColumnType;
import com.hongshu.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 岗位表 sys_post
 *
 * @author hongshu
 */
@Schema(description = "岗位信息")
public class SysPost extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 岗位序号
     */
    @Excel(name = "岗位序号", cellType = ColumnType.NUMERIC)
    @Getter
    @Setter
    @Schema(description = "岗位序号")
    private Long postId;

    /**
     * 岗位编码
     */
    @Excel(name = "岗位编码")
    @Setter
    @Schema(description = "岗位编码")
    private String postCode;

    /**
     * 岗位名称
     */
    @Excel(name = "岗位名称")
    @Setter
    @Schema(description = "岗位名称")
    private String postName;

    /**
     * 岗位排序
     */
    @Excel(name = "岗位排序")
    @Setter
    @Schema(description = "岗位排序")
    private Integer postSort;

    /**
     * 状态（0正常 1停用）
     */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    @Getter
    @Setter
    @Schema(description = "状态（0正常 1停用）")
    private String status;

    /**
     * 用户是否存在此岗位标识 默认不存在
     */
    @Getter
    @Setter
    @Schema(description = "用户是否存在此岗位标识 默认不存在")
    private boolean flag = false;

    @NotBlank(message = "岗位编码不能为空")
    @Size(max = 64, message = "岗位编码长度不能超过64个字符")
    public String getPostCode() {
        return postCode;
    }

    @NotBlank(message = "岗位名称不能为空")
    @Size(max = 50, message = "岗位名称长度不能超过50个字符")
    public String getPostName() {
        return postName;
    }

    @NotNull(message = "显示顺序不能为空")
    public Integer getPostSort() {
        return postSort;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("postId", getPostId())
                .append("postCode", getPostCode())
                .append("postName", getPostName())
                .append("postSort", getPostSort())
                .append("status", getStatus())
                .append("createBy", getCreateBy())
                .append("createTime", getCreateTime())
                .append("updateBy", getUpdateBy())
                .append("updateTime", getUpdateTime())
                .append("remark", getRemark())
                .toString();
    }
}
