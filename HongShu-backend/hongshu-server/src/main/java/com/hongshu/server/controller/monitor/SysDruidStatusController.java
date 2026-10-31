package com.hongshu.server.controller.monitor;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import com.hongshu.common.core.domain.AjaxResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "监控·数据源状态", description = "查询 Druid 监控控制台的启用状态")
@RestController
public class SysDruidStatusController {
    @Value("${spring.datasource.druid.statViewServlet.enabled:false}")
    private boolean enabled;

    @Operation(summary = "查询数据源监控启用状态",
            description = "返回 enabled 字段，表示 Druid 监控控制台是否开启；不返回数据库连接凭证。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('monitor:druid:list')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @GetMapping("/monitor/druidStatus")
    @PreAuthorize("@ss.hasPermi('monitor:druid:list')")
    public AjaxResult status() { return AjaxResult.success(java.util.Map.of("enabled", enabled)); }
}
