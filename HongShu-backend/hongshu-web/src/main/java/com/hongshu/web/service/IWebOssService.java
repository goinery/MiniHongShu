package com.hongshu.web.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * OSS
 *
 * @author hongshu
 */
public interface IWebOssService {

    /**
     * 上传文件
     *
     * @param file 文件
     */
    String save(MultipartFile file);

    /** 管理后台通用文件上传，保留文档等允许的文件类型。 */
    String saveFile(MultipartFile file);

    /**
     * 批量上传文件
     *
     * @param files 文件集
     */
    List<String> saveBatch(MultipartFile[] files);

    /** 上传通用文件批次；校验全部文件后上传，失败时回收本批次已上传文件。 */
    List<String> saveFiles(List<MultipartFile> files);

    /**
     * 删除文件
     *
     * @param path 路径
     */
    void delete(String path);

    /**
     * 批量删除文件
     *
     * @param filePaths 文件路径集
     */
    void batchDelete(List<String> filePaths);
}
