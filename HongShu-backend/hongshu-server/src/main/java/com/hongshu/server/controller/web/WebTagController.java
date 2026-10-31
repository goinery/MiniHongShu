package com.hongshu.server.controller.web;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hongshu.common.enums.Result;
import com.hongshu.web.domain.entity.WebTag;
import com.hongshu.web.domain.vo.TagVO;
import com.hongshu.web.service.IWebTagService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 标签
 *
 * @author hongshu
 */
@RequestMapping("/web/tag")
@Tag(name = "前台·标签查询", description = "热门标签与标签关键词检索")
@RestController
public class WebTagController {

    private final IWebTagService tagService;

    public WebTagController(IWebTagService tagService) {
        this.tagService = tagService;
    }

    /**
     * 获取热门标签
     */
    @Operation(summary = "获取热门标签",
            description = "按标签 like_count 倒序返回最多 20 个标签，供标签推荐选择。 需携带前台 accessToken。",
            security = @SecurityRequirement(name = "appAccessToken"))
    @GetMapping("getHotTagList")
    public Result<?> getHotTagList() {
        List<TagVO> voList = tagService.getHotTagList();
        return Result.ok(voList);
    }

    /**
     * 根据关键词获取标签
     *
     * @param currentPage 当前页
     * @param pageSize    分页数
     * @param keyword     关键词
     */
    @Operation(summary = "分页搜索标签",
            description = "按标签标题模糊匹配 keyword，按 like_count 倒序返回；空关键词返回可选标签。 需携带前台 accessToken。",
            security = @SecurityRequirement(name = "appAccessToken"))
    @GetMapping("getTagByKeyword/{currentPage}/{pageSize}")
    public Result<?> getTagByKeyword(
            @Parameter(description = "当前页码，从 1 开始") @PathVariable long currentPage,
            @Parameter(description = "每页条数；具体上限及默认值见接口说明") @PathVariable long pageSize,
            @Parameter(description = "搜索关键词") String keyword
    ) {
        Page<WebTag> page = tagService.getTagByKeyword(currentPage, pageSize, keyword);
        return Result.ok(page);
    }

}
