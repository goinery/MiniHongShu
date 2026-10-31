package com.hongshu.web.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONException;
import cn.hutool.json.JSONUtil;
import com.hongshu.common.exception.web.HongshuException;
import com.hongshu.web.domain.dto.NoteDTO;
import com.hongshu.web.domain.entity.WebNote;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * 笔记媒体地址校验、上传与历史文件清理。
 *
 * @author hongshu
 */
@Slf4j
@Service
public class NoteMediaService {

    private final IWebOssService ossService;
    private final FileAssetService assets;

    public NoteMediaService(IWebOssService ossService, FileAssetService assets) {
        this.ossService = ossService;
        this.assets = assets;
    }

    /**
     * 已存笔记由调用方完成归属校验，避免再次读取数据库。
     *
     * @param note     提交的笔记数据
     * @param files    新上传文件
     * @param existing 已校验归属的原笔记，新建时为空
     * @return 规范化后的图片地址列表
     */
    public List<String> resolveUrls(NoteDTO note, MultipartFile[] files, WebNote existing) {
        List<String> urls = new ArrayList<>();
        List<String> existingUrls = existing == null ? Collections.emptyList() : parseUrls(existing.getUrls());
        if (note.getUrls() != null) {
            for (String url : note.getUrls()) {
                if (isStoredUrl(url)) {
                    if (!existingUrls.contains(url)) {
                        assets.requireUsable(url);
                    }
                    urls.add(assets.normalize(url));
                }
            }
        }
        List<MultipartFile> validFiles = new ArrayList<>();
        if (files != null) {
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    validFiles.add(file);
                }
            }
        }
        if (!validFiles.isEmpty()) {
            try {
                urls.addAll(ossService.saveBatch(validFiles.toArray(new MultipartFile[0])));
            } catch (RuntimeException exception) {
                log.error("图片上传失败", exception);
                throw new HongshuException("图片上传失败", exception);
            }
        }
        if (urls.isEmpty()) {
            throw new HongshuException("请上传图片");
        }
        return urls;
    }

    /**
     * 从有效图片中选择封面。
     *
     * @param note 提交的笔记数据
     * @param urls 已校验且非空的图片地址列表
     * @return 规范化后的封面地址
     */
    public String resolveCover(NoteDTO note, List<String> urls) {
        if (isStoredUrl(note.getNoteCover())) {
            String cover = assets.normalize(note.getNoteCover());
            if (urls.contains(cover)) {
                return cover;
            }
        }
        return urls.getFirst();
    }

    /**
     * 事务提交后检查并清理不再被引用的旧媒体。
     *
     * @param cover   原封面地址
     * @param urls    原图片列表
     * @param content 原笔记正文
     */
    public void cleanAfterCommit(String cover, String urls, String content) {
        assets.cleanStoredReferencesAfterCommit(cover);
        assets.cleanStoredContentAfterCommit(urls, content);
    }

    private boolean isStoredUrl(String url) {
        if (StringUtils.isBlank(url)) {
            return false;
        }
        String value = url.trim();
        if (value.startsWith("/profile")) {
            return true;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.startsWith("http://127.0.0.1") || lower.startsWith("http://localhost")) {
            return false;
        }
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    private List<String> parseUrls(String urls) {
        List<String> result = new ArrayList<>();
        if (StringUtils.isBlank(urls)) {
            return result;
        }
        try {
            JSONArray array = JSONUtil.parseArray(urls);
            for (Object item : array) {
                if (item != null && StringUtils.isNotBlank(item.toString())) {
                    result.add(item.toString());
                }
            }
        } catch (JSONException exception) {
            log.warn("笔记图片列表格式无效", exception);
        }
        return result;
    }
}
