package com.hongshu.web.auth;

/**
 * @author hongshu
 */
public final class AuthContextHolder {

    private AuthContextHolder() {

    }

    private static final ThreadLocal<String> USER_ID = new ThreadLocal<>();

    public static void setUserId(String userId) {
        USER_ID.set(userId);
    }

    public static String getUserId() {
        return USER_ID.get();
    }

    public static void removeUserId() {
        USER_ID.remove();
    }
}
