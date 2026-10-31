package com.hongshu.web.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.hongshu.common.enums.ResultCodeEnum;
import com.hongshu.common.exception.web.HongshuException;
import com.hongshu.common.utils.ConvertUtils;
import com.hongshu.web.auth.AuthContextHolder;
import com.hongshu.web.domain.dto.NoteDTO;
import com.hongshu.web.domain.entity.WebLikeOrCollect;
import com.hongshu.web.domain.entity.WebNavbar;
import com.hongshu.web.domain.entity.WebNote;
import com.hongshu.web.domain.entity.WebUser;
import com.hongshu.web.domain.vo.NoteVO;
import com.hongshu.web.mapper.WebLikeOrCollectMapper;
import com.hongshu.web.mapper.WebNoteMapper;
import com.hongshu.web.mapper.WebUserMapper;
import com.hongshu.web.service.IWebFollowService;
import com.hongshu.web.service.IWebNavbarService;
import com.hongshu.web.service.IWebNoteService;
import com.hongshu.web.service.NoteAccessService;
import com.hongshu.web.service.NoteAlbumService;
import com.hongshu.web.service.NoteDeletionService;
import com.hongshu.web.service.NoteMediaService;
import com.hongshu.web.service.NoteTagService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author hongshu
 */
@Service
public class WebNoteServiceImpl extends ServiceImpl<WebNoteMapper, WebNote> implements IWebNoteService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_PINNED_NOTES = 3;
    private static final int NOTE_LIKE_TYPE = 1;
    private static final int NOTE_COLLECTION_TYPE = 3;
    private static final int VIDEO_NOTE_TYPE = 2;
    private static final String IMAGE_NOTE_TYPE = "1";
    private static final String AUDIT_APPROVED = "1";
    private static final String AUDIT_PENDING = "0";
    private static final String NOTE_ACTIVE = "1";
    private static final String PINNED = "1";
    private static final String UNPINNED = "0";
    private static final String ROOT_CATEGORY_ID = "0";
    private static final String LEGACY_ROOT_CATEGORY_ID = "-1";

    private final WebUserMapper userMapper;
    private final IWebNavbarService categoryService;
    private final IWebFollowService followerService;
    private final WebLikeOrCollectMapper likeOrCollectMapper;
    private final NoteAccessService noteAccess;
    private final NoteDeletionService noteDeletion;
    private final NoteTagService noteTags;
    private final NoteAlbumService noteAlbums;
    private final NoteMediaService noteMedia;

    public WebNoteServiceImpl(WebUserMapper userMapper, IWebNavbarService categoryService,
                              IWebFollowService followerService, WebLikeOrCollectMapper likeOrCollectMapper,
                              NoteAccessService noteAccess, NoteDeletionService noteDeletion,
                              NoteTagService noteTags, NoteAlbumService noteAlbums, NoteMediaService noteMedia) {
        this.userMapper = userMapper;
        this.categoryService = categoryService;
        this.followerService = followerService;
        this.likeOrCollectMapper = likeOrCollectMapper;
        this.noteAccess = noteAccess;
        this.noteDeletion = noteDeletion;
        this.noteTags = noteTags;
        this.noteAlbums = noteAlbums;
        this.noteMedia = noteMedia;
    }

    private void applyCategory(WebNote note, String cid) {
        if (note == null || StringUtils.isBlank(cid)) {
            throw new HongshuException("请选择分类");
        }
        WebNavbar category = categoryService.getById(cid);
        if (category == null) {
            throw new HongshuException("分类不存在");
        }
        note.setCid(String.valueOf(category.getId()));
        String pid = category.getPid();
        if (StringUtils.isBlank(pid) || ROOT_CATEGORY_ID.equals(pid) || LEGACY_ROOT_CATEGORY_ID.equals(pid)) {
            note.setCpid(String.valueOf(category.getId()));
        } else {
            note.setCpid(pid);
        }
    }

    private String resolveNoteType(NoteDTO noteDTO) {
        Integer type = noteDTO == null ? null : noteDTO.getType();
        if (Integer.valueOf(VIDEO_NOTE_TYPE).equals(type)) {
            return Integer.toString(VIDEO_NOTE_TYPE);
        }
        return IMAGE_NOTE_TYPE;
    }

    /**
     * 获取笔记
     *
     * @param noteId 笔记ID
     */
    @Override
    public NoteVO getNoteById(String noteId) {
        WebNote note = noteAccess.requireReadable(noteId);
        if (note == null) {
            throw new HongshuException(ResultCodeEnum.FAIL);
        }
        WebUser user = userMapper.selectById(note.getUid());
        if (user == null) {
            throw new HongshuException(ResultCodeEnum.FAIL);
        }
        NoteVO noteVo = ConvertUtils.sourceToTarget(note, NoteVO.class);
        noteVo.setUsername(user.getUsername())
                .setAvatar(user.getAvatar())
                .setTime(note.getUpdateTime() == null ? 0L : note.getUpdateTime().getTime());

        boolean follow = followerService.isFollow(String.valueOf(user.getId()));
        noteVo.setIsFollow(follow);

        String currentUid = AuthContextHolder.getUserId();
        Set<Integer> types = Collections.emptySet();
        if (StringUtils.isNotBlank(currentUid)) {
            types = likeOrCollectMapper.selectList(new QueryWrapper<WebLikeOrCollect>()
                            .eq("like_or_collection_id", noteId).eq("uid", currentUid))
                    .stream().map(WebLikeOrCollect::getType).collect(Collectors.toSet());
        }
        noteVo.setIsLike(types.contains(NOTE_LIKE_TYPE));
        noteVo.setIsCollection(types.contains(NOTE_COLLECTION_TYPE));
        noteVo.setTagList(noteTags.listByNote(noteId));

        // 阅读行为只递增统计字段，避免旧实体覆盖并发编辑或审核结果。
        baseMapper.incrementViewCount(noteId);
        return noteVo;
    }

    /**
     * 新增笔记
     *
     * @param noteData 笔记对象
     * @param files    图片文件
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String saveNoteByDTO(String noteData, MultipartFile[] files) {
        String currentUid = AuthContextHolder.getUserId();
        if (StringUtils.isBlank(currentUid)) {
            throw new HongshuException(ResultCodeEnum.TOKEN_FAIL);
        }
        WebUser user = userMapper.selectById(currentUid);
        if (user == null) {
            throw new HongshuException(ResultCodeEnum.FAIL);
        }
        NoteDTO noteDTO = JSONUtil.toBean(noteData, NoteDTO.class);
        if (noteDTO == null || StringUtils.isBlank(noteDTO.getTitle()) || StringUtils.isBlank(noteDTO.getCid())) {
            throw new HongshuException("请填写标题并选择分类");
        }
        List<String> urlList = noteMedia.resolveUrls(noteDTO, files, null);
        userMapper.incrementTrendCount(currentUid);

        WebNote note = ConvertUtils.sourceToTarget(noteDTO, WebNote.class);
        note.setId(null);
        note.setUid(currentUid);
        note.setAuthor(user.getUsername());
        note.setStatus(NOTE_ACTIVE);
        note.setAuditStatus(AUDIT_PENDING);
        applyCategory(note, noteDTO.getCid());
        note.setNoteType(resolveNoteType(noteDTO));
        note.setCreator(user.getUsername());
        note.setTime(System.currentTimeMillis());
        note.setCreateTime(new Date());
        note.setUpdateTime(new Date());
        note.setLikeCount(0L);
        note.setCollectionCount(0L);
        note.setCommentCount(0L);
        note.setViewCount(0L);
        note.setCount(urlList.size());
        note.setUrls(JSONUtil.toJsonStr(urlList));
        note.setNoteCover(noteMedia.resolveCover(noteDTO, urlList));
        this.save(note);
        noteTags.replace(note.getId(), noteDTO.getTagList());
        noteAlbums.bind(note.getId(), noteDTO.getAlbumId(), currentUid);
        return note.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNoteByIds(List<String> noteIds) {
        String currentUid = AuthContextHolder.getUserId();
        if (StringUtils.isBlank(currentUid)) {
            throw new HongshuException(ResultCodeEnum.TOKEN_FAIL);
        }
        if (noteIds == null || noteIds.isEmpty()) {
            return;
        }
        noteDeletion.deleteOwned(noteIds, currentUid);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateNoteByDTO(String noteData, MultipartFile[] files) {
        String currentUid = AuthContextHolder.getUserId();
        if (StringUtils.isBlank(currentUid)) {
            throw new HongshuException(ResultCodeEnum.TOKEN_FAIL);
        }
        NoteDTO noteDTO = JSONUtil.toBean(noteData, NoteDTO.class);
        if (noteDTO == null || StringUtils.isBlank(noteDTO.getId()) || StringUtils.isBlank(noteDTO.getTitle())) {
            throw new HongshuException(ResultCodeEnum.FAIL);
        }
        WebNote exist = baseMapper.selectList(new QueryWrapper<WebNote>().eq("id", noteDTO.getId()).last("FOR UPDATE"))
                .stream().findFirst().orElse(null);
        if (exist == null || !currentUid.equals(exist.getUid())) {
            throw new HongshuException(ResultCodeEnum.FAIL);
        }
        List<String> urlList = noteMedia.resolveUrls(noteDTO, files, exist);
        String oldUrls = exist.getUrls();
        String oldCover = exist.getNoteCover();
        String oldContent = exist.getContent();

        exist.setTitle(noteDTO.getTitle());
        exist.setContent(noteDTO.getContent());
        applyCategory(exist, noteDTO.getCid());
        exist.setNoteCoverHeight(noteDTO.getNoteCoverHeight());
        exist.setCount(urlList.size());
        exist.setUrls(JSONUtil.toJsonStr(urlList));
        exist.setNoteCover(noteMedia.resolveCover(noteDTO, urlList));
        exist.setAuditStatus(AUDIT_PENDING);
        exist.setNoteType(resolveNoteType(noteDTO));
        exist.setTime(System.currentTimeMillis());
        exist.setUpdateTime(new Date());
        if (!this.updateById(exist)) {
            throw new HongshuException("笔记保存失败");
        }
        noteMedia.cleanAfterCommit(oldCover, oldUrls, oldContent);
        noteTags.replace(exist.getId(), noteDTO.getTagList());
        noteAlbums.replace(exist.getId(), noteDTO.getAlbumId(), currentUid);
    }

    @Override
    public Page<NoteVO> getHotPage(long currentPage, long pageSize) {
        Page<WebNote> page = this.page(new Page<>(Math.max(1, currentPage), Math.clamp(pageSize, 1, MAX_PAGE_SIZE)),
                new QueryWrapper<WebNote>().eq("audit_status", AUDIT_APPROVED).orderByDesc("like_count", "view_count", "create_time"));
        Page<NoteVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        Set<String> userIds = page.getRecords().stream().map(WebNote::getUid)
                .filter(StringUtils::isNotBlank).collect(Collectors.toSet());
        Map<String, WebUser> users = userIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectByIds(userIds).stream()
                .collect(Collectors.toMap(WebUser::getId, user -> user));
        result.setRecords(page.getRecords().stream().map(note -> {
            NoteVO vo = ConvertUtils.sourceToTarget(note, NoteVO.class);
            WebUser user = users.get(note.getUid());
            if (user != null) {
                vo.setUsername(user.getUsername()).setAvatar(user.getAvatar());
            }
            vo.setTime(note.getCreateTime() == null ? 0L : note.getCreateTime().getTime());
            return vo;
        }).collect(Collectors.toList()));
        return result;
    }

    @Override
    public boolean pinnedNote(String noteId) {
        String currentUid = AuthContextHolder.getUserId();
        WebNote note = this.getById(noteId);
        if (note == null || !Objects.equals(currentUid, note.getUid())) {
            throw new HongshuException("只能置顶自己的笔记", 403);
        }
        if (PINNED.equals(note.getPinned())) {
            note.setPinned(UNPINNED);
        } else {
            long count = this.count(new QueryWrapper<WebNote>().eq("uid", currentUid).eq("pinned", PINNED));
            if (count >= MAX_PINNED_NOTES) {
                throw new HongshuException("最多只能置顶3个笔记");
            }
            note.setPinned(PINNED);
            note.setUpdateTime(new Date());
        }
        return this.update(new UpdateWrapper<WebNote>().eq("id", noteId)
                .set("pinned", note.getPinned()).set("update_time", new Date()));
    }
}
