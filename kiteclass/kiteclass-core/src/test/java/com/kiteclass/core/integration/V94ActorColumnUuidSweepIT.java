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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GAP-877 (KC portion) — proof that V94 retyped the SAFE actor user-id columns
 * BIGINT/VARCHAR → uuid AND that a real UUID actor value round-trips without a
 * binding / parse failure on real Postgres.
 *
 * <p><strong>Why a standalone Flyway test (not @DataJpaTest):</strong> the kc-core
 * {@code test} profile disables Flyway and uses {@code hibernate.ddl-auto: create-drop},
 * so a slice test would assert against a Hibernate-generated schema and never exercise
 * the V94 migration. Per the {@code Wave14EntityDriftMigrationsIT} precedent we run the
 * real Flyway chain V1..V94 on real Postgres and assert the post-migration column types
 * + a UUID INSERT round-trip (the round-trip is the part {@code information_schema} alone
 * cannot prove — it catches the exact "UUID into BIGINT column" bind failure the gap is
 * about).
 *
 * <p>Scope = the 5 SAFE columns V94 converts (no live writer threads a numeric domain id
 * into them today): payments.received_by, payments.payer_id, reward_redemptions.approved_by,
 * user_roles.assigned_by, moderation_queue.assigned_reviewer_id. The remaining
 * numeric-threaded actor columns are deferred (see V94 header comment) and are out of
 * this test's scope.
 *
 * @since GAP-877 (Wave p0-local-1 Bucket A)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class V94ActorColumnUuidSweepIT {

    @SuppressWarnings("resource") // Lifecycle managed by @BeforeAll/@AfterAll
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
                    .withDatabaseName("gap877_actor_uuid")
                    .withUsername("test")
                    .withPassword("test");

    @BeforeAll
    void setup() {
        POSTGRES.start();
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

    // ——— Type assertions (migration applied the conversion) ———————————————————

    @Test
    @DisplayName("V94 retypes all 5 SAFE actor columns to uuid")
    void all_safe_actor_columns_are_uuid() throws SQLException {
        assertColumnType("payments", "received_by", "uuid");
        assertColumnType("payments", "payer_id", "uuid");
        assertColumnType("reward_redemptions", "approved_by", "uuid");
        assertColumnType("user_roles", "assigned_by", "uuid");
        assertColumnType("moderation_queue", "assigned_reviewer_id", "uuid");
    }

    // ——— UUID INSERT round-trips (≥3 columns — proves no parse/bind failure) ———

    @Test
    @DisplayName("moderation_queue.assigned_reviewer_id accepts a real UUID actor")
    void moderation_queue_assigned_reviewer_uuid_roundtrip() throws SQLException {
        UUID tenant = UUID.randomUUID();
        UUID reviewer = UUID.randomUUID();
        // moderation_queue is FK-free; superuser bypasses FORCE RLS (schema-shape proof only).
        exec("INSERT INTO moderation_queue "
                + "(instance_id, target_type, target_id, status, score, assigned_reviewer_id, created_at, updated_at) "
                + "VALUES ('" + tenant + "', 'POST', 'post-1', 'NEEDS_HUMAN_REVIEW', 0.9, "
                + " '" + reviewer + "', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
        long n = count("SELECT count(*) FROM moderation_queue "
                + "WHERE assigned_reviewer_id = '" + reviewer + "'::uuid");
        assertThat(n).as("moderation_queue row with UUID assigned_reviewer_id is queryable").isEqualTo(1L);
    }

    @Test
    @DisplayName("user_roles.assigned_by accepts a real UUID actor")
    void user_roles_assigned_by_uuid_roundtrip() throws SQLException {
        UUID tenant = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        // roles is FK-free; seed one role so user_roles.role_id FK is satisfied.
        long roleId = insertReturningId("INSERT INTO roles (instance_id, name, level) "
                + "VALUES ('" + tenant + "', 'TEACHER', 3) RETURNING id");
        exec("INSERT INTO user_roles (instance_id, user_id, role_id, assigned_by) "
                + "VALUES ('" + tenant + "', 1001, " + roleId + ", '" + actor + "')");
        long n = count("SELECT count(*) FROM user_roles WHERE assigned_by = '" + actor + "'::uuid");
        assertThat(n).as("user_roles row with UUID assigned_by is queryable").isEqualTo(1L);
    }

    @Test
    @DisplayName("reward_redemptions.approved_by accepts a real UUID actor")
    void reward_redemptions_approved_by_uuid_roundtrip() throws SQLException {
        UUID tenant = UUID.randomUUID();
        UUID approver = UUID.randomUUID();
        // students + rewards are FK-free; seed one of each to satisfy reward_redemptions FKs.
        long studentId = insertReturningId("INSERT INTO students (instance_id, name) "
                + "VALUES ('" + tenant + "', 'Tran Thi Hong') RETURNING id");
        long rewardId = insertReturningId("INSERT INTO rewards (instance_id, code, name, points_required) "
                + "VALUES ('" + tenant + "', 'RW1', 'Pen', 100) RETURNING id");
        exec("INSERT INTO reward_redemptions (instance_id, student_id, reward_id, points_spent, approved_by) "
                + "VALUES ('" + tenant + "', " + studentId + ", " + rewardId + ", 100, '" + approver + "')");
        long n = count("SELECT count(*) FROM reward_redemptions WHERE approved_by = '" + approver + "'::uuid");
        assertThat(n).as("reward_redemptions row with UUID approved_by is queryable").isEqualTo(1L);
    }

    @Test
    @DisplayName("Flyway applied the full chain through V94 successfully")
    void flyway_chain_applied_through_v94() throws SQLException {
        long applied = count("SELECT count(*) FROM flyway_schema_history "
                + "WHERE success = true AND version = '94'");
        assertThat(applied).as("V94 applied").isEqualTo(1L);
    }

    // ——— helpers ————————————————————————————————————————————————————————————

    private Connection conn() throws SQLException {
        return DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private void exec(String sql) throws SQLException {
        try (Connection c = conn(); Statement st = c.createStatement()) {
            st.executeUpdate(sql);
        }
    }

    private long insertReturningId(String sql) throws SQLException {
        try (Connection c = conn(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            assertThat(rs.next()).as("INSERT ... RETURNING id produced a row").isTrue();
            return rs.getLong(1);
        }
    }

    private long count(String sql) throws SQLException {
        try (Connection c = conn(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            assertThat(rs.next()).isTrue();
            return rs.getLong(1);
        }
    }

    private void assertColumnType(String table, String column, String expectedType) throws SQLException {
        try (Connection c = conn();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT data_type FROM information_schema.columns "
                             + "WHERE table_schema = 'public' AND table_name = ? AND column_name = ?")) {
            ps.setString(1, table);
            ps.setString(2, column);
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).as("column %s.%s exists", table, column).isTrue();
                assertThat(rs.getString(1)).as("column %s.%s data_type", table, column).isEqualTo(expectedType);
            }
        }
    }
}
