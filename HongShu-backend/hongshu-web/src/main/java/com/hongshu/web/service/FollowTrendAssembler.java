package com.hongshu.web.service;

import cn.hutool.core.convert.ConvertException;
import cn.hutool.json.JSONException;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hongshu.web.domain.entity.WebLikeOrCollect;
import com.hongshu.web.domain.entity.WebNote;
import com.hongshu.web.domain.entity.WebUser;
import com.hongshu.web.domain.vo.TrendVO;
import com.hongshu.web.mapper.WebLikeOrCollectMapper;
import com.hongshu.web.mapper.WebUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 组装关注动态视图；关联用户和点赞均限定在当前笔记页内。
 *
 * @author hongshu
 */
@Slf4j
@Service
public class FollowTrendAssembler {

    private static final int NOTE_LIKE_TYPE = 1;
    private static final int MAX_PREVIEW_IMAGES = 4;
    private static final String DEFAULT_USERNAME = "用户";

    private final WebUserMapper userMapper;
    private final WebLikeOrCollectMapper likeMapper;

    public FollowTrendAssembler(WebUserMapper userMapper, WebLikeOrCollectMapper likeMapper) {
        this.userMapper = userMapper;
        this.likeMapper = likeMapper;
    }

    /** 两个历史入口保留各自的正文、图片数量和缺失作者处理方式。 */
    public enum ViewMode {
        TIMELINE,
        PREVIEW
    }

    /**
     * 把当前页笔记转换为兼容现有接口的动态视图。
     *
     * @param notes 当前页笔记
     * @param currentUserId 当前用户 ID
     * @param mode 动态展示方式
     * @return 动态视图列表
     */
    public List<TrendVO> assemble(List<WebNote> notes, String currentUserId, ViewMode mode) {
        if (notes == null || notes.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> userIds = notes.stream().map(WebNote::getUid)
                .filter(StringUtils::isNotBlank).collect(Collectors.toSet());
        Map<String, WebUser> users = userIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectByIds(userIds).stream()
                        .collect(Collectors.toMap(WebUser::getId, user -> user));
        Set<String> likedNoteIds = findLikedNoteIds(notes, currentUserId);
        List<TrendVO> result = new ArrayList<>(notes.size());
        for (WebNote note : notes) {
            WebUser user = users.get(note.getUid());
            if (mode == ViewMode.PREVIEW && user == null) {
                continue;
            }
            TrendVO view = new TrendVO();
            view.setUid(note.getUid())
                    .setUsername(user == null ? DEFAULT_USERNAME : user.getUsername())
                    .setAvatar(user == null ? null : user.getAvatar())
                    .setNid(note.getId())
                    .setTime(note.getUpdateTime() == null ? System.currentTimeMillis() : note.getUpdateTime().getTime())
                    .setContent(resolveContent(note, mode))
                    .setCommentCount(note.getCommentCount())
                    .setLikeCount(note.getLikeCount())
                    .setIsLike(likedNoteIds.contains(note.getId()))
                    .setIsLoading(false);
            List<String> images = parseImages(note.getUrls(), note.getNoteCover());
            if (mode == ViewMode.PREVIEW) {
                view.setViewCount(note.getViewCount());
                if (images.size() > MAX_PREVIEW_IMAGES) {
                    images = new ArrayList<>(images.subList(0, MAX_PREVIEW_IMAGES));
                }
            }
            view.setImgUrls(images);
            result.add(view);
        }
        return result;
    }

    private Set<String> findLikedNoteIds(List<WebNote> notes, String currentUserId) {
        if (StringUtils.isBlank(currentUserId)) {
            return Collections.emptySet();
        }
        Set<String> noteIds = notes.stream().map(WebNote::getId)
                .filter(StringUtils::isNotBlank).collect(Collectors.toSet());
        if (noteIds.isEmpty()) {
            return Collections.emptySet();
        }
        return likeMapper.selectList(new QueryWrapper<WebLikeOrCollect>()
                        .select("like_or_collection_id").eq("uid", currentUserId).eq("type", NOTE_LIKE_TYPE)
                        .in("like_or_collection_id", noteIds))
                .stream().map(WebLikeOrCollect::getLikeOrCollectionId).collect(Collectors.toSet());
    }

    private String resolveContent(WebNote note, ViewMode mode) {
        if (mode == ViewMode.TIMELINE && StringUtils.isNotBlank(note.getTitle())) {
            return note.getTitle();
        }
        return note.getContent();
    }

    private List<String> parseImages(String urls, String cover) {
        List<String> images = new ArrayList<>();
        if (StringUtils.isNotBlank(urls)) {
            try {
                List<String> parsed = JSONUtil.toList(urls.trim(), String.class);
                if (parsed != null) {
                    for (String image : parsed) {
                        if (StringUtils.isNotBlank(image)) {
                            images.add(image);
                        }
                    }
                }
            } catch (JSONException | ConvertException exception) {
                // 兼容历史单地址和逗号分隔的图片列表。
                log.debug("关注动态图片采用历史文本格式");
                for (String image : urls.split(",")) {
                    if (StringUtils.isNotBlank(image)) {
                        images.add(image.trim());
                    }
                }
            }
        }
        if (images.isEmpty() && StringUtils.isNotBlank(cover)) {
            images.add(cover);
        }
        return images;
    }
}
