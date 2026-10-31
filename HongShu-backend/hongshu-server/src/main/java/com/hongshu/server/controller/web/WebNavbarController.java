package com.hongshu.server.controller.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import com.hongshu.common.enums.Result;
import com.hongshu.common.validator.myVaildator.noLogin.NoLoginIntercept;
import com.hongshu.web.domain.vo.NavbarVO;
import com.hongshu.web.service.IWebNavbarService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 分类
 *
 * @author hongshu
 */
@RequestMapping("/web/category")
@Tag(name = "前台·内容分类", description = "社区展示与发布使用的父子分类树")
@RestController
public class WebNavbarController {

    private final IWebNavbarService navbarService;

    public WebNavbarController(IWebNavbarService navbarService) {
        this.navbarService = navbarService;
    }

    /**
     * 获取树形分类数据
     */
    @Operation(summary = "获取内容分类树",
            description = "返回父子分类层级，供首页分类导航、搜索筛选和图文发布选择使用。 无需登录；公开接口携带有效令牌时可识别当前用户。")
    @GetMapping("getCategoryTreeData")
    @NoLoginIntercept
    public Result<?> getCategoryTreeData() {
        List<NavbarVO> navbarList = navbarService.getNavbarTreeData();
        return Result.ok(navbarList);
    }
}
