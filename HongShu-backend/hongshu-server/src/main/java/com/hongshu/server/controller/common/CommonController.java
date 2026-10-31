package com.hongshu.server.controller.common;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import com.hongshu.common.config.HongshuConfig;
import com.hongshu.common.constant.Constants;
import com.hongshu.common.core.domain.AjaxResult;
import com.hongshu.common.utils.StringUtils;
import com.hongshu.common.utils.file.FileUtils;
import com.hongshu.web.service.IWebOssService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * 通用请求处理
 *
 * @author hongshu
 */
@Tag(name = "通用·文件上传下载", description = "管理端通用文件上传、导出文件与历史本地资源下载")
@RestController
@RequestMapping("/common")
public class CommonController {

    private static final Logger log = LoggerFactory.getLogger(CommonController.class);

    private final IWebOssService ossService;

    private static final String FILE_DELIMITER = ",";

    public CommonController(IWebOssService ossService) {
        this.ossService = ossService;
    }

    /**
     * 通用下载请求
     *
     * @param fileName 文件名称
     * @param delete   是否删除
     */
    @Operation(summary = "下载导出文件",
            description = "根据导出文件名下载本地导出目录中的文件；delete 为 true 时下载后删除该文件。 需携带管理端 Bearer JWT。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @GetMapping("/download")
    public void fileDownload(
            @Parameter(description = "导出目录内的文件名") String fileName,
            @Parameter(description = "是否在下载后删除导出文件") Boolean delete,
            HttpServletResponse response,
            HttpServletRequest request
    ) {
        try {
            if (!FileUtils.checkAllowDownload(fileName)) {
                throw new Exception(StringUtils.format("文件名称({})非法，不允许下载。 ", fileName));
            }
            String realFileName = System.currentTimeMillis() + fileName.substring(fileName.indexOf("_") + 1);
            String filePath = HongshuConfig.getDownloadPath() + fileName;

            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            FileUtils.setAttachmentResponseHeader(response, realFileName);
            FileUtils.writeBytes(filePath, response.getOutputStream());
            if (Boolean.TRUE.equals(delete)) {
                FileUtils.deleteFile(filePath);
            }
        } catch (Exception e) {
            log.error("下载文件失败", e);
        }
    }

    /**
     * 通用上传请求（单个）
     */
    @Operation(summary = "上传单个文件",
            description = "以 multipart/form-data 提交 file，将允许类型的文件保存到 OSS，返回访问地址、文件名和原始文件名。 需携带管理端 Bearer JWT。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @PostMapping("/upload")
    public AjaxResult uploadFile(@Parameter(description = "上传文件，使用 multipart/form-data 提交；原始字段名以接口参数名为准") MultipartFile file) {
        try {
            String fileName = ossService.saveFile(file);
            AjaxResult ajax = AjaxResult.success();
            ajax.put("url", fileName);
            ajax.put("fileName", fileName);
            ajax.put("newFileName", FileUtils.getName(fileName));
            ajax.put("originalFilename", file.getOriginalFilename());
            return ajax;
        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 通用上传请求（多个）
     */
    @Operation(summary = "批量上传文件",
            description = "以 multipart/form-data 重复提交 files 字段，统一校验并上传，返回多个文件地址和名称。 需携带管理端 Bearer JWT。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @PostMapping("/uploads")
    public AjaxResult uploadFiles(@Parameter(description = "上传文件列表，使用 multipart/form-data 重复提交同名字段") List<MultipartFile> files) {
        try {
            List<String> fileNames = ossService.saveFiles(files);
            List<String> newFileNames = new ArrayList<>(fileNames.size());
            for (String fileName : fileNames) {
                newFileNames.add(FileUtils.getName(fileName));
            }
            List<String> originalFilenames = new ArrayList<>(files.size());
            for (MultipartFile file : files) {
                originalFilenames.add(file.getOriginalFilename());
            }
            AjaxResult ajax = AjaxResult.success();
            String joinedFileNames = StringUtils.join(fileNames, FILE_DELIMITER);
            ajax.put("urls", joinedFileNames);
            ajax.put("fileNames", joinedFileNames);
            ajax.put("newFileNames", StringUtils.join(newFileNames, FILE_DELIMITER));
            ajax.put("originalFilenames", StringUtils.join(originalFilenames, FILE_DELIMITER));
            return ajax;
        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 本地资源通用下载
     */
    @Operation(summary = "下载历史本地资源",
            description = "根据 /profile 开头的历史资源路径下载本地文件；此接口不代理远程 OSS 文件。 需携带管理端 Bearer JWT。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @GetMapping("/download/resource")
    public void resourceDownload(
            @Parameter(description = "历史本地资源路径，例如 /profile/目录/文件名") String resource,
            HttpServletRequest request,
            HttpServletResponse response
    )
            {
        try {
            if (!FileUtils.checkAllowDownload(resource)) {
                throw new Exception(StringUtils.format("资源文件({})非法，不允许下载。 ", resource));
            }
            // 本地资源路径
            String localPath = HongshuConfig.getProfile();
            // 数据库资源地址
            String downloadPath = localPath + StringUtils.substringAfter(resource, Constants.RESOURCE_PREFIX);
            // 下载名称
            String downloadName = StringUtils.substringAfterLast(downloadPath, "/");
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            FileUtils.setAttachmentResponseHeader(response, downloadName);
            FileUtils.writeBytes(downloadPath, response.getOutputStream());
        } catch (Exception e) {
            log.error("下载文件失败", e);
        }
    }
}
