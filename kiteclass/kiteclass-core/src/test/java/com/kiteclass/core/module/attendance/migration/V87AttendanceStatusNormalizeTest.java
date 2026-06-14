package com.kiteclass.core.module.attendance.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * GAP-1066 — proves V87 normalizes legacy lowercase {@code attendance.status} BEFORE adding the
 * UPPERCASE {@code chk_attendance_status} constraint, so kiteclass-core boots clean on a DB that
 * still holds lowercase rows (the {@code kiteclass_shared} 1230-restart crash-loop scenario:
 * 450 lowercase rows + new UPPERCASE CHECK → SQLSTATE 23514 at V87 line 30 → flywayInitializer
 * bean fails → context fails → restart loop).
 *
 * <p>Why this is NOT a {@code @DataJpaTest}: the project test profile runs
 * {@code spring.flyway.enabled=false} + {@code ddl-auto=create-drop}, so Hibernate builds the
 * schema from entities — no CHECK constraint, no legacy lowercase rows. Such a test is blind to
 * exactly the schema-vs-data drift this gap is about. This test runs the REAL Flyway chain in two
 * phases against a fresh Testcontainers PostgreSQL — the production-equivalent boot path:
 *
 * <ol>
 *   <li><b>Phase 1</b>: migrate up to V86. At V86 {@code chk_attendance_status} (from V1) still
 *       allows ONLY lowercase {@code present|absent|late|excused}. Seed legacy lowercase rows
 *       (reproduces the {@code kiteclass_shared} restore data).</li>
 *   <li><b>Phase 2</b>: migrate V87..latest. V87 MUST {@code UPPER()} the seeded rows before the
 *       new UPPERCASE constraint, otherwise this {@code migrate()} throws SQLSTATE 23514.</li>
 * </ol>
 *
 * <p>Legacy rows are seeded with FK triggers disabled ({@code session_replication_role=replica} —
 * the {@code pg_restore} mechanism) so the fixture stays decoupled from the deep
 * {@code attendance → class_sessions → classes → courses} parent chain. The bug is about status
 * <i>data</i>, not referential integrity; CHECK / NOT NULL constraints remain fully enforced under
 * replica role, so lowercase seeds still validate against the V86 constraint and the post-V87
 * UPPERCASE constraint is still exercised.
 *
 * <p>CI-bound: named {@code *Test} → runs under surefire (the CI gate), unlike {@code *IT}.
 */
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class V87AttendanceStatusNormalizeTest {

    private static final UUID TENANT = UUID.fromString("00000000-0000-0000-0000-0000000000c1");

    /** The four legacy lowercase values V1's {@code chk_attendance_status} permitted. */
    private static final String[] LEGACY_LOWERCASE = {"present", "absent", "late", "excused"};

    /** {@code notes} marker isolating the seeded legacy rows from ad-hoc rows other tests add. */
    private static final String SEED_TAG = "GAP-1066-SEED";

    @Container
    @SuppressWarnings("resource") // lifecycle managed by @Container + JVM shutdown hook
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("kiteclass_gap1066")
                    .withUsername("test")
                    .withPassword("test");

    private Connection conn;
    private final AtomicLong fkSeq = new AtomicLong(900000L);

    @BeforeAll
    void migrateWithLegacyLowercaseRows() throws SQLException {
        // Phase 1 — bring schema to V86 (lowercase-only chk_attendance_status still in place).
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .target(MigrationVersion.fromVersion("86"))
                .load()
                .migrate();

        conn = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());

        // Defensive: satisfy the V59 tenant_isolation RLS policy. The Testcontainers superuser
        // bypasses RLS anyway, but this keeps the fixture robust to image/role changes.
        // Disable FK triggers so we can seed attendance rows without the parent chain.
        try (Statement st = conn.createStatement()) {
            st.execute("SET app.current_tenant_id = '" + TENANT + "'");
            st.execute("SET session_replication_role = replica");
        }

        // Seed legacy lowercase rows — reproduces kiteclass_shared restore data. These INSERTs
        // still pass the V86 lowercase CHECK (CHECK is not a trigger → enforced under replica).
        // Tagged via notes so the normalize assertion is isolated from ad-hoc rows other tests
        // add (PER_CLASS shares one DB; test execution order is not guaranteed).
        for (String legacy : LEGACY_LOWERCASE) {
            insertAttendance(legacy, SEED_TAG);
        }

        // Phase 2 — migrate V87..latest. If V87 did NOT normalize first, this throws SQLSTATE
        // 23514 (chk_attendance_status violated by some row) and the whole test errors out — i.e.
        // a regression of GAP-1066 fails this test loudly via @BeforeAll.
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load()
                .migrate();
    }

    @AfterAll
    void closeConnection() throws SQLException {
        if (conn != null) {
            conn.close();
        }
    }

    @Test
    @DisplayName("V87 chain applies cleanly on a DB holding legacy lowercase rows (no crash-loop)")
    void flywayChainAppliedCleanlyIncludingV87() throws SQLException {
        // @BeforeAll already proved migrate() did not throw; assert the history row explicitly.
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT success FROM flyway_schema_history WHERE version = '87'");
                ResultSet rs = ps.executeQuery()) {
            assertThat(rs.next()).as("V87 must be recorded in flyway_schema_history").isTrue();
            assertThat(rs.getBoolean(1)).as("V87 must be marked success=true").isTrue();
        }
        // V88 must also have applied (AC: "Flyway V87+V88 applied").
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '88' AND success = true");
                ResultSet rs = ps.executeQuery()) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt(1)).as("V88 must be applied after V87").isEqualTo(1);
        }
    }

    @Test
    @DisplayName("V87 normalizes seeded lowercase rows to UPPERCASE; zero violating rows remain")
    void legacyLowercaseRowsNormalizedToUppercase() throws SQLException {
        List<String> statuses = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT status FROM attendance WHERE notes = ? ORDER BY id")) {
            ps.setString(1, SEED_TAG);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    statuses.add(rs.getString(1));
                }
            }
        }
        assertThat(statuses)
                .as("the 4 seeded lowercase rows must now be UPPERCASE")
                .containsExactly("PRESENT", "ABSENT", "LATE", "EXCUSED");

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM attendance WHERE status IS NOT NULL AND status <> UPPER(status)");
                ResultSet rs = ps.executeQuery()) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt(1)).as("no lowercase status row may survive V87").isZero();
        }
    }

    @Test
    @DisplayName("chk_attendance_status exists, allows MAKEUP, forbids lowercase (UPPERCASE-only)")
    void chkConstraintIsUppercaseEnumWithMakeup() throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT pg_get_constraintdef(oid) FROM pg_constraint "
                        + "WHERE conname = 'chk_attendance_status' "
                        + "AND conrelid = 'attendance'::regclass");
                ResultSet rs = ps.executeQuery()) {
            assertThat(rs.next()).as("chk_attendance_status must exist on attendance").isTrue();
            String def = rs.getString(1);
            assertThat(def)
                    .as("constraint must enumerate UPPERCASE values incl. MAKEUP")
                    .contains("PRESENT")
                    .contains("MAKEUP");
            assertThat(def)
                    .as("constraint must NOT allow the legacy lowercase value")
                    .doesNotContain("'present'");
        }
    }

    @Test
    @DisplayName("After V87, a new UPPERCASE row (incl. MAKEUP) is accepted")
    void uppercaseStatusAcceptedAfterV87() throws SQLException {
        insertAttendance("MAKEUP", "GAP-1066-ADHOC"); // new value introduced by V87
        insertAttendance("PRESENT", "GAP-1066-ADHOC");
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM attendance WHERE status IN ('MAKEUP', 'PRESENT')");
                ResultSet rs = ps.executeQuery()) {
            assertThat(rs.next()).isTrue();
            // 1 normalized 'present'->'PRESENT' seed + 1 new PRESENT + 1 MAKEUP = 3
            assertThat(rs.getInt(1)).isGreaterThanOrEqualTo(2);
        }
    }

    @Test
    @DisplayName("After V87, a lowercase status row is rejected by chk_attendance_status")
    void lowercaseStatusRejectedAfterV87() {
        assertThatThrownBy(() -> insertAttendance("present", "GAP-1066-ADHOC"))
                .as("chk_attendance_status must reject legacy lowercase after V87")
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("chk_attendance_status");
    }

    @Test
    @DisplayName("V87 normalize UPDATE is idempotent — re-running it changes 0 rows")
    void normalizeUpdateIsIdempotent() throws SQLException {
        // The exact statement V87 runs. After V87 every row is already UPPERCASE, so re-running
        // it must be a no-op (durable + safe on reseed) — the idempotency the gap requires.
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE attendance SET status = UPPER(status) "
                        + "WHERE status IS NOT NULL AND status <> UPPER(status)")) {
            int changed = ps.executeUpdate();
            assertThat(changed).as("normalize must be idempotent after V87").isZero();
        }
    }

    // ----- fixture helper (raw JDBC, FK triggers disabled) -----

    /**
     * Inserts an attendance row with the given status. {@code session_id} / {@code student_id} are
     * NOT NULL (until V87 drops student_id NOT NULL) so dummy values are supplied; FK triggers are
     * disabled for the connection so no parent rows are needed. CHECK / NOT NULL constraints are
     * still enforced, so this faithfully exercises {@code chk_attendance_status}.
     */
    private void insertAttendance(String status, String notes) throws SQLException {
        long fk = fkSeq.incrementAndGet();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO attendance (instance_id, session_id, student_id, status, notes) "
                        + "VALUES (?, ?, ?, ?, ?)")) {
            ps.setObject(1, TENANT);
            ps.setLong(2, fk);
            ps.setLong(3, fk);
            ps.setString(4, status);
            ps.setString(5, notes);
            ps.executeUpdate();
        }
    }
}
