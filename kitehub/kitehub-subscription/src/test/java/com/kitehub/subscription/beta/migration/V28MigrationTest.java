package com.kitehub.subscription.beta.migration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * File-content sanity test for the GAP-372 V28 migration.
 *
 * <p>This module's test suite does not boot Postgres TestContainers (no other
 * migration test does either — see {@code FlywayMigrationServiceTest}); the
 * Flyway-clean apply runs in CI's full integration stage. This test guards the
 * SQL file shape (table name, columns, indexes, COMMENT statements) so a typo
 * in the migration is caught at unit-test time.</p>
 */
@DisplayName("V28__create_beta_access_request migration content")
class V28MigrationTest {

    private static final Path V28 = Paths.get(
            "src/main/resources/db/migration/V28__create_beta_access_request.sql");

    @Test
    @DisplayName("File exists and is non-empty")
    void fileExists() throws IOException {
        assertThat(Files.exists(V28)).isTrue();
        assertThat(Files.size(V28)).isGreaterThan(200L);
    }

    @Test
    @DisplayName("Defines the beta_access_request table with all required columns")
    void tableShape() throws IOException {
        String sql = Files.readString(V28);
        assertThat(sql).contains("CREATE TABLE beta_access_request");
        // PK + identity
        assertThat(sql).contains("id BIGSERIAL PRIMARY KEY");
        // Required PII / business columns
        assertThat(sql).contains("email VARCHAR");
        assertThat(sql).contains("name VARCHAR");
        assertThat(sql).contains("org_name VARCHAR");
        assertThat(sql).contains("persona VARCHAR");
        assertThat(sql).contains("referral_source VARCHAR");
        // Lifecycle columns
        assertThat(sql).contains("status VARCHAR");
        assertThat(sql).contains("invite_token UUID");
        assertThat(sql).contains("invite_token_expiry TIMESTAMP");
        assertThat(sql).contains("invite_sent_at TIMESTAMP");
        assertThat(sql).contains("approver_id");
        assertThat(sql).contains("approved_at TIMESTAMP");
        assertThat(sql).contains("rejected_at TIMESTAMP");
        assertThat(sql).contains("rejection_reason TEXT");
        // Audit columns
        assertThat(sql).contains("created_at TIMESTAMP");
        assertThat(sql).contains("updated_at TIMESTAMP");
        // Default + status seed
        assertThat(sql).contains("DEFAULT 'PENDING'");
    }

    @Test
    @DisplayName("Creates indexes on status, email, and partial unique on invite_token")
    void indexes() throws IOException {
        String sql = Files.readString(V28);
        assertThat(sql).contains("CREATE INDEX idx_beta_access_request_status");
        assertThat(sql).contains("CREATE INDEX idx_beta_access_request_email");
        assertThat(sql).contains("CREATE UNIQUE INDEX idx_beta_access_request_token");
        assertThat(sql).contains("WHERE invite_token IS NOT NULL");
    }
}
