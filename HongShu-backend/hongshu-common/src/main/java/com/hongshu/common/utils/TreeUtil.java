package com.hongshu.common.utils;

import com.hongshu.common.core.domain.Tree;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 树形结构递归处理类
 *
 * @author hongshu
 */
public class TreeUtil {

    private static final String PID = "pid";
    private static final String ID = "id";
    private static final String CHILDREN = "children";

    /**
     * 递归方法
     */
    public static List<Tree> tree(List<?> source) {
        List<Tree> trees = DozerUtil.convertor(source, Tree.class);
        //获取父节点
        return trees.stream().filter(m -> m.getParentId() == 0).peek(
                m -> m.setChildren(getChildrens(m, trees))
        ).collect(Collectors.toList());
    }

    /**
     * 获取当前节点的所有子节点
     */
    private static List<Tree> getChildrens(Tree root, List<Tree> all) {
        return all.stream().filter(m -> Objects.equals(m.getParentId(), root.getId())).peek(
                m -> m.setChildren(getChildrens(m, all))
        ).collect(Collectors.toList());
    }

    /**
     * 递归方法
     * 必须有参数parentId和children
     */
    public static <T, S> List<T> tree(List<S> source, Class<T> cs) {
        List<T> trees = DozerUtil.convertor(source, cs);
        //获取父节点
        return trees.stream().filter(m -> {
            try {
                Field field = m.getClass().getDeclaredField(PID);
                field.setAccessible(true);
                return Integer.valueOf(field.get(m).toString()).equals(0);
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger(TreeUtil.class).warn("操作失败", e);
                return false;
            }
        }).peek(
                (m) -> {
                    try {
                        Field field = m.getClass().getDeclaredField(CHILDREN);
                        field.setAccessible(true);
                        field.set(m, getChildrens(m, trees));

                    } catch (Exception e) {
                        org.slf4j.LoggerFactory.getLogger(TreeUtil.class).warn("操作失败", e);
                    }

                }
        ).collect(Collectors.toList());
    }

    /**
     * 获取当前节点的所有子节点
     */
    private static <T> List<T> getChildrens(T t, List<T> all) {
        return all.stream().filter(m -> {
            try {
                Field field = m.getClass().getDeclaredField(PID);
                field.setAccessible(true);
                Field fieldt = t.getClass().getDeclaredField(ID);
                fieldt.setAccessible(true);
                return Objects.equals(field.get(m), fieldt.get(t));
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger(TreeUtil.class).warn("操作失败", e);
            }
            return false;
        }).peek(
                (m) -> {
                    try {
                        Field field = m.getClass().getDeclaredField(CHILDREN);
                        field.setAccessible(true);
                        field.set(m, getChildrens(m, all));
                    } catch (Exception e) {
                        org.slf4j.LoggerFactory.getLogger(TreeUtil.class).warn("操作失败", e);
                    }

                }
        ).collect(Collectors.toList());
    }

}
