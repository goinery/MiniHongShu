package com.hongshu.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.hongshu.common.utils.ConvertUtils;
import com.hongshu.common.exception.web.HongshuException;
import com.hongshu.web.auth.AuthContextHolder;
import com.hongshu.web.domain.dto.LikeOrCollectDTO;
import com.hongshu.web.domain.entity.*;
import com.hongshu.web.domain.vo.CommentVO;
import com.hongshu.web.domain.vo.LikeOrCollectVO;
import com.hongshu.web.mapper.*;
import com.hongshu.web.service.IWebLikeOrCollectService;
import com.hongshu.web.websocket.im.ChatUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * 点赞/收藏
 *
 * @author hongshu
 */
@Service
public class WebLikeOrCollectServiceImpl extends ServiceImpl<WebLikeOrCollectMapper, WebLikeOrCollect> implements IWebLikeOrCollectService {

    private final WebUserMapper userMapper;
    private final WebNoteMapper noteMapper;
    private final WebAlbumMapper albumMapper;
    private final WebCommentMapper commentMapper;
    private final WebAlbumNoteRelationMapper albumNoteRelationMapper;
    private final ChatUtils chatUtils;
    private final com.hongshu.web.service.NoteAccessService noteAccess;
    private final Executor noticeExecutor;

    public WebLikeOrCollectServiceImpl(WebUserMapper userMapper, WebNoteMapper noteMapper, WebAlbumMapper albumMapper, WebCommentMapper commentMapper, WebAlbumNoteRelationMapper albumNoteRelationMapper, ChatUtils chatUtils, com.hongshu.web.service.NoteAccessService noteAccess, @Qualifier("threadPoolTaskExecutor") Executor noticeExecutor) {
        this.userMapper = userMapper;
        this.noteMapper = noteMapper;
        this.albumMapper = albumMapper;
        this.commentMapper = commentMapper;
        this.albumNoteRelationMapper = albumNoteRelationMapper;
        this.chatUtils = chatUtils;
        this.noteAccess = noteAccess;
        this.noticeExecutor = noticeExecutor;
    }

    /**
     * 点赞或收藏
     *
     * @param likeOrCollectDTO 点赞收藏
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void likeOrCollectionByDTO(LikeOrCollectDTO likeOrCollectDTO) {
        String currentUid = AuthContextHolder.getUserId();
        String targetId = likeOrCollectDTO.getLikeOrCollectionId();
        Integer type = likeOrCollectDTO.getType();
        if (type == null || type < 1 || type > 4 || StringUtils.isBlank(targetId))
            throw new com.hongshu.common.exception.web.HongshuException("操作参数无效");
        if (type == 1 || type == 3) likeOrCollectDTO.setPublishUid(noteAccess.requireReadable(targetId).getUid());
        if (type == 2) {
            WebComment comment = commentMapper.selectById(targetId);
            if (comment == null) throw new com.hongshu.common.exception.web.HongshuException("评论不存在", 404);
            noteAccess.requireReadable(comment.getNid());
            likeOrCollectDTO.setPublishUid(comment.getUid());
        }
        if (type == 4) {
            WebAlbum album = albumMapper.selectById(targetId);
            if (album == null) throw new com.hongshu.common.exception.web.HongshuException("专辑不存在", 404);
            likeOrCollectDTO.setPublishUid(album.getUid());
        }
        if (isLikeOrCollection(likeOrCollectDTO)) {
            this.remove(new QueryWrapper<WebLikeOrCollect>().eq("uid", currentUid).eq("like_or_collection_id", likeOrCollectDTO.getLikeOrCollectionId()).eq("type", likeOrCollectDTO.getType()));
            this.updateLikeCollectionCount(likeOrCollectDTO, -1);
        } else {
            // 点赞评论或者笔记
            WebLikeOrCollect likeOrCollection = ConvertUtils.sourceToTarget(likeOrCollectDTO, WebLikeOrCollect.class);
            likeOrCollection.setTimestamp(System.currentTimeMillis());
            likeOrCollection.setUid(currentUid);
            likeOrCollection.setCreateTime(new Date());
            likeOrCollection.setUpdateTime(new Date());
            this.save(likeOrCollection);
            this.updateLikeCollectionCount(likeOrCollectDTO, 1);
            // 不是当前用户才进行通知
            if (StringUtils.isNotBlank(likeOrCollectDTO.getPublishUid()) && !likeOrCollectDTO.getPublishUid().equals(currentUid)) {
                chatUtils.sendMessage(likeOrCollectDTO.getPublishUid(), 0);
            }
        }
    }

    /**
     * 是否点赞或收藏
     *
     * @param likeOrCollectDTO 点赞收藏
     */
    @Override
    public boolean isLikeOrCollection(LikeOrCollectDTO likeOrCollectDTO) {
        String currentUid = AuthContextHolder.getUserId();
        long count = this.count(new QueryWrapper<WebLikeOrCollect>().eq("uid", currentUid).eq("like_or_collection_id", likeOrCollectDTO.getLikeOrCollectionId()).eq("type", likeOrCollectDTO.getType()));
        return count > 0;
    }

    /**
     * 获取当前用户最新的点赞和收藏信息
     *
     * @param currentPage 当前页
     * @param pageSize    分页数
     */
    @Override
    public Page<LikeOrCollectVO> getNoticeLikeOrCollection(long currentPage, long pageSize) {
        Page<LikeOrCollectVO> result = new Page<>();
        String currentUid = AuthContextHolder.getUserId();

        Page<WebLikeOrCollect> likeOrCollectionPage = this.page(new Page<>((int) currentPage, (int) pageSize), new QueryWrapper<WebLikeOrCollect>().eq("publish_uid", currentUid).ne("uid", currentUid).orderByDesc("create_time"));
        List<WebLikeOrCollect> likeOrCollectionList = likeOrCollectionPage.getRecords();
        long total = likeOrCollectionPage.getTotal();
        result.setTotal(total);
        if (likeOrCollectionList.isEmpty()) {
            return result;
        }

        Set<String> uids = likeOrCollectionList.stream().map(WebLikeOrCollect::getUid).collect(Collectors.toSet());
        Set<String> nids = likeOrCollectionList.stream().filter(e -> e.getType() == 1 || e.getType() == 3).map(WebLikeOrCollect::getLikeOrCollectionId).collect(Collectors.toSet());
        Set<String> cids = likeOrCollectionList.stream().filter(e -> e.getType() == 2).map(WebLikeOrCollect::getLikeOrCollectionId).collect(Collectors.toSet());
        Set<String> aids = likeOrCollectionList.stream().filter(e -> e.getType() == 4).map(WebLikeOrCollect::getLikeOrCollectionId).collect(Collectors.toSet());

        // 仅把已确定的 ID 传入工作线程，用户身份仍在请求线程中读取。
        CompletableFuture<Map<String, WebUser>> usersFuture = CompletableFuture.supplyAsync(
                () -> uids.isEmpty() ? Collections.emptyMap() : userMapper.selectByIds(uids).stream().collect(Collectors.toMap(WebUser::getId, user -> user)), noticeExecutor);
        CompletableFuture<Map<String, WebNote>> notesFuture = CompletableFuture.supplyAsync(
                () -> nids.isEmpty() ? Collections.emptyMap() : noteMapper.selectByIds(nids).stream().collect(Collectors.toMap(WebNote::getId, note -> note)), noticeExecutor);
        CompletableFuture<Map<String, CommentVO>> commentsFuture = CompletableFuture.supplyAsync(
                () -> loadCommentDetails(cids), noticeExecutor);
        CompletableFuture<Map<String, WebAlbum>> albumsFuture = CompletableFuture.supplyAsync(
                () -> aids.isEmpty() ? Collections.emptyMap() : albumMapper.selectByIds(aids).stream().collect(Collectors.toMap(WebAlbum::getId, album -> album)), noticeExecutor);
        CompletableFuture.allOf(usersFuture, notesFuture, commentsFuture, albumsFuture).join();

        Map<String, WebUser> userMap = usersFuture.join();
        Map<String, WebNote> noteMap = notesFuture.join();
        Map<String, CommentVO> commentVoMap = commentsFuture.join();
        Map<String, WebAlbum> albumMap = albumsFuture.join();
        List<LikeOrCollectVO> likeOrCollectVOList = new ArrayList<>();
        for (WebLikeOrCollect model : likeOrCollectionList) {
            LikeOrCollectVO likeOrCollectVo = new LikeOrCollectVO();
            WebUser user = userMap.get(model.getUid());
            if (user == null) {
                continue;
            }
            likeOrCollectVo.setUid(String.valueOf(user.getId()))
                    .setUsername(user.getUsername())
                    .setAvatar(user.getAvatar())
                    .setTime(model.getTimestamp())
                    .setType(model.getType());
            switch (model.getType()) {
                case 2:
                    CommentVO commentVo = commentVoMap.get(model.getLikeOrCollectionId());
                    if (commentVo != null) {
                        likeOrCollectVo.setItemId(commentVo.getNid())
                                .setItemCover(commentVo.getNoteCover())
                                .setContent(commentVo.getContent());
                    }
                    break;
                case 4:
                    WebAlbum album = albumMap.get(model.getLikeOrCollectionId());
                    if (album != null) {
                        likeOrCollectVo.setItemId(String.valueOf(album.getId()))
                                .setItemCover(album.getAlbumCover())
                                .setContent(album.getTitle());
                    }
                    break;
                default:
                    WebNote note = noteMap.get(model.getLikeOrCollectionId());
                    if (note != null) {
                        likeOrCollectVo.setItemId(String.valueOf(note.getId()))
                                .setItemCover(note.getNoteCover());
                    }
                    break;
            }
            likeOrCollectVOList.add(likeOrCollectVo);
        }
        result.setRecords(likeOrCollectVOList);
        return result;
    }

    private Map<String, CommentVO> loadCommentDetails(Set<String> commentIds) {
        if (commentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<WebComment> comments = commentMapper.selectByIds(commentIds);
        Set<String> noteIds = comments.stream().map(WebComment::getNid).collect(Collectors.toSet());
        Map<String, WebNote> notes = noteIds.isEmpty() ? Collections.emptyMap()
                : noteMapper.selectByIds(noteIds).stream().collect(Collectors.toMap(WebNote::getId, note -> note));
        Map<String, CommentVO> result = new HashMap<>();
        for (WebComment comment : comments) {
            CommentVO commentVo = ConvertUtils.sourceToTarget(comment, CommentVO.class);
            WebNote note = notes.get(comment.getNid());
            if (note != null) {
                commentVo.setNoteCover(note.getNoteCover());
                commentVo.setNid(note.getId());
            }
            result.put(comment.getId(), commentVo);
        }
        return result;
    }

    /**
     * 点赞
     */
    private void updateLikeCollectionCount(LikeOrCollectDTO likeOrCollectDTO, int val) {
        switch (likeOrCollectDTO.getType()) {
            case 1:
                if (noteMapper.adjustLikeCount(likeOrCollectDTO.getLikeOrCollectionId(), val) == 0) {
                    throw new HongshuException("笔记不存在", 404);
                }
                break;
            case 2:
                if (commentMapper.adjustLikeCount(likeOrCollectDTO.getLikeOrCollectionId(), val) == 0) {
                    throw new HongshuException("评论不存在", 404);
                }
                break;
            case 3:
                String currentUid = AuthContextHolder.getUserId();
                WebNote note = noteMapper.selectById(likeOrCollectDTO.getLikeOrCollectionId());
                if (note == null || noteMapper.adjustCollectionCount(note.getId(), val) == 0) {
                    throw new HongshuException("笔记不存在", 404);
                }
                List<WebAlbum> matches = albumMapper.selectList(new QueryWrapper<WebAlbum>().eq("uid", currentUid).eq("type", 0).orderByAsc("id").last("LIMIT 1"));
                WebAlbum album = matches.isEmpty() ? null : matches.getFirst();
                if (album == null && val > 0) {
                    album = new WebAlbum();
                    album.setTitle("默认专辑");
                    album.setUid(currentUid);
                    album.setType(0);
                    album.setAlbumCover(note.getNoteCover());
                    album.setCreateTime(new Date());
                    album.setUpdateTime(new Date());
                    albumMapper.insert(album);
                }
                if (album == null) {
                    break;
                }
                QueryWrapper<WebAlbumNoteRelation> relationQuery = new QueryWrapper<WebAlbumNoteRelation>().eq("aid", album.getId()).eq("nid", note.getId());
                if (val > 0 && albumNoteRelationMapper.selectCount(relationQuery) == 0) {
                    WebAlbumNoteRelation relation = new WebAlbumNoteRelation();
                    relation.setAid(album.getId());
                    relation.setNid(note.getId());
                    relation.setSort(Math.toIntExact(albumNoteRelationMapper.selectCount(new QueryWrapper<WebAlbumNoteRelation>().eq("aid", album.getId()))));
                    albumNoteRelationMapper.insert(relation);
                } else if (val < 0) {
                    albumNoteRelationMapper.delete(relationQuery);
                }
                albumMapper.refreshImageCount(album.getId());
                break;
            default:
                // 收藏专辑
                if (albumMapper.adjustCollectionCount(likeOrCollectDTO.getLikeOrCollectionId(), val) == 0) {
                    throw new HongshuException("专辑不存在", 404);
                }
                break;
        }
    }
}
