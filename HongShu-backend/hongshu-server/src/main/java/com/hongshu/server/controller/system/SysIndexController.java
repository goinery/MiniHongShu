package com.hongshu.server.controller.system;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import com.hongshu.common.config.HongshuConfig;
import com.hongshu.common.utils.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页
 *
 * @author hongshu
 */
@Tag(name = "通用·服务状态", description = "后端服务的基础响应与版本信息")
@RestController
public class SysIndexController {

    /**
     * 系统基础配置
     */
    private final HongshuConfig hongshuConfig;

    public SysIndexController(HongshuConfig hongshuConfig) {
        this.hongshuConfig = hongshuConfig;
    }

    /**
     * 访问首页，提示语
     */
    @Operation(summary = "查询后端服务状态",
            description = "返回服务名称、版本和运行提示，可用于基础连通性检查。 无需登录。")
    @RequestMapping("/")
    public String index() {
        return StringUtils.format("欢迎使用{}后台管理框架，当前版本：v{}，请通过前端地址访问。", hongshuConfig.getName(), hongshuConfig.getVersion());
    }
}
