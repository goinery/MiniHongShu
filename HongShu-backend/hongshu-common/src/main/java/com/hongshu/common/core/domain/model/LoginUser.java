package com.hongshu.common.core.domain.model;

import java.io.Serial;

import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullMarked;

import com.alibaba.fastjson2.annotation.JSONField;
import com.hongshu.common.core.domain.entity.SysUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * 登录用户身份权限
 *
 * @author hongshu
 */
public class LoginUser implements UserDetails {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @Getter
    @Setter
    private Long userId;

    /**
     * 部门ID
     */
    @Getter
    @Setter
    private Long deptId;

    /**
     * 用户唯一标识
     */
    @Getter
    @Setter
    private String token;

    /**
     * 登录时间
     */
    @Getter
    @Setter
    private Long loginTime;

    /**
     * 过期时间
     */
    @Getter
    @Setter
    private Long expireTime;

    /**
     * 登录IP地址
     */
    @Getter
    @Setter
    private String ipaddr;

    /**
     * 登录地点
     */
    @Getter
    @Setter
    private String loginLocation;

    /**
     * 浏览器类型
     */
    @Getter
    @Setter
    private String browser;

    /**
     * 操作系统
     */
    @Getter
    @Setter
    private String os;

    /**
     * 权限列表
     */
    @Getter
    @Setter
    private Set<String> permissions;

    /**
     * 用户信息
     */
    @Getter
    @Setter
    private SysUser user;

    public LoginUser() {
    }

    public LoginUser(SysUser user, Set<String> permissions) {
        this.user = user;
        this.permissions = permissions;
    }

    public LoginUser(Long userId, Long deptId, SysUser user, Set<String> permissions) {
        this.userId = userId;
        this.deptId = deptId;
        this.user = user;
        this.permissions = permissions;
    }

    @JSONField(serialize = false)
    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    @NullMarked
    public String getUsername() {
        return user.getUserName();
    }

    /**
     * 账户是否未过期,过期无法验证
     */
    @JSONField(serialize = false)
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * 指定用户是否解锁,锁定的用户无法进行身份验证
     *
     */
    @JSONField(serialize = false)
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * 指示是否已过期的用户的凭据(密码),过期的凭据防止认证
     *
     */
    @JSONField(serialize = false)
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * 是否可用 ,禁用的用户不能身份验证
     *
     */
    @JSONField(serialize = false)
    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    @NullMarked
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 业务权限仍由 PermissionService 读取 permissions；UserDetails 要求非空集合。
        return Collections.emptyList();
    }
}
