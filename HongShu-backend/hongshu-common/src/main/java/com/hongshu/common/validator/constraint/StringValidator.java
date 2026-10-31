package com.hongshu.common.validator.constraint;

import com.hongshu.common.annotation.NotBlank;
import com.hongshu.common.utils.StringUtils;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * 判断是否为空字符串【校验器】
 *
 * @author hongshu
 */
public class StringValidator implements ConstraintValidator<NotBlank, String> {


    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return !(value == null || StringUtils.isBlank(value) || StringUtils.isEmpty(value.trim()));
    }
}
