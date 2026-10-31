package com.hongshu.server.controller.web;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hongshu.common.enums.Result;
import com.hongshu.common.validator.ValidatorUtils;
import com.hongshu.common.validator.group.AddGroup;
import com.hongshu.common.validator.myVaildator.noLogin.NoLoginIntercept;
import com.hongshu.web.domain.dto.CommentDTO;
import com.hongshu.web.domain.vo.CommentVO;
import com.hongshu.web.service.IWebCommentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 评论
 *
 * @author hongshu
 */
@RequestMapping("/web/comment")
@Tag(name = "前台·评论回复", description = "评论、二级回复、回复预览及评论通知")
@RestController
public class WebCommentController {

    private final IWebCommentService commentService;

    public WebCommentController(IWebCommentService commentService) {
        this.commentService = commentService;
    }

    /**
     * 获取所有一级分类
     *
     * @param currentPage 当前页
     * @param pageSize    分页数
     * @param noteId      笔记ID
     */
    @Operation(summary = "分页查询笔记一级评论",
            description = "验证笔记可见性后返回一级评论，按点赞数、创建时间和 ID 倒序；不附带回复内容。 无需登录；公开接口携带有效令牌时可识别当前用户。")
    @GetMapping("getOneCommentByNoteId/{currentPage}/{pageSize}")
    @NoLoginIntercept
    public Result<?> getOneCommentByNoteId(
            @Parameter(description = "当前页码，从 1 开始") @PathVariable long currentPage,
            @Parameter(description = "每页条数；具体上限及默认值见接口说明") @PathVariable long pageSize,
            @Parameter(description = "笔记 ID") String noteId
    ) {
        Page<CommentVO> pageInfo = commentService.getOneCommentByNoteId(currentPage, pageSize, noteId);
        return Result.ok(pageInfo);
    }

    /**
     * 获取当前评论
     *
     * @param commentId 评论ID
     */
    @Operation(summary = "获取评论详情",
            description = "返回指定评论、作者、回复对象和点赞状态，并验证所属笔记可见性。 需携带前台 accessToken。",
            security = @SecurityRequirement(name = "appAccessToken"))
    @GetMapping("getCommentById")
    public Result<?> getCommentById(@Parameter(description = "评论 ID") String commentId) {
        return Result.ok(commentService.getCommentById(commentId));
    }

    /**
     * 保存评论
     *
     * @param commentDTO 评论
     */
    @Operation(summary = "发表一级评论或二级回复",
            description = "content 为 1 至 2000 字；pid 为空或 0 表示一级评论，否则必须指向同笔记根评论，replyId 可指定被回复评论；身份和接收者由后端确定。 需携带前台 accessToken。",
            security = @SecurityRequirement(name = "appAccessToken"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "请求数据，字段含义见下方模型；具体取值与操作约束见接口说明"))
    @PostMapping("saveCommentByDTO")
    public Result<?> saveCommentByDTO(@RequestBody CommentDTO commentDTO) {
        ValidatorUtils.validateEntity(commentDTO, AddGroup.class);
        CommentVO commentVo = commentService.saveCommentByDTO(commentDTO);
        return Result.ok(commentVo);
    }

    /**
     * 根据评论ID同步评论集
     *
     * @param commentIds 评论ID数据集
     */
    @Operation(summary = "评论同步兼容入口",
            description = "当前评论统一存储在 web_comment，本方法为空兼容实现，提交 ID 列表不会触发复制或异步同步。 需携带前台 accessToken。",
            security = @SecurityRequirement(name = "appAccessToken"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "JSON 数组，包含本次操作所需的 ID、文件路径或缓存前缀，具体内容见接口说明"))
    @PostMapping("syncCommentByIds")
    public Result<?> syncCommentByIds(@RequestBody List<String> commentIds) {
        commentService.syncCommentByIds(commentIds);
        return Result.ok();
    }

    /**
     * 根据一级评论ID获取所有二级评论
     *
     * @param currentPage  当前页
     * @param pageSize     分页数
     * @param oneCommentId 一级评论ID
     */
    @Operation(summary = "分页展开二级回复",
            description = "根据一级评论 ID 查询回复，验证笔记可见性后按点赞数、时间和 ID 排序。 无需登录；公开接口携带有效令牌时可识别当前用户。")
    @GetMapping("getTwoCommentByOneCommentId/{currentPage}/{pageSize}")
    @NoLoginIntercept
    public Result<?> getTwoCommentByOneCommentId(
            @Parameter(description = "当前页码，从 1 开始") @PathVariable long currentPage,
            @Parameter(description = "每页条数；具体上限及默认值见接口说明") @PathVariable long pageSize,
            @Parameter(description = "一级根评论 ID") String oneCommentId
    ) {
        IPage<CommentVO> pageInfo = commentService.getTwoCommentByOneCommentId(currentPage, pageSize, oneCommentId);
        return Result.ok(pageInfo);
    }

    /**
     * 获取当前用户通知的评论集
     *
     * @param currentPage 当前页
     * @param pageSize    分页数
     */
    @Operation(summary = "分页查询收到的评论通知",
            description = "查询当前会员笔记收到的评论和针对本人的回复，排除本人操作。 需携带前台 accessToken。",
            security = @SecurityRequirement(name = "appAccessToken"))
    @GetMapping("getNoticeComment/{currentPage}/{pageSize}")
    public Result<?> getNoticeComment(
            @Parameter(description = "当前页码，从 1 开始") @PathVariable long currentPage,
            @Parameter(description = "每页条数；具体上限及默认值见接口说明") @PathVariable long pageSize
    ) {
        IPage<CommentVO> pageInfo = commentService.getNoticeComment(currentPage, pageSize);
        return Result.ok(pageInfo);
    }

    /**
     * 获取所有的一级评论并携带二级评论
     *
     * @param currentPage 当前页
     * @param pageSize    分页数
     * @param noteId      笔记ID
     */
    @Operation(summary = "分页查询评论及回复预览",
            description = "分页返回一级评论，每条附带最多 3 条二级回复；更多回复使用展开接口获取。 无需登录；公开接口携带有效令牌时可识别当前用户。")
    @GetMapping("getCommentWithCommentByNoteId/{currentPage}/{pageSize}")
    @NoLoginIntercept
    public Result<?> getCommentWithCommentByNoteId(
            @Parameter(description = "当前页码，从 1 开始") @PathVariable long currentPage,
            @Parameter(description = "每页条数；具体上限及默认值见接口说明") @PathVariable long pageSize,
            @Parameter(description = "笔记 ID") String noteId
    ) {
        Page<CommentVO> pageInfo = commentService.getCommentWithCommentByNoteId(currentPage, pageSize, noteId);
        return Result.ok(pageInfo);
    }

    /**
     * 删除评论
     */
    @Operation(summary = "删除评论及下级回复",
            description = "允许评论作者或笔记作者操作，删除下级回复及点赞关系并调整评论统计。 需携带前台 accessToken。",
            security = @SecurityRequirement(name = "appAccessToken"))
    @DeleteMapping("deleteComment")
    public Result<?> deleteComment(@Parameter(description = "评论 ID") String commentId) {
        commentService.deleteCommentById(commentId);
        return Result.ok();
    }
}
