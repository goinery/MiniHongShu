package com.hongshu.server.controller.web;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import com.hongshu.common.enums.Result;
import com.hongshu.web.service.FeedbackService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;

@Tag(name = "前台·意见反馈", description = "会员提交联系邮箱和反馈内容")
@RestController
@RequestMapping("/web/feedback")
public class WebFeedbackController {
    private final FeedbackService service;
    public WebFeedbackController(FeedbackService service) { this.service = service; }
    @Schema(description = "意见反馈提交参数")
    public record Feedback(
            @Schema(description = "联系邮箱，必须为有效邮箱且不超过 254 字符")
            @NotBlank @Email @Size(max = 254) String email,
            @Schema(description = "反馈正文，1 至 5000 字") @NotBlank @Size(max = 5000) String content) {}

    @Operation(summary = "提交意见反馈",
            description = "提交有效联系邮箱和 1 至 5000 字的正文；按当前用户近一天记录数限制最多 20 次，初始状态为未处理。 需携带前台 accessToken。",
            security = @SecurityRequirement(name = "appAccessToken"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "请求数据，字段含义见下方模型；具体取值与操作约束见接口说明"))
    @PostMapping
    public Result<?> submit(@Valid @RequestBody Feedback body) {
        service.submit(body.email(), body.content());
        return Result.ok();
    }
}
