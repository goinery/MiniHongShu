package com.hongshu.web.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * ES-笔记
 *
 * @author hongshu
 */
@Data
@Schema(description = "搜索记录条件")
public class EsRecordDTO implements Serializable {

    @Schema(description = "搜索关键词")
    private String keyword;

    @Schema(description = "兼容用户 ID，实际搜索记录始终归属当前认证会员")
    private String uid;

}
