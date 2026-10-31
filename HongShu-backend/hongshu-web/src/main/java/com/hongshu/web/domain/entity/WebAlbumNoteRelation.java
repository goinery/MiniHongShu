package com.hongshu.web.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hongshu.web.domain.HongshuBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 专辑-笔记
 *
 * @author hongshu
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("web_album_note_relation")
public class WebAlbumNoteRelation extends HongshuBaseEntity {

    /**
     * 专辑ID
     */
    private Integer sort;

    private String aid;

    /**
     * 笔记ID
     */
    private String nid;

}
