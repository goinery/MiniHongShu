package com.hongshu.server.controller.web;

import org.springdoc.core.annotations.ParameterObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.pagehelper.PageInfo;
import com.hongshu.common.constant.HttpStatus;
import com.hongshu.common.core.page.TableDataInfo;
import com.hongshu.common.enums.Result;
import com.hongshu.common.validator.myVaildator.noLogin.NoLoginIntercept;
import com.hongshu.web.domain.entity.WebUser;
import com.hongshu.web.domain.dto.ChangePasswordDTO;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
import com.hongshu.common.exception.web.HongshuException;
import com.hongshu.web.domain.vo.NoteSearchVO;
import com.hongshu.web.service.IWebUserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.hongshu.common.utils.PageUtils.startPage;

/**
 * 用户
 *
 * @author hongshu
 */
@RequestMapping("/web/user")
@Tag(name = "前台·用户主页", description = "公开资料、个人作品点赞收藏、资料修改与用户搜索")
@RestController
public class WebUserController {

    private final IWebUserService userService;

    public WebUserController(IWebUserService userService) {
        this.userService = userService;
    }

    /**
     * 获取当前用户信息
     *
     * @param currentPage 当前页
     * @param pageSize    分页数
     * @param userId      用户ID
     * @param type        类型
     */
    @Operation(summary = "分页获取用户作品点赞收藏",
            description = "type：1 发布作品、2 赞过笔记、3 收藏笔记；本人作品支持查看待审内容，其他作者内容受审核可见性限制。 无需登录；公开接口携带有效令牌时可识别当前用户。")
    @NoLoginIntercept
    @GetMapping("getTrendByUser/{currentPage}/{pageSize}")
    public Result<?> getTrendByUser(
            @Parameter(description = "当前页码，从 1 开始") @PathVariable long currentPage,
            @Parameter(description = "每页条数；具体上限及默认值见接口说明") @PathVariable long pageSize,
            @Parameter(description = "用户 ID") String userId,
            @Parameter(description = "查询类型：1 发布作品，2 赞过笔记，3 收藏笔记") Integer type
    ) {
        Page<NoteSearchVO> pageInfo = userService.getTrendByUser(currentPage, pageSize, userId, type);
        return Result.ok(pageInfo);
    }

    /**
     * 获取用户信息
     *
     * @param userId 用户ID
     */
    @Operation(summary = "获取用户公开主页资料",
            description = "返回公开资料和按访问者身份计算的笔记数量；本人额外返回手机号、邮箱和登录信息，其他访问者不可见。始终隐藏密码。无需登录；携带有效令牌时识别当前用户。")
    @NoLoginIntercept
    @GetMapping("getUserById")
    public Result<?> getUserById(@Parameter(description = "用户 ID") String userId) {
        WebUser user = userService.getUserById(userId);
        return Result.ok(user);
    }

    /**
     * 更新用户信息
     *
     * @param user 用户
     */
    @Operation(summary = "修改本人社区资料",
            description = "按当前身份更新昵称、头像、简介、标签、性别、生日、手机号和邮箱；IP属地由服务端自动识别，忽略客户端地址和其他账号身份。手机号与邮箱不能被其他账号占用。需携带前台 accessToken。",
            security = @SecurityRequirement(name = "appAccessToken"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "请求数据，字段含义见下方模型；具体取值与操作约束见接口说明"))
    @PostMapping("updateUser")
    public Result<?> updateUser(@RequestBody WebUser user) {
        WebUser updateUser = userService.updateUser(user);
        return Result.ok(updateUser);
    }

    @Operation(summary = "修改本人密码",
            description = "校验原密码及新密码确认值，更新密码并撤销现有会话，成功后需重新登录。",
            security = @SecurityRequirement(name = "appAccessToken"))
    @PostMapping("changePassword")
    public Result<?> changePassword(@Valid @RequestBody ChangePasswordDTO passwords, BindingResult validation) {
        // 不让全局校验异常日志记录密码字段的 rejectedValue。
        if (validation.hasErrors()) {
            throw new HongshuException(validation.getAllErrors().getFirst().getDefaultMessage());
        }
        userService.changePassword(passwords);
        return Result.ok();
    }

    /**
     * 查找用户信息
     *
     * @param keyword 关键词
     */
    @Operation(summary = "分页搜索社区用户",
            description = "根据昵称模糊检索用户，返回已裁剪敏感字段的公开资料。 无需登录；公开接口携带有效令牌时可识别当前用户。")
    @NoLoginIntercept
    @GetMapping("getUserByKeyword/{currentPage}/{pageSize}")
    public Result<?> getUserByKeyword(
            @Parameter(description = "当前页码，从 1 开始") @PathVariable long currentPage,
            @Parameter(description = "每页条数；具体上限及默认值见接口说明") @PathVariable long pageSize,
            @Parameter(description = "搜索关键词") String keyword
    ) {
        Page<WebUser> pageInfo = userService.getUserByKeyword(currentPage, pageSize, keyword);
        return Result.ok(pageInfo);
    }

    /**
     * 保存用户的搜索记录
     *
     * @param keyword 关键词
     */
    @Operation(summary = "保存本人搜索记录",
            description = "将关键词写入当前用户的服务端搜索记录，已有关键词累加次数并更新时间。 需携带前台 accessToken。",
            security = @SecurityRequirement(name = "appAccessToken"))
    @GetMapping("saveUserSearchRecord")
    public Result<?> saveUserSearchRecord(@Parameter(description = "搜索关键词") String keyword) {
        userService.saveUserSearchRecord(keyword);
        return Result.ok();
    }

    /**
     * 会员列表
     *
     * @param user 用户
     */
    @Parameters({
            @Parameter(name = "pageNum", in = ParameterIn.QUERY, description = "当前页码，从 1 开始，默认 1", schema = @Schema(type = "integer", defaultValue = "1")),
            @Parameter(name = "pageSize", in = ParameterIn.QUERY, description = "每页条数，默认 10", schema = @Schema(type = "integer", defaultValue = "10"))
    })
    @Operation(summary = "分页查询社区用户列表",
            description = "按昵称等已支持条件查询用户列表，使用 pageNum 和 pageSize 分页，返回公开资料。 需携带前台 accessToken。",
            security = @SecurityRequirement(name = "appAccessToken"))
    @GetMapping("getUserList")
    public TableDataInfo getUserList(@ParameterObject @Parameter(description = "用户筛选条件，具体字段见关联模型；分页使用 pageNum 和 pageSize") WebUser user) {
        startPage();
        List<WebUser> userList = userService.getUserList(user);
        return getDataTable(userList);
    }

    /**
     * 响应请求分页数据
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected TableDataInfo getDataTable(List<?> list) {
        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(HttpStatus.SUCCESS);
        rspData.setMsg("查询成功");
        rspData.setRows(list);
        rspData.setTotal(new PageInfo(list).getTotal());
        return rspData;
    }
}
