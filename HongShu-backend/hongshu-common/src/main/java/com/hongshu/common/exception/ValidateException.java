package com.hongshu.common.exception;

import java.io.Serial;

import com.hongshu.common.enums.ResponseEnum;
import lombok.Getter;
import lombok.Setter;

/**
 * 参数验证失败异常
 *
 * @author hongshu
 */
@Getter
@Setter
public class ValidateException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer code;
    private String msg;

    public ValidateException() {
        this(ResponseEnum.BAD_REQUEST.getMsg());
    }

    public ValidateException(String msg) {
        super(msg);
        this.code = ResponseEnum.BAD_REQUEST.getCode();
        this.msg = msg;
    }

}
