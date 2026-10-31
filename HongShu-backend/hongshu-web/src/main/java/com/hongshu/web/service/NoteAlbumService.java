package com.hongshu.web.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hongshu.common.exception.web.HongshuException;
import com.hongshu.web.domain.entity.WebAlbum;
import com.hongshu.web.domain.entity.WebAlbumNoteRelation;
import com.hongshu.web.mapper.WebAlbumMapper;
import com.hongshu.web.mapper.WebAlbumNoteRelationMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 管理作者发布、编辑笔记时的专辑归属。
 *
 * @author hongshu
 */
@Service
public class NoteAlbumService {

    private final WebAlbumMapper albumMapper;
    private final WebAlbumNoteRelationMapper relationMapper;

    public NoteAlbumService(WebAlbumMapper albumMapper, WebAlbumNoteRelationMapper relationMapper) {
        this.albumMapper = albumMapper;
        this.relationMapper = relationMapper;
    }

    /**
     * 把新笔记关联到作者选择的专辑。
     *
     * @param noteId 笔记 ID
     * @param albumId 专辑 ID，空值表示不关联
     * @param ownerId 已认证的作者 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void bind(String noteId, String albumId, String ownerId) {
        if (StringUtils.isBlank(albumId)) {
            return;
        }
        WebAlbum album = albumMapper.selectById(albumId);
        if (album == null || !Objects.equals(album.getUid(), ownerId)) {
            throw new HongshuException("只能将笔记添加到自己的专辑", 403);
        }
        WebAlbumNoteRelation relation = new WebAlbumNoteRelation();
        relation.setNid(noteId);
        relation.setAid(albumId);
        relation.setSort(Math.toIntExact(relationMapper.selectCount(
                new QueryWrapper<WebAlbumNoteRelation>().eq("aid", albumId))));
        relationMapper.insert(relation);
        recount(albumId);
    }

    /**
     * 替换作者自己的专辑关联，仅重算受影响专辑的数量。
     *
     * @param noteId 笔记 ID
     * @param albumId 新专辑 ID，空值表示移除原有归属
     * @param ownerId 已认证的作者 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void replace(String noteId, String albumId, String ownerId) {
        List<String> ownedAlbumIds = albumMapper.selectList(new QueryWrapper<WebAlbum>().eq("uid", ownerId))
                .stream().map(WebAlbum::getId).toList();
        Set<String> affectedAlbumIds = new LinkedHashSet<>();
        if (!ownedAlbumIds.isEmpty()) {
            QueryWrapper<WebAlbumNoteRelation> query = new QueryWrapper<WebAlbumNoteRelation>()
                    .eq("nid", noteId).in("aid", ownedAlbumIds);
            relationMapper.selectList(query).stream().map(WebAlbumNoteRelation::getAid)
                    .forEach(affectedAlbumIds::add);
            relationMapper.delete(query);
        }
        bind(noteId, albumId, ownerId);
        affectedAlbumIds.remove(albumId);
        for (String affectedAlbumId : affectedAlbumIds) {
            recount(affectedAlbumId);
        }
    }

    private void recount(String albumId) {
        albumMapper.refreshImageCount(albumId);
    }
}
