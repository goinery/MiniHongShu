package com.hongshu.web.auth;

/**
 * 前台用户会话已被替换或撤销，订阅方负责清理各自持有的连接资源。
 *
 * @param userId 用户 ID
 */
public record WebSessionInvalidatedEvent(String userId) {
}
