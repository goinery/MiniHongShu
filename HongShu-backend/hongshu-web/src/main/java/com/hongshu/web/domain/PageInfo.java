package com.hongshu.web.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * PageVO  用于分页
 *
 * @author hongshu
 * 创建时间： 2019-12-03-22:38
 */
@Data
@Schema(description = "业务分页条件")
public class PageInfo<T> {

    /**
     * 关键字
     */
    @Schema(description = "关键字")
    private String keyword;

    /**
     * 当前页
     */
    @Schema(description = "当前页")
    private Long currentPage;

    /**
     * 页大小
     */
    @Schema(description = "页大小")
    private Long pageSize;
}
