package com.hongshu.server.controller.system.web;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import com.hongshu.common.core.domain.AjaxResult;
import com.hongshu.web.service.FeedbackService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "运营·反馈处理", description = "查看用户意见并记录处理状态和说明")
@RestController
@RequestMapping("/feedback")
public class SysFeedbackController {
    private final FeedbackService service;
    public SysFeedbackController(FeedbackService service) { this.service = service; }
    @Schema(description = "用户反馈处理参数")
    public record Resolution(
            @Schema(description = "反馈记录 ID") @Positive long id,
            @Schema(description = "处理状态：0 未处理，1 已处理") @NotBlank String status,
            @Schema(description = "处理说明，最多 2000 字") @Size(max = 2000) String resolution) {}

    @Operation(summary = "分页查询用户反馈",
            description = "按处理状态查询反馈，包含提交用户、邮箱、正文、处理说明和时间；每页最多 100 条。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('web:feedback:list')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('web:feedback:list')")
    public AjaxResult list(
            @Parameter(description = "当前页码，从 1 开始，默认 1") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页条数；具体上限及默认值见接口说明") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "处理状态：0 未处理，1 已处理；不传表示全部") @RequestParam(required = false) String status
    ) {
        return AjaxResult.success(service.list(pageNum, pageSize, status));
    }

    @Operation(summary = "处理用户反馈",
            description = "提交反馈 ID、状态和处理说明；status 为 0 表示未处理，1 表示已处理，说明最长 2000 字。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('web:feedback:edit')。",
            security = @SecurityRequirement(name = "adminBearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "请求数据，字段含义见下方模型；具体取值与操作约束见接口说明"))
    @PutMapping
    @PreAuthorize("@ss.hasPermi('web:feedback:edit')")
    public AjaxResult resolve(@Valid @RequestBody Resolution body) {
        service.resolve(body.id(), body.status(), body.resolution());
        return AjaxResult.success();
    }
}
