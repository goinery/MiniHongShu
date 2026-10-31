package com.hongshu.web.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hongshu.common.exception.web.HongshuException;
import com.hongshu.web.domain.entity.*;
import com.hongshu.web.mapper.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class AlbumContentService {
    private final WebAlbumMapper albums;
    private final WebAlbumNoteRelationMapper relations;
    private final WebNoteMapper notes;
    private final JdbcTemplate jdbc;

    public AlbumContentService(WebAlbumMapper albums, WebAlbumNoteRelationMapper relations, WebNoteMapper notes, JdbcTemplate jdbc) {
        this.albums = albums;
        this.relations = relations;
        this.notes = notes;
        this.jdbc = jdbc;
    }

    private WebAlbum requireAlbum(String id) {
        WebAlbum album = albums.selectById(id);
        if (album == null) throw new HongshuException("专辑不存在", 404);
        return album;
    }

    public Map<String, Object> list(String id) {
        WebAlbum album = requireAlbum(id);
        List<Map<String, Object>> items = jdbc.queryForList("""
                SELECT CAST(r.id AS CHAR) AS id, CAST(r.nid AS CHAR) AS nid, r.sort,
                       n.title, n.note_cover AS noteCover,
                       DATE_FORMAT(n.create_time, '%Y-%m-%d %H:%i:%s') AS createTime,
                       CAST(n.audit_status AS CHAR) AS auditStatus
                FROM web_album_note_relation r JOIN web_note n ON r.nid = n.id
                WHERE r.aid = ? ORDER BY r.sort, r.id
                """, id);
        return Map.of("album", album, "items", items);
    }

    @Transactional(rollbackFor = Exception.class)
    public void add(String aid, List<String> noteIds) {
        WebAlbum album = requireAlbum(aid);
        if (noteIds == null || noteIds.isEmpty()) throw new HongshuException("请选择笔记");
        for (String nid : new LinkedHashSet<>(noteIds)) {
            WebNote note = notes.selectById(nid);
            if (note == null || !"1".equals(note.getAuditStatus())) throw new HongshuException("只能添加已审核通过的笔记");
            if (relations.selectCount(new QueryWrapper<WebAlbumNoteRelation>().eq("aid", aid).eq("nid", nid)) > 0) continue;
            WebAlbumNoteRelation item = new WebAlbumNoteRelation();
            item.setAid(aid);
            item.setNid(nid);
            Integer max = jdbc.queryForObject("SELECT COALESCE(MAX(sort), -1) + 1 FROM web_album_note_relation WHERE aid = ?", Integer.class, aid);
            item.setSort(max);
            relations.insert(item);
        }
        recount(album);
    }

    @Transactional(rollbackFor = Exception.class)
    public void reorder(String aid, List<String> ids) {
        requireAlbum(aid);
        List<String> existing = jdbc.queryForList("SELECT CAST(id AS CHAR) FROM web_album_note_relation WHERE aid = ?", String.class, aid);
        if (ids == null || ids.size() != existing.size() || new HashSet<>(ids).size() != ids.size() || !new HashSet<>(existing).equals(new HashSet<>(ids))) {
            throw new HongshuException("专辑内容已变化，请刷新后重试");
        }
        for (int i = 0; i < ids.size(); i++) jdbc.update("UPDATE web_album_note_relation SET sort = ? WHERE id = ? AND aid = ?", i, ids.get(i), aid);
    }

    @Transactional(rollbackFor = Exception.class)
    public void sortByTime(String aid) {
        requireAlbum(aid);
        List<String> ids = jdbc.queryForList("SELECT CAST(r.id AS CHAR) FROM web_album_note_relation r JOIN web_note n ON r.nid = n.id WHERE r.aid = ? ORDER BY n.create_time DESC, r.id DESC", String.class, aid);
        reorder(aid, ids);
    }

    @Transactional(rollbackFor = Exception.class)
    public void remove(String aid, List<String> ids) {
        WebAlbum album = requireAlbum(aid);
        if (ids != null && !ids.isEmpty()) relations.delete(new QueryWrapper<WebAlbumNoteRelation>().eq("aid", aid).in("id", ids));
        recount(album);
    }

    private void recount(WebAlbum album) {
        album.setImgCount(relations.selectCount(new QueryWrapper<WebAlbumNoteRelation>().eq("aid", album.getId())));
        albums.updateById(album);
    }
}
