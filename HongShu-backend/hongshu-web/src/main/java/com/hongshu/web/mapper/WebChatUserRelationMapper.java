package com.hongshu.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hongshu.web.domain.entity.WebChatUserRelation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @author hongshu
 */
@Mapper
public interface WebChatUserRelationMapper extends BaseMapper<WebChatUserRelation> {

    /**
     * 更新会话摘要，并在数据库中原子增加或清零未读数量。
     */
    int updatePreview(@Param("id") String id, @Param("content") String content,
                      @Param("timestamp") long timestamp, @Param("unread") boolean unread);
}
