package com.hongshu.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hongshu.web.domain.dto.CommentReplySummary;
import com.hongshu.web.domain.entity.WebComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;

/**
 * @author hongshu
 */
@Mapper
public interface WebCommentMapper extends BaseMapper<WebComment> {

    /**
     * 每个根评论仅返回指定数量的回复，避免在应用层加载全部回复后截断。
     */
    List<WebComment> selectReplyPreviews(@Param("parentIds") Collection<String> parentIds,
                                         @Param("limit") int limit);

    /**
     * 调用方仅传入计数为空的根评论 ID，且必须保证集合非空。
     */
    List<CommentReplySummary> selectReplyCounts(@Param("parentIds") Collection<String> parentIds);

    /**
     * 原子调整评论点赞数，兼容历史空值并避免负数。
     */
    @Update("UPDATE web_comment SET like_count = GREATEST(0, COALESCE(like_count, 0) + #{delta}) WHERE id = #{commentId}")
    int adjustLikeCount(@Param("commentId") String commentId, @Param("delta") int delta);

    /**
     * 原子调整二级回复数，不覆盖评论内容。
     */
    @Update("UPDATE web_comment SET two_comment_count = GREATEST(0, COALESCE(two_comment_count, 0) + #{delta}) WHERE id = #{commentId}")
    int adjustReplyCount(@Param("commentId") String commentId, @Param("delta") int delta);
}
