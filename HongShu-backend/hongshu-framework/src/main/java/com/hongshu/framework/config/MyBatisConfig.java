package com.hongshu.framework.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * Mybatis支持*匹配扫描包
 *
 * @author hongshu
 */
@Configuration
public class MyBatisConfig {

    static final String DEFAULT_RESOURCE_PATTERN = "**/*.class";

    public static String setTypeAliasesPackage(String typeAliasesPackage) {
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory(resolver);
        List<String> allResult = new ArrayList<>();
        try {
            for (String aliasesPackage : typeAliasesPackage.split(",")) {
                List<String> result = new ArrayList<>();
                aliasesPackage = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX
                        + ClassUtils.convertClassNameToResourcePath(aliasesPackage.trim()) + "/" + DEFAULT_RESOURCE_PATTERN;
                Resource[] resources = resolver.getResources(aliasesPackage);
                if (resources.length > 0) {
                    MetadataReader metadataReader;
                    for (Resource resource : resources) {
                        if (resource.isReadable()) {
                            metadataReader = metadataReaderFactory.getMetadataReader(resource);
                            try {
                                result.add(Class.forName(metadataReader.getClassMetadata().getClassName()).getPackage().getName());
                            } catch (ClassNotFoundException e) {
                                org.slf4j.LoggerFactory.getLogger(MyBatisConfig.class).warn("操作失败", e);
                            }
                        }
                    }
                }
                if (!result.isEmpty()) {
                    HashSet<String> hashResult = new HashSet<>(result);
                    allResult.addAll(hashResult);
                }
            }
            if (!allResult.isEmpty()) {
                typeAliasesPackage = String.join(",", allResult.toArray(String[]::new));
            } else {
                throw new RuntimeException("mybatis typeAliasesPackage 路径扫描错误,参数typeAliasesPackage:" + typeAliasesPackage + "未找到任何包");
            }
        } catch (IOException e) {
            org.slf4j.LoggerFactory.getLogger(MyBatisConfig.class).warn("操作失败", e);
        }
        return typeAliasesPackage;
    }

    public Resource[] resolveMapperLocations(String[] mapperLocations) {
        ResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();
        List<Resource> resources = new ArrayList<>();
        if (mapperLocations != null) {
            for (String mapperLocation : mapperLocations) {
                try {
                    Resource[] mappers = resourceResolver.getResources(mapperLocation);
                    resources.addAll(Arrays.asList(mappers));
                } catch (IOException e) {
                    // ignore
                }
            }
        }
        return resources.toArray(Resource[]::new);
    }

}
