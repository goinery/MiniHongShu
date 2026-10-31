package com.hongshu.web.service;

import cn.hutool.json.JSONException;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hongshu.web.domain.entity.WebTag;
import com.hongshu.web.domain.entity.WebTagNoteRelation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 笔记标签的解析、关联和读取。
 *
 * @author hongshu
 */
@Slf4j
@Service
public class NoteTagService {

    private final IWebTagService tagService;
    private final IWebTagNoteRelationService relationService;

    public NoteTagService(IWebTagService tagService, IWebTagNoteRelationService relationService) {
        this.tagService = tagService;
        this.relationService = relationService;
    }

    /**
     * 替换标签关系，与笔记写入加入同一事务。
     *
     * @param noteId 笔记 ID
     * @param tags 兼容标题字符串和包含 title/name 的 JSON 字符串
     */
    @Transactional(rollbackFor = Exception.class)
    public void replace(String noteId, List<String> tags) {
        relationService.remove(new QueryWrapper<WebTagNoteRelation>().eq("nid", noteId));
        Set<String> titles = parseTitles(tags);
        List<WebTagNoteRelation> relations = new ArrayList<>(titles.size());
        for (String title : titles) {
            WebTag tag = tagService.getOne(new QueryWrapper<WebTag>().eq("title", title), false);
            if (tag == null) {
                tag = new WebTag();
                tag.setTitle(title);
                tag.setLikeCount(1L);
                tagService.save(tag);
            }
            WebTagNoteRelation relation = new WebTagNoteRelation();
            relation.setTid(tag.getId());
            relation.setNid(noteId);
            relations.add(relation);
        }
        if (!relations.isEmpty()) {
            relationService.saveBatch(relations);
        }
    }

    /**
     * 查询笔记的去重标签。
     *
     * @param noteId 笔记 ID
     * @return 标签列表，无标签时为空列表
     */
    public List<WebTag> listByNote(String noteId) {
        List<String> tagIds = relationService.list(new QueryWrapper<WebTagNoteRelation>().eq("nid", noteId))
                .stream().map(WebTagNoteRelation::getTid).distinct().toList();
        return tagIds.isEmpty() ? Collections.emptyList() : tagService.listByIds(tagIds);
    }

    private Set<String> parseTitles(List<String> tags) {
        Set<String> titles = new LinkedHashSet<>();
        if (tags == null) {
            return titles;
        }
        for (String tag : tags) {
            if (StringUtils.isBlank(tag)) {
                continue;
            }
            String title = tag.trim();
            if (title.startsWith("{")) {
                try {
                    JSONObject value = JSONUtil.parseObj(title);
                    title = StringUtils.defaultIfBlank(value.getStr("title"), value.getStr("name"));
                } catch (JSONException exception) {
                    // 兼容以左花括号开头的普通标签，不把标签内容写入日志。
                    log.debug("标签不是合法 JSON，按普通标题处理", exception);
                }
            }
            if (StringUtils.isNotBlank(title)) {
                titles.add(title.trim());
            }
        }
        return titles;
    }
}
