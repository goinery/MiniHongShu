package com.hongshu.web.service;

import cn.hutool.json.JSONUtil;
import com.hongshu.common.constant.ImConstant;
import com.hongshu.common.exception.web.HongshuException;
import com.hongshu.web.websocket.WebSocketSessionRegistry;
import com.hongshu.web.websocket.im.CountMessage;
import com.hongshu.web.websocket.im.Message;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 社交通知计数的唯一读写入口，保留现有 Redis JSON 结构并原子更新。
 */
@Service
public class ChatNotificationService {

    private static final int NOTIFICATION_MESSAGE_TYPE = 0;

    private static final DefaultRedisScript<String> UPDATE_COUNT_SCRIPT = new DefaultRedisScript<>("""
            local json = redis.call('GET', KEYS[1])
            local counts = json and cjson.decode(json) or {}
            counts.uid = ARGV[3]
            for _, field in ipairs({'likeOrCollectionCount', 'commentCount', 'followCount'}) do
                if type(counts[field]) ~= 'number' then counts[field] = 0 end
            end
            if ARGV[2] == 'increment' then
                counts[ARGV[1]] = counts[ARGV[1]] + 1
            else
                counts[ARGV[1]] = 0
            end
            local result = cjson.encode(counts)
            redis.call('SET', KEYS[1], result)
            return result
            """, String.class);

    private final StringRedisTemplate redisTemplate;
    private final WebSocketSessionRegistry connections;

    public ChatNotificationService(StringRedisTemplate redisTemplate, WebSocketSessionRegistry connections) {
        this.redisTemplate = redisTemplate;
        this.connections = connections;
    }

    public CountMessage getCount(String userId) {
        String json = redisTemplate.opsForValue().get(ImConstant.MESSAGE_COUNT_KEY + userId);
        return parseCount(json, userId);
    }

    public void incrementAndPush(String userId, Integer type) {
        CountMessage count = updateCount(userId, type, "increment");
        Message message = new Message();
        message.setAcceptUid(userId);
        message.setMsgType(NOTIFICATION_MESSAGE_TYPE);
        message.setContent(count);
        connections.sendInfo(message);
    }

    public void clearCount(String userId, Integer type) {
        updateCount(userId, type, "clear");
    }

    private CountMessage updateCount(String userId, Integer type, String operation) {
        if (StringUtils.isBlank(userId)) {
            throw new HongshuException("通知用户不能为空");
        }
        String field = countField(type);
        String json = redisTemplate.execute(UPDATE_COUNT_SCRIPT,
                List.of(ImConstant.MESSAGE_COUNT_KEY + userId), field, operation, userId);
        return parseCount(json, userId);
    }

    private String countField(Integer type) {
        if (type == null) {
            throw new HongshuException("通知类型不能为空");
        }
        return switch (type) {
            case 0 -> "likeOrCollectionCount";
            case 1 -> "commentCount";
            case 2 -> "followCount";
            default -> throw new HongshuException("不支持的通知类型");
        };
    }

    private CountMessage parseCount(String json, String userId) {
        CountMessage count = StringUtils.isBlank(json) ? null : JSONUtil.toBean(json, CountMessage.class);
        if (count == null) {
            count = new CountMessage();
        }
        count.setUid(userId);
        if (count.getLikeOrCollectionCount() == null) {
            count.setLikeOrCollectionCount(0L);
        }
        if (count.getCommentCount() == null) {
            count.setCommentCount(0L);
        }
        if (count.getFollowCount() == null) {
            count.setFollowCount(0L);
        }
        return count;
    }
}
