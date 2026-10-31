package com.hongshu.common.core.domain;

import java.io.Serial;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.spring.activerecord.Model;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.hongshu.common.enums.EStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * Entity基类
 *
 * @author hongshu
 */
@Data
// Model 仅提供 ActiveRecord 操作和 transient 实体类型元数据，其 Object 相等语义不参与实体值比较。
@EqualsAndHashCode(callSuper = false)
public class SuperEntity<T extends SuperEntity<T>> extends Model<T> {

    @Serial
    private static final long serialVersionUID = -4851055162892178225L;

    /**
     * 唯一UID
     */
    @TableId(value = "uid", type = IdType.ASSIGN_UUID)
    private String uid;

    /**
     * 状态 0：失效  1：生效
     */
    private int status;

    /**
     * {@code @TableField} 配置需要填充的字段
     * 创建时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date createTime;

    /**
     * 更新时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    public SuperEntity() {
        this.status = EStatus.ENABLE;
        this.createTime = new Date();
        this.updateTime = new Date();
    }
}
