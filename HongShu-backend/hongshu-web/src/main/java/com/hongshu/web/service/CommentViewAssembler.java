package com.hongshu.web.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hongshu.common.utils.ConvertUtils;
import com.hongshu.web.domain.entity.WebComment;
import com.hongshu.web.domain.entity.WebLikeOrCollect;
import com.hongshu.web.domain.entity.WebNote;
import com.hongshu.web.domain.entity.WebUser;
import com.hongshu.web.domain.vo.CommentVO;
import com.hongshu.web.mapper.WebCommentMapper;
import com.hongshu.web.mapper.WebLikeOrCollectMapper;
import com.hongshu.web.mapper.WebNoteMapper;
import com.hongshu.web.mapper.WebUserMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** 批量组装评论展示数据，不负责评论的查询条件或写入。 */
@Component
public class CommentViewAssembler {

    private static final int COMMENT_LIKE_TYPE = 2;
    private static final String ROOT_COMMENT_ID = "0";
    private static final String UNKNOWN_USERNAME = "用户";

    private final WebUserMapper userMapper;
    private final WebLikeOrCollectMapper likeMapper;
    private final WebNoteMapper noteMapper;
    private final WebCommentMapper commentMapper;

    public CommentViewAssembler(WebUserMapper userMapper, WebLikeOrCollectMapper likeMapper,
                                WebNoteMapper noteMapper, WebCommentMapper commentMapper) {
        this.userMapper = userMapper;
        this.likeMapper = likeMapper;
        this.noteMapper = noteMapper;
        this.commentMapper = commentMapper;
    }

    public List<CommentVO> assemble(List<WebComment> comments, String currentUid) {
        if (comments == null || comments.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> userIds = comments.stream()
                .flatMap(comment -> Stream.of(comment.getUid(), comment.getReplyUid()))
                .filter(this::isEntityId).collect(Collectors.toSet());
        Map<String, WebUser> users = userIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectList(new QueryWrapper<WebUser>()
                        .select("id", "username", "avatar").in("id", userIds)).stream()
                        .collect(Collectors.toMap(WebUser::getId, Function.identity()));
        Set<String> commentIds = comments.stream().map(WebComment::getId)
                .filter(this::isEntityId).collect(Collectors.toSet());
        Set<String> likedIds = loadLikedIds(commentIds, currentUid);
        List<CommentVO> views = new ArrayList<>(comments.size());
        for (WebComment comment : comments) {
            CommentVO view = ConvertUtils.sourceToTarget(comment, CommentVO.class);
            WebUser author = users.get(comment.getUid());
            view.setUsername(author == null ? UNKNOWN_USERNAME : author.getUsername());
            if (author != null) {
                view.setAvatar(author.getAvatar());
            }
            WebUser replyUser = users.get(comment.getReplyUid());
            if (replyUser != null) {
                view.setReplyUsername(replyUser.getUsername()).setReplyAvatar(replyUser.getAvatar());
            }
            view.setTime(comment.getCreateTime() == null ? 0L : comment.getCreateTime().getTime());
            view.setIsLike(likedIds.contains(comment.getId()));
            views.add(view);
        }
        return views;
    }

    public List<CommentVO> assembleNotices(List<WebComment> comments, String currentUid) {
        List<CommentVO> views = assemble(comments, currentUid);
        if (views.isEmpty()) {
            return views;
        }
        Set<String> noteIds = comments.stream().map(WebComment::getNid)
                .filter(this::isEntityId).collect(Collectors.toSet());
        Map<String, WebNote> notes = noteIds.isEmpty() ? Collections.emptyMap()
                : noteMapper.selectList(new QueryWrapper<WebNote>()
                        .select("id", "title", "note_cover").in("id", noteIds)).stream()
                        .collect(Collectors.toMap(WebNote::getId, Function.identity()));
        Set<String> replyIds = comments.stream().filter(comment -> isEntityId(comment.getPid()))
                .map(WebComment::getReplyId).filter(this::isEntityId).collect(Collectors.toSet());
        Map<String, WebComment> replies = replyIds.isEmpty() ? Collections.emptyMap()
                : commentMapper.selectList(new QueryWrapper<WebComment>()
                        .select("id", "content").in("id", replyIds)).stream()
                        .collect(Collectors.toMap(WebComment::getId, Function.identity()));
        for (CommentVO view : views) {
            WebNote note = notes.get(view.getNid());
            if (note != null) {
                view.setNoteCover(note.getNoteCover()).setTitle(note.getTitle());
            }
            WebComment reply = replies.get(view.getReplyId());
            if (reply != null) {
                view.setReplyContent(reply.getContent());
            }
            if (Objects.equals(currentUid, view.getReplyUid())) {
                view.setReplyUsername(null);
            }
        }
        return views;
    }

    private Set<String> loadLikedIds(Set<String> commentIds, String currentUid) {
        if (commentIds.isEmpty() || StringUtils.isBlank(currentUid)) {
            return Collections.emptySet();
        }
        List<WebLikeOrCollect> likes = likeMapper.selectList(new QueryWrapper<WebLikeOrCollect>()
                .select("like_or_collection_id").eq("uid", currentUid).eq("type", COMMENT_LIKE_TYPE)
                .in("like_or_collection_id", commentIds));
        Set<String> likedIds = new HashSet<>(likes.size());
        for (WebLikeOrCollect like : likes) {
            likedIds.add(like.getLikeOrCollectionId());
        }
        return likedIds;
    }

    private boolean isEntityId(String value) {
        return StringUtils.isNotBlank(value) && !ROOT_COMMENT_ID.equals(value);
    }
}
