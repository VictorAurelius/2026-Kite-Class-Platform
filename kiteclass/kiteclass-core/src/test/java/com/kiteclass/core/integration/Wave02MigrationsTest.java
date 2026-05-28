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
 * Testcontainers and runs Flyway directly to prove the full migration chain V1..V40
 * applies cleanly on a fresh (empty) database.
 *
 * <p>Covers:
 * <ul>
 *   <li>V1..V27: Core schema, students, teachers, courses, etc.</li>
 *   <li>V28: academic_years, semesters, holidays</li>
 *   <li>V29: homeroom_classes, subject_sections, curricula, subject_grades</li>
 *   <li>V30: permissions, roles, role_permissions, user_roles (+ level CHECK 1..10)</li>
 *   <li>V31: frontend_instances (+ status CHECK)</li>
 *   <li>V32: branding_resources (+ category FK CHECKs)</li>
 *   <li>V40: branding table (GAP-065 fresh-deploy fix)</li>
 * </ul>
 *
 * <p>GAP-065 fix: previously baselined at V27 because V25 ALTER'd a table created
 * by kitehub-branding. Now V25 is conditional and V40 creates branding IF NOT EXISTS,
 * so the full chain runs from V1 on empty Postgres.
 *
 * @since Wave 2 Sub-PR follow-up (quality-audit 2026-04-14)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Wave02MigrationsTest {

    // Lifecycle managed by @BeforeAll/@AfterAll below; lint can't see that pattern.
    @SuppressWarnings("resource")
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
                    .withDatabaseName("wave02_migrations")
                    .withUsername("test")
                    .withPassword("test");

    @BeforeAll
    void setup() {
        POSTGRES.start();

        // GAP-065 fix: V25 is now conditional (skips if branding table missing) and
        // V40 creates branding IF NOT EXISTS, so the full chain V1..V40 runs cleanly
        // on a fresh empty Postgres. No more baseline workaround needed.
        Flyway flyway = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load();
        flyway.migrate();
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
    @DisplayName("Flyway recorded all migrations as successful (full chain, no baseline)")
    void flyway_schema_history_shows_all_migrations_applied() throws SQLException {
        try (Connection conn = dataSource();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT COUNT(*) FROM flyway_schema_history "
                             + "WHERE success = true AND type = 'SQL'"
             )) {
            assertThat(rs.next()).isTrue();
            // At least V1..V42 applied. Exact count grows as migrations are added —
            // avoid hardcoded count to reduce false-positive breakage on new migrations.
            assertThat(rs.getInt(1)).isGreaterThanOrEqualTo(42);
        }
    }

    @Test
    @DisplayName("V42 creates parent portal tables (GAP-052a Wave 2)")
    void v42_parent_portal_tables_present() throws SQLException {
        assertTableExists("parents");
        assertTableExists("parent_student_links");
        assertTableExists("parent_invitations");
    }

    @Test
    @DisplayName("V40 creates branding table for fresh deploy (GAP-065)")
    void v40_branding_table_exists() throws SQLException {
        assertTableExists("branding");
    }

    @Test
    @DisplayName("V73 migrates created_by / updated_by to UUID across all audit tables (GAP-795)")
    void v46_audit_columns_aligned_to_bigint() throws SQLException {
        // V46 first aligned these audit columns to BIGINT (BaseEntity Long createdBy).
        // GAP-795 V73 then migrated created_by / updated_by Long→UUID (X-User-Id is the
        // JWT sub UUID; there is no numeric user id). After V73 every audit column on
        // these 19 tables MUST be UUID.
        // role_permissions intentionally excluded — pure junction table, no audit columns.
        String[] tables = {
                "academic_years", "semesters", "holidays",
                "homeroom_classes", "subject_sections", "curricula", "subject_grades",
                "permissions", "roles", "user_roles",
                "frontend_instances", "branding_resources", "outbox_events",
                "rebrand_approvals", "audit_log", "moderation_queue",
                "dmca_takedown_requests", "quality_reports", "class_schedule_slots"
        };
        for (String table : tables) {
            assertColumnType(table, "created_by", "uuid");
            assertColumnType(table, "updated_by", "uuid");
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

    private void assertColumnType(String table, String column, String expectedType) throws SQLException {
        try (Connection conn = dataSource();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT data_type FROM information_schema.columns "
                             + "WHERE table_schema = 'public' "
                             + "AND table_name = '" + table + "' "
                             + "AND column_name = '" + column + "'"
             )) {
            assertThat(rs.next())
                    .as("column %s.%s should exist", table, column)
                    .isTrue();
            assertThat(rs.getString(1))
                    .as("column %s.%s data_type", table, column)
                    .isEqualTo(expectedType);
        }
    }
}
