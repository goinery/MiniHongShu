package com.hongshu.common.validator.constraint;

import com.hongshu.common.annotation.Numeric;
import com.hongshu.common.utils.StringUtils;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * 判断是否为数字【校验器】
 *
 * @author hongshu
 * 创建时间：2019年12月4日13:16:36
 */
public class NumericValidator implements ConstraintValidator<Numeric, String> {


    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || StringUtils.isBlank(value)) {
            return false;
        }
        return StringUtils.isNumeric(value);
    }
}
