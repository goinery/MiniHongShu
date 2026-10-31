package com.hongshu.framework.config;

import com.hongshu.common.utils.JwtUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtAppSecretConfigTest {

    private final Object originalSecret = ReflectionTestUtils.getField(JwtUtils.class, "appSecret");

    @AfterEach
    void restoreSecret() {
        ReflectionTestUtils.setField(JwtUtils.class, "appSecret", originalSecret);
    }

    @Test
    void configuredSecretSignsAndVerifiesTokens() {
        JwtAppSecretConfig config = new JwtAppSecretConfig();
        String secret = Base64.getEncoder().encodeToString(
                "jwt-regression-test-only-signing-key".getBytes(StandardCharsets.UTF_8));
        ReflectionTestUtils.setField(config, "appSecret", secret);
        config.init();

        String token = JwtUtils.getJwtToken("42", 60_000);
        assertTrue(JwtUtils.checkToken(token));
        assertEquals("42", JwtUtils.getUserId(token));
    }

    @Test
    void blankSecretIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> JwtUtils.setAppSecret(" "));
    }

    @Test
    void missingSecretCannotSignTokens() {
        ReflectionTestUtils.setField(JwtUtils.class, "appSecret", null);
        assertThrows(IllegalStateException.class, () -> JwtUtils.getJwtToken("42", 60_000));
    }
}
