package com.hongshu.common.enums;

import lombok.Getter;

/**
 * 用户状态
 *
 * @author hongshu
 */
public enum UserStatus {

    OK("0", "正常"),
    DISABLE("1", "停用"),
    DELETED("2", "删除");

    @Getter
    private final String code;
    @Getter
    private final String info;

    UserStatus(String code, String info) {
        this.code = code;
        this.info = info;
    }

}
