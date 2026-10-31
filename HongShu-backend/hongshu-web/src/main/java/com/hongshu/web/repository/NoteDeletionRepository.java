package com.hongshu.web.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/** 笔记删除关联数据访问；调用方负责开启事务并锁定待删笔记。 */
@Repository
public class NoteDeletionRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public NoteDeletionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    /** 删除前保留各专辑关联笔记的旧封面，用于精确更新受影响的专辑。 */
    public List<AlbumCoverReference> findAlbumReferences(Collection<String> noteIds) {
        if (noteIds.isEmpty()) {
            return List.of();
        }
        return jdbcTemplate.query("""
                SELECT DISTINCT relation.aid, note.note_cover
                FROM web_album_note_relation relation
                JOIN web_note note ON note.id = relation.nid
                WHERE relation.nid IN (:noteIds)
                """, Map.of("noteIds", noteIds), (result, rowNumber) ->
                new AlbumCoverReference(result.getString("aid"), result.getString("note_cover")));
    }

    /** 批量清理关联，先删除评论点赞再删除评论，避免失去关联条件。 */
    public void deleteRelations(Collection<String> noteIds) {
        if (noteIds.isEmpty()) {
            return;
        }
        Map<String, ?> parameters = Map.of("noteIds", noteIds);
        jdbcTemplate.update("""
                DELETE likes FROM web_like_or_collect likes
                JOIN web_comment c ON c.id = likes.like_or_collection_id
                WHERE likes.type = 2 AND c.nid IN (:noteIds)
                """, parameters);
        jdbcTemplate.update("DELETE FROM web_like_or_collect WHERE like_or_collection_id IN (:noteIds) AND type IN (1, 3)",
                parameters);
        jdbcTemplate.update("DELETE FROM web_comment WHERE nid IN (:noteIds)", parameters);
        jdbcTemplate.update("DELETE FROM web_comment_sync WHERE nid IN (:noteIds)", parameters);
        jdbcTemplate.update("DELETE FROM web_tag_note_relation WHERE nid IN (:noteIds)", parameters);
        jdbcTemplate.update("DELETE FROM web_album_note_relation WHERE nid IN (:noteIds)", parameters);
        jdbcTemplate.update("DELETE FROM web_user_note_relation WHERE nid IN (:noteIds)", parameters);
    }

    public void refreshAuthorCounts(Collection<String> authorIds) {
        if (authorIds.isEmpty()) {
            return;
        }
        // 与聊天、关注统一使用数据库实际 ID 的字典序，避免批量 UPDATE 采用不同的锁顺序。
        List<String> canonicalIds = jdbcTemplate.queryForList("SELECT id FROM web_user WHERE id IN (:authorIds)",
                Map.of("authorIds", authorIds), String.class).stream().sorted().toList();
        if (canonicalIds.isEmpty()) {
            return;
        }
        for (String authorId : canonicalIds) {
            jdbcTemplate.queryForList("SELECT id FROM web_user WHERE id = :authorId FOR UPDATE",
                    Map.of("authorId", authorId), String.class);
        }
        jdbcTemplate.update("""
                UPDATE web_user
                SET trend_count = (SELECT COUNT(*) FROM web_note WHERE uid = web_user.id)
                WHERE id IN (:authorIds)
                """, Map.of("authorIds", canonicalIds));
    }

    /** 只有封面来自本次移除的专辑内容时才替换，保留自定义封面。 */
    public void refreshAlbum(String albumId, Collection<String> removedCovers) {
        MapSqlParameterSource parameters = new MapSqlParameterSource("albumId", albumId);
        String coverUpdate = "";
        if (!removedCovers.isEmpty()) {
            parameters.addValue("removedCovers", removedCovers);
            coverUpdate = """
                    , album_cover = CASE WHEN album_cover IN (:removedCovers) THEN (
                        SELECT note.note_cover FROM web_album_note_relation relation
                        JOIN web_note note ON relation.nid = note.id
                        WHERE relation.aid = :albumId ORDER BY relation.sort, relation.id LIMIT 1
                    ) ELSE album_cover END
                    """;
        }
        jdbcTemplate.update("""
                UPDATE web_album
                SET img_count = (SELECT COUNT(*) FROM web_album_note_relation WHERE aid = :albumId)
                """ + coverUpdate + " WHERE id = :albumId", parameters);
    }

    public record AlbumCoverReference(String albumId, String removedCover) {
    }
}
