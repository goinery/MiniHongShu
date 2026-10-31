package com.hongshu.server.controller.system.web;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import com.hongshu.common.core.domain.AjaxResult;
import com.hongshu.web.service.AlbumContentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "运营·专辑内容", description = "专辑内笔记的添加、排序和移出")
@RestController
@RequestMapping("/albumItem")
public class SysAlbumItemController {
    private final AlbumContentService service;
    public SysAlbumItemController(AlbumContentService service) { this.service = service; }

    @Schema(description = "专辑内容操作参数，不同操作按说明选择 noteIds 或 ids")
    public record Items(
            @Schema(description = "专辑 ID") @NotBlank String albumId,
            @Schema(description = "添加时使用的笔记 ID 数组，最多 500 个") @Size(max = 500) List<String> noteIds,
            @Schema(description = "专辑内容关联记录 ID 数组；排序须包含全部关联 ID，移出时填写选中关联 ID，最多 5000 个")
            @Size(max = 5000) List<String> ids) {}

    @Operation(summary = "查看专辑内笔记",
            description = "根据 albumId 返回专辑基本信息以及按 sort 排序的笔记内容和关联记录 ID。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('web:album:query')。",
            security = @SecurityRequirement(name = "adminBearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "请求数据，字段含义见下方模型；具体取值与操作约束见接口说明"))
    @PostMapping("/getList")
    @PreAuthorize("@ss.hasPermi('web:album:query')")
    public AjaxResult list(@Valid @RequestBody Items body) { return AjaxResult.success(service.list(body.albumId())); }

    @Operation(summary = "向专辑添加笔记",
            description = "提交 albumId 和 noteIds，只允许添加已审核通过的笔记，跳过已有关系并重算专辑内容数量。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('web:album:edit')。",
            security = @SecurityRequirement(name = "adminBearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "请求数据，字段含义见下方模型；具体取值与操作约束见接口说明"))
    @PostMapping("/add")
    @PreAuthorize("@ss.hasPermi('web:album:edit')")
    public AjaxResult add(@Valid @RequestBody Items body) {
        service.add(body.albumId(), body.noteIds());
        return AjaxResult.success();
    }

    @Operation(summary = "调整专辑内容顺序",
            description = "提交 albumId 和按目标顺序排列的关联记录 ids；必须包含当前专辑全部关联 ID，且不能重复。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('web:album:edit')。",
            security = @SecurityRequirement(name = "adminBearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "请求数据，字段含义见下方模型；具体取值与操作约束见接口说明"))
    @PostMapping("/edit")
    @PreAuthorize("@ss.hasPermi('web:album:edit')")
    public AjaxResult edit(@Valid @RequestBody Items body) {
        service.reorder(body.albumId(), body.ids());
        return AjaxResult.success();
    }

    @Operation(summary = "按发布时间重排专辑",
            description = "将指定专辑中的笔记按发布时间倒序排列，并保存新的 sort 值。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('web:album:edit')。",
            security = @SecurityRequirement(name = "adminBearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "请求数据，字段含义见下方模型；具体取值与操作约束见接口说明"))
    @PostMapping("/sortByCreateTime")
    @PreAuthorize("@ss.hasPermi('web:album:edit')")
    public AjaxResult sort(@Valid @RequestBody Items body) {
        service.sortByTime(body.albumId());
        return AjaxResult.success();
    }

    @Operation(summary = "批量移出专辑内容",
            description = "提交 albumId 和关联记录 ids，仅移除专辑关联并重算数量，笔记本身保留。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('web:album:edit')。",
            security = @SecurityRequirement(name = "adminBearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "请求数据，字段含义见下方模型；具体取值与操作约束见接口说明"))
    @PostMapping("/deleteBatch")
    @PreAuthorize("@ss.hasPermi('web:album:edit')")
    public AjaxResult remove(@Valid @RequestBody Items body) {
        service.remove(body.albumId(), body.ids());
        return AjaxResult.success();
    }
}
