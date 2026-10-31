package com.hongshu.web.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hongshu.common.exception.web.HongshuException;
import com.hongshu.web.domain.entity.WebFollow;
import com.hongshu.web.domain.entity.WebNote;
import com.hongshu.web.domain.entity.WebUser;
import com.hongshu.web.domain.vo.FollowVO;
import com.hongshu.web.domain.vo.TrendVO;
import com.hongshu.web.mapper.WebFollowMapper;
import com.hongshu.web.mapper.WebNoteMapper;
import com.hongshu.web.mapper.WebUserMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 关注关系、关注动态和关注通知的只读查询。
 *
 * @author hongshu
 */
@Service
public class FollowQueryService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final String ACTIVE_NOTE_STATUS = "1";
    private static final String APPROVED_AUDIT_STATUS = "1";
    private static final String VIDEO_NOTE_TYPE = "2";
    private static final String FOLLOWED_AUTHOR_EXISTS =
            "EXISTS (SELECT 1 FROM web_follow f WHERE f.uid = {0} AND f.fid = web_note.uid)";

    private final WebFollowMapper followMapper;
    private final WebNoteMapper noteMapper;
    private final WebUserMapper userMapper;
    private final FollowTrendAssembler trendAssembler;

    public FollowQueryService(WebFollowMapper followMapper, WebNoteMapper noteMapper,
                              WebUserMapper userMapper, FollowTrendAssembler trendAssembler) {
        this.followMapper = followMapper;
        this.noteMapper = noteMapper;
        this.userMapper = userMapper;
        this.trendAssembler = trendAssembler;
    }

    /**
     * 查询本人及关注用户的已审核图文动态。
     *
     * @param userId      当前用户 ID
     * @param currentPage 页码
     * @param pageSize    每页数量
     * @return 动态分页
     */
    public Page<TrendVO> getTimeline(String userId, long currentPage, long pageSize) {
        Page<WebNote> page = newPage(currentPage, pageSize);
        if (StringUtils.isBlank(userId)) {
            return emptyResult(page);
        }
        QueryWrapper<WebNote> query = new QueryWrapper<WebNote>()
                .eq("status", ACTIVE_NOTE_STATUS).eq("audit_status", APPROVED_AUDIT_STATUS)
                .and(condition -> condition.ne("note_type", VIDEO_NOTE_TYPE).or().isNull("note_type"))
                .and(condition -> condition.eq("uid", userId).or().apply(FOLLOWED_AUTHOR_EXISTS, userId))
                .orderByDesc("update_time", "id");
        noteMapper.selectPage(page, query);
        return toTrendPage(page, userId, FollowTrendAssembler.ViewMode.TIMELINE);
    }

    /**
     * 保留关注列表的随机推荐顺序和最多四张预览图片。
     *
     * @param userId      当前用户 ID
     * @param currentPage 页码
     * @param pageSize    每页数量
     * @return 关注笔记分页
     */
    public Page<TrendVO> getPreview(String userId, long currentPage, long pageSize) {
        Page<WebNote> page = newPage(currentPage, pageSize);
        if (StringUtils.isBlank(userId)) {
            return emptyResult(page);
        }
        QueryWrapper<WebNote> query = new QueryWrapper<WebNote>()
                .eq("audit_status", APPROVED_AUDIT_STATUS).apply(FOLLOWED_AUTHOR_EXISTS, userId)
                .last("ORDER BY RAND()");
        noteMapper.selectPage(page, query);
        return toTrendPage(page, userId, FollowTrendAssembler.ViewMode.PREVIEW);
    }

    /**
     * 判断关注关系；游客、空目标和本人均返回未关注。
     *
     * @param userId         当前用户 ID
     * @param followedUserId 目标用户 ID
     * @return 是否关注
     */
    public boolean isFollowing(String userId, String followedUserId) {
        if (StringUtils.isBlank(userId) || StringUtils.isBlank(followedUserId) || userId.equals(followedUserId)) {
            return false;
        }
        return followMapper.selectCount(new QueryWrapper<WebFollow>()
                .eq("uid", userId).eq("fid", followedUserId)) > 0;
    }

    /**
     * 查询指定主页的关注或粉丝，只返回列表展示所需的公开资料。
     */
    public Page<FollowVO> getUserRelations(String userId, String type, long currentPage, long pageSize, String viewerId) {
        if (!"following".equals(type) && !"fans".equals(type)) {
            throw new HongshuException("关系类型无效");
        }
        if (StringUtils.isBlank(userId) || userMapper.selectById(userId) == null) {
            throw new HongshuException("用户不存在", 404);
        }
        Page<WebUser> page = newPage(currentPage, pageSize);
        String relation = "following".equals(type)
                ? "EXISTS (SELECT 1 FROM web_follow f WHERE f.uid = {0} AND f.fid = web_user.id)"
                : "EXISTS (SELECT 1 FROM web_follow f WHERE f.fid = {0} AND f.uid = web_user.id)";
        // 从用户表分页，避免历史重复关系或已删除用户造成重复项、空页。
        userMapper.selectPage(page, new QueryWrapper<WebUser>()
                .select("id", "username", "avatar", "hs_id", "fan_count")
                .ne("id", userId).apply(relation, userId).orderByDesc("id"));
        Page<FollowVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        Set<String> userIds = page.getRecords().stream().map(WebUser::getId).collect(Collectors.toSet());
        Set<String> followedIds = StringUtils.isBlank(viewerId) || userIds.isEmpty() ? Set.of()
                : followMapper.selectList(new QueryWrapper<WebFollow>()
                        .select("fid").eq("uid", viewerId).ne("fid", viewerId).in("fid", userIds))
                        .stream().map(WebFollow::getFid).collect(Collectors.toSet());
        result.setRecords(page.getRecords().stream().map(user -> new FollowVO()
                .setUid(user.getId()).setUsername(user.getUsername()).setAvatar(user.getAvatar())
                .setHsId(user.getHsId()).setFanCount(user.getFanCount())
                .setIsFollow(followedIds.contains(user.getId()))).collect(Collectors.toList()));
        return result;
    }

    /**
     * 查询关注通知，回关状态只读取本页出现的用户。
     *
     * @param userId      当前用户 ID
     * @param currentPage 页码
     * @param pageSize    每页数量
     * @return 关注通知分页
     */
    public Page<FollowVO> getNotices(String userId, long currentPage, long pageSize) {
        Page<WebFollow> page = newPage(currentPage, pageSize);
        Page<FollowVO> result = emptyResult(page);
        if (StringUtils.isBlank(userId)) {
            return result;
        }
        followMapper.selectPage(page, new QueryWrapper<WebFollow>()
                .eq("fid", userId).ne("uid", userId).orderByDesc("create_time", "id"));
        result.setTotal(page.getTotal());
        List<WebFollow> records = page.getRecords();
        Set<String> userIds = records.stream().map(WebFollow::getUid)
                .filter(StringUtils::isNotBlank).collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return result;
        }
        Map<String, WebUser> users = userMapper.selectByIds(userIds).stream()
                .collect(Collectors.toMap(WebUser::getId, user -> user));
        Set<String> followedIds = followMapper.selectList(new QueryWrapper<WebFollow>()
                        .select("fid").eq("uid", userId).in("fid", userIds))
                .stream().map(WebFollow::getFid).collect(Collectors.toSet());
        List<FollowVO> notices = new ArrayList<>(records.size());
        for (WebFollow relation : records) {
            WebUser user = users.get(relation.getUid());
            if (user == null) {
                continue;
            }
            FollowVO view = new FollowVO();
            view.setUid(user.getId()).setUsername(user.getUsername()).setAvatar(user.getAvatar())
                    .setTime(relation.getCreateTime() == null ? 0L : relation.getCreateTime().getTime())
                    .setIsFollow(followedIds.contains(relation.getUid()));
            notices.add(view);
        }
        result.setRecords(notices);
        return result;
    }

    private Page<TrendVO> toTrendPage(Page<WebNote> page, String userId, FollowTrendAssembler.ViewMode mode) {
        Page<TrendVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(trendAssembler.assemble(page.getRecords(), userId, mode));
        return result;
    }

    private <T> Page<T> newPage(long currentPage, long pageSize) {
        long current = Math.max(1, currentPage);
        long size = Math.clamp(pageSize, 1, MAX_PAGE_SIZE);
        if (current - 1 > Long.MAX_VALUE / size) {
            throw new HongshuException("分页参数超出范围");
        }
        return new Page<>(current, size);
    }

    private <T> Page<T> emptyResult(Page<?> source) {
        Page<T> result = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        result.setRecords(new ArrayList<>());
        return result;
    }
}
