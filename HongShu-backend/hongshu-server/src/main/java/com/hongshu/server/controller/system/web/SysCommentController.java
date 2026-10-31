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
import com.hongshu.web.domain.vo.CommentVO;
import com.hongshu.web.service.ISysCommentService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 评论操作处理
 *
 * @author hongshu
 */
@Tag(name = "运营·评论管理", description = "社区评论、回复查询及批量删除")
@RestController
@RequestMapping("/comment")
public class SysCommentController extends BaseController {

    private final ISysCommentService commentService;

    public SysCommentController(ISysCommentService commentService) {
        this.commentService = commentService;
    }

    /**
     * 获取所有一级分类列表
     */
    @Parameters({
            @Parameter(name = "pageNum", in = ParameterIn.QUERY, description = "当前页码，从 1 开始，默认 1", schema = @Schema(type = "integer", defaultValue = "1")),
            @Parameter(name = "pageSize", in = ParameterIn.QUERY, description = "每页条数，默认 10", schema = @Schema(type = "integer", defaultValue = "10")),
            @Parameter(name = "uid", in = ParameterIn.QUERY, description = "评论作者用户 ID", schema = @Schema(type = "string")),
            @Parameter(name = "nid", in = ParameterIn.QUERY, description = "所属笔记 ID", schema = @Schema(type = "string")),
            @Parameter(name = "content", in = ParameterIn.QUERY, description = "评论内容关键词，仅一级列表支持", schema = @Schema(type = "string")),
            @Parameter(name = "params[beginTime]", in = ParameterIn.QUERY, description = "创建时间范围起点，格式 yyyy-MM-dd HH:mm:ss", schema = @Schema(type = "string")),
            @Parameter(name = "params[endTime]", in = ParameterIn.QUERY, description = "创建时间范围终点，格式 yyyy-MM-dd HH:mm:ss", schema = @Schema(type = "string"))
    })
    @Operation(summary = "分页查询社区评论",
            description = "支持作者、笔记、评论内容和创建时间筛选；未指定笔记时默认查询一级评论。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('web:comment:list')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @PreAuthorize("@ss.hasPermi('web:comment:list')")
    @GetMapping("/list")
    public TableDataInfo list(@Parameter(hidden = true) @RequestParam Map<String, Object> map) {
        this.startPage();
        List<CommentVO> commentList = commentService.selectCommentList(new Query(map));
        return getDataTable(commentList);
    }

    /**
     * 查询一级以下评论
     */
    @Parameters({
            @Parameter(name = "pageNum", in = ParameterIn.QUERY, description = "当前页码，从 1 开始，默认 1", schema = @Schema(type = "integer", defaultValue = "1")),
            @Parameter(name = "pageSize", in = ParameterIn.QUERY, description = "每页条数，默认 10", schema = @Schema(type = "integer", defaultValue = "10")),
            @Parameter(name = "uid", in = ParameterIn.QUERY, description = "评论作者用户 ID", schema = @Schema(type = "string")),
            @Parameter(name = "nid", in = ParameterIn.QUERY, description = "所属笔记 ID", schema = @Schema(type = "string")),
            @Parameter(name = "content", in = ParameterIn.QUERY, description = "评论内容关键词，仅一级列表支持", schema = @Schema(type = "string")),
            @Parameter(name = "params[beginTime]", in = ParameterIn.QUERY, description = "创建时间范围起点，格式 yyyy-MM-dd HH:mm:ss", schema = @Schema(type = "string")),
            @Parameter(name = "params[endTime]", in = ParameterIn.QUERY, description = "创建时间范围终点，格式 yyyy-MM-dd HH:mm:ss", schema = @Schema(type = "string"))
    })
    @Operation(summary = "查询笔记下的评论回复",
            description = "按笔记、作者和创建时间查询评论记录，供管理端展开评论内容。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('web:comment:list')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @PreAuthorize("@ss.hasPermi('web:comment:list')")
    @GetMapping("/treeList")
    public TableDataInfo treeList(@Parameter(hidden = true) @RequestParam Map<String, Object> map) {
        this.startPage();
        List<CommentVO> commentList = commentService.selectTreeList(new Query(map));
        return getDataTable(commentList);
    }

    /**
     * 根据评论编号获取详细信息
     */
    @Operation(summary = "获取评论详情",
            description = "根据路径中的主键 ID 查询指定评论的详细信息。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('web:comment:query')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @PreAuthorize("@ss.hasPermi('web:comment:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@Parameter(description = "评论 ID") @PathVariable Long id) {
        return success(commentService.selectCommentById(id));
    }

    /**
     * 根据笔记编号获取详细信息
     */
    @Operation(summary = "按笔记查询评论",
            description = "根据笔记 ID 查询其评论内容，供运营查看指定笔记的讨论情况。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('web:comment:query')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @PreAuthorize("@ss.hasPermi('web:comment:query')")
    @GetMapping(value = "/list/{nid}")
    public AjaxResult getComment(@Parameter(description = "笔记 ID") @PathVariable String nid) {
        return success(commentService.selectCommentByNid(nid));
    }

    /**
     * 删除评论
     */
    @Operation(summary = "批量删除评论及回复",
            description = "删除指定评论、其下级回复和评论点赞关系，并调整笔记评论数及上级回复数。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('web:comment:remove')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @PreAuthorize("@ss.hasPermi('web:comment:remove')")
    @Log(title = "评论管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@Parameter(description = "评论 ID 列表，多个值以英文逗号分隔") @PathVariable Long[] ids) {
        return toAjax(commentService.deleteCommentByIds(ids));
    }
}
