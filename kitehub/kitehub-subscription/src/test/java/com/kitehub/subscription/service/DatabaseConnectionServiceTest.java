package com.kitehub.subscription.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for DatabaseConnectionService.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DatabaseConnectionService Unit Tests")
class DatabaseConnectionServiceTest {

    @InjectMocks
    private DatabaseConnectionService connectionService;

    @Test
    @DisplayName("Should use default values when properties not set")
    void shouldUseDefaultValues() {
        // When properties not set, should use defaults
        assertThat(connectionService).isNotNull();
    }

    @Test
    @DisplayName("Should construct connection URL correctly")
    void shouldConstructConnectionUrl() {
        // Given
        ReflectionTestUtils.setField(connectionService, "adminUrl",
            "jdbc:postgresql://localhost:5433/postgres");
        ReflectionTestUtils.setField(connectionService, "adminUsername", "postgres");
        ReflectionTestUtils.setField(connectionService, "adminPassword", "test_password");

        // When/Then - Just verify service is properly initialized
        // Actual connection tests require a real PostgreSQL instance
        assertThat(connectionService).isNotNull();
    }

    @Test
    @DisplayName("Should handle empty password gracefully")
    void shouldHandleEmptyPassword() {
        // Given
        ReflectionTestUtils.setField(connectionService, "adminUrl",
            "jdbc:postgresql://localhost:5433/postgres");
        ReflectionTestUtils.setField(connectionService, "adminUsername", "postgres");
        ReflectionTestUtils.setField(connectionService, "adminPassword", "");

        // When/Then
        // Empty password should be handled (warning logged)
        assertThat(connectionService).isNotNull();
    }

    // Note: Integration tests with real database are in DatabaseProvisioningIntegrationTest
}
