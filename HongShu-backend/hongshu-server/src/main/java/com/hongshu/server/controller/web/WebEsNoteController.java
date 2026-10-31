package com.hongshu.server.controller.web;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hongshu.common.enums.Result;
import com.hongshu.common.validator.myVaildator.noLogin.NoLoginIntercept;
import com.hongshu.web.domain.dto.EsNoteDTO;
import com.hongshu.web.domain.entity.WebNavbar;
import com.hongshu.web.domain.vo.NoteSearchVO;
import com.hongshu.web.service.IWebEsNoteService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ES
 *
 * @author hongshu
 */
@Tag(name = "前台·笔记搜索", description = "使用 MySQL 查询笔记；保留的搜索索引维护接口为空兼容实现")
@RestController
@RequestMapping("/web/es/note")
public class WebEsNoteController {

    private final IWebEsNoteService esNoteService;

    public WebEsNoteController(IWebEsNoteService esNoteService) {
        this.esNoteService = esNoteService;
    }

    /**
     * 搜索对应的笔记
     *
     * @param currentPage 当前页
     * @param pageSize    分页数
     * @param esNoteDTO   笔记
     */
    @Operation(summary = "分页搜索图文笔记",
            description = "实际使用 MySQL 对标题、正文、作者进行模糊搜索，支持父子分类和点赞或时间排序，仅返回已审核通过的图文。 无需登录；公开接口携带有效令牌时可识别当前用户。",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "请求数据，字段含义见下方模型；具体取值与操作约束见接口说明"))
    @NoLoginIntercept
    @PostMapping("getNoteByDTO/{currentPage}/{pageSize}")
    public Result<?> getNoteByDTO(
            @Parameter(description = "当前页码，从 1 开始") @PathVariable long currentPage,
            @Parameter(description = "每页条数；具体上限及默认值见接口说明") @PathVariable long pageSize,
            @RequestBody EsNoteDTO esNoteDTO
    ) {
        Page<NoteSearchVO> page = esNoteService.getNoteByDTO(currentPage, pageSize, esNoteDTO);
        return Result.ok(page);
    }

    /**
     * 搜索对应的笔记
     *
     * @param esNoteDTO 笔记
     */
    @Operation(summary = "查询搜索分类选项",
            description = "实际按分类标题查询分类表，返回分类选项；不是 Elasticsearch 聚合，也不返回关键词命中的分类计数。 无需登录；公开接口携带有效令牌时可识别当前用户。",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "请求数据，字段含义见下方模型；具体取值与操作约束见接口说明"))
    @NoLoginIntercept
    @PostMapping("getCategoryAgg")
    public Result<?> getCategoryAgg(@RequestBody EsNoteDTO esNoteDTO) {
        List<WebNavbar> categoryList = esNoteService.getCategoryAgg(esNoteDTO);
        return Result.ok(categoryList);
    }

    /**
     * 分页查询笔记
     *
     * @param currentPage 当前页
     * @param pageSize    分页数
     */
    @Operation(summary = "分页获取首页图文",
            description = "按发布时间和更新时间倒序返回审核通过的图文，附带作者及当前用户点赞状态；当前没有个性化推荐算法。 无需登录；公开接口携带有效令牌时可识别当前用户。")
    @NoLoginIntercept
    @GetMapping("getRecommendNote/{currentPage}/{pageSize}")
    public Result<?> getRecommendNote(
            @Parameter(description = "当前页码，从 1 开始") @PathVariable long currentPage,
            @Parameter(description = "每页条数；具体上限及默认值见接口说明") @PathVariable long pageSize
    ) {
        Page<NoteSearchVO> page = esNoteService.getRecommendNote(currentPage, pageSize);
        return Result.ok(page);
    }

    /**
     * 增加笔记
     *
     * @param noteSearchVo 笔记
     */
    @Operation(summary = "搜索索引新增兼容入口",
            description = "当前直接从 MySQL 搜索，该方法为空实现；不会新增数据库笔记或写入搜索引擎。 需携带前台 accessToken。",
            security = @SecurityRequirement(name = "appAccessToken"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "请求数据，字段含义见下方模型；具体取值与操作约束见接口说明"))
    @PostMapping("addNote")
    public void addNote(@RequestBody NoteSearchVO noteSearchVo) {
        esNoteService.addNote(noteSearchVo);
    }

    /**
     * 修改笔记
     *
     * @param noteSearchVo 笔记
     */
    @Operation(summary = "搜索索引更新兼容入口",
            description = "当前直接从 MySQL 搜索，该方法为空实现；不会更新数据库笔记或搜索索引。 需携带前台 accessToken。",
            security = @SecurityRequirement(name = "appAccessToken"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "请求数据，字段含义见下方模型；具体取值与操作约束见接口说明"))
    @PostMapping("updateNote")
    public void updateNote(@RequestBody NoteSearchVO noteSearchVo) {
        esNoteService.updateNote(noteSearchVo);
    }

    /**
     * 删除es中的笔记
     *
     * @param noteId 笔记ID
     */
    @Operation(summary = "搜索索引删除兼容入口",
            description = "当前直接从 MySQL 搜索，该方法为空实现；不会删除数据库笔记或搜索索引。 需携带前台 accessToken。",
            security = @SecurityRequirement(name = "appAccessToken"))
    @RequestMapping("deleteNote/{noteId}")
    public void deleteNote(@Parameter(description = "笔记 ID") @PathVariable String noteId) {
        esNoteService.deleteNote(noteId);
    }

    /**
     * 批量增加笔记
     */
    @Operation(summary = "搜索索引批量导入兼容入口",
            description = "当前未使用独立搜索引擎，该方法为空实现，不执行批量导入。 无需登录；公开接口携带有效令牌时可识别当前用户。")
    @PostMapping("addNoteBulkData")
    @NoLoginIntercept
    public void addNoteBulkData() {
        esNoteService.addNoteBulkData();
    }

    /**
     * 清空笔记
     */
    @Operation(summary = "搜索索引清空兼容入口",
            description = "当前未使用独立搜索引擎，该方法为空实现，不删除数据库内容。 无需登录；公开接口携带有效令牌时可识别当前用户。")
    @DeleteMapping("delNoteBulkData")
    @NoLoginIntercept
    public void delNoteBulkData() {
        esNoteService.delNoteBulkData();
    }

    /**
     * 重置
     */
    @Operation(summary = "搜索索引刷新兼容入口",
            description = "当前未使用独立搜索引擎，该方法为空实现，仅返回兼容成功响应。 无需登录；公开接口携带有效令牌时可识别当前用户。")
    @PostMapping("refreshNoteData")
    @NoLoginIntercept
    public Result<?> refreshNoteData() {
        esNoteService.refreshNoteData();
        return Result.ok();
    }
}
