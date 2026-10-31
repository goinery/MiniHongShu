package com.hongshu.web.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.hongshu.common.enums.ResultCodeEnum;
import com.hongshu.common.exception.web.HongshuException;
import com.hongshu.common.utils.ConvertUtils;
import com.hongshu.web.auth.AuthContextHolder;
import com.hongshu.web.domain.dto.CommentDTO;
import com.hongshu.web.domain.entity.WebComment;
import com.hongshu.web.domain.entity.WebNote;
import com.hongshu.web.domain.vo.CommentVO;
import com.hongshu.web.mapper.WebCommentMapper;
import com.hongshu.web.mapper.WebNoteMapper;
import com.hongshu.web.service.CommentDeletionService;
import com.hongshu.web.service.CommentQueryService;
import com.hongshu.web.service.CommentViewAssembler;
import com.hongshu.web.service.IWebCommentService;
import com.hongshu.web.service.NoteAccessService;
import com.hongshu.web.websocket.im.ChatUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 评论应用服务：写入校验、计数维护和通知编排。
 */
@Service
public class WebCommentServiceImpl extends ServiceImpl<WebCommentMapper, WebComment> implements IWebCommentService {

    private static final String ROOT_COMMENT_ID = "0";
    private static final int MAX_CONTENT_LENGTH = 2000;
    private static final int COMMENT_NOTIFICATION_TYPE = 1;
    private static final int ROOT_COMMENT_LEVEL = 1;
    private static final int REPLY_COMMENT_LEVEL = 2;

    private final WebNoteMapper noteMapper;
    private final NoteAccessService noteAccess;
    private final CommentQueryService queries;
    private final CommentViewAssembler assembler;
    private final CommentDeletionService deletion;
    private final ChatUtils chatUtils;

    public WebCommentServiceImpl(WebNoteMapper noteMapper, NoteAccessService noteAccess,
                                 CommentQueryService queries, CommentViewAssembler assembler,
                                 CommentDeletionService deletion, ChatUtils chatUtils) {
        this.noteMapper = noteMapper;
        this.noteAccess = noteAccess;
        this.queries = queries;
        this.assembler = assembler;
        this.deletion = deletion;
        this.chatUtils = chatUtils;
    }

    @Override
    public Page<CommentVO> getOneCommentByNoteId(long currentPage, long pageSize, String noteId) {
        return queries.getRootComments(currentPage, pageSize, noteId, false);
    }

    @Override
    public Object getCommentById(String commentId) {
        return queries.getComment(commentId);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public CommentVO saveCommentByDTO(CommentDTO commentDTO) {
        String currentUid = requireCurrentUser();
        if (commentDTO == null || StringUtils.isBlank(commentDTO.getContent())
                || commentDTO.getContent().length() > MAX_CONTENT_LENGTH) {
            throw new HongshuException("评论需为 1 至 2000 字");
        }
        WebNote note = noteAccess.requireReadableForUpdate(commentDTO.getNid());
        WebComment comment = ConvertUtils.sourceToTarget(commentDTO, WebComment.class);
        normalizeReply(comment);
        Date now = new Date();
        comment.setId(null);
        comment.setUid(currentUid);
        comment.setNoteUid(note.getUid());
        comment.setCreateTime(now);
        comment.setUpdateTime(now);
        comment.setLikeCount(0L);
        comment.setTwoCommentCount(0L);
        comment.setSort(0);
        if (!save(comment)) {
            throw new HongshuException("评论保存失败");
        }
        if (noteMapper.adjustCommentCount(comment.getNid(), 1) == 0) {
            throw new HongshuException("笔记不存在", 404);
        }
        if (!ROOT_COMMENT_ID.equals(comment.getPid()) && baseMapper.adjustReplyCount(comment.getPid(), 1) == 0) {
            throw new HongshuException("回复的评论不存在", 404);
        }
        notifyComment(comment, currentUid);
        return assembler.assemble(List.of(comment), currentUid).getFirst();
    }

    private void normalizeReply(WebComment comment) {
        if (StringUtils.isBlank(comment.getPid()) || ROOT_COMMENT_ID.equals(comment.getPid())) {
            comment.setPid(ROOT_COMMENT_ID);
            comment.setReplyId(ROOT_COMMENT_ID);
            comment.setReplyUid(ROOT_COMMENT_ID);
            comment.setLevel(ROOT_COMMENT_LEVEL);
            return;
        }
        WebComment parent = getById(comment.getPid());
        if (parent == null || !Objects.equals(parent.getNid(), comment.getNid())
                || !ROOT_COMMENT_ID.equals(parent.getPid())) {
            throw new HongshuException("回复的评论不存在");
        }
        WebComment reply = StringUtils.isBlank(comment.getReplyId()) || ROOT_COMMENT_ID.equals(comment.getReplyId())
                ? parent : getById(comment.getReplyId());
        if (reply == null || !Objects.equals(reply.getNid(), comment.getNid())
                || (!Objects.equals(reply.getId(), parent.getId()) && !Objects.equals(reply.getPid(), parent.getId()))) {
            throw new HongshuException("回复的评论不存在");
        }
        comment.setReplyId(reply.getId());
        comment.setReplyUid(reply.getUid());
        comment.setLevel(REPLY_COMMENT_LEVEL);
    }

    @Override
    public void syncCommentByIds(List<String> commentIds) {
        // 保留兼容入口；评论已统一存储于 web_comment，无须复制同步表。
    }

    @Override
    public Page<CommentVO> getTwoCommentByOneCommentId(long currentPage, long pageSize, String oneCommentId) {
        return queries.getReplies(currentPage, pageSize, oneCommentId);
    }

    @Override
    public IPage<CommentVO> getNoticeComment(long currentPage, long pageSize) {
        return queries.getNotices(currentPage, pageSize);
    }

    @Override
    public Page<CommentVO> getCommentWithCommentByNoteId(long currentPage, long pageSize, String noteId) {
        return queries.getRootComments(currentPage, pageSize, noteId, true);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteCommentById(String commentId) {
        String currentUid = requireCurrentUser();
        if (StringUtils.isBlank(commentId)) {
            throw new HongshuException("评论 ID 不能为空");
        }
        deletion.deleteOwnedComment(commentId, currentUid);
    }

    private String requireCurrentUser() {
        String currentUid = AuthContextHolder.getUserId();
        if (StringUtils.isBlank(currentUid)) {
            throw new HongshuException(ResultCodeEnum.TOKEN_FAIL);
        }
        return currentUid;
    }

    private void notifyComment(WebComment comment, String currentUid) {
        String noteUid = comment.getNoteUid();
        String replyUid = comment.getReplyUid();
        if (StringUtils.isNotBlank(noteUid) && !currentUid.equals(noteUid) && !Objects.equals(replyUid, noteUid)) {
            chatUtils.sendMessage(noteUid, COMMENT_NOTIFICATION_TYPE);
        }
        if (StringUtils.isNotBlank(replyUid) && !ROOT_COMMENT_ID.equals(replyUid) && !currentUid.equals(replyUid)) {
            chatUtils.sendMessage(replyUid, COMMENT_NOTIFICATION_TYPE);
        }
    }
}
