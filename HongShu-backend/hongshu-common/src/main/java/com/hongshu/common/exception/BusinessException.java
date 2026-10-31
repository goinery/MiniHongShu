package com.hongshu.common.exception;

import java.io.Serial;

import com.hongshu.common.enums.ResponseEnum;
import lombok.Getter;
import lombok.Setter;

/**
 * 业务异常
 *
 * @author hongshu
 */
@Getter
@Setter
public class BusinessException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer code;
    private String msg;

    public BusinessException() {
        this(ResponseEnum.BUSINESS_ERROR.getMsg());
    }

    public BusinessException(String msg) {
        super(msg);
        this.code = ResponseEnum.BUSINESS_ERROR.getCode();
        this.msg = msg;
    }

}
