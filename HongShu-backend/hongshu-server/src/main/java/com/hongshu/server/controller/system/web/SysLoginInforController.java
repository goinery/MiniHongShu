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
import com.hongshu.web.domain.entity.WebLoginLog;
import com.hongshu.web.service.ISysLoginInforService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 系统访问记录
 *
 * @author hongshu
 */
@Tag(name = "运营·会员登录记录", description = "社区会员登录记录查询与清理")
@RestController
@RequestMapping("/loginInfo")
public class SysLoginInforController extends BaseController {

    private final ISysLoginInforService loginInforService;

    public SysLoginInforController(ISysLoginInforService loginInforService) {
        this.loginInforService = loginInforService;
    }

    @Parameters({
            @Parameter(name = "pageNum", in = ParameterIn.QUERY, description = "当前页码，从 1 开始，默认 1", schema = @Schema(type = "integer", defaultValue = "1")),
            @Parameter(name = "pageSize", in = ParameterIn.QUERY, description = "每页条数，默认 10", schema = @Schema(type = "integer", defaultValue = "10")),
            @Parameter(name = "ipaddr", in = ParameterIn.QUERY, description = "会员登录 IP", schema = @Schema(type = "string")),
            @Parameter(name = "username", in = ParameterIn.QUERY, description = "会员昵称", schema = @Schema(type = "string")),
            @Parameter(name = "status", in = ParameterIn.QUERY, description = "登录状态：0 成功，1 失败", schema = @Schema(type = "string")),
            @Parameter(name = "params[beginTime]", in = ParameterIn.QUERY, description = "创建时间范围起点，格式 yyyy-MM-dd HH:mm:ss", schema = @Schema(type = "string")),
            @Parameter(name = "params[endTime]", in = ParameterIn.QUERY, description = "创建时间范围终点，格式 yyyy-MM-dd HH:mm:ss", schema = @Schema(type = "string"))
    })
    @Operation(summary = "分页查询会员登录记录",
            description = "按请求中的筛选条件查询会员登录记录，分页接口使用 pageNum 和 pageSize。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('web:logininfor:list')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @PreAuthorize("@ss.hasPermi('web:logininfor:list')")
    @GetMapping("/list")
    public TableDataInfo list(@Parameter(hidden = true) @RequestParam Map<String, Object> map) {
        startPage();
        List<WebLoginLog> list = loginInforService.selectLoginInforList(new Query(map));
        return getDataTable(list);
    }

    @Operation(summary = "删除会员登录记录",
            description = "按路径中的 ID 删除会员登录记录；数组形式的 ID 以英文逗号分隔，具体业务约束由服务端校验。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('web:logininfor:remove')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @PreAuthorize("@ss.hasPermi('web:logininfor:remove')")
    @Log(title = "登录日志", businessType = BusinessType.DELETE)
    @DeleteMapping("/{infoIds}")
    public AjaxResult remove(@Parameter(description = "登录日志 ID 列表，多个值以英文逗号分隔") @PathVariable Long[] infoIds) {
        return toAjax(loginInforService.deleteLoginInforByIds(infoIds));
    }

    @Operation(summary = "清空会员登录记录",
            description = "删除全部会员登录记录记录，执行后无法通过本接口恢复，请谨慎调用。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('web:logininfor:remove')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @PreAuthorize("@ss.hasPermi('web:logininfor:remove')")
    @Log(title = "登录日志", businessType = BusinessType.CLEAN)
    @DeleteMapping("/clean")
    public AjaxResult clean() {
        loginInforService.cleanLoginInfor();
        return success();
    }
}
