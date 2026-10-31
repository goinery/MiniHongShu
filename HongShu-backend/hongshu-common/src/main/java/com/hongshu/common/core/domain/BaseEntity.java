package com.hongshu.common.core.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import lombok.Getter;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Entity基类
 *
 * @author hongshu
 */
@Schema(description = "系统实体通用字段")
public class BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 搜索值
     */
    @JsonIgnore
    @Getter
    @Setter
    @Schema(description = "搜索值")
    private String searchValue;

    /**
     * 创建者
     */
    @Getter
    @Setter
    @Schema(description = "创建者")
    private String createBy;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Getter
    @Setter
    @Schema(description = "创建时间")
    private Date createTime;

    /**
     * 更新者
     */
    @Getter
    @Setter
    @Schema(description = "更新者")
    private String updateBy;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Getter
    @Setter
    @Schema(description = "更新时间")
    private Date updateTime;

    /**
     * 备注
     */
    @Getter
    @Setter
    @Schema(description = "备注")
    private String remark;

    /**
     * 请求参数
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @Setter
    @Schema(description = "请求参数")
    private Map<String, Object> params;

    public Map<String, Object> getParams() {
        if (params == null) {
            params = new HashMap<>();
        }
        return params;
    }

}
