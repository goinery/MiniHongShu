package com.hongshu.framework.config;

import com.hongshu.common.utils.DozerUtil;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 防止移除 XML 运行库后破坏导航和树节点使用的 Bean 转换。
 */
class DozerCompatibilityTest {
    @Test
    void convertsListsAndCopiesIntoExistingBeansWithoutXmlRuntime() {
        Source source = new Source();
        source.setName("导航");
        Target target = DozerUtil.convertor(List.of(source), Target.class).getFirst();
        assertEquals("导航", target.getName());

        source.setName("树节点");
        DozerUtil.copyConvertor(source, target);
        assertEquals("树节点", target.getName());
    }

    @Getter
    @Setter
    public static class Source {
        private String name;
    }

    @Getter
    @Setter
    public static class Target {
        private String name;
    }
}
