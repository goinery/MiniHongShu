package com.hongshu.framework.config;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JacksonDateCompatibilityTest {

    @Test
    void configuredMapperKeepsExistingDatePatternTimezoneAndExplicitNull() {
        JsonMapper mapper = configuredMapper();
        TimeZone zone = TimeZone.getDefault();
        Instant instant = Instant.parse("2024-01-02T03:04:05Z");
        DatePayload payload = new DatePayload();
        payload.createTime = Date.from(instant);

        JsonNode json = mapper.readTree(mapper.writeValueAsString(payload));
        assertEquals(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                        .withZone(zone.toZoneId()).format(instant),
                json.get("createTime").asString());
        assertTrue(json.has("remark"));
        assertTrue(json.get("remark").isNull());
        assertEquals(payload.createTime,
                mapper.readValue(mapper.writeValueAsString(payload), DatePayload.class).createTime);
    }

    @Test
    void unannotatedDateUsesEpochMillisecondsWhileAnnotatedDateStaysAString() {
        JsonMapper mapper = configuredMapper();
        DatePayload payload = new DatePayload();
        payload.loginDate = Date.from(Instant.parse("2024-01-02T03:04:05.123Z"));
        payload.createTime = Date.from(Instant.parse("2024-01-02T03:04:05Z"));

        String jsonText = mapper.writeValueAsString(payload);
        JsonNode json = mapper.readTree(jsonText);
        assertTrue(json.get("loginDate").isIntegralNumber());
        assertEquals(payload.loginDate.getTime(), json.get("loginDate").longValue());
        assertTrue(json.get("createTime").isString());
        assertEquals(payload.loginDate, mapper.readValue(jsonText, DatePayload.class).loginDate);
    }

    private JsonMapper configuredMapper() {
        JsonMapper.Builder builder = JsonMapper.builderWithJackson2Defaults();
        new ApplicationConfig().jacksonObjectMapperCustomization().customize(builder);
        return builder.build();
    }

    static class DatePayload {
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        public Date createTime;
        public Date loginDate;
        public String remark;
    }
}
