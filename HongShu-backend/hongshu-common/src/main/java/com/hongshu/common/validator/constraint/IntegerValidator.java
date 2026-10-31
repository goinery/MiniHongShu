package com.hongshu.common.validator.constraint;

import com.hongshu.common.annotation.IntegerNotNull;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * 判断Integer是否为空【校验器】
 *
 * @author hongshu
 */
public class IntegerValidator implements ConstraintValidator<IntegerNotNull, Integer> {


    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        return !(value == null);
    }
}
