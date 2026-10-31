package com.hongshu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

/**
 * 启动程序
 *
 * @author hongshu
 */
// MyBatis 自动配置类仅用于满足 PageHelper 的排序元数据；SqlSessionFactory 仍由 MyBatis-Plus 配置。
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class},
        excludeName = "org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration")
public class HongshuApplication {
    public static void main(String[] args) {
        // System.setProperty("spring.devtools.restart.enabled", "false");
        SpringApplication.run(HongshuApplication.class, args);
        System.out.println("==========启动成功==========");
    }
}
