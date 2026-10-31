package com.hongshu.system.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 用户和角色关联 sys_user_role
 *
 * @author hongshu
 */
@Schema(description = "用户角色关联")
public class SysUserRole {

    /**
     * 用户ID
     */
    @Getter
    @Setter
    @Schema(description = "用户ID")
    private Long userId;

    /**
     * 角色ID
     */
    @Getter
    @Setter
    @Schema(description = "角色ID")
    private Long roleId;

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("userId", getUserId())
                .append("roleId", getRoleId())
                .toString();
    }
}
