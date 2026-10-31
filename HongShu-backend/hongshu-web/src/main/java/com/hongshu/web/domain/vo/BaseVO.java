package com.hongshu.web.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import com.hongshu.common.annotation.IdValid;
import com.hongshu.common.validator.group.Delete;
import com.hongshu.common.validator.group.Update;
import com.hongshu.web.domain.PageInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * BaseVO   view object 表现层 基类对象
 *
 * @author hongshu
 * 创建时间：: 2019-12-03-22:38
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "业务请求通用字段")
public class BaseVO<T> extends PageInfo<T> {

    /**
     * 唯一UID
     */
    @IdValid(groups = {Update.class, Delete.class})
    @Schema(description = "业务记录唯一 ID，具体含义以请求模型为准")
    private String uid;

    @Schema(description = "状态，具体枚举按所属业务解释")
    private Integer status;
}
