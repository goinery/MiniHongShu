package com.hongshu.web.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/** 上传登记及文件引用查询，集中管理文件生命周期所需的数据访问。 */
@Repository
public class FileAssetRepository {

    private final JdbcTemplate jdbcTemplate;

    public FileAssetRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void register(String path, String owner, int storageType) {
        jdbcTemplate.update("INSERT INTO web_upload (path, uid, storage_type, create_time) VALUES (?, ?, ?, NOW())",
                path, owner, storageType);
    }

    public void assignOwner(String path, String owner, String previousOwner) {
        jdbcTemplate.update("UPDATE web_upload SET uid = ? WHERE path = ? AND uid = ?",
                owner, path, previousOwner);
    }

    public List<String> findOwners(String path) {
        return jdbcTemplate.queryForList("SELECT uid FROM web_upload WHERE path = ?", String.class, path);
    }

    public List<Integer> findStorageTypes(String path) {
        return jdbcTemplate.queryForList("SELECT storage_type FROM web_upload WHERE path = ?", Integer.class, path);
    }

    public List<String> findPathsInContent(String content) {
        return jdbcTemplate.queryForList("SELECT path FROM web_upload WHERE INSTR(?, path) > 0", String.class, content);
    }

    public void delete(String path) {
        jdbcTemplate.update("DELETE FROM web_upload WHERE path = ?", path);
    }

    /** 只判断是否存在引用，避免统计所有匹配记录。 */
    public boolean isReferenced(String path) {
        Boolean referenced = jdbcTemplate.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM web_note
                              WHERE INSTR(COALESCE(urls, ''), ?) > 0
                                 OR INSTR(COALESCE(note_cover, ''), ?) > 0
                                 OR INSTR(COALESCE(content, ''), ?) > 0)
                    OR EXISTS(SELECT 1 FROM web_user
                              WHERE INSTR(COALESCE(avatar, ''), ?) > 0)
                    OR EXISTS(SELECT 1 FROM web_album WHERE INSTR(COALESCE(album_cover, ''), ?) > 0)
                    OR EXISTS(SELECT 1 FROM web_chat WHERE msg_type = 2 AND INSTR(COALESCE(content, ''), ?) > 0)
                    OR EXISTS(SELECT 1 FROM web_navbar
                              WHERE INSTR(COALESCE(normal_cover, ''), ?) > 0
                                 OR INSTR(COALESCE(hot_cover, ''), ?) > 0)
                    OR EXISTS(SELECT 1 FROM sys_user WHERE INSTR(COALESCE(avatar, ''), ?) > 0)
                    OR EXISTS(SELECT 1 FROM sys_notice WHERE INSTR(COALESCE(notice_content, ''), ?) > 0)
                    OR EXISTS(SELECT 1 FROM t_system_config WHERE INSTR(COALESCE(dashboard_notification, ''), ?) > 0)
                """, Boolean.class, path, path, path, path, path, path, path, path, path, path, path);
        return Boolean.TRUE.equals(referenced);
    }
}
