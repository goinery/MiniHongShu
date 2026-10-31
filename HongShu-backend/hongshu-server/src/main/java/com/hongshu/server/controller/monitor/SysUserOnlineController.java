package com.hongshu.server.controller.monitor;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import com.hongshu.common.annotation.Log;
import com.hongshu.common.constant.CacheConstants;
import com.hongshu.common.core.controller.BaseController;
import com.hongshu.common.core.domain.AjaxResult;
import com.hongshu.common.core.domain.model.LoginUser;
import com.hongshu.common.core.page.TableDataInfo;
import com.hongshu.common.core.redis.RedisCache;
import com.hongshu.common.enums.BusinessType;
import com.hongshu.common.utils.StringUtils;
import com.hongshu.system.domain.SysUserOnline;
import com.hongshu.system.service.ISysUserOnlineService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 在线用户监控
 *
 * @author hongshu
 */
@Tag(name = "监控·在线管理员", description = "查询管理端在线会话并强制退出指定会话")
@RestController
@RequestMapping("/monitor/online")
public class SysUserOnlineController extends BaseController {

    private final ISysUserOnlineService userOnlineService;
    private final RedisCache redisCache;

    public SysUserOnlineController(ISysUserOnlineService userOnlineService, RedisCache redisCache) {
        this.userOnlineService = userOnlineService;
        this.redisCache = redisCache;
    }

    @Operation(summary = "查询在线管理员",
            description = "从 Redis 登录会话中按用户名和 IP 筛选在线管理员，返回会话标识与登录信息。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('monitor:online:list')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @PreAuthorize("@ss.hasPermi('monitor:online:list')")
    @GetMapping("/list")
    public TableDataInfo list(
            @Parameter(description = "登录 IP 地址筛选条件") String ipaddr,
            @Parameter(description = "管理账号用户名") String userName
    ) {
        Collection<String> keys = redisCache.keys(CacheConstants.LOGIN_TOKEN_KEY + "*");
        List<SysUserOnline> userOnlineList = new ArrayList<>();
        for (String key : keys) {
            LoginUser user = redisCache.getCacheObject(key);
            if (StringUtils.isNotEmpty(ipaddr) && StringUtils.isNotEmpty(userName)) {
                userOnlineList.add(userOnlineService.selectOnlineByInfo(ipaddr, userName, user));
            } else if (StringUtils.isNotEmpty(ipaddr)) {
                userOnlineList.add(userOnlineService.selectOnlineByIpaddr(ipaddr, user));
            } else if (StringUtils.isNotEmpty(userName) && StringUtils.isNotNull(user.getUser())) {
                userOnlineList.add(userOnlineService.selectOnlineByUserName(userName, user));
            } else {
                userOnlineList.add(userOnlineService.loginUserToUserOnline(user));
            }
        }
        Collections.reverse(userOnlineList);
        userOnlineList.removeAll(Collections.singleton(null));
        return getDataTable(userOnlineList);
    }

    /**
     * 强退用户
     */
    @Operation(summary = "强制退出管理会话",
            description = "删除 tokenId 对应的 Redis 登录记录，使该管理端会话失效。 需携带管理端 Bearer JWT。 权限校验：@ss.hasPermi('monitor:online:forceLogout')。",
            security = @SecurityRequirement(name = "adminBearerAuth"))
    @PreAuthorize("@ss.hasPermi('monitor:online:forceLogout')")
    @Log(title = "在线用户", businessType = BusinessType.FORCE)
    @DeleteMapping("/{tokenId}")
    public AjaxResult forceLogout(@Parameter(description = "管理端在线会话标识") @PathVariable String tokenId) {
        redisCache.deleteObject(CacheConstants.LOGIN_TOKEN_KEY + tokenId);
        return success();
    }
}
