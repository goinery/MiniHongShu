package com.hongshu.server.controller.common;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import com.google.code.kaptcha.Producer;
import com.hongshu.common.config.HongshuConfig;
import com.hongshu.common.constant.CacheConstants;
import com.hongshu.common.constant.Constants;
import com.hongshu.common.core.domain.AjaxResult;
import com.hongshu.common.core.redis.RedisCache;
import com.hongshu.common.utils.sign.Base64;
import com.hongshu.common.utils.uuid.IdUtils;
import com.hongshu.system.service.ISysConfigService;
import org.springframework.util.FastByteArrayOutputStream;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;

import javax.imageio.ImageIO;

import jakarta.servlet.http.HttpServletResponse;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 验证码操作处理
 *
 * @author hongshu
 */
@Tag(name = "通用·登录验证码", description = "管理端图形验证码的生成与获取")
@RestController
public class CaptchaController {

    @Resource(name = "captchaProducer")
    private Producer captchaProducer;
    @Resource(name = "captchaProducerMath")
    private Producer captchaProducerMath;
    private final RedisCache redisCache;
    private final ISysConfigService configService;

    public CaptchaController(RedisCache redisCache, ISysConfigService configService) {
        this.redisCache = redisCache;
        this.configService = configService;
    }

    /**
     * 生成验证码
     */
    @Operation(summary = "获取登录验证码",
            description = "根据验证码开关返回是否启用、验证码 UUID 和 Base64 图片；支持字符或算术验证码，答案暂存 Redis。 无需登录。")
    @GetMapping("/captchaImage")
    public AjaxResult getCode(HttpServletResponse response) {
        AjaxResult ajax = AjaxResult.success();
        boolean captchaEnabled = configService.selectCaptchaEnabled();
        ajax.put("captchaEnabled", captchaEnabled);
        if (!captchaEnabled) {
            return ajax;
        }

        // 保存验证码信息
        String uuid = IdUtils.simpleUUID();
        String verifyKey = CacheConstants.CAPTCHA_CODE_KEY + uuid;

        String capStr;
        String code;
        BufferedImage image;

        // 生成验证码
        String captchaType = HongshuConfig.getCaptchaType();
        if ("math".equals(captchaType)) {
            String capText = captchaProducerMath.createText();
            capStr = capText.substring(0, capText.lastIndexOf("@"));
            code = capText.substring(capText.lastIndexOf("@") + 1);
            image = captchaProducerMath.createImage(capStr);
        } else if ("char".equals(captchaType)) {
            capStr = code = captchaProducer.createText();
            image = captchaProducer.createImage(capStr);
        } else {
            return AjaxResult.error("验证码类型配置错误");
        }
        if (image == null) {
            return AjaxResult.error("验证码图片生成失败");
        }

        redisCache.setCacheObject(verifyKey, code, Constants.CAPTCHA_EXPIRATION, TimeUnit.MINUTES);
        // 转换流信息写出
        FastByteArrayOutputStream os = new FastByteArrayOutputStream();
        try {
            ImageIO.write(image, "jpg", os);
        } catch (IOException e) {
            return AjaxResult.error(e.getMessage());
        }

        ajax.put("uuid", uuid);
        ajax.put("img", Base64.encode(os.toByteArray()));
        return ajax;
    }
}
