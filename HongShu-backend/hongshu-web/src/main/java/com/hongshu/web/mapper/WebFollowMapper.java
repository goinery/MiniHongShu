package com.hongshu.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hongshu.web.domain.entity.WebFollow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @author hongshu
 */
@Mapper
public interface WebFollowMapper extends BaseMapper<WebFollow> {

    /**
     * 对关注关系执行当前读，须先按固定顺序锁定双方用户行。
     *
     * @param userId 关注者的实际用户 ID
     * @param followedUserId 被关注者的实际用户 ID
     * @return 已有关注关系 ID，兼容历史重复记录
     */
    @Select("SELECT id FROM web_follow WHERE uid = #{userId} AND fid = #{followedUserId} FOR UPDATE")
    List<String> selectRelationIdsForUpdate(@Param("userId") String userId,
                                          @Param("followedUserId") String followedUserId);
}
