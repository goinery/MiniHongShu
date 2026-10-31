package com.hongshu.web.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hongshu.common.constant.Constantss;
import com.hongshu.common.core.domain.Query;
import com.hongshu.common.enums.ResultCodeEnum;
import com.hongshu.common.exception.web.HongshuException;
import com.hongshu.common.global.SysConf;
import com.hongshu.common.utils.*;
import com.hongshu.common.validator.ValidatorUtil;
import com.hongshu.web.domain.entity.WebNavbar;
import com.hongshu.web.domain.entity.WebNote;
import com.hongshu.web.mapper.SysNavbarMapper;
import com.hongshu.web.mapper.SysNoteMapper;
import com.hongshu.web.service.ISysNoteService;
import com.hongshu.web.service.IWebOssService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

/**
 * 笔记信息 服务层处理
 *
 * @author hongshu
 */
@Slf4j
@Service
public class SysNoteServiceImpl implements ISysNoteService {

    private final com.hongshu.web.service.NoteDeletionService noteDeletion;
    private final com.hongshu.web.service.FileAssetService assets;
    private final SysNoteMapper noteMapper;
    private final SysNavbarMapper navbarMapper;
    private final IWebOssService ossService;

    public SysNoteServiceImpl(com.hongshu.web.service.NoteDeletionService noteDeletion, com.hongshu.web.service.FileAssetService assets, SysNoteMapper noteMapper, SysNavbarMapper navbarMapper, IWebOssService ossService) {
        this.noteDeletion = noteDeletion;
        this.assets = assets;
        this.noteMapper = noteMapper;
        this.navbarMapper = navbarMapper;
        this.ossService = ossService;
    }

    /**
     * 查询笔记信息集合
     */
    @Override
    public List<WebNote> getAllNoteList() {
        QueryWrapper<WebNote> qw = new QueryWrapper<>();
        // 审核通过的笔记
        qw.lambda().eq(WebNote::getAuditStatus, "1");
        return noteMapper.selectList(qw);
    }

    /**
     * 查询笔记信息集合
     *
     * @param query 笔记信息
     */
    @Override
    public List<WebNote> selectNoteList(Query query) {
        QueryWrapper<WebNote> qw = new QueryWrapper<>();
        qw.lambda().like(ValidatorUtil.isNotNull(query.getTitle()), WebNote::getTitle, query.getTitle());
        qw.lambda().like(ValidatorUtil.isNotNull(query.getUid()), WebNote::getUid, query.getUid());
        qw.lambda().like(ValidatorUtil.isNotNull(query.getNoteType()), WebNote::getNoteType, query.getNoteType());
        qw.lambda().eq(ValidatorUtil.isNotNull(query.get("auditStatus")), WebNote::getAuditStatus, query.get("auditStatus"));
        qw.lambda().eq(ValidatorUtil.isNotNull(query.get("pid")), WebNote::getCpid, query.get("pid"));
        qw.lambda().ge(ValidatorUtil.isNotNull(query.get("params[beginTime]")), WebNote::getCreateTime, query.get("params[beginTime]"));
        qw.lambda().le(ValidatorUtil.isNotNull(query.get("params[endTime]")), WebNote::getCreateTime, query.get("params[endTime]"));
        qw.lambda().orderByDesc(WebNote::getUpdateTime);
        return noteMapper.selectList(qw);
    }

    /**
     * 获取未审核笔记列表
     */
    @Override
    public List<WebNote> selectUnAuditNoteList(Query query) {
        QueryWrapper<WebNote> qw = new QueryWrapper<>();
        qw.lambda().like(ValidatorUtil.isNotNull(query.getTitle()), WebNote::getTitle, query.getTitle());
        qw.lambda().like(ValidatorUtil.isNotNull(query.getUid()), WebNote::getUid, query.getUid());
        qw.lambda().like(ValidatorUtil.isNotNull(query.getNoteType()), WebNote::getNoteType, query.getNoteType());
        qw.lambda().like(ValidatorUtil.isNull(query.get("auditStatus")), WebNote::getAuditStatus, 0);
        qw.lambda().ge(ValidatorUtil.isNotNull(query.get("params[beginTime]")), WebNote::getCreateTime, query.get("params[beginTime]"));
        qw.lambda().le(ValidatorUtil.isNotNull(query.get("params[endTime]")), WebNote::getCreateTime, query.get("params[endTime]"));
        qw.lambda().orderByDesc(WebNote::getCreateTime);
        return noteMapper.selectList(qw);
    }

    /**
     * 通过笔记ID查询笔记信息
     *
     * @param id 笔记ID
     */
    @Override
    public WebNote selectNoteById(Long id) {
        return noteMapper.selectById(id);
    }

    /**
     * 新增笔记信息
     *
     * @param note 笔记信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertNote(WebNote note, MultipartFile file) {
        // 上传头像
        if (ObjectUtils.isNotEmpty(file)) {
            String noteCover = ossService.save(file);
            note.setNoteCover(noteCover);
            assets.assignOwner(noteCover, note.getUid());
        }
        note.setCreator("System");
        note.setCreateTime(new Date());
        note.setUpdateTime(new Date());
        int inserted = noteMapper.insert(note);
        if (inserted != 1) throw new HongshuException("笔记保存失败");
        return inserted;
    }

    /**
     * 修改保存笔记信息
     *
     * @param note 笔记信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateNote(WebNote note, MultipartFile file) {
        WebNote previous = noteMapper.selectList(new QueryWrapper<WebNote>().eq("id", note.getId()).last("FOR UPDATE"))
                .stream().findFirst().orElse(null);
        if (previous == null) throw new HongshuException("笔记不存在");
        // 上传头像
        if (ObjectUtils.isNotEmpty(file)) {
            String noteCover = ossService.save(file);
            note.setNoteCover(noteCover);
            assets.assignOwner(noteCover, note.getUid() == null ? previous.getUid() : note.getUid());
        }
        note.setUpdater("System");
        note.setUpdateTime(new Date());
        int updated = noteMapper.updateById(note);
        if (updated != 1) throw new HongshuException("笔记保存失败");
        assets.cleanStoredReferencesAfterCommit(previous.getNoteCover());
        assets.cleanStoredContentAfterCommit(previous.getUrls(), previous.getContent());
        return updated;
    }

    /**
     * 批量删除笔记信息
     *
     * @param ids 需要删除的笔记ID
     */
    @Override
    public int deleteNoteByIds(Long[] ids) {
        return noteDeletion.delete(Arrays.stream(ids).map(String::valueOf).toList());
    }

    @Override
    public Integer getNoteCount(int status) {
        QueryWrapper<WebNote> queryWrapper = new QueryWrapper<>();
        return Math.toIntExact(noteMapper.selectCount(queryWrapper));
    }

    @Override
    public Map<String, Object> getNoteContributeCount() {

        // 获取今天结束时间
        String endTime = DateUtilss.getNowTime();
        // 获取365天前的日期
        Date temp = DateUtilss.getDate(endTime, -365);
        String startTime = DateUtilss.dateTimeToStr(temp);
        List<Map<String, Object>> noteContributeMap = noteMapper.getNoteContributeCount(startTime, endTime);
        List<String> dateList = DateUtilss.getDayBetweenDates(startTime, endTime);
        Map<String, Object> dateMap = new HashMap<>();
        for (Map<String, Object> itemMap : noteContributeMap) {
            dateMap.put(itemMap.get("DATE").toString(), itemMap.get("COUNT"));
        }

        List<List<Object>> resultList = new ArrayList<>();
        for (String item : dateList) {
            int count = 0;
            if (dateMap.get(item) != null) {
                count = Integer.parseInt(dateMap.get(item).toString());
            }
            List<Object> objectList = new ArrayList<>();
            objectList.add(item);
            objectList.add(count);
            resultList.add(objectList);
        }

        Map<String, Object> resultMap = new HashMap<>(Constantss.NUM_TWO);
        List<String> contributeDateList = new ArrayList<>();
        contributeDateList.add(startTime);
        contributeDateList.add(endTime);
        resultMap.put(SysConf.CONTRIBUTE_DATE, contributeDateList);
        resultMap.put(SysConf.BLOG_CONTRIBUTE_COUNT, resultList);
        // 将 全年博客贡献度 存入到Redis【过期时间2小时】
        return resultMap;
    }

    @Override
    public List<Map<String, Object>> getNoteCountByCategory() {
        // 从Redis中获取博客分类下包含的博客数量
        List<Map<String, Object>> noteCountByBlogSortMap = noteMapper.getNoteCountByCategory();
        Map<String, Integer> categoryMap = new HashMap<>();
        for (Map<String, Object> item : noteCountByBlogSortMap) {
            String cpid = String.valueOf(item.get("cpid"));
            // java.lang.Number是Integer,Long的父类
            Number num = (Number) item.get(SysConf.COUNT);
            int count = 0;
            if (num != null) {
                count = num.intValue();
            }
            categoryMap.put(cpid, count);
        }

        //把查询到的BlogSort放到Map中
        Set<String> blogSortUids = categoryMap.keySet();
        Collection<WebNavbar> blogSortCollection = new ArrayList<>();

        if (!blogSortUids.isEmpty()) {
            blogSortCollection = navbarMapper.selectByIds(blogSortUids);
        }

        Map<String, String> blogSortEntityMap = new HashMap<>();
        for (WebNavbar category : blogSortCollection) {
            if (StringUtilss.isNotEmpty(category.getTitle())) {
                blogSortEntityMap.put(String.valueOf(category.getId()), category.getTitle());
            }
        }

        List<Map<String, Object>> resultList = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : categoryMap.entrySet()) {

            String blogSortUid = entry.getKey();

            if (blogSortEntityMap.get(blogSortUid) != null) {
                String blogSortName = blogSortEntityMap.get(blogSortUid);
                Integer count = entry.getValue();
                Map<String, Object> itemResultMap = new HashMap<>();
                itemResultMap.put(SysConf.BLOG_SORT_UID, blogSortUid);
                itemResultMap.put(SysConf.NAME, blogSortName);
                itemResultMap.put(SysConf.VALUE, count);
                resultList.add(itemResultMap);
            }
        }
        return resultList;
    }

    /**
     * 按标签统计笔记数量。
     */
    @Override
    public List<Map<String, Object>> getNoteCountByTag() {
        return noteMapper.getNoteCountByTag();
    }

    /**
     * 审核管理
     *
     * @param noteId    笔记ID
     * @param auditType 审核状态
     */
    @Override
    public boolean auditNote(String noteId, String auditType) {
        if (!"pass".equals(auditType) && !"reject".equals(auditType)) throw new HongshuException("审核状态无效");
        WebNote note = noteMapper.selectOne(new QueryWrapper<WebNote>().eq("id", noteId));
        if (ObjectUtil.isEmpty(note)) {
            throw new HongshuException(ResultCodeEnum.FAIL);
        }
        // 通过：1
        if ("pass".equals(auditType)) {
            note.setAuditStatus("1");
        }
        // 拒绝：2
        if ("reject".equals(auditType)) {
            note.setAuditStatus("2");
        }
        note.setUpdateTime(new Date());
        noteMapper.updateById(note);
        return true;
    }
}
