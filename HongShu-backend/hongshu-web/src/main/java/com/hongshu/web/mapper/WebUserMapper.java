package com.hongshu.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hongshu.web.domain.entity.WebUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * @author hongshu
 */
@Mapper
public interface WebUserMapper extends BaseMapper<WebUser> {

    /**
     * 原子递增发文量，避免覆盖用户并发更新的资料。
     *
     * @param userId 用户 ID
     * @return 更新行数
     */
    @Update("UPDATE web_user SET trend_count = COALESCE(trend_count, 0) + 1 WHERE id = #{userId}")
    int incrementTrendCount(@Param("userId") String userId);

    /**
     * 当前读并锁定用户行；多个用户必须按实际 ID 的字典顺序调用。
     *
     * @param userId 数据库返回的实际用户 ID
     * @return 当前用户状态，不存在时为空
     */
    @Select("SELECT id, status FROM web_user WHERE id = #{userId} FOR UPDATE")
    WebUser selectByIdForUpdate(@Param("userId") String userId);

    /**
     * 原子调整关注数量，保留用户资料及其他统计字段。
     *
     * @param userId 用户 ID
     * @param delta 关注时为 1，取消关注时为 -1
     * @return 更新行数
     */
    @Update("UPDATE web_user SET follower_count = GREATEST(0, COALESCE(follower_count, 0) + #{delta}) WHERE id = #{userId}")
    int adjustFollowerCount(@Param("userId") String userId, @Param("delta") int delta);

    /**
     * 原子调整粉丝数量，兼容历史空值并避免负数。
     *
     * @param userId 用户 ID
     * @param delta 新增粉丝时为 1，取消关注时为 -1
     * @return 更新行数
     */
    @Update("UPDATE web_user SET fan_count = GREATEST(0, COALESCE(fan_count, 0) + #{delta}) WHERE id = #{userId}")
    int adjustFanCount(@Param("userId") String userId, @Param("delta") int delta);
}
