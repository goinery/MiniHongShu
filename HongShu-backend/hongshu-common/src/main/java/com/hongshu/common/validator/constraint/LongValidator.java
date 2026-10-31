package com.hongshu.common.validator.constraint;

import com.hongshu.common.annotation.LongNotNull;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * 判断Long是否为空【校验器】
 *
 * @author hongshu
 */
public class LongValidator implements ConstraintValidator<LongNotNull, Long> {


    @Override
    public boolean isValid(Long value, ConstraintValidatorContext context) {
        return !(value == null);
    }
}
