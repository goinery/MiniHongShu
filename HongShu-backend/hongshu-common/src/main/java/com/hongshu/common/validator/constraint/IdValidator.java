package com.hongshu.common.validator.constraint;

import com.hongshu.common.annotation.IdValid;
import com.hongshu.common.constant.Constantss;
import com.hongshu.common.utils.StringUtils;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * ID校验器，主要判断是否为空，并且长度是否为32
 *
 * @author hongshu
 */
public class IdValidator implements ConstraintValidator<IdValid, String> {



    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return !(value == null || StringUtils.isBlank(value) || StringUtils.isEmpty(value.trim()) || value.length() != Constantss.THIRTY_TWO);
    }
}
