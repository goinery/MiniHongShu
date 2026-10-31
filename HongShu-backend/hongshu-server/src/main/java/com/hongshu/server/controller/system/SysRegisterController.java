package com.hongshu.server.controller.system;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import com.hongshu.common.core.controller.BaseController;
import com.hongshu.common.core.domain.AjaxResult;
import com.hongshu.common.core.domain.model.RegisterBody;
import com.hongshu.common.utils.StringUtils;
import com.hongshu.framework.web.service.SysRegisterService;
import com.hongshu.system.service.ISysConfigService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 注册验证
 *
 * @author hongshu
 */
@Tag(name = "系统·管理员注册", description = "由系统注册开关控制的后台账号注册入口")
@RestController
public class SysRegisterController extends BaseController {

    private final SysRegisterService registerService;
    private final ISysConfigService configService;

    public SysRegisterController(SysRegisterService registerService, ISysConfigService configService) {
        this.registerService = registerService;
        this.configService = configService;
    }

    @Operation(summary = "注册后台管理账号",
            description = "根据系统注册开关决定是否允许注册；提交账号密码及按配置启用的验证码，关闭注册时返回失败。 无需登录。",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "请求数据，字段含义见下方模型；具体取值与操作约束见接口说明"))
    @PostMapping("/register")
    public AjaxResult register(@RequestBody RegisterBody user) {
        if (!("true".equals(configService.selectConfigByKey("sys.account.registerUser")))) {
            return error("当前系统没有开启注册功能！");
        }
        String msg = registerService.register(user);
        return StringUtils.isEmpty(msg) ? success() : error(msg);
    }
}
