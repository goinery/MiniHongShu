package com.hongshu.web.service;

import com.hongshu.common.config.HongshuConfig;
import com.hongshu.common.exception.web.HongshuException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

/** 历史本地文件的路径校验与回收，不承担上传登记及归属判断。 */
@Component
public class LocalFileAssetStorage {

    public static final int STORAGE_TYPE = 0;
    private static final String RESOURCE_PREFIX = "/profile/";

    public boolean isLocal(String path) {
        return path.startsWith(RESOURCE_PREFIX);
    }

    public String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new HongshuException("文件路径不能为空");
        }
        String path = value.trim();
        if (path.startsWith("http://") || path.startsWith("https://")) {
            try {
                String uriPath = URI.create(path).getPath();
                if (uriPath != null && isLocal(uriPath)) {
                    path = uriPath;
                }
            } catch (IllegalArgumentException e) {
                throw new HongshuException("无效的文件路径", e);
            }
        }
        if (isLocal(path)) {
            resolve(path, Path.of(HongshuConfig.getProfile()).toAbsolutePath().normalize());
        }
        return path;
    }

    public void delete(String path) throws IOException {
        Path root = Path.of(HongshuConfig.getProfile()).toRealPath();
        Path target = resolve(path, root);
        if (Files.exists(target)) {
            if (!target.toRealPath().startsWith(root) || !Files.isRegularFile(target)) {
                throw new HongshuException("无效的文件路径");
            }
            Files.delete(target);
        }
    }

    private Path resolve(String path, Path root) {
        if (!isLocal(path)) {
            throw new HongshuException("无效的本地文件路径");
        }
        String relative = path.substring(RESOURCE_PREFIX.length());
        if (relative.isBlank() || relative.contains("..") || relative.contains("\\")
                || relative.contains("%") || relative.contains(":") || relative.contains("?") || relative.contains("#")) {
            throw new HongshuException("无效的文件路径");
        }
        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root) || target.equals(root)) {
            throw new HongshuException("无效的文件路径");
        }
        return target;
    }
}
