package com.hongshu.server.controller.web;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hongshu.common.enums.Result;
import com.hongshu.common.validator.ValidatorUtils;
import com.hongshu.common.validator.group.AddGroup;
import com.hongshu.web.domain.dto.LikeOrCollectDTO;
import com.hongshu.web.domain.vo.LikeOrCollectVO;
import com.hongshu.web.service.IWebLikeOrCollectService;
import org.springframework.web.bind.annotation.*;

/**
 * 点赞/收藏
 *
 * @author hongshu
 */
@RequestMapping("/web/likeOrCollection")
@Tag(name = "前台·点赞收藏", description = "笔记评论点赞、笔记专辑收藏、互动状态与通知")
@RestController
public class WebLikeOrCollectController {

    private final IWebLikeOrCollectService likeOrCollectionService;

    public WebLikeOrCollectController(IWebLikeOrCollectService likeOrCollectionService) {
        this.likeOrCollectionService = likeOrCollectionService;
    }

    /**
     * 点赞或收藏
     *
     * @param likeOrCollectDTO 点赞收藏实体
     */
    @Operation(summary = "切换点赞或收藏状态",
            description = "type：1 笔记点赞、2 评论点赞、3 笔记收藏、4 专辑收藏；再次调用取消。后端解析资源作者并更新计数，收藏笔记联动默认专辑。 需携带前台 accessToken。",
            security = @SecurityRequirement(name = "appAccessToken"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "请求数据，字段含义见下方模型；具体取值与操作约束见接口说明"))
    @PostMapping("likeOrCollectionByDTO")
    public Result<?> likeOrCollectionByDTO(@RequestBody LikeOrCollectDTO likeOrCollectDTO) {
        ValidatorUtils.validateEntity(likeOrCollectDTO, AddGroup.class);
        likeOrCollectionService.likeOrCollectionByDTO(likeOrCollectDTO);
        return Result.ok();
    }

    /**
     * 是否点赞或收藏
     *
     * @param likeOrCollectDTO 点赞收藏实体
     */
    @Operation(summary = "判断本人点赞或收藏状态",
            description = "按当前登录用户、目标 ID 和 type 查询互动关系，返回布尔值。 需携带前台 accessToken。",
            security = @SecurityRequirement(name = "appAccessToken"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "请求数据，字段含义见下方模型；具体取值与操作约束见接口说明"))
    @PostMapping("isLikeOrCollection")
    public Result<?> isLikeOrCollection(@RequestBody LikeOrCollectDTO likeOrCollectDTO) {
        boolean flag = likeOrCollectionService.isLikeOrCollection(likeOrCollectDTO);
        return Result.ok(flag);
    }

    /**
     * 获取当前用户最新的点赞和收藏信息
     *
     * @param currentPage 当前页
     * @param pageSize    分页数
     */
    @Operation(summary = "分页查询收到的点赞收藏",
            description = "返回其他用户对本人内容的点赞或收藏，附带用户和目标内容信息。 需携带前台 accessToken。",
            security = @SecurityRequirement(name = "appAccessToken"))
    @GetMapping("getNoticeLikeOrCollection/{currentPage}/{pageSize}")
    public Result<?> getNoticeLikeOrCollection(
            @Parameter(description = "当前页码，从 1 开始") @PathVariable long currentPage,
            @Parameter(description = "每页条数；具体上限及默认值见接口说明") @PathVariable long pageSize
    ) {
        Page<LikeOrCollectVO> pageInfo = likeOrCollectionService.getNoticeLikeOrCollection(currentPage, pageSize);
        return Result.ok(pageInfo);
    }
}
