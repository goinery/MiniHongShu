package com.hongshu.web.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hongshu.common.exception.web.HongshuException;
import com.hongshu.web.domain.entity.WebNote;
import com.hongshu.web.mapper.WebNoteMapper;
import com.hongshu.web.repository.NoteDeletionRepository;
import com.hongshu.web.repository.NoteDeletionRepository.AlbumCoverReference;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** 笔记删除事务编排：锁定、归属校验、关联清理和提交后文件回收。 */
@Service
public class NoteDeletionService {

    private final WebNoteMapper notes;
    private final NoteDeletionRepository repository;
    private final FileAssetService assets;

    public NoteDeletionService(WebNoteMapper notes, NoteDeletionRepository repository, FileAssetService assets) {
        this.notes = notes;
        this.repository = repository;
        this.assets = assets;
    }

    /** 后台已完成权限校验后使用，可批量删除不同作者的笔记。 */
    @Transactional(rollbackFor = Exception.class)
    public int delete(Collection<String> ids) {
        return deleteLocked(ids, null);
    }

    /** 前台在锁内校验作者，避免校验后归属发生变化。 */
    @Transactional(rollbackFor = Exception.class)
    public int deleteOwned(Collection<String> ids, String ownerId) {
        if (StringUtils.isBlank(ownerId)) {
            throw new HongshuException("请先登录", 401);
        }
        return deleteLocked(ids, ownerId);
    }

    private int deleteLocked(Collection<String> ids, String ownerId) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        if (ids.stream().anyMatch(StringUtils::isBlank)) {
            throw new HongshuException("笔记 ID 不能为空");
        }
        List<WebNote> records = notes.selectList(new QueryWrapper<WebNote>()
                .in("id", new LinkedHashSet<>(ids)).orderByAsc("id").last("FOR UPDATE"));
        if (records.isEmpty()) {
            return 0;
        }
        if (ownerId != null && records.stream().anyMatch(note -> !ownerId.equals(note.getUid()))) {
            throw new HongshuException("只能删除自己的笔记", 403);
        }
        List<String> noteIds = records.stream().map(WebNote::getId).toList();
        List<AlbumCoverReference> albumReferences = repository.findAlbumReferences(noteIds);
        repository.deleteRelations(noteIds);
        int deleted = notes.delete(new QueryWrapper<WebNote>().in("id", noteIds));
        if (deleted != records.size()) {
            throw new HongshuException("笔记删除失败，请重试");
        }
        Set<String> authors = records.stream().map(WebNote::getUid).filter(StringUtils::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        repository.refreshAuthorCounts(authors);
        refreshAlbums(albumReferences);
        cleanFilesAfterCommit(records);
        return deleted;
    }

    private void refreshAlbums(List<AlbumCoverReference> references) {
        Map<String, Set<String>> coversByAlbum = new TreeMap<>();
        for (AlbumCoverReference reference : references) {
            if (StringUtils.isBlank(reference.albumId())) {
                continue;
            }
            Set<String> covers = coversByAlbum.computeIfAbsent(reference.albumId(), key -> new LinkedHashSet<>());
            if (reference.removedCover() != null) {
                covers.add(reference.removedCover());
            }
        }
        coversByAlbum.forEach(repository::refreshAlbum);
    }

    private void cleanFilesAfterCommit(List<WebNote> records) {
        Set<String> covers = new LinkedHashSet<>();
        List<String> contents = new ArrayList<>(records.size() * 2);
        for (WebNote note : records) {
            covers.add(note.getNoteCover());
            contents.add(note.getUrls());
            contents.add(note.getContent());
        }
        assets.cleanStoredReferencesAfterCommit(covers.toArray(String[]::new));
        assets.cleanStoredContentAfterCommit(contents.toArray(String[]::new));
    }
}
