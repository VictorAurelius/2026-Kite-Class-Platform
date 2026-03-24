package com.kiteclass.core.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for SecurityConfigValidator.
 * @since 2026-03-24
 */
class SecurityConfigValidatorTest {

    private SecurityConfigValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SecurityConfigValidator();
    }

    @Test
    @DisplayName("Should pass with valid production credentials")
    void validateSecurityConfig_validCredentials_passes() {
        ReflectionTestUtils.setField(validator, "dbPassword", "secure-prod-password");
        ReflectionTestUtils.setField(validator, "internalApiSecret", "secure-prod-secret");
        assertDoesNotThrow(() -> validator.validateSecurityConfig());
    }

    @Test
    @DisplayName("Should fail with default DB password")
    void validateSecurityConfig_defaultDbPassword_throws() {
        ReflectionTestUtils.setField(validator, "dbPassword", "kiteclass123");
        ReflectionTestUtils.setField(validator, "internalApiSecret", "secure-prod-secret");
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> validator.validateSecurityConfig());
        assertTrue(ex.getMessage().contains("Database password"));
    }

    @Test
    @DisplayName("Should fail with blank DB password")
    void validateSecurityConfig_blankDbPassword_throws() {
        ReflectionTestUtils.setField(validator, "dbPassword", "");
        ReflectionTestUtils.setField(validator, "internalApiSecret", "secure-prod-secret");
        assertThrows(IllegalStateException.class, () -> validator.validateSecurityConfig());
    }

    @Test
    @DisplayName("Should fail with default internal API secret")
    void validateSecurityConfig_defaultApiSecret_throws() {
        ReflectionTestUtils.setField(validator, "dbPassword", "secure-prod-password");
        ReflectionTestUtils.setField(validator, "internalApiSecret", "dev-internal-secret-change-in-production");
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> validator.validateSecurityConfig());
        assertTrue(ex.getMessage().contains("Internal API secret"));
    }

    @Test
    @DisplayName("Should fail with blank API secret")
    void validateSecurityConfig_blankApiSecret_throws() {
        ReflectionTestUtils.setField(validator, "dbPassword", "secure-prod-password");
        ReflectionTestUtils.setField(validator, "internalApiSecret", "");
        assertThrows(IllegalStateException.class, () -> validator.validateSecurityConfig());
    }
}
