package com.hongshu.framework.config;

import com.hongshu.common.core.domain.entity.SysUser;
import com.hongshu.common.core.domain.model.LoginUser;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证迁移后的旧白名单边界和已有 BCrypt 账号认证。
 */
class SecurityConfigTest {

    @Test
    void nestedStaticFilesRemainPublicOnlyForGet() {
        assertTrue(SecurityConfig.legacyRequestMatcher("/**/*.js", HttpMethod.GET)
                .matches(request("GET", "/assets/app/main.js")));
        assertFalse(SecurityConfig.legacyRequestMatcher("/**/*.js", HttpMethod.GET)
                .matches(request("POST", "/assets/app/main.js")));
        assertFalse(SecurityConfig.legacyRequestMatcher("/**/*.js", HttpMethod.GET)
                .matches(request("GET", "/system/user/list")));
    }

    @Test
    void publicWebNamespaceDoesNotPermitSimilarBackendPaths() {
        assertTrue(SecurityConfig.legacyRequestMatcher("/web/**", null)
                .matches(request("GET", "/web/ws/42")));
        assertFalse(SecurityConfig.legacyRequestMatcher("/web/**", null)
                .matches(request("GET", "/web-admin/user")));
        assertFalse(SecurityConfig.legacyRequestMatcher("/web/**", null)
                .matches(request("GET", "/system/user")));
    }

    @Test
    void matchingUsesServletPathAndPathInfoWithoutContextPath() {
        MockHttpServletRequest request = request("GET", "/web");
        request.setContextPath("/hongshu");
        request.setPathInfo("/note/getNoteById");
        assertTrue(SecurityConfig.legacyRequestMatcher("/web/**", null).matches(request));
    }

    @Test
    void loginManagerAuthenticatesRealLoginUserAndRetainsBusinessPermissions() {
        SecurityConfig config = new SecurityConfig();
        BCryptPasswordEncoder encoder = config.bCryptPasswordEncoder();
        String encodedPassword = encoder.encode("regression-only-password");
        SysUser user = new SysUser();
        user.setUserName("test-admin");
        user.setPassword(encodedPassword);
        Set<String> permissions = Set.of("system:user:list");
        LoginUser loginUser = new LoginUser(42L, 1L, user, permissions);
        AuthenticationManager manager = config.authenticationManager(username -> loginUser, encoder);

        Authentication result = manager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("test-admin", "regression-only-password"));
        assertTrue(result.isAuthenticated());
        assertEquals("test-admin", result.getName());
        assertSame(loginUser, result.getPrincipal());
        assertTrue(loginUser.getAuthorities().isEmpty());
        // Security 7 为密码认证记录因子标记；业务权限继续由原 permissions 集合校验。
        assertEquals(Set.of("FACTOR_PASSWORD"), result.getAuthorities().stream()
                .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                .collect(java.util.stream.Collectors.toSet()));
        LoginUser principal = assertInstanceOf(LoginUser.class, result.getPrincipal());
        assertEquals(permissions, principal.getPermissions());
        assertThrows(BadCredentialsException.class, () -> manager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("test-admin", "incorrect-password")));
    }

    private MockHttpServletRequest request(String method, String servletPath) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod(method);
        request.setServletPath(servletPath);
        return request;
    }
}
