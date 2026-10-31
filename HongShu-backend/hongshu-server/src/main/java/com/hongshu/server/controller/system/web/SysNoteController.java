package com.hongshu.server.controller.system.web;

import org.springdoc.core.annotations.ParameterObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import cn.hutool.json.JSONUtil;
import com.hongshu.common.annotation.Log;
import com.hongshu.common.core.controller.BaseController;
import com.hongshu.common.core.domain.AjaxResult;
import com.hongshu.common.core.domain.Query;
import com.hongshu.common.core.page.TableDataInfo;
import com.hongshu.common.enums.BusinessType;
import com.hongshu.common.enums.Result;
import com.hongshu.common.utils.poi.ExcelUtil;
import com.hongshu.web.domain.entity.WebNote;
import com.hongshu.web.service.ISysNoteService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;

import java.util.List;
import java.util.Map;

/**
 * 笔记操作处理
 *
 * @author hongshu
 */
@Tag(name = "运营·笔记审核管理", description = "笔记维护、待审核查询、通过或拒绝及导出")
@RestController
@RequestMapping("/note")
public class SysNoteController extends BaseController {

    private final ISysNoteService noteService;

    public SysNoteController(ISysNoteService noteService) {
        this.noteService = noteService;
    }

    /**
     * 获取笔记列表
     */
    @Parameters({
            @Parameter(name = "pageNum", in = ParameterIn.QUERY, description = "当前页码，从 1 开始，默认 1", schema = @Schema(type = "integer", defaultValue = "1")),
            @Parameter(name = "pageSize", in = ParameterIn.QUERY, description = "每页条数，默认 10", schema = @Schema(type = "integer", defaultValue = "10"))
    })
    @Operation(summary = "分页查询可选的已审核笔记",
            description = "返回审核通过的笔记，供专辑添加等操作选择；通过 pageNum 和 pageSize 分页。 需携带管理端 Bearer JWT。 权限校验：@ss.hasAnyPermi('web:note:list,web:album:query')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @PreAuthorize("@ss.hasAnyPermi('web:note:list,web:album:query')")
    @GetMapping("/allList")
    public TableDataInfo list() {
        this.startPage();
        List<WebNote> noteList = noteService.getAllNoteList();
        return getDataTable(noteList);
    }

    /**
     * 获取笔记列表
     */
    @Parameters({
            @Parameter(name = "pageNum", in = ParameterIn.QUERY, description = "当前页码，从 1 开始，默认 1", schema = @Schema(type = "integer", defaultValue = "1")),
            @Parameter(name = "pageSize", in = ParameterIn.QUERY, description = "每页条数，默认 10", schema = @Schema(type = "integer", defaultValue = "10")),
            @Parameter(name = "title", in = ParameterIn.QUERY, description = "笔记标题", schema = @Schema(type = "string")),
            @Parameter(name = "uid", in = ParameterIn.QUERY, description = "作者用户 ID", schema = @Schema(type = "string")),
            @Parameter(name = "noteType", in = ParameterIn.QUERY, description = "笔记类型：1 图文，2 视频预留", schema = @Schema(type = "string")),
            @Parameter(name = "auditStatus", in = ParameterIn.QUERY, description = "审核状态：0 待审核，1 通过，2 拒绝", schema = @Schema(type = "string")),
            @Parameter(name = "pid", in = ParameterIn.QUERY, description = "父分类 ID", schema = @Schema(type = "string")),
            @Parameter(name = "params[beginTime]", in = ParameterIn.QUERY, description = "创建时间范围起点，格式 yyyy-MM-dd HH:mm:ss", schema = @Schema(type = "string")),
            @Parameter(name = "params[endTime]", in = ParameterIn.QUERY, description = "创建时间范围终点，格式 yyyy-MM-dd HH:mm:ss", schema = @Schema(type = "string"))
    })
    @Operation(summary = "分页查询运营笔记",
            description = "支持标题、作者 ID、笔记类型、审核状态、父分类和创建时间筛选。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('web:note:list')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @PreAuthorize("@ss.hasPermi('web:note:list')")
    @GetMapping("/list")
    public TableDataInfo list(@Parameter(hidden = true) @RequestParam Map<String, Object> map) {
        this.startPage();
        List<WebNote> noteList = noteService.selectNoteList(new Query(map));
        return getDataTable(noteList);
    }

    /**
     * 获取未审核笔记列表
     */
    @Parameters({
            @Parameter(name = "pageNum", in = ParameterIn.QUERY, description = "当前页码，从 1 开始，默认 1", schema = @Schema(type = "integer", defaultValue = "1")),
            @Parameter(name = "pageSize", in = ParameterIn.QUERY, description = "每页条数，默认 10", schema = @Schema(type = "integer", defaultValue = "10")),
            @Parameter(name = "title", in = ParameterIn.QUERY, description = "笔记标题", schema = @Schema(type = "string")),
            @Parameter(name = "uid", in = ParameterIn.QUERY, description = "作者用户 ID", schema = @Schema(type = "string")),
            @Parameter(name = "noteType", in = ParameterIn.QUERY, description = "笔记类型：1 图文，2 视频预留", schema = @Schema(type = "string")),
            @Parameter(name = "auditStatus", in = ParameterIn.QUERY, description = "审核状态：0 待审核，1 通过，2 拒绝", schema = @Schema(type = "string")),
            @Parameter(name = "pid", in = ParameterIn.QUERY, description = "父分类 ID", schema = @Schema(type = "string")),
            @Parameter(name = "params[beginTime]", in = ParameterIn.QUERY, description = "创建时间范围起点，格式 yyyy-MM-dd HH:mm:ss", schema = @Schema(type = "string")),
            @Parameter(name = "params[endTime]", in = ParameterIn.QUERY, description = "创建时间范围终点，格式 yyyy-MM-dd HH:mm:ss", schema = @Schema(type = "string"))
    })
    @Operation(summary = "分页查询待审核笔记",
            description = "查询等待审核的笔记，支持标题、作者、类型和创建时间条件。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('web:note:list')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @PreAuthorize("@ss.hasPermi('web:note:list')")
    @GetMapping("/unAuditList")
    public TableDataInfo UnAuditList(@Parameter(hidden = true) @RequestParam Map<String, Object> map) {
        this.startPage();
        List<WebNote> noteList = noteService.selectUnAuditNoteList(new Query(map));
        return getDataTable(noteList);
    }

    /**
     * 根据笔记编号获取详细信息
     */
    @Operation(summary = "获取笔记详情",
            description = "根据路径中的主键 ID 查询指定笔记的详细信息。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('web:note:query')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @PreAuthorize("@ss.hasPermi('web:note:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@Parameter(description = "笔记 ID") @PathVariable Long id) {
        return success(noteService.selectNoteById(id));
    }

    /**
     * 新增笔记
     */
    @Operation(summary = "后台新增笔记",
            description = "以 multipart/form-data 提交 note JSON 字符串及可选封面 file，保存运营录入的笔记数据。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('web:note:add')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @PreAuthorize("@ss.hasPermi('web:note:add')")
    @Log(title = "笔记管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(
            @Parameter(description = "笔记数据 JSON 字符串；修改须包含笔记 id") @RequestParam("note") String note,
            @Parameter(description = "上传文件，使用 multipart/form-data 提交；原始字段名以接口参数名为准") @RequestParam(value = "file", required = false) MultipartFile file
    ) {
        WebNote webNote = JSONUtil.toBean(note, WebNote.class);
        return toAjax(noteService.insertNote(webNote, file));
    }

    /**
     * 修改笔记
     */
    @Operation(summary = "后台修改笔记",
            description = "以 multipart/form-data 提交 note JSON 字符串及可选封面 file，保存内容并在提交后检查旧媒体引用。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('web:note:edit')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @PreAuthorize("@ss.hasPermi('web:note:edit')")
    @Log(title = "笔记管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(
            @Parameter(description = "笔记数据 JSON 字符串；修改须包含笔记 id") @RequestParam("note") String note,
            @Parameter(description = "上传文件，使用 multipart/form-data 提交；原始字段名以接口参数名为准") @RequestParam(value = "file", required = false) MultipartFile file
    ) {
        WebNote webNote = JSONUtil.toBean(note, WebNote.class);
        return toAjax(noteService.updateNote(webNote, file));
    }

    /**
     * 删除笔记
     */
    @Operation(summary = "批量删除笔记及关联",
            description = "删除笔记、评论、点赞收藏、标签和专辑关系，刷新作者与专辑统计，并在提交后检查媒体文件回收。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('web:note:remove')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @PreAuthorize("@ss.hasPermi('web:note:remove')")
    @Log(title = "笔记管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@Parameter(description = "笔记 ID 列表，多个值以英文逗号分隔") @PathVariable Long[] ids) {
        return toAjax(noteService.deleteNoteByIds(ids));
    }

    /**
     * 导出笔记列表
     */
    @Operation(summary = "导出笔记为 Excel",
            description = "按筛选条件导出笔记，响应为 Excel 文件，不使用普通 JSON 列表格式。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('web:note:export')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @Log(title = "笔记管理", businessType = BusinessType.EXPORT)
    @PreAuthorize("@ss.hasPermi('web:note:export')")
    @PostMapping("/export")
    public void export(
            HttpServletResponse response,
            @ParameterObject @Parameter(description = "笔记筛选条件，具体字段见关联模型；分页使用 pageNum 和 pageSize") Query query
    ) {
        List<WebNote> noteList = noteService.selectNoteList(query);
        ExcelUtil<WebNote> util = new ExcelUtil<>(WebNote.class);
        util.exportExcel(response, noteList, "笔记数据");
    }

    /**
     * 审核管理(0:未审核 1：通过 2：拒绝)
     */
    @Operation(summary = "审核通过或拒绝笔记",
            description = "请求体包含 noteId 和 auditType；auditType 只接受 pass 或 reject，对应审核状态通过或拒绝。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('web:note:audit')。",
            security = @SecurityRequirement(name = "adminBearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "JSON 对象：noteId 为笔记 ID；auditType 为 pass（通过）或 reject（拒绝）"))
    @PreAuthorize("@ss.hasPermi('web:note:audit')")
    @PutMapping("auditNote")
    public Result<?> auditNote(@RequestBody Map<String, String> map) {
        String noteId = map.get("noteId");
        String auditType = map.get("auditType");
        noteService.auditNote(noteId, auditType);
        return Result.ok();
    }
}
