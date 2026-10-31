package com.hongshu.framework.config;

import org.jspecify.annotations.NonNull;

import com.hongshu.common.constant.UserConstants;
import com.hongshu.common.core.domain.entity.SysUser;
import com.hongshu.common.utils.SecurityUtils;
import com.hongshu.system.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 首次启动时设置超级管理员账密，初始化后不再覆盖数据库中的账号。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AdminAccountInitializer implements ApplicationRunner {

    private static final String PENDING_PASSWORD = "{ADMIN_INIT_REQUIRED}";

    // 仅用于识别旧版 SQL 中尚未更改的演示管理员，不能用作新账号的密码。
    private static final String LEGACY_SEED_PASSWORD =
            "$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2";

    private final SysUserMapper userMapper;
    private final String username;
    private final String password;

    public AdminAccountInitializer(SysUserMapper userMapper,
                                   @Value("${hongshu.admin.username:}") String username,
                                   @Value("${hongshu.admin.password:}") String password) {
        this.userMapper = userMapper;
        this.username = username;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(@NonNull ApplicationArguments args) {
        // 多个后端同时启动时，后到的实例应读取已初始化的账号，避免重复覆盖。
        SysUser admin = userMapper.selectAdminForUpdate();
        if (admin == null) {
            throw new IllegalStateException("缺少超级管理员记录，请先导入数据库初始化 SQL");
        }
        boolean pending = PENDING_PASSWORD.equals(admin.getPassword());
        boolean legacySeed = "admin".equals(admin.getUserName())
                && LEGACY_SEED_PASSWORD.equals(admin.getPassword())
                && UserConstants.NORMAL.equals(admin.getStatus());
        if ((!pending && !legacySeed) || !"0".equals(admin.getDelFlag())) {
            return;
        }

        validateCredentials();
        SysUser existing = userMapper.checkUserNameUnique(username);
        if (existing != null && !admin.getUserId().equals(existing.getUserId())) {
            throw new IllegalStateException("HONGSHU_ADMIN_USERNAME 已被其他账号使用");
        }

        SysUser update = new SysUser(admin.getUserId());
        update.setUserName(username);
        update.setPassword(SecurityUtils.encryptPassword(password));
        update.setStatus(UserConstants.NORMAL);
        update.setUpdateBy("admin-bootstrap");
        if (userMapper.updateUser(update) != 1) {
            throw new IllegalStateException("超级管理员初始化失败");
        }
    }

    private void validateCredentials() {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new IllegalStateException(
                    "首次启动必须配置 HONGSHU_ADMIN_USERNAME 和 HONGSHU_ADMIN_PASSWORD");
        }
        if (!username.equals(username.trim())
                || username.length() < UserConstants.USERNAME_MIN_LENGTH
                || username.length() > UserConstants.USERNAME_MAX_LENGTH) {
            throw new IllegalStateException("HONGSHU_ADMIN_USERNAME 必须为 2-20 个字符，且不能包含首尾空格");
        }
        if (password.length() < UserConstants.PASSWORD_MIN_LENGTH
                || password.length() > UserConstants.PASSWORD_MAX_LENGTH) {
            throw new IllegalStateException("HONGSHU_ADMIN_PASSWORD 必须为 5-20 个字符");
        }
    }
}
