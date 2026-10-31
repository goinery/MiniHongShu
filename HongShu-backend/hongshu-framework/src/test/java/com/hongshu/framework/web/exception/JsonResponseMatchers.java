package com.hongshu.framework.web.exception;

import org.springframework.test.web.servlet.ResultMatcher;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 使用应用已有的 Jackson 进行结构化 JSON 字段断言。 */
final class JsonResponseMatchers {
    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private JsonResponseMatchers() {
    }

    static ResultMatcher jsonField(String field, Object expected) {
        return result -> assertEquals(MAPPER.valueToTree(expected),
                MAPPER.readTree(result.getResponse().getContentAsByteArray()).get(field),
                "JSON response field: " + field);
    }
}
