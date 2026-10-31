package com.hongshu.common.validator.constraint;

import com.hongshu.common.annotation.BooleanNotNULL;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * 判断Boolean类型是否为空【校验器】
 *
 * @author hongshu
 */
public class BooleanValidator implements ConstraintValidator<BooleanNotNULL, Boolean> {


    @Override
    public boolean isValid(Boolean value, ConstraintValidatorContext context) {
        return !(value == null);
    }
}
