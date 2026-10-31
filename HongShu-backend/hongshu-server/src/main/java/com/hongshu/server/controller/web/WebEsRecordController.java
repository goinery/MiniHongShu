package com.hongshu.server.controller.web;

import org.springdoc.core.annotations.ParameterObject;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import com.hongshu.common.enums.Result;
import com.hongshu.common.validator.myVaildator.noLogin.NoLoginIntercept;
import com.hongshu.web.domain.dto.EsRecordDTO;
import com.hongshu.web.service.IWebEsRecordService;
import org.springframework.web.bind.annotation.*;

/**
 * ES
 *
 * @author hongshu
 */
@RequestMapping("/web/es/record")
@Tag(name = "前台·搜索记录热词", description = "当前会员搜索记录、关键词提示与热搜统计")
@RestController
public class WebEsRecordController {

    private final IWebEsRecordService esRecordService;

    public WebEsRecordController(IWebEsRecordService esRecordService) {
        this.esRecordService = esRecordService;
    }

    /**
     * 获取搜索记录
     */
    @Operation(summary = "查询本人搜索记录和关键词提示",
            description = "按关键词筛选当前认证会员的搜索记录，按最近更新时间倒序返回，最多 20 条。 需携带前台 accessToken。",
            security = @SecurityRequirement(name = "appAccessToken"))
    @GetMapping("getRecordByKeyWord")
    public Result<?> getRecordByKeyWord(@ParameterObject @Parameter(description = "搜索记录筛选条件，具体字段见关联模型；分页使用 pageNum 和 pageSize") EsRecordDTO esRecordDTO) {
        return Result.ok(esRecordService.getRecordByKeyWord(esRecordDTO));
    }

    /**
     * 热门关键词
     */
    @Operation(summary = "获取热门搜索词",
            description = "按关键词汇总搜索次数，倒序返回前 10 条热词；当前从 MySQL 聚合读取。 无需登录；公开接口携带有效令牌时可识别当前用户。")
    @NoLoginIntercept
    @GetMapping("getHotRecord")
    public Result<?> getHotRecord() {
        return Result.ok(esRecordService.getHotRecord());
    }

    /**
     * 增加搜索记录
     */
    @Operation(summary = "记录一次关键词搜索",
            description = "按当前会员和关键词新增记录或累加搜索次数，并更新最近搜索时间；空关键词忽略。 需携带前台 accessToken。",
            security = @SecurityRequirement(name = "appAccessToken"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "请求数据，字段含义见下方模型；具体取值与操作约束见接口说明"))
    @PostMapping("addRecord")
    public Result<?> addRecord(@RequestBody EsRecordDTO esRecordDTO) {
        esRecordService.addRecord(esRecordDTO);
        return Result.ok();
    }

}
