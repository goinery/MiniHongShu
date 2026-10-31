package com.hongshu.web.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hongshu.common.enums.ResultCodeEnum;
import com.hongshu.common.exception.web.HongshuException;
import com.hongshu.web.auth.AuthContextHolder;
import com.hongshu.web.domain.dto.CommentReplySummary;
import com.hongshu.web.domain.entity.WebComment;
import com.hongshu.web.domain.vo.CommentVO;
import com.hongshu.web.mapper.WebCommentMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 评论读取入口，集中处理可见性、分页和二级回复预览。
 */
@Service
public class CommentQueryService {

    private static final String ROOT_COMMENT_ID = "0";
    private static final int MAX_PAGE_SIZE = 100;
    private static final int REPLY_PREVIEW_SIZE = 3;

    private final WebCommentMapper commentMapper;
    private final NoteAccessService noteAccess;
    private final CommentViewAssembler assembler;

    public CommentQueryService(WebCommentMapper commentMapper, NoteAccessService noteAccess,
                               CommentViewAssembler assembler) {
        this.commentMapper = commentMapper;
        this.noteAccess = noteAccess;
        this.assembler = assembler;
    }

    public CommentVO getComment(String commentId) {
        WebComment comment = requireComment(commentId);
        noteAccess.requireReadable(comment.getNid());
        return assembler.assemble(List.of(comment), AuthContextHolder.getUserId()).getFirst();
    }

    public Page<CommentVO> getReplies(long currentPage, long pageSize, String parentId) {
        WebComment parent = requireComment(parentId);
        noteAccess.requireReadable(parent.getNid());
        Page<WebComment> source = commentMapper.selectPage(newPage(currentPage, pageSize),
                new QueryWrapper<WebComment>().eq("pid", parentId)
                        .orderByDesc("like_count", "create_time", "id"));
        return toPage(source, assembler.assemble(source.getRecords(), AuthContextHolder.getUserId()));
    }

    public Page<CommentVO> getNotices(long currentPage, long pageSize) {
        String currentUid = AuthContextHolder.getUserId();
        if (StringUtils.isBlank(currentUid)) {
            throw new HongshuException(ResultCodeEnum.TOKEN_FAIL);
        }
        Page<WebComment> source = commentMapper.selectPage(newPage(currentPage, pageSize),
                new QueryWrapper<WebComment>()
                        .and(query -> query.eq("note_uid", currentUid).or().eq("reply_uid", currentUid))
                        .ne("uid", currentUid).orderByDesc("create_time", "id"));
        return toPage(source, assembler.assembleNotices(source.getRecords(), currentUid));
    }

    public Page<CommentVO> getRootComments(long currentPage, long pageSize, String noteId, boolean includeReplies) {
        noteAccess.requireReadable(noteId);
        Page<WebComment> source = commentMapper.selectPage(newPage(currentPage, pageSize),
                new QueryWrapper<WebComment>().eq("nid", noteId).eq("pid", ROOT_COMMENT_ID)
                        .orderByDesc("like_count", "create_time", "id"));
        List<WebComment> roots = source.getRecords();
        if (roots.isEmpty()) {
            return toPage(source, Collections.emptyList());
        }
        fillMissingReplyCounts(roots);
        List<String> rootIds = roots.stream().map(WebComment::getId).collect(Collectors.toList());
        List<WebComment> replies = includeReplies
                ? commentMapper.selectReplyPreviews(rootIds, REPLY_PREVIEW_SIZE) : Collections.emptyList();
        List<WebComment> comments = new ArrayList<>(roots.size() + replies.size());
        comments.addAll(roots);
        comments.addAll(replies);
        List<CommentVO> views = assembler.assemble(comments, AuthContextHolder.getUserId());
        Map<String, List<CommentVO>> childrenByParent = views.subList(roots.size(), views.size()).stream()
                .collect(Collectors.groupingBy(CommentVO::getPid));
        List<CommentVO> rootViews = new ArrayList<>(views.subList(0, roots.size()));
        for (CommentVO root : rootViews) {
            root.setChildren(childrenByParent.getOrDefault(root.getId(), Collections.emptyList()));
        }
        return toPage(source, rootViews);
    }

    private void fillMissingReplyCounts(List<WebComment> roots) {
        List<String> missingIds = roots.stream().filter(comment -> comment.getTwoCommentCount() == null)
                .map(WebComment::getId).collect(Collectors.toList());
        if (missingIds.isEmpty()) {
            return;
        }
        Map<String, Long> counts = commentMapper.selectReplyCounts(missingIds).stream()
                .collect(Collectors.toMap(CommentReplySummary::getPid, CommentReplySummary::getReplyCount));
        for (WebComment root : roots) {
            if (root.getTwoCommentCount() == null) {
                root.setTwoCommentCount(counts.getOrDefault(root.getId(), 0L));
            }
        }
    }

    private WebComment requireComment(String commentId) {
        WebComment comment = StringUtils.isBlank(commentId) ? null : commentMapper.selectById(commentId);
        if (comment == null) {
            throw new HongshuException("评论不存在", 404);
        }
        return comment;
    }

    private Page<WebComment> newPage(long currentPage, long pageSize) {
        long size = Math.clamp(pageSize, 1, MAX_PAGE_SIZE);
        long current = Math.max(1, currentPage);
        if (current - 1 > Long.MAX_VALUE / size) {
            throw new HongshuException("分页参数超出范围");
        }
        return new Page<>(current, size);
    }

    private Page<CommentVO> toPage(Page<WebComment> source, List<CommentVO> views) {
        Page<CommentVO> result = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        result.setRecords(views);
        return result;
    }
}
