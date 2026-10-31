package com.hongshu.server.controller.web;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hongshu.common.enums.Result;
import com.hongshu.common.validator.myVaildator.noLogin.NoLoginIntercept;
import com.hongshu.web.domain.vo.NoteVO;
import com.hongshu.web.service.IWebNoteService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 笔记
 *
 * @author hongshu
 */
@RequestMapping("/web/note")
@Tag(name = "前台·图文笔记", description = "图文发布、修改、详情、热门、删除与作者置顶")
@RestController
public class WebNoteController {

    private final IWebNoteService noteService;

    public WebNoteController(IWebNoteService noteService) {
        this.noteService = noteService;
    }

    /**
     * 获取笔记
     *
     * @param noteId 笔记ID
     */
    @Operation(summary = "获取图文笔记详情",
            description = "返回正文、图片、作者、标签、关注和互动状态并增加阅读量；未审核通过的笔记仅作者可见。 无需登录；公开接口携带有效令牌时可识别当前用户。")
    @GetMapping("getNoteById")
    @NoLoginIntercept
    public Result<?> getNoteById(@Parameter(description = "笔记 ID") String noteId) {
        NoteVO noteVo = noteService.getNoteById(noteId);
        return Result.ok(noteVo);
    }

    /**
     * 新增笔记
     *
     * @param noteData 笔记对象
     * @param files    图片文件
     */
    @Operation(summary = "发布图文笔记",
            description = "以 multipart/form-data 提交 noteData JSON 和可选 uploadFiles；支持已有图片 URL 与新图片，至少一张，标题和分类必填，发布后待审核。 需携带前台 accessToken。",
            security = @SecurityRequirement(name = "appAccessToken"))
    @PostMapping("saveNoteByDTO")
    public Result<?> saveNoteByDTO(
            @Parameter(description = "笔记 JSON 字符串，包含 title、content、cid、urls、tagList；修改时还须包含 id") @RequestParam("noteData") String noteData,
            @Parameter(description = "图片列表，multipart/form-data 字段名为 uploadFiles，可不传；已有图片地址放入 noteData.urls") @RequestParam(value = "uploadFiles", required = false) MultipartFile[] files
    ) {
        noteService.saveNoteByDTO(noteData, files);
        return Result.ok();
    }

    /**
     * 删除笔记
     *
     * @param noteIds 笔记ID集合
     */
    @Operation(summary = "批量删除本人笔记",
            description = "请求体为笔记 ID 数组；锁内验证作者，清理评论、互动、标签和专辑关联，更新统计并安排旧文件回收。 需携带前台 accessToken。",
            security = @SecurityRequirement(name = "appAccessToken"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "JSON 数组，包含本次操作所需的 ID、文件路径或缓存前缀，具体内容见接口说明"))
    @PostMapping("deleteNoteByIds")
    public Result<?> deleteNoteByIds(@RequestBody List<String> noteIds) {
        noteService.deleteNoteByIds(noteIds);
        return Result.ok();
    }

    /**
     * 更新笔记
     *
     * @param noteData 笔记对象
     * @param files    图片文件
     */
    @Operation(summary = "修改本人图文笔记",
            description = "以 multipart/form-data 提交包含 id 的 noteData JSON 和可选图片；仅允许作者操作，替换标签和专辑关联，保存后重新待审核。 需携带前台 accessToken。",
            security = @SecurityRequirement(name = "appAccessToken"))
    @PostMapping("updateNoteByDTO")
    public Result<?> updateNoteByDTO(
            @Parameter(description = "笔记 JSON 字符串，包含 title、content、cid、urls、tagList；修改时还须包含 id") @RequestParam("noteData") String noteData,
            @Parameter(description = "图片列表，multipart/form-data 字段名为 uploadFiles，可不传；已有图片地址放入 noteData.urls") @RequestParam(value = "uploadFiles", required = false) MultipartFile[] files
    ) {
        noteService.updateNoteByDTO(noteData, files);
        return Result.ok();
    }

    /**
     * 获取热门笔记
     *
     * @param currentPage 当前页
     * @param pageSize    分页数
     */
    @Operation(summary = "分页获取热门笔记",
            description = "返回审核通过的笔记，按点赞数、浏览数和创建时间倒序，附带作者资料。 需携带前台 accessToken。",
            security = @SecurityRequirement(name = "appAccessToken"))
    @GetMapping("getHotPage/{currentPage}/{pageSize}")
    public Result<?> getHotPage(
            @Parameter(description = "当前页码，从 1 开始") @PathVariable long currentPage,
            @Parameter(description = "每页条数；具体上限及默认值见接口说明") @PathVariable long pageSize
    ) {
        Page<NoteVO> pageInfo = noteService.getHotPage(currentPage, pageSize);
        return Result.ok(pageInfo);
    }

    /**
     * 置顶笔记
     *
     * @param noteId 笔记ID
     */
    @Operation(summary = "置顶或取消置顶本人笔记",
            description = "根据当前置顶状态切换，仅作者可以操作，业务规则最多置顶 3 篇。 需携带前台 accessToken。",
            security = @SecurityRequirement(name = "appAccessToken"))
    @GetMapping("pinnedNote")
    public Result<?> pinnedNote(@Parameter(description = "笔记 ID") String noteId) {
        boolean flag = noteService.pinnedNote(noteId);
        return Result.ok(flag);
    }
}
