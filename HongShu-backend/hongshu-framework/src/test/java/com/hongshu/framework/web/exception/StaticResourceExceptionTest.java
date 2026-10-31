package com.hongshu.framework.web.exception;

import com.hongshu.common.exception.ServiceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static com.hongshu.framework.web.exception.JsonResponseMatchers.jsonField;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 通过真实 MVC 静态资源处理器验证升级后的缺失资源状态，不直接伪造异常调用。
 */
class StaticResourceExceptionTest {

    private AnnotationConfigWebApplicationContext context;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(ResourceConfiguration.class);
        context.refresh();
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void missingImageKeepsHttp404InsteadOfBusiness500() throws Exception {
        mvc.perform(get("/profile/missing-regression.png"))
                .andExpect(status().isNotFound())
                .andExpect(jsonField("code", 404));
    }

    @Test
    void missingImageHeadRequestAlsoKeeps404() throws Exception {
        mvc.perform(head("/profile/missing-regression.png"))
                .andExpect(status().isNotFound());
    }

    @Test
    void existingStaticFileRemainsReadable() throws Exception {
        mvc.perform(get("/profile/upgrade-readable.txt"))
                .andExpect(status().isOk())
                .andExpect(content().string("hongshu-static-regression\n"));
    }

    @Test
    void existingBusinessErrorContractIsUnchanged() throws Exception {
        mvc.perform(get("/regression/business-error"))
                .andExpect(status().isOk())
                .andExpect(jsonField("code", 409));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @Import(GlobalExceptionHandler.class)
    static class ResourceConfiguration implements WebMvcConfigurer {
        @Override
        public void addResourceHandlers(ResourceHandlerRegistry registry) {
            registry.addResourceHandler("/profile/**")
                    .addResourceLocations("classpath:/static-regression/");
        }

        @Bean
        BusinessErrorController businessErrorController() {
            return new BusinessErrorController();
        }
    }

    @RestController
    static class BusinessErrorController {
        @GetMapping("/regression/business-error")
        public void fail() {
            throw new ServiceException("测试业务冲突", 409);
        }
    }
}
