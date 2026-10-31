package com.hongshu.framework.config;

import com.hongshu.framework.config.properties.PermitAllUrlProperties;
import com.hongshu.framework.security.filter.JwtAuthenticationTokenFilter;
import com.hongshu.framework.security.handle.AuthenticationEntryPointImpl;
import com.hongshu.framework.security.handle.LogoutSuccessHandlerImpl;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.CorsFilter;

/**
 * 后台 JWT 认证和请求授权配置，保留升级前的公开接口及静态资源范围。
 */
@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfig {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    /**
     * 登录服务继续使用原 UserDetailsService 和 BCrypt 验证已有账号。
     */
    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
                                                       BCryptPasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   PermitAllUrlProperties permitAllUrl,
                                                   AuthenticationEntryPointImpl unauthorizedHandler,
                                                   LogoutSuccessHandlerImpl logoutSuccessHandler,
                                                   JwtAuthenticationTokenFilter authenticationTokenFilter,
                                                   CorsFilter corsFilter) {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers
                        .cacheControl(org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.CacheControlConfig::disable)
                        .frameOptions(org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig::disable))
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(unauthorizedHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> {
                    // 错误转发只负责呈现原请求的错误，避免被二次鉴权改写为未登录。
                    authorize.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll();
                    permitAllUrl.getUrls().forEach(url ->
                            authorize.requestMatchers(legacyRequestMatcher(url, null)).permitAll());
                    // C 端接口继续由原用户拦截器校验 accessToken；后台使用 Security JWT。
                    for (String url : new String[]{"/web/**", "/login", "/register", "/captchaImage"}) {
                        authorize.requestMatchers(legacyRequestMatcher(url, null)).permitAll();
                    }
                    for (String url : new String[]{"/", "/*.html", "/**/*.html", "/**/*.css", "/**/*.js", "/profile/**"}) {
                        authorize.requestMatchers(legacyRequestMatcher(url, HttpMethod.GET)).permitAll();
                    }
                    // 保留原文档入口，并允许迁移后的 springdoc 文档和页面资源。
                    for (String url : new String[]{"/swagger-ui.html", "/swagger-ui/**", "/swagger-resources/**",
                            "/webjars/**", "/*/api-docs", "/v3/api-docs/**", "/druid/**"}) {
                        authorize.requestMatchers(legacyRequestMatcher(url, null)).permitAll();
                    }
                    authorize.anyRequest().authenticated();
                })
                .logout(logout -> logout
                        .logoutRequestMatcher(legacyRequestMatcher("/logout", null))
                        .logoutSuccessHandler(logoutSuccessHandler));

        http.addFilterBefore(authenticationTokenFilter, UsernamePasswordAuthenticationFilter.class);
        // CORS 位于退出和 JWT 校验之前，避免预检请求因缺少令牌而失败。
        http.addFilterBefore(corsFilter, LogoutFilter.class);
        return http.build();
    }

    /**
     * 保留旧 antMatchers 的路径与请求方法语义。
     * Security 7 默认的 PathPattern 不接受旧静态资源规则中的中段 **。
     */
    static RequestMatcher legacyRequestMatcher(String pattern, HttpMethod method) {
        return request -> {
            if (method != null && !method.name().equals(request.getMethod())) {
                return false;
            }
            String path = request.getServletPath();
            if (request.getPathInfo() != null) {
                path += request.getPathInfo();
            }
            return PATH_MATCHER.match(pattern, path);
        };
    }

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
