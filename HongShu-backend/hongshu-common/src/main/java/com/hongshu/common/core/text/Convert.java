package com.hongshu.common.core.text;

import com.hongshu.common.utils.StringUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.text.NumberFormat;
import java.util.Set;

/**
 * 类型转换器
 *
 * @author hongshu
 */
public class Convert {

    /**
     * 转换为字符串<br>
     * 如果给定的值为null，或者转换失败，返回默认值<br>
     * 转换失败不会报错
     *
     * @param value        被转换的值
     * @param defaultValue 转换错误时的默认值
     * @return 结果
     */
    public static String toStr(Object value, String defaultValue) {
        if (null == value) {
            return defaultValue;
        }
        if (value instanceof String) {
            return (String) value;
        }
        return value.toString();
    }

    /**
     * 转换为字符串<br>
     * 如果给定的值为<code>null</code>，或者转换失败，返回默认值<code>null</code><br>
     * 转换失败不会报错
     *
     * @param value 被转换的值
     * @return 结果
     */
    public static String toStr(Object value) {
        return toStr(value, null);
    }

    /**
     * 转换为字符<br>
     * 如果给定的值为null，或者转换失败，返回默认值<br>
     * 转换失败不会报错
     *
     * @param value        被转换的值
     * @param defaultValue 转换错误时的默认值
     * @return 结果
     */
    public static Character toChar(Object value, Character defaultValue) {
        if (null == value) {
            return defaultValue;
        }
        if (value instanceof Character) {
            return (Character) value;
        }

        final String valueStr = toStr(value, null);
        return StringUtils.isEmpty(valueStr) ? defaultValue : valueStr.charAt(0);
    }

    /**
     * 转换为字符<br>
     * 如果给定的值为<code>null</code>，或者转换失败，返回默认值<code>null</code><br>
     * 转换失败不会报错
     *
     * @param value 被转换的值
     * @return 结果
     */
    public static Character toChar(Object value) {
        return toChar(value, null);
    }

    /**
     * 转换为byte<br>
     * 如果给定的值为<code>null</code>，或者转换失败，返回默认值<br>
     * 转换失败不会报错
     *
     * @param value        被转换的值
     * @param defaultValue 转换错误时的默认值
     * @return 结果
     */
    public static Byte toByte(Object value, Byte defaultValue) {
        switch (value) {
            case null -> {
                return defaultValue;
            }
            case Byte number -> {
                return number;
            }
            case Number number -> {
                return number.byteValue();
            }
            default -> {
            }
        }
        final String valueStr = toStr(value, null);
        if (StringUtils.isEmpty(valueStr)) {
            return defaultValue;
        }
        try {
            return Byte.parseByte(valueStr);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 转换为byte<br>
     * 如果给定的值为<code>null</code>，或者转换失败，返回默认值<code>null</code><br>
     * 转换失败不会报错
     *
     * @param value 被转换的值
     * @return 结果
     */
    public static Byte toByte(Object value) {
        return toByte(value, null);
    }

    /**
     * 转换为Short<br>
     * 如果给定的值为<code>null</code>，或者转换失败，返回默认值<br>
     * 转换失败不会报错
     *
     * @param value        被转换的值
     * @param defaultValue 转换错误时的默认值
     * @return 结果
     */
    public static Short toShort(Object value, Short defaultValue) {
        switch (value) {
            case null -> {
                return defaultValue;
            }
            case Short number -> {
                return number;
            }
            case Number number -> {
                return number.shortValue();
            }
            default -> {
            }
        }
        final String valueStr = toStr(value, null);
        if (StringUtils.isEmpty(valueStr)) {
            return defaultValue;
        }
        try {
            return Short.parseShort(valueStr.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 转换为Short<br>
     * 如果给定的值为<code>null</code>，或者转换失败，返回默认值<code>null</code><br>
     * 转换失败不会报错
     *
     * @param value 被转换的值
     * @return 结果
     */
    public static Short toShort(Object value) {
        return toShort(value, null);
    }

    /**
     * 转换为Number<br>
     * 如果给定的值为空，或者转换失败，返回默认值<br>
     * 转换失败不会报错
     *
     * @param value        被转换的值
     * @param defaultValue 转换错误时的默认值
     * @return 结果
     */
    public static Number toNumber(Object value, Number defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return (Number) value;
        }
        final String valueStr = toStr(value, null);
        if (StringUtils.isEmpty(valueStr)) {
            return defaultValue;
        }
        try {
            return NumberFormat.getInstance().parse(valueStr);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 转换为Number<br>
     * 如果给定的值为空，或者转换失败，返回默认值<code>null</code><br>
     * 转换失败不会报错
     *
     * @param value 被转换的值
     * @return 结果
     */
    public static Number toNumber(Object value) {
        return toNumber(value, null);
    }

    /**
     * 转换为int<br>
     * 如果给定的值为空，或者转换失败，返回默认值<br>
     * 转换失败不会报错
     *
     * @param value        被转换的值
     * @param defaultValue 转换错误时的默认值
     * @return 结果
     */
    public static Integer toInt(Object value, Integer defaultValue) {
        switch (value) {
            case null -> {
                return defaultValue;
            }
            case Integer number -> {
                return number;
            }
            case Number number -> {
                return number.intValue();
            }
            default -> {
            }
        }
        final String valueStr = toStr(value, null);
        if (StringUtils.isEmpty(valueStr)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(valueStr.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 转换为int<br>
     * 如果给定的值为<code>null</code>，或者转换失败，返回默认值<code>null</code><br>
     * 转换失败不会报错
     *
     * @param value 被转换的值
     * @return 结果
     */
    public static Integer toInt(Object value) {
        return toInt(value, null);
    }

    /**
     * 转换为Integer数组<br>
     *
     * @param str 被转换的值
     * @return 结果
     */
    public static Integer[] toIntArray(String str) {
        return toIntArray(",", str);
    }

    /**
     * 转换为Long数组<br>
     *
     * @param str 被转换的值
     * @return 结果
     */
    public static Long[] toLongArray(String str) {
        return toLongArray(",", str);
    }

    /**
     * 转换为Integer数组<br>
     *
     * @param split 分隔符
     * @return 结果
     */
    public static Integer[] toIntArray(String split, String str) {
        if (StringUtils.isEmpty(str)) {
            return new Integer[]{};
        }
        String[] arr = str.split(split);
        final Integer[] ints = new Integer[arr.length];
        for (int i = 0; i < arr.length; i++) {
            final Integer v = toInt(arr[i], 0);
            ints[i] = v;
        }
        return ints;
    }

    /**
     * 转换为Long数组<br>
     *
     * @param split 分隔符
     * @param str   被转换的值
     * @return 结果
     */
    public static Long[] toLongArray(String split, String str) {
        if (StringUtils.isEmpty(str)) {
            return new Long[]{};
        }
        String[] arr = str.split(split);
        final Long[] longs = new Long[arr.length];
        for (int i = 0; i < arr.length; i++) {
            final Long v = toLong(arr[i], null);
            longs[i] = v;
        }
        return longs;
    }

    /**
     * 转换为String数组<br>
     *
     * @param str 被转换的值
     * @return 结果
     */
    public static String[] toStrArray(String str) {
        return toStrArray(",", str);
    }

    /**
     * 转换为String数组<br>
     *
     * @param split 分隔符
     * @return 结果
     */
    public static String[] toStrArray(String split, String str) {
        return str.split(split);
    }

    /**
     * 转换为long<br>
     * 如果给定的值为空，或者转换失败，返回默认值<br>
     * 转换失败不会报错
     *
     * @param value        被转换的值
     * @param defaultValue 转换错误时的默认值
     * @return 结果
     */
    public static Long toLong(Object value, Long defaultValue) {
        switch (value) {
            case null -> {
                return defaultValue;
            }
            case Long number -> {
                return number;
            }
            case Number number -> {
                return number.longValue();
            }
            default -> {
            }
        }
        final String valueStr = toStr(value, null);
        if (StringUtils.isEmpty(valueStr)) {
            return defaultValue;
        }
        try {
            // 支持科学计数法
            return new BigDecimal(valueStr.trim()).longValue();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 转换为long<br>
     * 如果给定的值为<code>null</code>，或者转换失败，返回默认值<code>null</code><br>
     * 转换失败不会报错
     *
     * @param value 被转换的值
     * @return 结果
     */
    public static Long toLong(Object value) {
        return toLong(value, null);
    }
}
