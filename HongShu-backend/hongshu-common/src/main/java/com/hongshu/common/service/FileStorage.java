package com.hongshu.common.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储端口，隔离业务代码与存储供应商。
 */
public interface FileStorage {

    /** @return 上传登记使用的存储类型 */
    int getStorageType();

    /** 校验允许上传的图片类型及大小。 */
    void validateImage(MultipartFile file);

    /** 校验允许上传的通用文件类型及大小。 */
    void validateFile(MultipartFile file);

    /** @return 上传图片的稳定访问地址 */
    String uploadImage(MultipartFile file);

    /** @return 上传文件的稳定访问地址 */
    String upload(MultipartFile file);

    /** 删除当前存储管理的文件，拒绝任意外部地址。 */
    void delete(String path);
}
