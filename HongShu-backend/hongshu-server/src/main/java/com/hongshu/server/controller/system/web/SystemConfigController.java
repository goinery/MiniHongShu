package com.hongshu.server.controller.system.web;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import com.hongshu.common.annotation.AuthorityVerify.AuthorityVerify;
import com.hongshu.common.core.domain.AjaxResult;
import com.hongshu.web.domain.vo.SystemConfigVO;
import com.hongshu.web.service.SystemConfigService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.hongshu.common.core.domain.AjaxResult.success;

/**
 * 系统配置表 RestApi
 *
 * @author hongshu
 * 创建时间：2020年1月21日09:24:37
 */
@Tag(name = "运营·业务配置", description = "业务配置读取更新与 Redis 缓存清理；部分配置字段为预留")
@RestController
@RequestMapping("/systemConfig")
@Slf4j
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    public SystemConfigController(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    @Operation(summary = "读取业务系统配置",
            description = "读取当前有效业务配置，优先使用 Redis 缓存；配置中的部分邮件、编辑器与搜索模式字段仅为预留。 需携带管理端 Bearer JWT。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @AuthorityVerify
    @GetMapping("/getSystemConfig")
    public AjaxResult getSystemConfig() {
        return success(systemConfigService.getConfig());
    }

    @Operation(summary = "按前缀清理业务缓存",
            description = "请求体为缓存前缀数组；前缀只允许字母、数字、冒号、下划线和连字符，特殊全部清理值参见配置常量，清理可能使会话失效。 需携带管理端 Bearer JWT。",
            security = @SecurityRequirement(name = "adminBearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "JSON 数组，包含本次操作所需的 ID、文件路径或缓存前缀，具体内容见接口说明"))
    @AuthorityVerify
    @PostMapping("/cleanRedisByKey")
    public String cleanRedisByKey(@RequestBody List<String> key) {
        return systemConfigService.cleanRedisByKey(key);
    }

    @AuthorityVerify
//    @OperationLogger(value = "修改系统配置")
    @Operation(summary = "保存业务系统配置",
            description = "新增或修改网站、邮箱等业务配置，保存后删除配置缓存；文件存储凭证由环境变量管理，不在此配置。 需携带管理端 Bearer JWT。",
            security = @SecurityRequirement(name = "adminBearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "请求数据，字段含义见下方模型；具体取值与操作约束见接口说明"))
    @PostMapping("/editSystemConfig")
    public String editSystemConfig(@RequestBody SystemConfigVO systemConfigVO) {
        return systemConfigService.editSystemConfig(systemConfigVO);
    }
}
