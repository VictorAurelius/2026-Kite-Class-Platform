package com.kiteclass.core.integration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Standalone Flyway migration test — the default {@code test} profile disables Flyway
 * and uses {@code hibernate.ddl-auto: create-drop}, so @SpringBootTest-based ITs do not
 * actually exercise our SQL migrations. This test spins up a dedicated Postgres via
 * Testcontainers and runs Flyway directly to prove migrations V28..V32 apply cleanly
 * and their CHECK constraints are enforced.
 *
 * <p>Covers:
 * <ul>
 *   <li>V28: academic_years, semesters, holidays</li>
 *   <li>V29: homeroom_classes, subject_sections, curricula, subject_grades</li>
 *   <li>V30: permissions, roles, role_permissions, user_roles (+ level CHECK 1..10)</li>
 *   <li>V31: frontend_instances (+ status CHECK)</li>
 *   <li>V32: branding_resources (+ category FK CHECKs)</li>
 * </ul>
 *
 * @since Wave 2 Sub-PR follow-up (quality-audit 2026-04-14)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Wave02MigrationsTest {

    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
                    .withDatabaseName("wave02_migrations")
                    .withUsername("test")
                    .withPassword("test");

    @BeforeAll
    void setup() {
        POSTGRES.start();

        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }

    @AfterAll
    void tearDown() {
        POSTGRES.stop();
    }

    @Test
    @DisplayName("V28 creates academic_years / semesters / holidays tables")
    void v28_academic_year_tables_present() throws SQLException {
        assertTableExists("academic_years");
        assertTableExists("semesters");
        assertTableExists("holidays");
    }

    @Test
    @DisplayName("V29 creates K-12 tables")
    void v29_k12_tables_present() throws SQLException {
        assertTableExists("homeroom_classes");
        assertTableExists("subject_sections");
        assertTableExists("curricula");
        assertTableExists("subject_grades");
    }

    @Test
    @DisplayName("V30 creates role hierarchy tables")
    void v30_role_hierarchy_tables_present() throws SQLException {
        assertTableExists("permissions");
        assertTableExists("roles");
        assertTableExists("role_permissions");
        assertTableExists("user_roles");
    }

    @Test
    @DisplayName("V30 role level CHECK rejects values outside 1..10")
    void v30_role_level_check_enforced() {
        assertThatThrownBy(() -> exec(
                "INSERT INTO roles(instance_id, name, level, is_system, deleted, version) "
                        + "VALUES (gen_random_uuid(), 'BAD', 99, false, false, 0)"
        )).hasMessageContaining("chk_role_level");
    }

    @Test
    @DisplayName("V31 frontend_instances status CHECK rejects invalid values")
    void v31_frontend_instances_status_check() {
        assertTableExistsUnchecked("frontend_instances");

        assertThatThrownBy(() -> exec(
                "INSERT INTO frontend_instances"
                        + "(instance_id, tenant_id, slug, status, retry_count, branding_version, "
                        + "version, deleted) "
                        + "VALUES (gen_random_uuid(), 't1', 'bad-status', 'INVALID_STATUS', 0, 0, 0, false)"
        )).hasMessageContaining("chk_frontend_instance_status");
    }

    @Test
    @DisplayName("V32 branding_resources STATIC category forbids templateId")
    void v32_static_forbids_template_id() {
        assertTableExistsUnchecked("branding_resources");

        assertThatThrownBy(() -> exec(
                "INSERT INTO branding_resources"
                        + "(instance_id, type, category, template_id, version, deleted) "
                        + "VALUES (gen_random_uuid(), 'LOGO', 'STATIC', 42, 0, false)"
        )).hasMessageContaining("chk_branding_resource_static_no_fk");
    }

    @Test
    @DisplayName("V32 branding_resources TEMPLATE category requires templateId")
    void v32_template_requires_template_id() {
        assertThatThrownBy(() -> exec(
                "INSERT INTO branding_resources"
                        + "(instance_id, type, category, version, deleted) "
                        + "VALUES (gen_random_uuid(), 'BANNER', 'TEMPLATE', 0, false)"
        )).hasMessageContaining("chk_branding_resource_template_fk");
    }

    @Test
    @DisplayName("Flyway recorded V28..V32 as successful")
    void flyway_schema_history_shows_all_wave2_applied() throws SQLException {
        try (Connection conn = dataSource();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT COUNT(*) FROM flyway_schema_history "
                             + "WHERE version IN ('28','29','30','31','32') AND success = true"
             )) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt(1)).isEqualTo(5);
        }
    }

    // ——— helpers ————————————————————————————————————————————————

    private Connection dataSource() throws SQLException {
        return DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private void exec(String sql) throws SQLException {
        try (Connection conn = dataSource(); Statement st = conn.createStatement()) {
            st.executeUpdate(sql);
        }
    }

    private void assertTableExists(String table) throws SQLException {
        try (Connection conn = dataSource();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT COUNT(*) FROM information_schema.tables "
                             + "WHERE table_schema = 'public' AND table_name = '" + table + "'"
             )) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt(1)).as("table %s should exist", table).isEqualTo(1);
        }
    }

    /**
     * Variant that wraps SQLException — used inside lambdas where checked exceptions are awkward.
     */
    private void assertTableExistsUnchecked(String table) {
        try {
            assertTableExists(table);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
