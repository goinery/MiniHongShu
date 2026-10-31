package com.hongshu.web.auth;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 作者：: hongshu
 */
@Configuration
public class LoginMvcConfigureAdapter implements WebMvcConfigurer {
    private final UserLoginInterceptor loginInterceptor;

    public LoginMvcConfigureAdapter(UserLoginInterceptor loginInterceptor) {
        this.loginInterceptor = loginInterceptor;
    }

    /**
     * 拦截器
     *
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/profile/**");
        // 使用 MVC 扩展点，保留 Boot 的 JSON 转换器和 springdoc 的静态资源配置。
        // 上传路径统一由 framework/ResourcesConfig 注册。
    }
}
