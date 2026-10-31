package com.hongshu.web.service;

import com.hongshu.common.exception.web.HongshuException;
import com.hongshu.web.auth.AuthContextHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FeedbackService {
    private final JdbcTemplate jdbc;

    public FeedbackService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void submit(String email, String content) {
        String uid = AuthContextHolder.getUserId();
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM web_feedback WHERE uid = ? AND create_time > NOW() - INTERVAL 1 DAY", Long.class, uid);
        if (count != null && count >= 20) throw new HongshuException("今日反馈次数已达上限，请稍后再试");
        jdbc.update("INSERT INTO web_feedback (uid, email, content, status, create_time) VALUES (?, ?, ?, '0', NOW())", uid, email.trim(), content.trim());
    }

    public Map<String, Object> list(int pageNum, int pageSize, String status) {
        int size = Math.clamp(pageSize, 1, 100);
        long offset = (Math.max(1L, pageNum) - 1) * size;
        String where = "0".equals(status) || "1".equals(status) ? " WHERE f.status = ?" : "";
        List<Object> params = new ArrayList<>();
        if (!where.isEmpty()) params.add(status);
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM web_feedback f" + where, Long.class, params.toArray());
        params.add(size);
        params.add(offset);
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT CAST(f.id AS CHAR) AS id, f.uid, u.username, f.email, f.content, f.status, f.resolution, DATE_FORMAT(f.create_time, '%Y-%m-%d %H:%i:%s') AS createTime, DATE_FORMAT(f.update_time, '%Y-%m-%d %H:%i:%s') AS updateTime FROM web_feedback f LEFT JOIN web_user u ON f.uid = u.id" + where + " ORDER BY f.id DESC LIMIT ? OFFSET ?", params.toArray());
        return Map.of("rows", rows, "total", total == null ? 0 : total);
    }

    public void resolve(long id, String status, String resolution) {
        if (!"0".equals(status) && !"1".equals(status)) throw new HongshuException("处理状态无效");
        if (jdbc.update("UPDATE web_feedback SET status = ?, resolution = ?, update_time = NOW() WHERE id = ?", status, resolution, id) == 0) {
            throw new HongshuException("反馈不存在", 404);
        }
    }
}
