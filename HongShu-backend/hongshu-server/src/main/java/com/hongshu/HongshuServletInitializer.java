package com.hongshu;

import org.jspecify.annotations.NullMarked;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * web容器中进行部署
 *
 * @author hongshu
 */
public class HongshuServletInitializer extends SpringBootServletInitializer {
    @Override
    @NullMarked
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(HongshuApplication.class);
    }
}
