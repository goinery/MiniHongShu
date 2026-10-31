package com.hongshu.server.controller.web;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import com.hongshu.common.enums.Result;
import com.hongshu.web.service.IWebOssService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * OSS
 *
 * @author hongshu
 */
@RequestMapping("/web/oss")
@Tag(name = "前台·图片文件", description = "图片上传、批量上传及受管文件的归属与引用校验删除")
@RestController
public class WebOssController {

    private final IWebOssService ossService;

    public WebOssController(IWebOssService ossService) {
        this.ossService = ossService;
    }

    /**
     * 上传文件
     *
     * @param file 文件
     */
    @Operation(summary = "上传一张图片",
            description = "以 multipart/form-data 提交 file，校验图片类型与大小，上传 OSS 并登记当前用户归属，返回稳定访问 URL。 需携带前台 accessToken。",
            security = @SecurityRequirement(name = "appAccessToken"))
    @PostMapping("save")
    public Result<?> save(@Parameter(description = "上传文件，使用 multipart/form-data 提交；原始字段名以接口参数名为准") @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.build(500, "请选择图片");
        }
        try {
            String path = ossService.save(file);
            return Result.ok(path);
        } catch (Exception e) {
            return Result.build(500, e.getMessage() != null ? e.getMessage() : "上传失败");
        }
    }

    /**
     * 批量上传文件
     *
     * @param files 文件集
     */
    @Operation(summary = "批量上传图片",
            description = "以 multipart/form-data 重复提交 uploadFiles；先校验全部图片，失败时补偿已上传文件，返回图片 URL 数组。 需携带前台 accessToken。",
            security = @SecurityRequirement(name = "appAccessToken"))
    @PostMapping(value = "saveBatch")
    public Result<List<String>> saveBatch(@Parameter(description = "图片列表，multipart/form-data 字段名为 uploadFiles，可不传；已有图片地址放入 noteData.urls") @RequestParam("uploadFiles") MultipartFile[] files) {
        if (files.length == 0) {
            return Result.fail(null);
        }
        List<String> stringList = ossService.saveBatch(files);
        return Result.ok(stringList);
    }

    /**
     * 删除文件
     *
     * @param path 路径
     */
    @Operation(summary = "删除本人上传的闲置文件",
            description = "根据 path 验证上传归属和业务引用，仍被笔记、头像、专辑或消息等使用时拒绝删除。 需携带前台 accessToken。",
            security = @SecurityRequirement(name = "appAccessToken"))
    @GetMapping("delete")
    public Result<?> delete(@Parameter(description = "待删除文件的 OSS URL 或历史 /profile 本地路径") String path) {
        ossService.delete(path);
        return Result.ok();
    }

    /**
     * 批量删除文件
     *
     * @param filePaths 文件路径集
     */
    @Operation(summary = "批量删除本人闲置文件",
            description = "请求体为文件 URL 或历史本地路径数组；全部校验归属和引用后去重删除。 需携带前台 accessToken。",
            security = @SecurityRequirement(name = "appAccessToken"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "JSON 数组，包含本次操作所需的 ID、文件路径或缓存前缀，具体内容见接口说明"))
    @PostMapping(value = "deleteBatch")
    public Result<?> deleteBatch(@RequestBody List<String> filePaths) {
        if (filePaths.isEmpty()) {
            return Result.fail(null);
        }
        ossService.batchDelete(filePaths);
        return Result.ok();
    }
}
