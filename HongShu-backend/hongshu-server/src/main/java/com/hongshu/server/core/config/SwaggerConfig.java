package com.hongshu.server.core.config;

import com.hongshu.common.config.HongshuConfig;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * 社区与管理端的 OpenAPI 文档配置，不改变业务接口的 MVC 路径。
 * 文档端点开关由 springdoc 配置引用原 swagger.enabled 属性。
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "swagger.enabled", havingValue = "true", matchIfMissing = true)
public class SwaggerConfig {

    @Bean
    public OpenAPI hongshuOpenApi(HongshuConfig config,
                                  @Value("${swagger.pathMapping:/}") String pathMapping) {
        Components components = new Components()
                .addSecuritySchemes("adminBearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("管理端登录接口返回的 JWT，使用 Authorization: Bearer 发送"))
                .addSecuritySchemes("appAccessToken", new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name("accessToken")
                        .description("用户端登录接口返回的 accessToken；用户身份由服务端验证，无需另外填写 userId"));

        OpenAPI api = new OpenAPI()
                .info(new Info()
                        .title(config.getName() + " 接口文档")
                        .description("图文社区用户端、运营后台与系统管理接口。按中文业务模块分组，每项操作提供用途、参数与认证说明。"
                                + "管理端先调用 /login，再在授权窗口填写 Bearer JWT；前台先调用 /web/auth/login，"
                                + "再填写 appAccessToken。公开接口无需授权。HTTP 成功不等于业务成功，请同时检查响应体中的 code 和 message/msg。"
                                + "标为兼容入口的操作以说明中的实际行为为准，不代表已接入搜索引擎或验证码发送服务。")
                        .version(config.getVersion())
                        .contact(new Contact().name(config.getName())))
                .components(components);
        // 原 pathMapping 表示代理请求前缀，使用相对服务器地址保留该意义。
        if (StringUtils.hasText(pathMapping)) {
            api.addServersItem(new Server().url(pathMapping.trim()));
        }
        return api;
    }

    /** 统一补齐框架默认生成的成功响应文案，不改变业务状态码或返回内容。 */
    @Bean
    public OperationCustomizer chineseResponseDescriptions() {
        return (operation, handlerMethod) -> {
            if (operation.getResponses() != null) {
                operation.getResponses().forEach((status, response) -> {
                    if ("200".equals(status) && (!StringUtils.hasText(response.getDescription())
                            || "OK".equals(response.getDescription()))) {
                        response.setDescription(handlerMethod.getReturnType().getParameterType() == void.class
                                ? "请求处理完成；下载或导出接口成功时返回文件，业务失败时以实际响应为准"
                                : "请求处理完成；请检查响应体中的业务状态码 code 和结果说明 message/msg");
                    }
                });
            }
            return operation;
        };
    }
}
