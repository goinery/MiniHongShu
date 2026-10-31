package com.hongshu.common.validator;

import com.hongshu.common.constant.StringPoolConstant;
import com.hongshu.common.exception.BusinessException;
import com.hongshu.common.exception.ValidateException;
import com.hongshu.common.utils.NumberUtil;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * hibernate-validator校验工具类
 * 参考文档：<a href="https://docs.jboss.org/hibernate/validator/5.4/reference/en-US/html_single/">Hibernate Validator 文档</a>
 *
 * @author hongshu
 */
public class ValidatorUtil {

    private static final jakarta.validation.ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator validator = VALIDATOR_FACTORY.getValidator();
    private static Pattern pattern;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(VALIDATOR_FACTORY::close, "validator-factory-shutdown"));
    }

    /**
     * 校验对象
     *
     * @param object 待校验对象
     * @param groups 待校验的组
     * @throws ValidateException 校验不通过时抛出
     */
    public static void validateEntity(Object object, Class<?>... groups)
            throws BusinessException {
        Set<ConstraintViolation<Object>> constraintViolations = validator.validate(object, groups);
        if (!constraintViolations.isEmpty()) {
            StringBuilder msg = new StringBuilder();
            for (ConstraintViolation<Object> constraint : constraintViolations) {
                msg.append(constraint.getMessage()).append("<br>");
            }
            throw new ValidateException(msg.toString());
        }
    }

    /**
     * 判断内容是空
     *
     */
    public static boolean isNull(Object str) {
        return !isNotNull(str);
    }

    /**
     * 判断内容不为空
     *
     */
    public static boolean isNotNull(Object str) {
        return str != null && !str.toString().trim().isEmpty() && !StringPoolConstant.NULL.equals(str + StringPoolConstant.EMPTY)
                && !StringPoolConstant.UNDEFINED.equals(str + StringPoolConstant.EMPTY);
    }

    /**
     * 判断内容是空(包含0)
     *
     */
    public static boolean isNullOrZero(Object str) {
        return !isNotNullAndZero(str);
    }

    /**
     * 判断内容不为空(包含0)
     *
     */
    public static boolean isNotNullAndZero(Object str) {
        return str != null && !str.toString().trim().isEmpty() && !StringPoolConstant.NULL.equals(str + StringPoolConstant.EMPTY)
                && !StringPoolConstant.UNDEFINED.equals(str + StringPoolConstant.EMPTY) && !Double.valueOf(0).equals(Double.valueOf(str.toString()));
    }

    /**
     * 检查对象是否为空（包含数组）
     *
     */
    public static boolean isNotNullIncludeArray(Object obj) {
        return !isNullIncludeArray(obj);
    }

    /**
     * 检查对象是否为空（包含数组）
     *
     * @param obj 要检查的数据(数据类型: String、Number、Boolean、Collection、Map、Object[])
     * @return true: 为空; false: 不为空 <li>String：值为 null、""、"0" 时返回 true <li>
     * Number：值为 null、0 时返回 true <li>Boolean：值为 null、false 时返回 true <li>
     * Collection：值为 null、size=0 时返回 true <li>Map：值为 null、size=0 时返回
     * true <li>Object[]：值为 null、length=0 时返回 true
     */
    public static boolean isNullIncludeArray(Object obj) {
        return switch (obj) {
            case null -> true;
            case String value -> value.isEmpty();
            case Number value -> value.doubleValue() == 0;
            case Boolean value -> !value;
            case Collection<?> value -> value.isEmpty();
            case Map<?, ?> value -> value.isEmpty();
            case Object[] value -> value.length == 0;
            default -> false;
        };
    }

    /**
     * 判断是否是合法邮箱地址
     *
     */
    public static boolean isEmail(String email) {
        String regex = "^\\w+([\\-+.]\\w+)*@\\w+([-.]\\w+)*\\.[a-z]{2,3}";
        pattern = Pattern.compile(regex);
        Matcher m = pattern.matcher(email);
        return m.matches();
    }

    /**
     * 只包含英文字母和数字、下划线
     *
     */
    public static boolean onlyNumAndChar(String str) {
        String regex = "^[a-zA-Z0-9_]+$";
        pattern = Pattern.compile(regex);
        return pattern.matcher(str).matches();
    }

    /**
     * 必须包含字母
     *
     */
    public static boolean hasLetterAndNum(String str) {
        String regex = "^(?=.*[a-zA-Z].*).{6,}$";
        pattern = Pattern.compile(regex);
        return pattern.matcher(str).matches();
    }

    /**
     * 是否长度符合
     *
     * @param min 最小
     * @param max 最大
     */
    public static boolean lengthBetween(String str, int min, int max) {
        return str.length() >= min && str.length() <= max;
    }

    /**
     * 判断字符串是否是数字
     *
     */
    public static boolean isNumeric(String str) {
        String regex = "[0-9]*";
        pattern = Pattern.compile(regex);
        Matcher isNum = pattern.matcher(str);
        return isNum.matches();
    }

    /**
     * 判断字符串是否是小数
     *
     */
    public static boolean isDouble(String str) {
        String regex = "\\d+\\.\\d+";
        pattern = Pattern.compile(regex);
        Matcher isNum = pattern.matcher(str);
        return isNum.matches();
    }

    /**
     * 判断数字区间
     *
     */
    public static boolean numericRange(Integer num, Integer min, Integer max) {
        return !(num < min || num > max);
    }

    /**
     * 判断是否日期格式
     *
     */
    public static boolean isDate(String str) {
        String regex = "\\d{4}-\\d{2}-\\d{2}";
        pattern = Pattern.compile(regex);
        Matcher isDate = pattern.matcher(str);
        return isDate.matches();
    }

    /**
     * 比较两个Double值
     *
     * @param a 参数A
     * @param b 参数B
     * @return true:少于；false: 不少于
     */
    public static boolean isStockShortage(Double a, Double b) {
        return !(NumberUtil.formatDouble(a) >= b);
    }

}
