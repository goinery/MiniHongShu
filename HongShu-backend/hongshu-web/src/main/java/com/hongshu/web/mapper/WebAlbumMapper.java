package com.hongshu.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hongshu.web.domain.entity.WebAlbum;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * @author hongshu
 */
@Mapper
public interface WebAlbumMapper extends BaseMapper<WebAlbum> {

    /** 原子调整专辑收藏数，避免覆盖并发编辑的专辑信息。 */
    @Update("UPDATE web_album SET collection_count = GREATEST(0, COALESCE(collection_count, 0) + #{delta}) WHERE id = #{albumId}")
    int adjustCollectionCount(@Param("albumId") String albumId, @Param("delta") int delta);

    /** 仅重算专辑内容数量，保留专辑的其他字段。 */
    @Update("UPDATE web_album SET img_count = (SELECT COUNT(*) FROM web_album_note_relation WHERE aid = #{albumId}) WHERE id = #{albumId}")
    int refreshImageCount(@Param("albumId") String albumId);
}
