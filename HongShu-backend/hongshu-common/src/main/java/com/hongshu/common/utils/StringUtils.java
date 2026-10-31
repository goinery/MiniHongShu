package com.hongshu.common.utils;

import com.hongshu.common.constant.Constants;
import com.hongshu.common.core.text.StrFormatter;
import org.springframework.util.AntPathMatcher;
import org.apache.commons.lang3.Strings;

import java.util.*;

/**
 * 字符串工具类
 *
 * @author hongshu
 */
public final class StringUtils {

    public static final String EMPTY = "";

    private StringUtils() {
    }

    // 显式委托，保留项目工具方法，避免继承 Commons Lang 已过时的构造器。

    public static boolean isEmpty(CharSequence cs) {
        return org.apache.commons.lang3.StringUtils.isEmpty(cs);
    }

    public static boolean isNotEmpty(CharSequence cs) {
        return org.apache.commons.lang3.StringUtils.isNotEmpty(cs);
    }

    public static boolean isBlank(CharSequence cs) {
        return org.apache.commons.lang3.StringUtils.isBlank(cs);
    }

    public static boolean isNotBlank(CharSequence cs) {
        return org.apache.commons.lang3.StringUtils.isNotBlank(cs);
    }

    public static boolean isNumeric(CharSequence cs) {
        return org.apache.commons.lang3.StringUtils.isNumeric(cs);
    }

    public static int length(CharSequence cs) {
        return org.apache.commons.lang3.StringUtils.length(cs);
    }

    public static String capitalize(String str) {
        return org.apache.commons.lang3.StringUtils.capitalize(str);
    }

    public static String defaultString(String str) {
        return org.apache.commons.lang3.StringUtils.defaultString(str);
    }

    public static <T extends CharSequence> T defaultIfEmpty(T str, T defaultStr) {
        return org.apache.commons.lang3.StringUtils.defaultIfEmpty(str, defaultStr);
    }

    public static <T extends CharSequence> T defaultIfBlank(T str, T defaultStr) {
        return org.apache.commons.lang3.StringUtils.defaultIfBlank(str, defaultStr);
    }

    public static boolean containsAny(CharSequence cs, CharSequence searchChars) {
        return org.apache.commons.lang3.StringUtils.containsAny(cs, searchChars);
    }

    public static int countMatches(CharSequence str, CharSequence sub) {
        return org.apache.commons.lang3.StringUtils.countMatches(str, sub);
    }

    public static String join(Object[] array, String separator) {
        return org.apache.commons.lang3.StringUtils.join(array, separator);
    }

    public static String join(Iterable<?> iterable, String separator) {
        return org.apache.commons.lang3.StringUtils.join(iterable, separator);
    }

    @SafeVarargs
    public static <T> String join(T... elements) {
        return org.apache.commons.lang3.StringUtils.join(elements);
    }

    public static String repeat(char ch, int repeat) {
        return org.apache.commons.lang3.StringUtils.repeat(ch, repeat);
    }

    public static String replaceEach(String text, String[] searchList, String[] replacementList) {
        return org.apache.commons.lang3.StringUtils.replaceEach(text, searchList, replacementList);
    }

    public static String[] split(String str, String separatorChars) {
        return org.apache.commons.lang3.StringUtils.split(str, separatorChars);
    }

    public static String stripEnd(String str, String stripChars) {
        return org.apache.commons.lang3.StringUtils.stripEnd(str, stripChars);
    }

    public static String substringAfter(String str, String separator) {
        return org.apache.commons.lang3.StringUtils.substringAfter(str, separator);
    }

    public static String substringAfterLast(String str, String separator) {
        return org.apache.commons.lang3.StringUtils.substringAfterLast(str, separator);
    }

    public static String substringBefore(String str, String separator) {
        return org.apache.commons.lang3.StringUtils.substringBefore(str, separator);
    }

    public static String substringBeforeLast(String str, String separator) {
        return org.apache.commons.lang3.StringUtils.substringBeforeLast(str, separator);
    }

    public static String substringBetween(String str, String open, String close) {
        return org.apache.commons.lang3.StringUtils.substringBetween(str, open, close);
    }

    public static String trimToEmpty(String str) {
        return org.apache.commons.lang3.StringUtils.trimToEmpty(str);
    }

    public static boolean contains(CharSequence cs, CharSequence search) {
        return Strings.CS.contains(cs, search);
    }

    public static boolean containsIgnoreCase(CharSequence cs, CharSequence search) {
        return Strings.CI.contains(cs, search);
    }

    public static boolean endsWith(CharSequence str, CharSequence suffix) {
        return Strings.CS.endsWith(str, suffix);
    }

    public static boolean equals(CharSequence str1, CharSequence str2) {
        return Strings.CS.equals(str1, str2);
    }

    public static boolean equalsAny(CharSequence str, CharSequence... searchStrings) {
        return Strings.CS.equalsAny(str, searchStrings);
    }

    public static int indexOfIgnoreCase(CharSequence str, CharSequence search) {
        return Strings.CI.indexOf(str, search);
    }

    public static String removeStart(String str, String remove) {
        return Strings.CS.removeStart(str, remove);
    }

    public static String replace(String text, String search, String replacement) {
        return Strings.CS.replace(text, search, replacement);
    }

    public static boolean startsWithAny(CharSequence str, CharSequence... searchStrings) {
        return Strings.CS.startsWithAny(str, searchStrings);
    }

    public static boolean startsWithIgnoreCase(CharSequence str, CharSequence prefix) {
        return Strings.CI.startsWith(str, prefix);
    }

    /**
     * 空字符串
     */
    private static final String NULLSTR = "";

    /**
     * 下划线
     */
    private static final char SEPARATOR = '_';

    /**
     * 星号
     */
    private static final char ASTERISK = '*';

    /**
     * 获取参数不为空值
     *
     * @param value defaultValue 要判断的value
     * @return value 返回值
     */
    public static <T> T nvl(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }

    /**
     * * 判断一个Collection是否为空， 包含List，Set，Queue
     *
     * @param coll 要判断的Collection
     * @return true：为空 false：非空
     */
    public static boolean isEmpty(Collection<?> coll) {
        return isNull(coll) || coll.isEmpty();
    }

    /**
     * * 判断一个Collection是否非空，包含List，Set，Queue
     *
     * @param coll 要判断的Collection
     * @return true：非空 false：空
     */
    public static boolean isNotEmpty(Collection<?> coll) {
        return !isEmpty(coll);
    }

    /**
     * * 判断一个对象数组是否为空
     *
     * @param objects 要判断的对象数组
     *                * @return true：为空 false：非空
     */
    public static boolean isEmpty(Object[] objects) {
        return isNull(objects) || (objects.length == 0);
    }

    /**
     * * 判断一个对象数组是否非空
     *
     * @param objects 要判断的对象数组
     * @return true：非空 false：空
     */
    public static boolean isNotEmpty(Object[] objects) {
        return !isEmpty(objects);
    }

    /**
     * * 判断一个Map是否为空
     *
     * @param map 要判断的Map
     * @return true：为空 false：非空
     */
    public static boolean isEmpty(Map<?, ?> map) {
        return isNull(map) || map.isEmpty();
    }

    /**
     * * 判断一个Map是否为空
     *
     * @param map 要判断的Map
     * @return true：非空 false：空
     */
    public static boolean isNotEmpty(Map<?, ?> map) {
        return !isEmpty(map);
    }

    /**
     * * 判断一个字符串是否为空串
     *
     * @param str String
     * @return true：为空 false：非空
     */
    public static boolean isEmpty(String str) {
        return isNull(str) || NULLSTR.equals(str.trim());
    }

    /**
     * * 判断一个字符串是否为非空串
     *
     * @param str String
     * @return true：非空串 false：空串
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    /**
     * * 判断一个对象是否为空
     *
     * @param object Object
     * @return true：为空 false：非空
     */
    public static boolean isNull(Object object) {
        return object == null;
    }

    /**
     * * 判断一个对象是否非空
     *
     * @param object Object
     * @return true：非空 false：空
     */
    public static boolean isNotNull(Object object) {
        return !isNull(object);
    }

    /**
     * * 判断一个对象是否是数组类型（Java基本型别的数组）
     *
     * @param object 对象
     * @return true：是数组 false：不是数组
     */
    public static boolean isArray(Object object) {
        return isNotNull(object) && object.getClass().isArray();
    }

    /**
     * 去空格
     */
    public static String trim(String str) {
        return (str == null ? "" : str.trim());
    }

    /**
     * 替换指定字符串的指定区间内字符为"*"
     *
     * @param str          字符串
     * @param startInclude 开始位置（包含）
     * @param endExclude   结束位置（不包含）
     * @return 替换后的字符串
     */
    public static String hide(CharSequence str, int startInclude, int endExclude) {
        if (isEmpty(str)) {
            return NULLSTR;
        }
        final int strLength = str.length();
        if (startInclude > strLength) {
            return NULLSTR;
        }
        if (endExclude > strLength) {
            endExclude = strLength;
        }
        if (startInclude > endExclude) {
            // 如果起始位置大于结束位置，不替换
            return NULLSTR;
        }
        final char[] chars = new char[strLength];
        for (int i = 0; i < strLength; i++) {
            if (i >= startInclude && i < endExclude) {
                chars[i] = ASTERISK;
            } else {
                chars[i] = str.charAt(i);
            }
        }
        return new String(chars);
    }

    /**
     * 截取字符串
     *
     * @param str   字符串
     * @param start 开始
     * @return 结果
     */
    public static String substring(final String str, int start) {
        if (str == null) {
            return NULLSTR;
        }

        if (start < 0) {
            start = str.length() + start;
        }

        if (start < 0) {
            start = 0;
        }
        if (start > str.length()) {
            return NULLSTR;
        }

        return str.substring(start);
    }
}
