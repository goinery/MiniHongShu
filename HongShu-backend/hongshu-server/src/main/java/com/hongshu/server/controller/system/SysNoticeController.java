package com.hongshu.server.controller.system;

import org.springdoc.core.annotations.ParameterObject;
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
import com.hongshu.common.core.page.TableDataInfo;
import com.hongshu.common.enums.BusinessType;
import com.hongshu.system.domain.SysNotice;
import com.hongshu.system.service.ISysNoticeService;
import com.hongshu.web.service.FileAssetService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.ArrayList;

/**
 * 公告 信息操作处理
 *
 * @author hongshu
 */
@Tag(name = "系统·通知公告", description = "后台通知公告的查询与发布维护")
@RestController
@RequestMapping("/system/notice")
public class SysNoticeController extends BaseController {

    private final ISysNoticeService noticeService;
    private final FileAssetService assets;
    private final JdbcTemplate jdbc;

    public SysNoticeController(ISysNoticeService noticeService, FileAssetService assets, JdbcTemplate jdbc) {
        this.noticeService = noticeService;
        this.assets = assets;
        this.jdbc = jdbc;
    }

    /**
     * 获取通知公告列表
     */
    @Parameters({
            @Parameter(name = "pageNum", in = ParameterIn.QUERY, description = "当前页码，从 1 开始，默认 1", schema = @Schema(type = "integer", defaultValue = "1")),
            @Parameter(name = "pageSize", in = ParameterIn.QUERY, description = "每页条数，默认 10", schema = @Schema(type = "integer", defaultValue = "10"))
    })
    @Operation(summary = "分页查询通知公告",
            description = "按请求中的筛选条件查询通知公告，分页接口使用 pageNum 和 pageSize。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('system:notice:list')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @PreAuthorize("@ss.hasPermi('system:notice:list')")
    @GetMapping("/list")
    public TableDataInfo list(@ParameterObject @Parameter(description = "通知公告筛选条件，具体字段见关联模型；分页使用 pageNum 和 pageSize") SysNotice notice) {
        startPage();
        List<SysNotice> list = noticeService.selectNoticeList(notice);
        return getDataTable(list);
    }

    /**
     * 根据通知公告编号获取详细信息
     */
    @Operation(summary = "获取通知公告详情",
            description = "根据路径中的主键 ID 查询指定通知公告的详细信息。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('system:notice:query')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @PreAuthorize("@ss.hasPermi('system:notice:query')")
    @GetMapping(value = "/{noticeId}")
    public AjaxResult getInfo(@Parameter(description = "公告 ID") @PathVariable Long noticeId) {
        return success(noticeService.selectNoticeById(noticeId));
    }

    /**
     * 新增通知公告
     */
    @Operation(summary = "新增通知公告",
            description = "提交通知公告数据，校验业务约束后保存。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('system:notice:add')。",
            security = @SecurityRequirement(name = "adminBearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "请求数据，字段含义见下方模型；具体取值与操作约束见接口说明"))
    @PreAuthorize("@ss.hasPermi('system:notice:add')")
    @Log(title = "通知公告", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysNotice notice) {
        notice.setCreateBy(getUsername());
        return toAjax(noticeService.insertNotice(notice));
    }

    /**
     * 修改通知公告
     */
    @Operation(summary = "修改通知公告",
            description = "提交包含主键 ID 的通知公告数据，校验操作权限和业务约束后更新。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('system:notice:edit')。",
            security = @SecurityRequirement(name = "adminBearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "请求数据，字段含义见下方模型；具体取值与操作约束见接口说明"))
    @PreAuthorize("@ss.hasPermi('system:notice:edit')")
    @Log(title = "通知公告", businessType = BusinessType.UPDATE)
    @PutMapping
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult edit(@Validated @RequestBody SysNotice notice) {
        List<String> previous = jdbc.queryForList("SELECT CAST(notice_content AS CHAR) FROM sys_notice WHERE notice_id = ? FOR UPDATE", String.class, notice.getNoticeId());
        notice.setUpdateBy(getUsername());
        int updated = noticeService.updateNotice(notice);
        if (updated > 0) assets.cleanStoredContentAfterCommit(previous.toArray(String[]::new));
        return toAjax(updated);
    }

    /**
     * 删除通知公告
     */
    @Operation(summary = "删除通知公告",
            description = "按路径中的 ID 删除通知公告；数组形式的 ID 以英文逗号分隔，具体业务约束由服务端校验。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('system:notice:remove')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @PreAuthorize("@ss.hasPermi('system:notice:remove')")
    @Log(title = "通知公告", businessType = BusinessType.DELETE)
    @DeleteMapping("/{noticeIds}")
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult remove(@Parameter(description = "公告 ID 列表，多个值以英文逗号分隔") @PathVariable Long[] noticeIds) {
        List<String> previous = new ArrayList<>();
        for (Long id : noticeIds) {
            previous.addAll(jdbc.queryForList("SELECT CAST(notice_content AS CHAR) FROM sys_notice WHERE notice_id = ? FOR UPDATE", String.class, id));
        }
        int deleted = noticeService.deleteNoticeByIds(noticeIds);
        if (deleted > 0) assets.cleanStoredContentAfterCommit(previous.toArray(String[]::new));
        return toAjax(deleted);
    }
}
