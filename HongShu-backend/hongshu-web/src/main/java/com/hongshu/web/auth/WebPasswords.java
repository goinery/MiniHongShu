package com.hongshu.web.auth;

import cn.hutool.crypto.SecureUtil;
import com.hongshu.common.exception.web.HongshuException;
import com.hongshu.common.utils.SecurityUtils;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class WebPasswords {
    private WebPasswords() {
    }

    public static String encode(String password) {
        if (password == null || password.length() < 6 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new HongshuException("密码至少6位，且不能超过72字节");
        }
        return SecurityUtils.encryptPassword(password);
    }

    public static boolean legacy(String encoded) {
        return encoded != null && encoded.matches("(?i)[0-9a-f]{32}");
    }

    public static boolean matches(String password, String encoded) {
        if (password == null || encoded == null || password.getBytes(StandardCharsets.UTF_8).length > 72) return false;
        if (legacy(encoded)) {
            return MessageDigest.isEqual(SecureUtil.md5(password).getBytes(StandardCharsets.US_ASCII),
                    encoded.toLowerCase(java.util.Locale.ROOT).getBytes(StandardCharsets.US_ASCII));
        }
        try { return SecurityUtils.matchesPassword(password, encoded); }
        catch (IllegalArgumentException e) { return false; }
    }
}
