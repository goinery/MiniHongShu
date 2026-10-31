package com.hongshu.common.exception.exceptionType;

import lombok.Getter;
import lombok.Setter;

/**
 * 自定义业务异常
 *
 * @author hongshu
 */
public class BusinessException extends RuntimeException {

    /**
     * 异常编码
     */
    @Getter
    @Setter
    private String code;

    public BusinessException() {
    }

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }

    public BusinessException(String message, String code) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message, String code, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

}
