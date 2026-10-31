package com.hongshu.common.xss;

import com.hongshu.common.constant.StringPoolConstant;
import com.hongshu.common.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

/**
 * SQL过滤
 *
 * @author hongshu
 */
public class SQLFilter {

    /**
     * SQL注入过滤
     *
     * @param str 待验证的字符串
     */
    public static String sqlInject(String str) {
        if (StringUtils.isBlank(str)) {
            return null;
        }
        //去掉'|"|;|\字符
        str = Strings.CS.replace(str, "'", StringPoolConstant.EMPTY);
        str = Strings.CS.replace(str, "\"", StringPoolConstant.EMPTY);
        str = Strings.CS.replace(str, ";", StringPoolConstant.EMPTY);
        str = Strings.CS.replace(str, "\\", StringPoolConstant.EMPTY);

        //转换成小写
        str = str.toLowerCase();

        //非法字符
        String[] keywords = {"master", "truncate", "insert", "select", "delete", "update", "declare", "alert",
                "drop"};
        //判断是否包含非法字符
        for (String keyword : keywords) {
            if (str.contains(keyword)) {
                throw new BusinessException("包含非法字符");
            }
        }
        return str;
    }

}
