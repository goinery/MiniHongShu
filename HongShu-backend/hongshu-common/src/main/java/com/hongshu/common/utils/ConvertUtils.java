
package com.hongshu.common.utils;

import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * 转换工具类
 *
 * @author hongshu
 */
public final class ConvertUtils {

    private ConvertUtils() {
    }

    public static <T> T sourceToTarget(Object source, Class<T> target) {
        if (source == null) {
            return null;
        }
        Objects.requireNonNull(target, "目标类型不能为空");
        try {
            T targetObject = target.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(source, targetObject);
            return targetObject;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("无法创建转换目标：" + target.getName(), e);
        }
    }

    public static <T> List<T> sourceToTarget(Collection<?> sourceList, Class<T> target) {
        if (sourceList == null) {
            return null;
        }

        List<T> targetList = new ArrayList<>(sourceList.size());
        for (Object source : sourceList) {
            targetList.add(sourceToTarget(source, target));
        }

        return targetList;
    }
}
