package com.hongshu.web.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hongshu.common.exception.web.HongshuException;
import com.hongshu.web.domain.entity.WebComment;
import com.hongshu.web.domain.entity.WebLikeOrCollect;
import com.hongshu.web.domain.entity.WebNote;
import com.hongshu.web.mapper.WebCommentMapper;
import com.hongshu.web.mapper.WebLikeOrCollectMapper;
import com.hongshu.web.mapper.WebNoteMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** 前后台共用的评论回收事务，调用方负责校验操作权限。 */
@Service
public class CommentDeletionService {

    private static final String ROOT_COMMENT_ID = "0";
    private static final int COMMENT_LIKE_TYPE = 2;

    private final WebCommentMapper commentMapper;
    private final WebLikeOrCollectMapper likeMapper;
    private final WebNoteMapper noteMapper;

    public CommentDeletionService(WebCommentMapper commentMapper, WebLikeOrCollectMapper likeMapper,
                                  WebNoteMapper noteMapper) {
        this.commentMapper = commentMapper;
        this.likeMapper = likeMapper;
        this.noteMapper = noteMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public int deleteComments(Collection<String> commentIds) {
        return deleteComments(commentIds, null);
    }

    /** 前台在锁内验证归属，后台入口由控制器校验管理权限。 */
    @Transactional(rollbackFor = Exception.class)
    public void deleteOwnedComment(String commentId, String actor) {
        if (StringUtils.isBlank(actor) || StringUtils.isBlank(commentId)) {
            throw new HongshuException("评论或操作用户不能为空");
        }
        deleteComments(List.of(commentId), actor);
    }

    private int deleteComments(Collection<String> commentIds, String actor) {
        if (commentIds == null || commentIds.isEmpty()) {
            return 0;
        }
        Set<String> requestedIds = commentIds.stream().filter(StringUtils::isNotBlank).collect(Collectors.toSet());
        if (requestedIds.isEmpty()) {
            return 0;
        }
        List<WebComment> requested = commentMapper.selectList(new QueryWrapper<WebComment>()
                .select("id", "nid").in("id", requestedIds));
        if (requested.isEmpty()) {
            return 0;
        }
        Set<String> noteIds = requested.stream().map(WebComment::getNid)
                .filter(StringUtils::isNotBlank).collect(Collectors.toSet());
        if (!noteIds.isEmpty()) {
            // 与笔记删除和新增评论保持一致，先锁笔记，再锁评论。
            noteMapper.selectList(new QueryWrapper<WebNote>()
                    .select("id").in("id", noteIds).orderByAsc("id").last("FOR UPDATE"));
        }
        Set<String> canonicalIds = requested.stream().map(WebComment::getId).collect(Collectors.toSet());
        List<WebComment> roots = commentMapper.selectList(new QueryWrapper<WebComment>()
                .in("id", canonicalIds).orderByAsc("id").last("FOR UPDATE"));
        if (actor != null) {
            for (WebComment root : roots) {
                if (!actor.equals(root.getUid()) && !actor.equals(root.getNoteUid())) {
                    throw new HongshuException("只能删除自己的评论");
                }
            }
        }
        Map<String, WebComment> comments = loadCommentTree(roots);
        if (comments.isEmpty()) {
            return 0;
        }
        Set<String> deletedIds = comments.keySet();
        likeMapper.delete(new QueryWrapper<WebLikeOrCollect>()
                .eq("type", COMMENT_LIKE_TYPE).in("like_or_collection_id", deletedIds));
        int deleted = commentMapper.deleteByIds(deletedIds);
        Map<String, Integer> noteCounts = new HashMap<>();
        Map<String, Integer> parentCounts = new HashMap<>();
        for (WebComment comment : comments.values()) {
            if (StringUtils.isNotBlank(comment.getNid())) {
                noteCounts.merge(comment.getNid(), 1, Integer::sum);
            }
            String parentId = comment.getPid();
            if (StringUtils.isNotBlank(parentId) && !ROOT_COMMENT_ID.equals(parentId)
                    && !deletedIds.contains(parentId)) {
                parentCounts.merge(parentId, 1, Integer::sum);
            }
        }
        noteCounts.forEach((noteId, count) -> noteMapper.adjustCommentCount(noteId, -count));
        parentCounts.forEach((parentId, count) -> commentMapper.adjustReplyCount(parentId, -count));
        return deleted;
    }

    private Map<String, WebComment> loadCommentTree(List<WebComment> roots) {
        List<WebComment> level = roots;
        Map<String, WebComment> comments = new LinkedHashMap<>();
        while (!level.isEmpty()) {
            Set<String> nextParents = level.stream().map(WebComment::getId)
                    .filter(id -> !comments.containsKey(id)).collect(Collectors.toSet());
            if (nextParents.isEmpty()) {
                break;
            }
            for (WebComment comment : level) {
                comments.put(comment.getId(), comment);
            }
            level = commentMapper.selectList(new QueryWrapper<WebComment>()
                    .in("pid", nextParents).orderByAsc("id").last("FOR UPDATE"));
        }
        return comments;
    }
}
