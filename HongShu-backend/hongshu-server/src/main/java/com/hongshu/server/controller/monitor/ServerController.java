package com.hongshu.server.controller.monitor;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import com.hongshu.common.core.domain.AjaxResult;
import com.hongshu.framework.web.domain.Server;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 服务器监控
 *
 * @author hongshu
 */
@Tag(name = "监控·服务器信息", description = "CPU、内存、JVM、磁盘与主机运行信息")
@RestController
@RequestMapping("/monitor/server")
public class ServerController {

    @Operation(summary = "查询服务器运行信息",
            description = "采集 CPU、内存、JVM、磁盘和主机信息，供管理端服务器监控页面展示。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('monitor:server:list')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @PreAuthorize("@ss.hasPermi('monitor:server:list')")
    @GetMapping()
    public AjaxResult getInfo() {
        Server server = new Server();
        server.copyTo();
        return AjaxResult.success(server);
    }
}
