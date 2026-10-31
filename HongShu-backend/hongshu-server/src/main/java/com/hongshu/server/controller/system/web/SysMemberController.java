package com.hongshu.server.controller.system.web;

import org.springdoc.core.annotations.ParameterObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import cn.hutool.json.JSONUtil;
import com.hongshu.common.annotation.Log;
import com.hongshu.common.core.controller.BaseController;
import com.hongshu.common.core.domain.AjaxResult;
import com.hongshu.common.core.domain.Query;
import com.hongshu.common.core.page.TableDataInfo;
import com.hongshu.common.enums.BusinessType;
import com.hongshu.common.utils.poi.ExcelUtil;
import com.hongshu.web.domain.entity.WebUser;
import com.hongshu.web.service.ISysMemberService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;

import java.util.List;
import java.util.Map;

/**
 * 会员操作处理
 *
 * @author hongshu
 */
@Tag(name = "运营·会员管理", description = "社区会员账号、资料、状态与登录会话管理")
@RestController
@RequestMapping("/member")
public class SysMemberController extends BaseController {

    private final ISysMemberService memberService;

    public SysMemberController(ISysMemberService memberService) {
        this.memberService = memberService;
    }

    /**
     * 获取会员列表
     */
    @Parameters({
            @Parameter(name = "pageNum", in = ParameterIn.QUERY, description = "当前页码，从 1 开始，默认 1", schema = @Schema(type = "integer", defaultValue = "1")),
            @Parameter(name = "pageSize", in = ParameterIn.QUERY, description = "每页条数，默认 10", schema = @Schema(type = "integer", defaultValue = "10")),
            @Parameter(name = "hsId", in = ParameterIn.QUERY, description = "会员号", schema = @Schema(type = "string")),
            @Parameter(name = "username", in = ParameterIn.QUERY, description = "会员昵称", schema = @Schema(type = "string")),
            @Parameter(name = "phone", in = ParameterIn.QUERY, description = "手机号", schema = @Schema(type = "string")),
            @Parameter(name = "status", in = ParameterIn.QUERY, description = "会员状态：0 正常，1 停用", schema = @Schema(type = "string")),
            @Parameter(name = "params[beginTime]", in = ParameterIn.QUERY, description = "创建时间范围起点，格式 yyyy-MM-dd HH:mm:ss", schema = @Schema(type = "string")),
            @Parameter(name = "params[endTime]", in = ParameterIn.QUERY, description = "创建时间范围终点，格式 yyyy-MM-dd HH:mm:ss", schema = @Schema(type = "string"))
    })
    @Operation(summary = "分页查询会员",
            description = "按请求中的筛选条件查询会员，分页接口使用 pageNum 和 pageSize。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('web:member:list')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @PreAuthorize("@ss.hasPermi('web:member:list')")
    @GetMapping("/list")
    public TableDataInfo list(@Parameter(hidden = true) @RequestParam Map<String, Object> map) {
        this.startPage();
        List<WebUser> userList = memberService.selectMemberList(new Query(map));
        return getDataTable(userList);
    }

    /**
     * 根据会员编号获取详细信息
     */
    @Operation(summary = "获取会员详情",
            description = "根据路径中的主键 ID 查询指定会员的详细信息。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('web:member:query')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @PreAuthorize("@ss.hasPermi('web:member:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@Parameter(description = "会员 ID") @PathVariable Long id) {
        return success(memberService.selectMemberById(id));
    }

    /**
     * 新增会员
     */
    @Operation(summary = "新增社区会员",
            description = "以 multipart/form-data 提交 user JSON 字符串及可选头像 file，设置会员资料并将密码编码为 BCrypt。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('web:member:add')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @PreAuthorize("@ss.hasPermi('web:member:add')")
    @Log(title = "会员管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(
            @Parameter(description = "会员资料 JSON 字符串；新增包含手机号和密码，修改包含会员 id，空密码保留原值") @RequestParam("user") String user,
            @Parameter(description = "上传文件，使用 multipart/form-data 提交；原始字段名以接口参数名为准") @RequestParam(value = "file", required = false) MultipartFile file
    ) {
        WebUser webUser = JSONUtil.toBean(user, WebUser.class);
        return toAjax(memberService.insertMember(webUser, file));
    }

    /**
     * 修改会员
     */
    @Operation(summary = "修改社区会员",
            description = "以 multipart/form-data 提交 user JSON 字符串及可选头像；密码留空保留原值，停用或改密时撤销前台会话。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('web:member:edit')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @PreAuthorize("@ss.hasPermi('web:member:edit')")
    @Log(title = "会员管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(
            @Parameter(description = "会员资料 JSON 字符串；新增包含手机号和密码，修改包含会员 id，空密码保留原值") @RequestParam("user") String user,
            @Parameter(description = "上传文件，使用 multipart/form-data 提交；原始字段名以接口参数名为准") @RequestParam(value = "file", required = false) MultipartFile file
    ) {
        WebUser webUser = JSONUtil.toBean(user, WebUser.class);
        return toAjax(memberService.updateMember(webUser, file));
    }

    /**
     * 删除会员
     */
    @Operation(summary = "批量删除社区会员",
            description = "删除指定会员并撤销其前台会话，提交后检查旧头像和背景是否仍被引用；不表示自动删除会员的全部历史内容。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('web:member:remove')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @PreAuthorize("@ss.hasPermi('web:member:remove')")
    @Log(title = "会员管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@Parameter(description = "会员 ID 列表，多个值以英文逗号分隔") @PathVariable Long[] ids) {
        return toAjax(memberService.deleteMemberByIds(ids));
    }

    /**
     * 导出会员列表
     */
    @Operation(summary = "导出会员为 Excel",
            description = "按筛选条件导出会员，响应为 Excel 文件，不使用普通 JSON 列表格式。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('web:member:export')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @Log(title = "会员管理", businessType = BusinessType.EXPORT)
    @PreAuthorize("@ss.hasPermi('web:member:export')")
    @PostMapping("/export")
    public void export(
            HttpServletResponse response,
            @ParameterObject @Parameter(description = "会员筛选条件，具体字段见关联模型；分页使用 pageNum 和 pageSize") Query query
    ) {
        List<WebUser> userList = memberService.selectMemberList(query);
        ExcelUtil<WebUser> util = new ExcelUtil<>(WebUser.class);
        util.exportExcel(response, userList, "会员数据");
    }

}
