package com.hongshu.web.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hongshu.common.constant.HttpStatus;
import com.hongshu.common.enums.ResultCodeEnum;
import com.hongshu.common.exception.web.HongshuException;
import com.hongshu.web.domain.entity.WebFollow;
import com.hongshu.web.domain.entity.WebUser;
import com.hongshu.web.mapper.WebFollowMapper;
import com.hongshu.web.mapper.WebUserMapper;
import com.hongshu.web.websocket.im.ChatUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 关注关系写入及双方统计更新，统一事务与数据库锁的边界。
 *
 * @author hongshu
 */
@Service
public class FollowCommandService {

    private static final String ENABLED_STATUS = "0";
    private static final int FOLLOW_NOTIFICATION_TYPE = 2;
    private static final int FOLLOW_INCREMENT = 1;
    private static final int FOLLOW_DECREMENT = -1;

    private final WebFollowMapper followMapper;
    private final WebUserMapper userMapper;
    private final ChatUtils notifications;

    public FollowCommandService(WebFollowMapper followMapper, WebUserMapper userMapper, ChatUtils notifications) {
        this.followMapper = followMapper;
        this.userMapper = userMapper;
        this.notifications = notifications;
    }

    /**
     * 切换关注状态；同一用户对的并发请求按数据库锁顺序串行执行。
     *
     * @param currentUserId 已认证的当前用户 ID
     * @param followedUserId 被关注者 ID
     */
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public void toggle(String currentUserId, String followedUserId) {
        if (StringUtils.isBlank(currentUserId)) {
            throw new HongshuException(ResultCodeEnum.TOKEN_FAIL);
        }
        if (StringUtils.isBlank(followedUserId)) {
            throw new HongshuException("请选择关注用户");
        }
        // 先解析实际主键，避免数据库 ID 归一后，同一行出现不同的加锁顺序或绕过自关注校验。
        WebUser currentUser = requireExisting(currentUserId);
        WebUser followedUser = requireExisting(followedUserId);
        String actorId = currentUser.getId();
        String targetId = followedUser.getId();
        if (actorId.equals(targetId)) {
            throw new HongshuException("不能关注自己");
        }
        if (actorId.compareTo(targetId) < 0) {
            currentUser = lockUser(actorId);
            followedUser = lockUser(targetId);
        } else {
            followedUser = lockUser(targetId);
            currentUser = lockUser(actorId);
        }
        requireEnabled(currentUser);

        // FOR UPDATE 使用当前读，不使用加锁前普通查询建立的事务快照。
        List<String> relationIds = followMapper.selectRelationIdsForUpdate(actorId, targetId);
        if (!relationIds.isEmpty()) {
            followMapper.delete(new QueryWrapper<WebFollow>().eq("uid", actorId).eq("fid", targetId));
            updateCounts(actorId, targetId, FOLLOW_DECREMENT);
            return;
        }
        requireEnabled(followedUser);
        Date now = new Date();
        WebFollow relation = new WebFollow();
        relation.setUid(actorId);
        relation.setFid(targetId);
        relation.setCreateTime(now);
        relation.setUpdateTime(now);
        followMapper.insert(relation);
        updateCounts(actorId, targetId, FOLLOW_INCREMENT);
        notifications.sendMessage(targetId, FOLLOW_NOTIFICATION_TYPE);
    }

    private WebUser requireExisting(String userId) {
        WebUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new HongshuException("用户不存在", HttpStatus.NOT_FOUND);
        }
        return user;
    }

    private WebUser lockUser(String userId) {
        WebUser user = userMapper.selectByIdForUpdate(userId);
        if (user == null) {
            throw new HongshuException("用户不存在", HttpStatus.NOT_FOUND);
        }
        return user;
    }

    private void requireEnabled(WebUser user) {
        if (StringUtils.isNotBlank(user.getStatus()) && !ENABLED_STATUS.equals(user.getStatus())) {
            throw new HongshuException("用户不可用", HttpStatus.FORBIDDEN);
        }
    }

    private void updateCounts(String actorId, String targetId, int delta) {
        userMapper.adjustFollowerCount(actorId, delta);
        userMapper.adjustFanCount(targetId, delta);
    }
}
