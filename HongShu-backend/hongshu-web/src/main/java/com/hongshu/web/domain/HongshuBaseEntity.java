package com.hongshu.web.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 基础实体类
 *
 * @author hongshu
 */
@Schema(description = "基础实体类")
@Data
public class HongshuBaseEntity implements Serializable {

    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private String id;

    @Schema(description = "创建用户")
    @TableField(fill = FieldFill.INSERT)
    private String creator;

    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @Schema(description = "修改用户")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updater;

    @Schema(description = "修改时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
