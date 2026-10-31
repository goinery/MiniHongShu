package com.hongshu.web.service.impl;

import com.hongshu.common.exception.web.HongshuException;
import com.hongshu.common.service.FileStorage;
import com.hongshu.web.service.FileAssetService;
import com.hongshu.web.service.IWebOssService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/** 上传编排：统一校验、登记和失败批次补偿。 */
@Service
public class WebOssServiceImpl implements IWebOssService {
    private final FileStorage storage;
    private final FileAssetService assets;

    public WebOssServiceImpl(FileStorage storage, FileAssetService assets) {
        this.storage = storage;
        this.assets = assets;
    }

    @Override
    public String save(MultipartFile file) {
        String path = storage.uploadImage(file);
        assets.register(path);
        return path;
    }

    @Override
    public String saveFile(MultipartFile file) {
        String path = storage.upload(file);
        assets.register(path);
        return path;
    }

    @Override
    public List<String> saveBatch(MultipartFile[] files) {
        if (files == null || files.length == 0) {
            throw new HongshuException("请选择图片");
        }
        return saveBatch(Arrays.asList(files), storage::validateImage, this::save);
    }

    @Override
    public List<String> saveFiles(List<MultipartFile> files) {
        return saveBatch(files, storage::validateFile, this::saveFile);
    }

    private List<String> saveBatch(List<MultipartFile> files, Consumer<MultipartFile> validator,
                                   Function<MultipartFile, String> uploader) {
        if (files == null || files.isEmpty()) {
            throw new HongshuException("请选择文件");
        }
        files.forEach(validator);
        List<String> result = new ArrayList<>(files.size());
        try {
            for (MultipartFile file : files) {
                result.add(uploader.apply(file));
            }
            return result;
        } catch (RuntimeException e) {
            result.forEach(assets::discardUpload);
            throw e;
        }
    }

    @Override
    public void delete(String path) {
        assets.deleteOwned(List.of(assets.normalize(path)));
    }

    @Override
    public void batchDelete(List<String> paths) {
        assets.deleteOwned(paths);
    }
}
