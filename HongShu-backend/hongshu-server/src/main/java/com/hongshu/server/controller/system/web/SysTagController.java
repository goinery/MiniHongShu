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
import com.hongshu.web.domain.entity.WebTag;
import com.hongshu.web.service.ISysTagService;
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
@Tag(name = "运营·标签管理", description = "社区内容标签的查询与维护")
@RestController
@RequestMapping("/tag")
public class SysTagController extends BaseController {

    private final ISysTagService tagService;

    public SysTagController(ISysTagService tagService) {
        this.tagService = tagService;
    }

    /**
     * 获取会员列表
     */
    @Parameters({
            @Parameter(name = "pageNum", in = ParameterIn.QUERY, description = "当前页码，从 1 开始，默认 1", schema = @Schema(type = "integer", defaultValue = "1")),
            @Parameter(name = "pageSize", in = ParameterIn.QUERY, description = "每页条数，默认 10", schema = @Schema(type = "integer", defaultValue = "10")),
            @Parameter(name = "title", in = ParameterIn.QUERY, description = "标签标题", schema = @Schema(type = "string")),
            @Parameter(name = "params[beginTime]", in = ParameterIn.QUERY, description = "创建时间范围起点，格式 yyyy-MM-dd HH:mm:ss", schema = @Schema(type = "string")),
            @Parameter(name = "params[endTime]", in = ParameterIn.QUERY, description = "创建时间范围终点，格式 yyyy-MM-dd HH:mm:ss", schema = @Schema(type = "string"))
    })
    @Operation(summary = "分页查询标签",
            description = "按请求中的筛选条件查询标签，分页接口使用 pageNum 和 pageSize。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('web:tag:list')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @PreAuthorize("@ss.hasPermi('web:tag:list')")
    @GetMapping("/list")
    public TableDataInfo list(@Parameter(hidden = true) @RequestParam Map<String, Object> map) {
        this.startPage();
        List<WebTag> tagList = tagService.selectTagList(new Query(map));
        return getDataTable(tagList);
    }

    /**
     * 根据会员编号获取详细信息
     */
    @Operation(summary = "获取标签详情",
            description = "根据路径中的主键 ID 查询指定标签的详细信息。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('web:tag:query')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @PreAuthorize("@ss.hasPermi('web:tag:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@Parameter(description = "标签 ID") @PathVariable Long id) {
        return success(tagService.selectTagById(id));
    }

    /**
     * 新增会员
     */
    @Operation(summary = "新增标签",
            description = "提交标签数据，校验业务约束后保存。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('web:tag:add')。",
            security = @SecurityRequirement(name = "adminBearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "请求数据，字段含义见下方模型；具体取值与操作约束见接口说明"))
    @PreAuthorize("@ss.hasPermi('web:tag:add')")
    @Log(title = "会员管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody WebTag tag) {
        return toAjax(tagService.insertTag(tag));
    }

    /**
     * 修改会员
     */
    @Operation(summary = "修改标签",
            description = "提交包含主键 ID 的标签数据，校验操作权限和业务约束后更新。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('web:tag:edit')。",
            security = @SecurityRequirement(name = "adminBearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "请求数据，字段含义见下方模型；具体取值与操作约束见接口说明"))
    @PreAuthorize("@ss.hasPermi('web:tag:edit')")
    @Log(title = "会员管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody WebTag tag) {
        return toAjax(tagService.updateTag(tag));
    }

    /**
     * 删除会员
     */
    @Operation(summary = "删除标签",
            description = "按路径中的 ID 删除标签；数组形式的 ID 以英文逗号分隔，具体业务约束由服务端校验。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('web:tag:remove')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @PreAuthorize("@ss.hasPermi('web:tag:remove')")
    @Log(title = "会员管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@Parameter(description = "标签 ID 列表，多个值以英文逗号分隔") @PathVariable Long[] ids) {
        return toAjax(tagService.deleteTagByIds(ids));
    }
}
