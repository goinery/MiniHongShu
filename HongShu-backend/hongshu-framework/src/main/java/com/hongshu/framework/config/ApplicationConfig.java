package com.hongshu.framework.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import tools.jackson.databind.cfg.DateTimeFeature;

import java.util.TimeZone;

/**
 * 程序注解配置
 *
 * @author hongshu
 */
@Configuration
// 表示通过aop框架暴露该代理对象,AopContext能够访问
@EnableAspectJAutoProxy(exposeProxy = true)
// 指定要扫描的Mapper类的包的路径
@MapperScan("com.hongshu.**.mapper")
public class ApplicationConfig {

    /**
     * 保留原接口的日期约定：普通 Date 输出毫秒时间戳，@JsonFormat 字段使用指定格式。
     */
    @Bean
    public JsonMapperBuilderCustomizer jacksonObjectMapperCustomization() {
        return builder -> builder.defaultTimeZone(TimeZone.getDefault())
                .enable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
