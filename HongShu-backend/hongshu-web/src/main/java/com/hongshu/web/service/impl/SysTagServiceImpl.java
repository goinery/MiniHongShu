package com.hongshu.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hongshu.common.constant.HttpStatus;
import com.hongshu.common.core.domain.Query;
import com.hongshu.common.exception.web.HongshuException;
import com.hongshu.common.validator.ValidatorUtil;
import com.hongshu.web.domain.entity.WebTag;
import com.hongshu.web.mapper.SysTagMapper;
import com.hongshu.web.service.ISysTagService;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 会员信息 服务层处理
 *
 * @author hongshu
 */
@Service
public class SysTagServiceImpl implements ISysTagService {

    private final SysTagMapper tagMapper;

    public SysTagServiceImpl(SysTagMapper tagMapper) {
        this.tagMapper = tagMapper;
    }

    /**
     * 查询会员信息集合
     *
     * @param query 会员信息
     */
    @Override
    public List<WebTag> selectTagList(Query query) {
        QueryWrapper<WebTag> qw = new QueryWrapper<>();
        qw.lambda().like(ValidatorUtil.isNotNull(query.getTitle()), WebTag::getTitle, query.getTitle());
        qw.lambda().ge(ValidatorUtil.isNotNull(query.get("params[beginTime]")), WebTag::getCreateTime, query.get("params[beginTime]"));
        qw.lambda().le(ValidatorUtil.isNotNull(query.get("params[endTime]")), WebTag::getCreateTime, query.get("params[endTime]"));
        qw.orderByDesc("like_count");
        return tagMapper.selectList(qw);
    }

    /**
     * 查询所有会员
     */
    @Override
    public List<WebTag> selectTagAll() {
        return tagMapper.selectList(new QueryWrapper<>());
    }

    /**
     * 通过会员ID查询会员信息
     *
     * @param id 会员ID
     */
    @Override
    public WebTag selectTagById(Long id) {
        return tagMapper.selectById(id);
    }

    /**
     * 新增会员信息
     *
     * @param tag 会员信息
     */
    @Override
    public int insertTag(WebTag tag) {
        tag.setTitle(tag.getTitle());
        tag.setSort(tag.getSort());
        tag.setCreateTime(new Date());
        tag.setUpdateTime(new Date());
        return tagMapper.insert(tag);
    }

    /**
     * 修改保存会员信息
     *
     * @param tag 会员信息
     */
    @Override
    public int updateTag(WebTag tag) {
        WebTag webTag = tagMapper.selectById(tag.getId());
        if (webTag == null) {
            throw new HongshuException("标签不存在", HttpStatus.NOT_FOUND);
        }
        webTag.setTitle(tag.getTitle());
        webTag.setSort(tag.getSort());
        webTag.setUpdater("System");
        webTag.setUpdateTime(new Date());
        return tagMapper.updateById(webTag);
    }

    /**
     * 删除会员信息
     *
     * @param id 会员ID
     */
    @Override
    public int deleteTagById(Long id) {
        return tagMapper.deleteById(id);
    }

    /**
     * 批量删除会员信息
     *
     * @param ids 需要删除的会员ID
     */
    @Override
    public int deleteTagByIds(Long[] ids) {
        if (ids == null || ids.length == 0) {
            return 0;
        }
        List<Long> tagIds = Arrays.stream(ids).filter(Objects::nonNull).distinct().toList();
        if (tagIds.isEmpty()) {
            return 0;
        }
        // SQL DELETE 本身忽略不存在的 ID，无需修改 Arrays.asList 返回的固定长度列表。
        return tagMapper.deleteByIds(tagIds);
    }
}
