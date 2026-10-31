package com.hongshu.framework.config;

import com.hongshu.common.annotation.Sensitive;
import com.hongshu.common.core.domain.entity.SysUser;
import com.hongshu.common.core.domain.model.LoginUser;
import com.hongshu.common.enums.DesensitizedType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SensitiveJsonSerializerTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void anonymousFieldsKeepTheirOwnMaskingRulesAcrossRepeatedSerialization() {
        SecurityContextHolder.clearContext();
        assertMasked(mapper.readTree(mapper.writeValueAsString(new Profile())));
        assertMasked(mapper.readTree(mapper.writeValueAsString(new Profile())));
    }

    @Test
    void regularUserIsMaskedAndAdministratorSeesOriginalValuesUsingTheSameMapper() {
        authenticate(2L);
        assertMasked(mapper.readTree(mapper.writeValueAsString(new Profile())));

        authenticate(1L);
        JsonNode admin = mapper.readTree(mapper.writeValueAsString(new Profile()));
        assertEquals("张三丰", admin.get("username").asString());
        assertEquals("13812345678", admin.get("phone").asString());
        assertEquals("alice@example.com", admin.get("email").asString());

        authenticate(2L);
        assertMasked(mapper.readTree(mapper.writeValueAsString(new Profile())));
    }

    @Test
    void nullSensitiveValuesAndUnannotatedFieldsKeepTheirJsonShape() {
        Profile profile = new Profile();
        profile.phone = null;
        JsonNode json = mapper.readTree(mapper.writeValueAsString(profile));
        assertTrue(json.has("phone"));
        assertTrue(json.get("phone").isNull());
        assertEquals("public-value", json.get("description").asString());
    }

    private void authenticate(long userId) {
        SysUser user = new SysUser();
        user.setUserId(userId);
        LoginUser loginUser = new LoginUser(user, Collections.emptySet());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList()));
    }

    private void assertMasked(JsonNode json) {
        assertEquals("张*丰", json.get("username").asString());
        assertEquals("138****5678", json.get("phone").asString());
        assertEquals("a****@example.com", json.get("email").asString());
    }

    static class Profile {
        @Sensitive(desensitizedType = DesensitizedType.USERNAME)
        public String username = "张三丰";
        @Sensitive(desensitizedType = DesensitizedType.PHONE)
        public String phone = "13812345678";
        @Sensitive(desensitizedType = DesensitizedType.EMAIL)
        public String email = "alice@example.com";
        public String description = "public-value";
    }
}
