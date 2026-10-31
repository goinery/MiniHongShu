package com.hongshu.web.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.hongshu.web.auth.AuthContextHolder;
import com.hongshu.web.domain.entity.WebFollow;
import com.hongshu.web.domain.vo.FollowVO;
import com.hongshu.web.domain.vo.TrendVO;
import com.hongshu.web.mapper.WebFollowMapper;
import com.hongshu.web.service.FollowCommandService;
import com.hongshu.web.service.FollowQueryService;
import com.hongshu.web.service.IWebFollowService;
import org.springframework.stereotype.Service;

/**
 * 关注业务兼容入口，查询和事务写入分别交由专职服务处理。
 *
 * @author hongshu
 */
@Service
public class WebFollowServiceImpl extends ServiceImpl<WebFollowMapper, WebFollow> implements IWebFollowService {

    private final FollowCommandService commands;
    private final FollowQueryService queries;

    public WebFollowServiceImpl(FollowCommandService commands, FollowQueryService queries) {
        this.commands = commands;
        this.queries = queries;
    }

    @Override
    public Page<TrendVO> getFollowTrend(long currentPage, long pageSize) {
        return queries.getTimeline(AuthContextHolder.getUserId(), currentPage, pageSize);
    }

    @Override
    public Page<TrendVO> getFollowList(long currentPage, long pageSize) {
        return queries.getPreview(AuthContextHolder.getUserId(), currentPage, pageSize);
    }

    @Override
    public Page<FollowVO> getUserRelations(String userId, String type, long currentPage, long pageSize) {
        return queries.getUserRelations(userId, type, currentPage, pageSize, AuthContextHolder.getUserId());
    }

    @Override
    public void followById(String followerId) {
        commands.toggle(AuthContextHolder.getUserId(), followerId);
    }

    @Override
    public boolean isFollow(String followerId) {
        return queries.isFollowing(AuthContextHolder.getUserId(), followerId);
    }

    @Override
    public Page<FollowVO> getNoticeFollower(long currentPage, long pageSize) {
        return queries.getNotices(AuthContextHolder.getUserId(), currentPage, pageSize);
    }
}
