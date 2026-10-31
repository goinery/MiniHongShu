package com.hongshu.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hongshu.common.validator.ValidatorUtil;
import com.hongshu.web.domain.NavbarTreeSelect;
import com.hongshu.web.domain.entity.WebNavbar;
import com.hongshu.web.mapper.SysNavbarMapper;
import com.hongshu.web.service.ISysNavbarService;
import com.hongshu.web.service.IWebOssService;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.hongshu.common.exception.web.HongshuException;
import com.hongshu.web.service.FileAssetService;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 导航栏 业务层处理
 *
 * @author hongshu
 */
@Service
public class SysNavbarServiceImpl implements ISysNavbarService {

    private final IWebOssService ossService;
    private final SysNavbarMapper navbarMapper;
    private final FileAssetService assets;

    public SysNavbarServiceImpl(IWebOssService ossService, SysNavbarMapper navbarMapper, FileAssetService assets) {
        this.ossService = ossService;
        this.navbarMapper = navbarMapper;
        this.assets = assets;
    }

    /**
     * 查询系统导航栏列表
     *
     * @param category 导航栏信息
     * @return 导航栏列表
     */
    @Override
    public List<WebNavbar> selectNavbarList(WebNavbar category) {
        QueryWrapper<WebNavbar> qw = new QueryWrapper<>();
        qw.lambda().like(ValidatorUtil.isNotNull(category.getTitle()), WebNavbar::getTitle, category.getTitle());
        qw.lambda().like(ValidatorUtil.isNotNull(category.getStatus()), WebNavbar::getStatus, category.getStatus());
        qw.orderByAsc("sort");
        return navbarMapper.selectList(qw);
    }

    /**
     * 根据导航栏ID查询信息
     *
     * @param id 导航栏ID
     * @return 导航栏信息
     */
    @Override
    public WebNavbar selectById(Long id) {
        return navbarMapper.selectById(id);
    }

    /**
     * 是否存在导航栏子节点
     *
     * @param id 导航栏ID
     * @return 结果
     */
    @Override
    public boolean hasChildById(Long id) {
        int result = navbarMapper.hasChildById(id);
        return result > 0;
    }

    /**
     * 新增导航栏信息
     *
     * @param category 导航栏信息
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertNavbar(WebNavbar category, MultipartFile file) {
        String normalCover = ossService.save(file);
        category.setNormalCover(normalCover);
        category.setCreator("system");
        category.setCreateTime(new Date());
        category.setUpdateTime(new Date());
        int inserted = navbarMapper.insert(category);
        if (inserted != 1) throw new HongshuException("分类保存失败");
        return inserted;
    }

    /**
     * 修改导航栏信息
     *
     * @param category 导航栏信息
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateNavbar(WebNavbar category, MultipartFile file) {
        WebNavbar previous = navbarMapper.selectList(new QueryWrapper<WebNavbar>().eq("id", category.getId()).last("FOR UPDATE"))
                .stream().findFirst().orElse(null);
        if (previous == null) throw new HongshuException("分类不存在");
        if (ObjectUtils.isNotEmpty(file)) {
            String normalCover = ossService.save(file);
            category.setNormalCover(normalCover);
        }
        category.setUpdater("system");
        category.setUpdateTime(new Date());
        int updated = navbarMapper.updateById(category);
        if (updated != 1) throw new HongshuException("分类保存失败");
        assets.cleanStoredReferencesAfterCommit(previous.getNormalCover(), previous.getHotCover());
        return updated;
    }

    /**
     * 删除导航栏信息
     *
     * @param id 导航栏ID
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteById(Long id) {
        WebNavbar previous = navbarMapper.selectList(new QueryWrapper<WebNavbar>().eq("id", id).last("FOR UPDATE"))
                .stream().findFirst().orElse(null);
        if (previous == null) return 0;
        int deleted = navbarMapper.deleteById(id);
        if (deleted > 0) assets.cleanStoredReferencesAfterCommit(previous.getNormalCover(), previous.getHotCover());
        return deleted;
    }

    /**
     * 递归列表
     *
     * @param list 分类表
     * @param t    子节点
     */
    private void recursionFn(List<WebNavbar> list, WebNavbar t) {
        // 得到子节点列表
        List<WebNavbar> childList = this.getChildList(list, t);
        t.setChildren(childList);
        for (WebNavbar tChild : childList) {
            if (hasChild(list, tChild)) {
                this.recursionFn(list, tChild);
            }
        }
    }

    /**
     * 构建前端所需要树结构
     *
     * @param categoryList 菜单列表
     * @return 树结构列表
     */
    @Override
    public List<WebNavbar> buildNavbarTree(List<WebNavbar> categoryList) {
        List<WebNavbar> returnList = new ArrayList<>();
        List<String> tempList = categoryList.stream().map(WebNavbar::getId).toList();
        for (WebNavbar category : categoryList) {
            // 如果是顶级节点, 遍历该父节点的所有子节点
            if (!tempList.contains(category.getPid())) {
                this.recursionFn(categoryList, category);
                returnList.add(category);
            }
        }
        if (returnList.isEmpty()) {
            returnList = categoryList;
        }
        return returnList;
    }

    /**
     * 构建前端所需要下拉树结构
     *
     * @param categoryList 菜单列表
     * @return 下拉树结构列表
     */
    @Override
    public List<NavbarTreeSelect> buildNavbarTreeSelect(List<WebNavbar> categoryList) {
        List<WebNavbar> navbarTrees = this.buildNavbarTree(categoryList);
        return navbarTrees.stream().map(NavbarTreeSelect::new).collect(Collectors.toList());
    }

    /**
     * 得到子节点列表
     */
    private List<WebNavbar> getChildList(List<WebNavbar> list, WebNavbar t) {
        List<WebNavbar> tlist = new ArrayList<>();
        for (WebNavbar n : list) {
            if (t.getId() != null && t.getId().equals(n.getPid())) {
                tlist.add(n);
            }
        }
        return tlist;
    }

    /**
     * 判断是否有子节点
     */
    private boolean hasChild(List<WebNavbar> list, WebNavbar t) {
        return !getChildList(list, t).isEmpty();
    }
}
