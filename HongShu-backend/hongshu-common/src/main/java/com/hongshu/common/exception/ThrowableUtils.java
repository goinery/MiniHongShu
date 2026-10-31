package com.hongshu.common.exception;

import cn.hutool.core.collection.CollectionUtil;
import com.hongshu.common.constant.Constantss;
import com.hongshu.common.exception.exceptionType.ApiInvalidParamException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.util.List;
import java.util.Objects;

/**
 * 抛出异常工具类
 *
 * @author hongshu
 */
public class ThrowableUtils {

    /**
     * 校验参数正确,拼装字段名和值到错误信息
     *
     */
    public static void checkParamArgument(BindingResult result) {
        if (result != null && result.hasErrors()) {
            StringBuilder sb = new StringBuilder();
            List<FieldError> errors = result.getFieldErrors();
            if (CollectionUtil.isNotEmpty(errors)) {
                FieldError error = errors.getFirst();
                String rejectedValue = Objects.toString(error.getRejectedValue(), "");
                String defMsg = Objects.requireNonNullElse(error.getDefaultMessage(), "参数校验失败");
                // 排除类上面的注解提示
                if (rejectedValue.contains(Constantss.DELIMITER_TO)) {
                    // 自己去确定错误字段
                    sb.append(defMsg);
                } else {
                    sb.append(error.getField()).append(" ").append(defMsg);
                }
            } else {
                String msg = result.getAllErrors().getFirst().getDefaultMessage();
                sb.append(msg);
            }
            throw new ApiInvalidParamException(sb.toString());
        }

    }
}
