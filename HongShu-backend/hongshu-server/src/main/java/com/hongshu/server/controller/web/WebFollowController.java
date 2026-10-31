package com.hongshu.server.controller.web;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hongshu.common.enums.Result;
import com.hongshu.common.validator.myVaildator.noLogin.NoLoginIntercept;
import com.hongshu.web.domain.vo.FollowVO;
import com.hongshu.web.domain.vo.TrendVO;
import com.hongshu.web.service.IWebFollowService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 关注
 *
 * @author hongshu
 */
@RequestMapping("/web/follower")
@Tag(name = "前台·关注动态", description = "关注切换、关系判断、关注动态与新增粉丝通知")
@RestController
public class WebFollowController {

    private final IWebFollowService followService;

    public WebFollowController(IWebFollowService followService) {
        this.followService = followService;
    }

    @Operation(summary = "分页查询用户关注或粉丝",
            description = "查询指定用户的公开关系列表及当前访问者对每个用户的关注状态；type 为 following（关注）或 fans（粉丝）。无需登录，每页最多 100 条。")
    @NoLoginIntercept
    @GetMapping("getUserRelations/{currentPage}/{pageSize}")
    public Result<?> getUserRelations(
            @Parameter(description = "当前页码，从 1 开始") @PathVariable long currentPage,
            @Parameter(description = "每页条数，1–100") @PathVariable long pageSize,
            @Parameter(description = "主页用户 ID") @RequestParam String userId,
            @Parameter(description = "following：关注；fans：粉丝") @RequestParam String type
    ) {
        return Result.ok(followService.getUserRelations(userId, type, currentPage, pageSize));
    }

    /**
     * 获取关注用户的所有动态
     *
     * @param currentPage 当前页
     * @param pageSize    分页数
     */
    @Operation(summary = "分页获取关注动态",
            description = "查询本人及已关注作者的有效、审核通过图文，按更新时间和 ID 倒序，包含作者、互动状态和图片预览。 需携带前台 accessToken。",
            security = @SecurityRequirement(name = "appAccessToken"))
    @GetMapping("getFollowTrend/{currentPage}/{pageSize}")
    public Result<?> getFollowTrend(
            @Parameter(description = "当前页码，从 1 开始") @PathVariable long currentPage,
            @Parameter(description = "每页条数；具体上限及默认值见接口说明") @PathVariable long pageSize
    ) {
        Page<TrendVO> pageInfo = followService.getFollowTrend(currentPage, pageSize);
        return Result.ok(pageInfo);
    }

    /**
     * 关注用户
     *
     * @param followerId 关注用户ID
     */
    @Operation(summary = "关注或取消关注用户",
            description = "根据当前关系切换状态，返回操作后的关注状态，禁止关注自己；事务内固定顺序锁定双方用户，更新关注数和粉丝数，提交后通知。 需携带前台 accessToken。",
            security = @SecurityRequirement(name = "appAccessToken"))
    @GetMapping("followById")
    public Result<?> followById(@Parameter(description = "要关注或查询关注状态的用户 ID") String followerId) {
        followService.followById(followerId);
        return Result.ok(followService.isFollow(followerId));
    }

    /**
     * 当前用户是否关注
     *
     * @param followerId 关注的用户ID
     */
    @Operation(summary = "判断是否关注指定用户",
            description = "返回当前会员是否已关注 followerId；游客、本人或空目标返回未关注。 需携带前台 accessToken。",
            security = @SecurityRequirement(name = "appAccessToken"))
    @GetMapping("isFollow")
    public Result<?> isFollow(@Parameter(description = "要关注或查询关注状态的用户 ID") String followerId) {
        boolean flag = followService.isFollow(followerId);
        return Result.ok(flag);
    }

    /**
     * 获取当前用户的最新关注信息
     *
     * @param currentPage 当前页
     * @param pageSize    分页数
     */
    @Operation(summary = "分页查询新增粉丝通知",
            description = "返回关注了当前会员的用户及当前回关状态，排除本人记录。 需携带前台 accessToken。",
            security = @SecurityRequirement(name = "appAccessToken"))
    @GetMapping("getNoticeFollower/{currentPage}/{pageSize}")
    public Result<?> getNoticeFollower(
            @Parameter(description = "当前页码，从 1 开始") @PathVariable long currentPage,
            @Parameter(description = "每页条数；具体上限及默认值见接口说明") @PathVariable long pageSize
    ) {
        Page<FollowVO> pageInfo = followService.getNoticeFollower(currentPage, pageSize);
        return Result.ok(pageInfo);
    }
}
