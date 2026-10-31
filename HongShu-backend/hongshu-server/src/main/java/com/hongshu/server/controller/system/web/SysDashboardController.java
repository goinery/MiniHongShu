package com.hongshu.server.controller.system.web;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import com.hongshu.common.constant.Constantss;
import com.hongshu.common.core.controller.BaseController;
import com.hongshu.common.core.domain.AjaxResult;
import com.hongshu.common.enums.EStatus;
import com.hongshu.common.global.SysConf;
import com.hongshu.web.service.ISysCommentService;
import com.hongshu.web.service.ISysMemberService;
import com.hongshu.web.service.ISysNoteService;
import com.hongshu.web.service.ISysVisitService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 首页RestApi
 *
 * @author hongshu
 * 创建时间：2018年10月22日下午3:27:24
 */
@Tag(name = "运营·数据统计", description = "会员内容总览、近七天趋势及分类标签分布")
@RestController
@RequestMapping("/index")
@Slf4j
public class SysDashboardController extends BaseController {

    private final ISysNoteService noteService;
    private final ISysCommentService commentService;
    private final ISysVisitService visitService;
    private final ISysMemberService memberService;

    public SysDashboardController(ISysNoteService noteService, ISysCommentService commentService, ISysVisitService visitService, ISysMemberService memberService) {
        this.noteService = noteService;
        this.commentService = commentService;
        this.visitService = visitService;
        this.memberService = memberService;
    }

    @Operation(summary = "获取运营首页统计总览",
            description = "返回访问、会员、笔记和评论数量；今日访问当前按会员登录日志中的 IP 统计，非完整站点访问采集。 需携带管理端 Bearer JWT。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @RequestMapping(value = "/init", method = RequestMethod.GET)
    public AjaxResult init() {
        Map<String, Object> map = new HashMap<>(Constantss.NUM_FOUR);
        map.put(SysConf.VISIT_COUNT, visitService.getWebVisitCount());
        map.put(SysConf.USER_COUNT, memberService.getMemberCount(0));
        map.put(SysConf.BLOG_COUNT, noteService.getNoteCount(EStatus.ENABLE));
        map.put(SysConf.COMMENT_COUNT, commentService.getCommentCount(EStatus.ENABLE));
        return success(map);
    }

    @Operation(summary = "获取近七天访问趋势",
            description = "从已有 web_visit 数据统计每日 PV 和独立 IP 数，缺失日期补零，结果采用按日期隔离的短期缓存。 需携带管理端 Bearer JWT。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @RequestMapping(value = "/getVisitByWeek", method = RequestMethod.GET)
    public AjaxResult getVisitByWeek() {
        Map<String, Object> visitByWeek = visitService.getVisitByWeek();
        return success(visitByWeek);
    }

    @Operation(summary = "统计各标签笔记数量",
            description = "按标签关联统计笔记数量，返回名称和数量用于图表展示。 需携带管理端 Bearer JWT。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @RequestMapping(value = "/getBlogCountByTag", method = RequestMethod.GET)
    public AjaxResult getBlogCountByTag() {
        List<Map<String, Object>> blogCountByTag = noteService.getNoteCountByTag();
        return success(blogCountByTag);
    }

    @Operation(summary = "统计各分类笔记数量",
            description = "按父分类汇总笔记数量，返回分类名称和数量。 需携带管理端 Bearer JWT。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @RequestMapping(value = "/getBlogCountByBlogSort", method = RequestMethod.GET)
    public AjaxResult getBlogCountByBlogSort() {
        List<Map<String, Object>> blogCountByTag = noteService.getNoteCountByCategory();
        return success(blogCountByTag);
    }

    @Operation(summary = "获取近一年笔记发布分布",
            description = "按日期统计近一年笔记发布数量，补齐没有发布的日期，供贡献日历图展示。 需携带管理端 Bearer JWT。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @RequestMapping(value = "/getBlogContributeCount", method = RequestMethod.GET)
    public AjaxResult getBlogContributeCount() {
        Map<String, Object> resultMap = noteService.getNoteContributeCount();
        return success(resultMap);
    }
}
