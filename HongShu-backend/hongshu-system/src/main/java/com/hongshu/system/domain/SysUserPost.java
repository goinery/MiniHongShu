package com.hongshu.system.domain;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 用户和岗位关联 sys_user_post
 *
 * @author hongshu
 */
public class SysUserPost {

    /**
     * 用户ID
     */
    @Getter
    @Setter
    private Long userId;

    /**
     * 岗位ID
     */
    @Getter
    @Setter
    private Long postId;

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("userId", getUserId())
                .append("postId", getPostId())
                .toString();
    }
}
