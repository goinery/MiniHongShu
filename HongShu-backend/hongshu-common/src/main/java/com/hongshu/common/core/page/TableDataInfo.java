package com.hongshu.common.core.page;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * 表格分页数据对象
 *
 * @author hongshu
 */
@Schema(description = "分页查询响应")
public class TableDataInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 总记录数
     */
    @Getter
    @Setter
    @Schema(description = "总记录数")
    private long total;

    /**
     * 列表数据
     */
    @Getter
    @Setter
    @Schema(description = "列表数据")
    private List<?> rows;

    /**
     * 消息状态码
     */
    @Getter
    @Setter
    @Schema(description = "消息状态码")
    private int code;

    /**
     * 消息内容
     */
    @Getter
    @Setter
    @Schema(description = "消息内容")
    private String msg;

    /**
     * 表格数据对象
     */
    public TableDataInfo() {
    }

    /**
     * 分页
     *
     * @param list  列表数据
     * @param total 总记录数
     */
    public TableDataInfo(List<?> list, int total) {
        this.rows = list;
        this.total = total;
    }

}
