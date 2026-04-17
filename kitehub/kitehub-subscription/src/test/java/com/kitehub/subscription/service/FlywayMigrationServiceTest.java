package com.kitehub.subscription.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for FlywayMigrationService.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FlywayMigrationService Unit Tests")
class FlywayMigrationServiceTest {

    @InjectMocks
    private FlywayMigrationService migrationService;

    @Test
    @DisplayName("Should be instantiated correctly")
    void shouldBeInstantiated() {
        assertThat(migrationService).isNotNull();
    }

    @Test
    @DisplayName("Should throw exception for invalid database URL")
    void shouldThrowExceptionForInvalidUrl() {
        // Given
        String invalidUrl = "invalid-url";
        String username = "test";
        String password = "test";

        // When/Then
        assertThatThrownBy(() -> migrationService.runMigrations(invalidUrl, username, password))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Database migration failed");
    }

    @Test
    @DisplayName("Should handle empty credentials")
    void shouldHandleEmptyCredentials() {
        // Given
        String url = "jdbc:postgresql://localhost:5433/test";
        String username = "";
        String password = "";

        // When/Then
        assertThatThrownBy(() -> migrationService.runMigrations(url, username, password))
            .isInstanceOf(RuntimeException.class);
    }

    // Note: Actual migration tests require a real database
    // See DatabaseProvisioningIntegrationTest for integration tests
}
