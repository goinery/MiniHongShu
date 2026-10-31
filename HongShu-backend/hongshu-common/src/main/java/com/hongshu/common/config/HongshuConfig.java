package com.hongshu.common.config;

import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 读取项目相关配置
 *
 * @author hongshu
 */
@Component
@ConfigurationProperties(prefix = "hongshu")
public class HongshuConfig {

    /**
     * 项目名称
     */
    @Getter
    @Setter
    private String name;

    /**
     * 版本
     */
    @Getter
    @Setter
    private String version;

    /**
     * 版权年份
     */
    @Getter
    @Setter
    private String copyrightYear;

    /**
     * 上传路径
     */
    @Getter
    private static String profile;

    /**
     * 获取地址开关
     */
    @Getter
    private static boolean addressEnabled;

    /**
     * 验证码类型
     */
    @Getter
    private static String captchaType;

    public void setProfile(String profile) {
        HongshuConfig.profile = profile;
    }

    public void setAddressEnabled(boolean addressEnabled) {
        HongshuConfig.addressEnabled = addressEnabled;
    }

    public void setCaptchaType(String captchaType) {
        HongshuConfig.captchaType = captchaType;
    }

    /**
     * 获取导入上传路径
     */
    public static String getImportPath() {
        return getProfile() + "/import";
    }

    /**
     * 获取头像上传路径
     */
    public static String getAvatarPath() {
        return getProfile() + "/avatar";
    }

    /**
     * 获取下载路径
     */
    public static String getDownloadPath() {
        return getProfile() + "/download/";
    }

    /**
     * 获取上传路径
     */
    public static String getUploadPath() {
        return getProfile() + "/upload";
    }
}
