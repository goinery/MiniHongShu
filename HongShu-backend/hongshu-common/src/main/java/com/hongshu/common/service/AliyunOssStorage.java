package com.hongshu.common.service;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.ObjectMetadata;
import com.hongshu.common.config.OssConfig;
import com.hongshu.common.exception.file.FileSizeLimitExceededException;
import com.hongshu.common.exception.file.InvalidExtensionException;
import com.hongshu.common.exception.web.HongshuException;
import com.hongshu.common.utils.file.FileUploadUtils;
import com.hongshu.common.utils.file.MimeTypeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/** 新上传文件统一写入 OSS，数据库保存不含签名的稳定 URL。 */
@Slf4j
@Service
public class AliyunOssStorage implements FileStorage {
    public static final int STORAGE_TYPE = 3;
    private static final int MAX_PUBLIC_URL_LENGTH = 350;
    private static final ZoneId STORAGE_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final Pattern OBJECT_KEY = Pattern.compile(
            "hongshu/(images|files)/\\d{4}/\\d{2}/\\d{2}/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.[a-z0-9]+");

    private final OSS client;
    private final String bucket;
    private final String publicUrl;
    private final String defaultPublicUrl;

    public AliyunOssStorage(OSS client, OssConfig config) {
        this.client = client;
        this.bucket = config.getBucket();
        this.defaultPublicUrl = "https://" + bucket + ".oss-" + config.getRegion() + ".aliyuncs.com";
        String baseUrl = config.getPublicUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = defaultPublicUrl;
        }
        URI uri = URI.create(baseUrl.trim());
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null
                || (uri.getPath() != null && !uri.getPath().isEmpty() && !"/".equals(uri.getPath()))) {
            throw new IllegalArgumentException("OSS_PUBLIC_URL 必须为不含路径、参数的 HTTPS 域名");
        }
        this.publicUrl = baseUrl.trim().replaceAll("/+$", "");
        if (this.publicUrl.length() > MAX_PUBLIC_URL_LENGTH) {
            throw new IllegalArgumentException("OSS_PUBLIC_URL 过长");
        }
    }

    @Override
    public int getStorageType() {
        return STORAGE_TYPE;
    }

    @Override
    public String uploadImage(MultipartFile file) {
        return upload(file, MimeTypeUtils.IMAGE_EXTENSION);
    }

    @Override
    public String upload(MultipartFile file) {
        return upload(file, MimeTypeUtils.DEFAULT_ALLOWED_EXTENSION);
    }

    @Override
    public void validateImage(MultipartFile file) {
        validate(file, MimeTypeUtils.IMAGE_EXTENSION);
    }

    @Override
    public void validateFile(MultipartFile file) {
        validate(file, MimeTypeUtils.DEFAULT_ALLOWED_EXTENSION);
    }

    private void validate(MultipartFile file, String[] extensions) {
        if (file == null || file.isEmpty()) {
            throw new HongshuException("请选择文件");
        }
        try {
            FileUploadUtils.assertAllowed(file, extensions);
        } catch (FileSizeLimitExceededException | InvalidExtensionException e) {
            throw new HongshuException(e.getMessage(), e);
        }
    }

    private String upload(MultipartFile file, String[] extensions) {
        validate(file, extensions);
        String extension = FileUploadUtils.getExtension(file).toLowerCase(Locale.ROOT);
        boolean image = FileUploadUtils.isAllowedExtension(extension, MimeTypeUtils.IMAGE_EXTENSION);
        String key = "hongshu/" + (image ? "images/" : "files/")
                + LocalDate.now(STORAGE_ZONE).format(DATE_PATH)
                + "/" + UUID.randomUUID() + "." + extension;
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(MediaTypeFactory.getMediaType("file." + extension)
                .map(Object::toString).orElse("application/octet-stream"));
        metadata.setContentDisposition(image ? "inline" : "attachment");
        try (InputStream input = file.getInputStream()) {
            client.putObject(bucket, key, input, metadata);
            return publicUrl + "/" + key;
        } catch (OSSException e) {
            log.warn("OSS 上传失败，错误码：{}，请求 ID：{}", e.getErrorCode(), e.getRequestId());
            if ("PublicEndpointForbidden".equals(e.getErrorCode())) {
                throw new HongshuException("OSS 默认外网域名受限，请绑定自定义域名并配置 OSS_ENDPOINT 和 OSS_PUBLIC_URL", e);
            }
            throw new HongshuException("文件上传失败，请检查 OSS 配置和写入权限", e);
        } catch (ClientException | IOException e) {
            log.warn("OSS 上传连接或文件读取失败：{}", e.getClass().getSimpleName());
            throw new HongshuException("文件上传失败，请稍后重试", e);
        }
    }

    /** 只删除本项目生成的完整对象 Key，不接受任意外部 URL。 */
    @Override
    public void delete(String url) {
        String prefix = publicUrl + "/";
        if (url != null && !url.startsWith(prefix)) {
            prefix = defaultPublicUrl + "/";
        }
        if (url == null || !url.startsWith(prefix)) {
            throw new HongshuException("无效的 OSS 文件地址");
        }
        String key = url.substring(prefix.length());
        if (!OBJECT_KEY.matcher(key).matches()) {
            throw new HongshuException("无效的 OSS 文件路径");
        }
        try {
            client.deleteObject(bucket, key);
        } catch (OSSException e) {
            log.warn("OSS 删除失败，错误码：{}，请求 ID：{}", e.getErrorCode(), e.getRequestId());
            throw new HongshuException("删除文件失败，请检查 OSS 删除权限", e);
        } catch (ClientException e) {
            throw new HongshuException("删除文件失败，请稍后重试", e);
        }
    }
}
