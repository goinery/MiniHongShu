package com.hongshu.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hongshu.common.core.domain.Query;
import com.hongshu.common.validator.ValidatorUtil;
import com.hongshu.web.domain.entity.WebAlbum;
import com.hongshu.web.mapper.SysAlbumMapper;
import com.hongshu.web.service.ISysAlbumService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.hongshu.common.exception.web.HongshuException;
import com.hongshu.web.service.FileAssetService;

import java.util.Date;
import java.util.List;

/**
 * 会员信息 服务层处理
 *
 * @author hongshu
 */
@Slf4j
@Service
public class SysAlbumServiceImpl implements ISysAlbumService {

    private final SysAlbumMapper albumMapper;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    private final FileAssetService assets;

    public SysAlbumServiceImpl(SysAlbumMapper albumMapper, org.springframework.jdbc.core.JdbcTemplate jdbc, FileAssetService assets) {
        this.albumMapper = albumMapper;
        this.jdbc = jdbc;
        this.assets = assets;
    }

    /**
     * 查询会员信息集合
     *
     * @param query 会员信息
     */
    @Override
    public List<WebAlbum> getAlbumList(Query query) {
        QueryWrapper<WebAlbum> qw = new QueryWrapper<>();
        qw.lambda().like(ValidatorUtil.isNotNull(query.getTitle()), WebAlbum::getTitle, query.getTitle());
        qw.lambda().ge(ValidatorUtil.isNotNull(query.get("params[beginTime]")), WebAlbum::getCreateTime, query.get("params[beginTime]"));
        qw.lambda().le(ValidatorUtil.isNotNull(query.get("params[endTime]")), WebAlbum::getCreateTime, query.get("params[endTime]"));
        qw.orderByDesc("create_time");
        return albumMapper.selectList(qw);
    }

    /**
     * 查询所有会员
     */
    @Override
    public List<WebAlbum> selectAlbumAll() {
        return albumMapper.selectList(new QueryWrapper<>());
    }

    /**
     * 通过会员ID查询会员信息
     *
     * @param id 会员ID
     */
    @Override
    public WebAlbum getAlbumById(Long id) {
        return albumMapper.selectById(id);
    }

    /**
     * 新增会员信息
     *
     * @param album 会员信息
     */
    @Override
    public int insertAlbum(WebAlbum album) {
        album.setTitle(album.getTitle());
        album.setSort(album.getSort());
        album.setCreateTime(new Date());
        album.setUpdateTime(new Date());
        return albumMapper.insert(album);
    }

    /**
     * 修改保存会员信息
     *
     * @param album 会员信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateAlbum(WebAlbum album) {
        WebAlbum webAlbum = albumMapper.selectList(new QueryWrapper<WebAlbum>().eq("id", album.getId()).last("FOR UPDATE"))
                .stream().findFirst().orElse(null);
        if (webAlbum == null) throw new HongshuException("专辑不存在");
        String oldCover = webAlbum.getAlbumCover();
        webAlbum.setTitle(album.getTitle());
        webAlbum.setSort(album.getSort());
        if (album.getAlbumCover() != null) webAlbum.setAlbumCover(album.getAlbumCover());
        webAlbum.setUpdater("System");
        webAlbum.setUpdateTime(new Date());
        int updated = albumMapper.updateById(webAlbum);
        if (updated != 1) throw new HongshuException("专辑保存失败");
        assets.cleanStoredReferencesAfterCommit(oldCover);
        return updated;
    }

    /**
     * 删除会员信息
     *
     * @param id 会员ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteAlbumById(Long id) {
        return deleteAlbumByIds(new Long[]{id});
    }

    /**
     * 批量删除会员信息
     *
     * @param ids 需要删除的会员ID
     */
    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public int deleteAlbumByIds(Long[] ids) {
        int count = 0;
        for (Long id : ids) {
            WebAlbum previous = albumMapper.selectList(new QueryWrapper<WebAlbum>().eq("id", id).last("FOR UPDATE"))
                    .stream().findFirst().orElse(null);
            if (previous == null) continue;
            jdbc.update("DELETE FROM web_album_note_relation WHERE aid = ?", id);
            jdbc.update("DELETE FROM web_like_or_collect WHERE type = 4 AND like_or_collection_id = ?", id);
            int deleted = albumMapper.deleteById(id);
            count += deleted;
            if (deleted > 0) assets.cleanStoredReferencesAfterCommit(previous.getAlbumCover());
        }
        return count;
    }
}
