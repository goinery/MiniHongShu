package com.hongshu.common.core.page;

import lombok.Getter;
import lombok.Setter;

import com.hongshu.common.utils.StringUtils;

/**
 * 分页数据
 *
 * @author hongshu
 */
public class PageDomain {

    /**
     * 当前记录起始索引
     */
    @Getter
    @Setter
    private Integer pageNum;

    /**
     * 每页显示记录数
     */
    @Getter
    @Setter
    private Integer pageSize;

    /**
     * 排序列
     */
    @Getter
    @Setter
    private String orderByColumn;

    /**
     * 排序的方向desc或者asc
     */
    @Getter
    private String isAsc = "asc";

    /**
     * 分页参数合理化
     */
    @Setter
    @SuppressWarnings("FieldCanBeLocal") // 请求分页参数通过 Lombok 生成的 setter 绑定。
    private Boolean reasonable = true;

    public String getOrderBy() {
        if (StringUtils.isEmpty(orderByColumn)) {
            return "";
        }
        return StringUtils.toUnderScoreCase(orderByColumn) + " " + isAsc;
    }

    public void setIsAsc(String isAsc) {
        if (StringUtils.isNotEmpty(isAsc)) {
            // 兼容前端排序类型
            if ("ascending".equals(isAsc)) {
                isAsc = "asc";
            } else if ("descending".equals(isAsc)) {
                isAsc = "desc";
            }
            this.isAsc = isAsc;
        }
    }

    public Boolean getReasonable() {
        if (StringUtils.isNull(reasonable)) {
            return Boolean.TRUE;
        }
        return reasonable;
    }

}
