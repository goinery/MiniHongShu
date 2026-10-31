package com.hongshu.server.controller.system.web;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import com.hongshu.common.annotation.Log;
import com.hongshu.common.core.controller.BaseController;
import com.hongshu.common.core.domain.AjaxResult;
import com.hongshu.common.core.domain.Query;
import com.hongshu.common.core.page.TableDataInfo;
import com.hongshu.common.enums.BusinessType;
import com.hongshu.web.domain.entity.WebAlbum;
import com.hongshu.web.service.ISysAlbumService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 标签操作处理
 *
 * @author hongshu
 */
@Tag(name = "运营·专辑管理", description = "专辑信息、封面、排序及关联数据维护")
@RestController
@RequestMapping("/album")
public class SysAlbumController extends BaseController {

    private final ISysAlbumService albumService;

    public SysAlbumController(ISysAlbumService albumService) {
        this.albumService = albumService;
    }

    /**
     * 获取会员列表
     */
    @Parameters({
            @Parameter(name = "pageNum", in = ParameterIn.QUERY, description = "当前页码，从 1 开始，默认 1", schema = @Schema(type = "integer", defaultValue = "1")),
            @Parameter(name = "pageSize", in = ParameterIn.QUERY, description = "每页条数，默认 10", schema = @Schema(type = "integer", defaultValue = "10")),
            @Parameter(name = "title", in = ParameterIn.QUERY, description = "专辑标题", schema = @Schema(type = "string")),
            @Parameter(name = "params[beginTime]", in = ParameterIn.QUERY, description = "创建时间范围起点，格式 yyyy-MM-dd HH:mm:ss", schema = @Schema(type = "string")),
            @Parameter(name = "params[endTime]", in = ParameterIn.QUERY, description = "创建时间范围终点，格式 yyyy-MM-dd HH:mm:ss", schema = @Schema(type = "string"))
    })
    @Operation(summary = "分页查询专辑",
            description = "按请求中的筛选条件查询专辑，分页接口使用 pageNum 和 pageSize。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('web:album:list')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @PreAuthorize("@ss.hasPermi('web:album:list')")
    @GetMapping("/list")
    public TableDataInfo list(@Parameter(hidden = true) @RequestParam Map<String, Object> map) {
        this.startPage();
        List<WebAlbum> albumList = albumService.getAlbumList(new Query(map));
        return getDataTable(albumList);
    }

    /**
     * 根据会员编号获取详细信息
     */
    @Operation(summary = "获取专辑详情",
            description = "根据路径中的主键 ID 查询指定专辑的详细信息。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('web:album:query')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @PreAuthorize("@ss.hasPermi('web:album:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@Parameter(description = "专辑 ID") @PathVariable Long id) {
        return success(albumService.getAlbumById(id));
    }

    /**
     * 新增会员
     */
    @Operation(summary = "新增专辑",
            description = "提交专辑数据，校验业务约束后保存。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('web:album:add')。",
            security = @SecurityRequirement(name = "adminBearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "请求数据，字段含义见下方模型；具体取值与操作约束见接口说明"))
    @PreAuthorize("@ss.hasPermi('web:album:add')")
    @Log(title = "会员管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody WebAlbum album) {
        return toAjax(albumService.insertAlbum(album));
    }

    /**
     * 修改会员
     */
    @Operation(summary = "修改专辑",
            description = "提交包含主键 ID 的专辑数据，校验操作权限和业务约束后更新。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('web:album:edit')。",
            security = @SecurityRequirement(name = "adminBearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "请求数据，字段含义见下方模型；具体取值与操作约束见接口说明"))
    @PreAuthorize("@ss.hasPermi('web:album:edit')")
    @Log(title = "会员管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody WebAlbum album) {
        return toAjax(albumService.updateAlbum(album));
    }

    /**
     * 删除会员
     */
    @Operation(summary = "批量删除专辑",
            description = "删除指定专辑并清理专辑笔记关系、专辑收藏关系，提交后检查旧封面是否需要回收；笔记本身保留。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('web:album:remove')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @PreAuthorize("@ss.hasPermi('web:album:remove')")
    @Log(title = "会员管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@Parameter(description = "专辑 ID 列表，多个值以英文逗号分隔") @PathVariable Long[] ids) {
        return toAjax(albumService.deleteAlbumByIds(ids));
    }
}
