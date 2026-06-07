package com.kiteclass.core.module.invoice.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kiteclass.core.common.constant.InvoiceItemType;
import com.kiteclass.core.common.constant.InvoiceStatus;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * GAP-882 — verifies invoice enum CHECK constraints on the REAL Flyway-migrated schema.
 *
 * <p>Why this is NOT a normal {@code @DataJpaTest}: the project's test profile runs with
 * {@code spring.flyway.enabled=false} + {@code ddl-auto=create-drop}, so Hibernate generates
 * the schema from entities — which carry NO CHECK constraint. Such tests are blind to the
 * schema-vs-entity drift this gap is about. This test instead runs the full Flyway migration
 * chain (V1..V92) against a fresh Testcontainers PostgreSQL, then exercises the CHECK
 * constraints via raw JDBC — the production-equivalent path.
 *
 * <p>Parity guard: the happy-path tests iterate {@link InvoiceStatus#values()} and
 * {@link InvoiceItemType#values()} so any future Java enum value added without a matching
 * migration CHECK update will fail here.
 */
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InvoiceEnumCheckConstraintTest {

    private static final UUID INSTANCE_ID = UUID.fromString("00000000-0000-0000-0000-0000000000a1");

    @Container
    @SuppressWarnings("resource") // lifecycle managed by @Container + JVM shutdown hook
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("kiteclass_gap882")
                    .withUsername("test")
                    .withPassword("test");

    private Connection conn;
    private long studentId;
    private final AtomicInteger seq = new AtomicInteger();

    @BeforeAll
    void migrateAndConnect() throws SQLException {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load()
                .migrate();

        conn = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());

        // Defensive: set tenant GUC so RLS-FORCEd tables accept writes even for non-superuser
        // roles (Testcontainers default user is superuser → already bypasses RLS, this is belt
        // and suspenders so the fixture is robust to image/role changes).
        try (Statement st = conn.createStatement()) {
            st.execute("SET app.current_tenant_id = '" + INSTANCE_ID + "'");
        }
        studentId = insertStudent();
    }

    @AfterAll
    void closeConnection() throws SQLException {
        if (conn != null) {
            conn.close();
        }
    }

    @Test
    void allInvoiceStatusEnumValuesPersistOnFlywaySchema() throws SQLException {
        for (InvoiceStatus status : InvoiceStatus.values()) {
            long id = insertInvoice(status.name());
            assertThat(readInvoiceStatus(id))
                    .as("invoices.status round-trip for %s", status)
                    .isEqualTo(status.name());
        }
    }

    @Test
    void lowercaseInvoiceStatusRejectedByCheckConstraint() {
        assertThatThrownBy(() -> insertInvoice("pending"))
                .as("chk_invoices_status must reject legacy lowercase value")
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("chk_invoices_status");
    }

    @Test
    void allInvoiceItemTypeEnumValuesPersistOnFlywaySchema() throws SQLException {
        long invoiceId = insertInvoice(InvoiceStatus.DRAFT.name());
        for (InvoiceItemType type : InvoiceItemType.values()) {
            long itemId = insertInvoiceItem(invoiceId, type.name());
            assertThat(readItemType(itemId))
                    .as("invoice_items.item_type round-trip for %s", type)
                    .isEqualTo(type.name());
        }
        // item_type is nullable — NULL must be accepted.
        long nullItemId = insertInvoiceItem(invoiceId, null);
        assertThat(readItemType(nullItemId)).as("NULL item_type allowed").isNull();
    }

    @Test
    void invalidInvoiceItemTypeRejectedByCheckConstraint() throws SQLException {
        long invoiceId = insertInvoice(InvoiceStatus.SENT.name());
        assertThatThrownBy(() -> insertInvoiceItem(invoiceId, "GARBAGE"))
                .as("chk_invoice_items_type must reject non-canonical value")
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("chk_invoice_items_type");
    }

    @Test
    void lowercaseInvoiceItemTypeRejectedByCheckConstraint() throws SQLException {
        long invoiceId = insertInvoice(InvoiceStatus.SENT.name());
        assertThatThrownBy(() -> insertInvoiceItem(invoiceId, "tuition"))
                .as("chk_invoice_items_type must reject legacy lowercase value")
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("chk_invoice_items_type");
    }

    // ----- fixture helpers (raw JDBC) -----

    private long insertStudent() throws SQLException {
        String sql = "INSERT INTO students (instance_id, name, status) VALUES (?, ?, 'ACTIVE') RETURNING id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, INSTANCE_ID);
            ps.setString(2, "GAP-882 Fixture Student");
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private long insertInvoice(String status) throws SQLException {
        String sql =
                "INSERT INTO invoices "
                + "(instance_id, invoice_number, student_id, period_start, period_end, "
                + " subtotal, discount, total, amount_paid, due_date, status) "
                + "VALUES (?, ?, ?, CURRENT_DATE, CURRENT_DATE, 0, 0, 0, 0, CURRENT_DATE, ?) "
                + "RETURNING id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, INSTANCE_ID);
            ps.setString(2, "INV-GAP882-" + seq.incrementAndGet());
            ps.setLong(3, studentId);
            ps.setString(4, status);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private long insertInvoiceItem(long invoiceId, String itemType) throws SQLException {
        String sql =
                "INSERT INTO invoice_items (invoice_id, description, quantity, unit_price, amount, item_type) "
                + "VALUES (?, ?, 1, 100.00, 100.00, ?) RETURNING id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, invoiceId);
            ps.setString(2, "GAP-882 line item");
            if (itemType == null) {
                ps.setNull(3, java.sql.Types.VARCHAR);
            } else {
                ps.setString(3, itemType);
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private String readInvoiceStatus(long id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT status FROM invoices WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getString(1);
            }
        }
    }

    private String readItemType(long id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT item_type FROM invoice_items WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getString(1);
            }
        }
    }
}
