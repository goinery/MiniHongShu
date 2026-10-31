package com.hongshu.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.hongshu.common.utils.ConvertUtils;
import com.hongshu.web.auth.AuthContextHolder;
import com.hongshu.web.domain.dto.EsNoteDTO;
import com.hongshu.web.domain.entity.WebLikeOrCollect;
import com.hongshu.web.domain.entity.WebNavbar;
import com.hongshu.web.domain.entity.WebNote;
import com.hongshu.web.domain.entity.WebUser;
import com.hongshu.web.domain.vo.NoteSearchVO;
import com.hongshu.web.mapper.WebLikeOrCollectMapper;
import com.hongshu.web.mapper.WebNavbarMapper;
import com.hongshu.web.mapper.WebNoteMapper;
import com.hongshu.web.mapper.WebUserMapper;
import com.hongshu.web.service.IWebEsNoteService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 笔记搜索（开源版走 MySQL，接口路径保持兼容）
 *
 * @author hongshu
 */
@Service
public class WebEsNoteServiceImpl extends ServiceImpl<WebNoteMapper, WebNote> implements IWebEsNoteService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int NOTE_LIKE_TYPE = 1;
    private static final int SORT_BY_LIKES = 1;
    private static final String AUDIT_APPROVED = "1";
    private static final String VIDEO_NOTE_TYPE = "2";

    private final WebUserMapper userMapper;
    private final WebNavbarMapper categoryMapper;
    private final WebLikeOrCollectMapper likeOrCollectionMapper;

    public WebEsNoteServiceImpl(WebUserMapper userMapper, WebNavbarMapper categoryMapper,
                                WebLikeOrCollectMapper likeOrCollectionMapper) {
        this.userMapper = userMapper;
        this.categoryMapper = categoryMapper;
        this.likeOrCollectionMapper = likeOrCollectionMapper;
    }

    @Override
    public Page<NoteSearchVO> getNoteByDTO(long currentPage, long pageSize, EsNoteDTO esNoteDTO) {
        QueryWrapper<WebNote> qw = approvedNote();
        if (esNoteDTO != null) {
            if (StringUtils.isNotBlank(esNoteDTO.getKeyword())) {
                String keyword = esNoteDTO.getKeyword().trim();
                qw.and(w -> w.like("title", keyword).or().like("content", keyword).or().like("author", keyword));
            }
            String cid = esNoteDTO.getCid();
            String cpid = esNoteDTO.getCpid();
            if (StringUtils.isNotBlank(cid) && StringUtils.isNotBlank(cpid)) {
                qw.and(w -> w.eq("cid", cid).or().eq("cpid", cpid).or().eq("cid", cpid).or().eq("cpid", cid));
            } else if (StringUtils.isNotBlank(cid)) {
                qw.and(w -> w.eq("cid", cid).or().eq("cpid", cid));
            } else if (StringUtils.isNotBlank(cpid)) {
                qw.and(w -> w.eq("cpid", cpid).or().eq("cid", cpid));
            }
            Integer type = esNoteDTO.getType();
            if (Integer.valueOf(SORT_BY_LIKES).equals(type)) {
                qw.orderByDesc("like_count");
            } else {
                qw.orderByDesc("time").orderByDesc("update_time");
            }
        } else {
            qw.orderByDesc("time").orderByDesc("update_time");
        }
        return toSearchPage(currentPage, pageSize, qw);
    }

    @Override
    public List<WebNavbar> getCategoryAgg(EsNoteDTO esNoteDTO) {
        QueryWrapper<WebNavbar> qw = new QueryWrapper<>();
        if (esNoteDTO != null && StringUtils.isNotBlank(esNoteDTO.getKeyword())) {
            qw.like("title", esNoteDTO.getKeyword());
        }
        return categoryMapper.selectList(qw);
    }

    @Override
    public Page<NoteSearchVO> getRecommendNote(long currentPage, long pageSize) {
        QueryWrapper<WebNote> qw = approvedNote().orderByDesc("time").orderByDesc("update_time");
        return toSearchPage(currentPage, pageSize, qw);
    }

    @Override
    public void addNote(NoteSearchVO noteSearchVo) {
        // 开源版直接读 MySQL，不再写搜索引擎
    }

    @Override
    public void updateNote(NoteSearchVO noteSearchVo) {
        // 开源版直接读 MySQL，不再写搜索引擎
    }

    @Override
    public void deleteNote(String noteId) {
        // 开源版直接读 MySQL，不再写搜索引擎
    }

    @Override
    public void addNoteBulkData() {
        // 开源版直接读 MySQL，不再写搜索引擎
    }

    @Override
    public void delNoteBulkData() {
        // 开源版直接读 MySQL，不再写搜索引擎
    }

    @Override
    public void refreshNoteData() {
        // 开源版直接读 MySQL，不再写搜索引擎
    }

    /**
     * 开源版只展示已审核通过的图文。
     * 图文兼容历史类型值 0/1，视频类型为 2。
     */
    private QueryWrapper<WebNote> approvedNote() {
        return new QueryWrapper<WebNote>()
                .eq("audit_status", AUDIT_APPROVED)
                .and(w -> w.ne("note_type", VIDEO_NOTE_TYPE).or().isNull("note_type"));
    }

    private Page<NoteSearchVO> toSearchPage(long currentPage, long pageSize, QueryWrapper<WebNote> qw) {
        long pageNumber = Math.max(1, currentPage);
        long limit = Math.clamp(pageSize, 1, MAX_PAGE_SIZE);
        Page<WebNote> notePage = baseMapper.selectPage(new Page<>(pageNumber, limit), qw);
        Page<NoteSearchVO> page = new Page<>(notePage.getCurrent(), notePage.getSize());
        page.setTotal(notePage.getTotal());
        page.setRecords(toSearchVo(notePage.getRecords()));
        return page;
    }

    private List<NoteSearchVO> toSearchVo(List<WebNote> notes) {
        if (notes == null || notes.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> userIds = notes.stream().map(WebNote::getUid)
                .filter(StringUtils::isNotBlank).collect(Collectors.toSet());
        Map<String, WebUser> userMap = userIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectByIds(userIds).stream()
                .collect(Collectors.toMap(WebUser::getId, user -> user));
        Set<String> noteIds = notes.stream().map(WebNote::getId).collect(Collectors.toSet());
        String currentUid = AuthContextHolder.getUserId();
        Set<String> likedIds = Collections.emptySet();
        if (StringUtils.isNotBlank(currentUid)) {
            likedIds = likeOrCollectionMapper.selectList(new QueryWrapper<WebLikeOrCollect>()
                            .eq("uid", currentUid).eq("type", NOTE_LIKE_TYPE).in("like_or_collection_id", noteIds))
                    .stream().map(WebLikeOrCollect::getLikeOrCollectionId).collect(Collectors.toSet());
        }
        Set<String> liked = likedIds;
        return notes.stream().map(note -> {
            NoteSearchVO vo = ConvertUtils.sourceToTarget(note, NoteSearchVO.class);
            WebUser user = userMap.get(note.getUid());
            if (user != null) {
                vo.setUsername(user.getUsername());
                vo.setAvatar(user.getAvatar());
            }
            vo.setIsLike(liked.contains(note.getId()));
            vo.setIsLoading(true);
            return vo;
        }).collect(Collectors.toList());
    }
}
