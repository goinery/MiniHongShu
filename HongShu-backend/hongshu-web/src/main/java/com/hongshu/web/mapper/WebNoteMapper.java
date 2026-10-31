package com.hongshu.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hongshu.web.domain.entity.WebNote;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * @author hongshu
 */
@Mapper
public interface WebNoteMapper extends BaseMapper<WebNote> {

    /**
     * 原子递增浏览量，不修改笔记内容和编辑时间。
     *
     * @param noteId 笔记 ID
     * @return 更新行数
     */
    @Update("UPDATE web_note SET view_count = COALESCE(view_count, 0) + 1 WHERE id = #{noteId}")
    int incrementViewCount(@Param("noteId") String noteId);

    /** 原子调整点赞数，兼容历史空值并避免负数。 */
    @Update("UPDATE web_note SET like_count = GREATEST(0, COALESCE(like_count, 0) + #{delta}) WHERE id = #{noteId}")
    int adjustLikeCount(@Param("noteId") String noteId, @Param("delta") int delta);

    /** 原子调整收藏数，不覆盖笔记内容。 */
    @Update("UPDATE web_note SET collection_count = GREATEST(0, COALESCE(collection_count, 0) + #{delta}) WHERE id = #{noteId}")
    int adjustCollectionCount(@Param("noteId") String noteId, @Param("delta") int delta);

    /** 原子调整评论数，不改变笔记编辑时间。 */
    @Update("UPDATE web_note SET comment_count = GREATEST(0, COALESCE(comment_count, 0) + #{delta}) WHERE id = #{noteId}")
    int adjustCommentCount(@Param("noteId") String noteId, @Param("delta") int delta);
}
