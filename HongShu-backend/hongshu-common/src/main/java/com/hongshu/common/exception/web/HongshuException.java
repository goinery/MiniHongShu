package com.hongshu.common.exception.web;

import java.io.Serial;

import com.hongshu.common.enums.ResultCodeEnum;
import com.hongshu.common.utils.MessageUtil;
import lombok.Getter;
import lombok.Setter;

/**
 * 异常
 *
 * @author hongshu
 */
@Getter
@Setter
public class HongshuException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    // 异常状态码
    private Integer code;

    /**
     * 通过状态码和错误消息创建异常对象
     *
     */
    public HongshuException(String message, Integer code) {
        super(message);
        this.code = code;
    }

    public HongshuException(int code, String... params) {
        super(MessageUtil.getMessage(code, params));
        this.code = code;
    }

    public HongshuException(String msg) {
        super(msg);
        this.code = ResultCodeEnum.FAIL.getCode();
    }

    /**
     * 保留底层异常，响应仍使用面向用户的错误消息。
     */
    public HongshuException(String message, Throwable cause) {
        super(message, cause);
        this.code = ResultCodeEnum.FAIL.getCode();
    }

    /**
     * 接收枚举类型对象
     *
     */
    public HongshuException(ResultCodeEnum resultCodeEnum) {
        super(resultCodeEnum.getMessage());
        this.code = resultCodeEnum.getCode();
    }
}
