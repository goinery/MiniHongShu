package com.hongshu.common.exception.base;

import java.io.Serial;

import lombok.Getter;

import com.hongshu.common.utils.MessageUtils;
import com.hongshu.common.utils.StringUtils;

/**
 * 基础异常
 *
 * @author hongshu
 */
public class BaseException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 所属模块
     */
    @Getter
    private final String module;

    /**
     * 错误码
     */
    @Getter
    private final String code;

    /**
     * 错误码对应的参数
     */
    @Getter
    private final Object[] args;

    /**
     * 错误消息
     */
    @Getter
    private final String defaultMessage;

    public BaseException(String module, String code, Object[] args, String defaultMessage) {
        this.module = module;
        this.code = code;
        this.args = args;
        this.defaultMessage = defaultMessage;
    }

    public BaseException(String module, String code, Object[] args) {
        this(module, code, args, null);
    }

    public BaseException(String module, String defaultMessage) {
        this(module, null, null, defaultMessage);
    }

    public BaseException(String code, Object[] args) {
        this(null, code, args, null);
    }

    public BaseException(String defaultMessage) {
        this(null, null, null, defaultMessage);
    }

    @Override
    public String getMessage() {
        String message = null;
        if (!StringUtils.isEmpty(code)) {
            message = MessageUtils.message(code, args);
        }
        if (message == null) {
            message = defaultMessage;
        }
        return message;
    }

}
