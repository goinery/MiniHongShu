package com.hongshu.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.hongshu.web.domain.entity.WebTag;
import com.hongshu.web.domain.vo.TagVO;
import com.hongshu.web.mapper.WebTagMapper;
import com.hongshu.web.service.IWebTagService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 标签
 *
 * @author hongshu
 */
@Service
public class WebTagServiceImpl extends ServiceImpl<WebTagMapper, WebTag> implements IWebTagService {

    /**
     * 获取热门标签
     */
    @Override
    public List<TagVO> getHotTagList() {
        return this.list(new QueryWrapper<WebTag>().orderByDesc("like_count").last("LIMIT 20")).stream()
                .map(tag -> com.hongshu.common.utils.ConvertUtils.sourceToTarget(tag, TagVO.class)).toList();
    }

    /**
     * 根据关键词获取标签
     *
     * @param currentPage 当前页
     * @param pageSize    分页数
     * @param keyword     关键词
     */
    @Override
    public Page<WebTag> getTagByKeyword(long currentPage, long pageSize, String keyword) {
        QueryWrapper<WebTag> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(keyword)) {
            queryWrapper.like("title", keyword);
        }
        queryWrapper.orderByDesc("like_count");
        return this.page(new Page<>((int) currentPage, (int) pageSize), queryWrapper);
    }
}
