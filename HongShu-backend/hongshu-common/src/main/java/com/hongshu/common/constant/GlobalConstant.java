package com.hongshu.common.constant;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 全局配置
 *
 * @author hongshu
 */
@Schema(description = "全局配置变量")
public interface GlobalConstant {

    @Schema(description = "开发环境")
    String SPRING_PROFILE_DEVELOPMENT = "dev";

    @Schema(description = "测试环境")
    String SPRING_PROFILE_TEST = "test";
}
