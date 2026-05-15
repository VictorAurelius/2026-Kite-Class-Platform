package com.kiteclass.core.common.datasource;

import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.config.TestContainersConfiguration;
import com.kiteclass.core.config.TestSecurityConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Wave 85 Bucket B — verifies the 4 P0 CRITICAL AC enhancements added on top of V58 RLS:
 *
 * <ol>
 *   <li><b>B-AC8</b> NULL force-fail — RLS policy rejects queries when GUC unset/empty,
 *       no silent fallback to default tenant.</li>
 *   <li><b>B-AC7</b> Admin-bypass clause — when GUC {@code app.is_platform_admin=true},
 *       policy returns rows across tenants (for support workflows).</li>
 *   <li><b>B-AC6</b> HikariCP {@code connection-init-sql} RESET — fresh connection
 *       starts with empty GUC (defense-in-depth on top of {@code SET LOCAL} tx-scope).</li>
 *   <li><b>B-AC2</b> Immutable {@code admin_audit_logs} — UPDATE/DELETE blocked by RLS
 *       even by the table owner; INSERT-only append.</li>
 * </ol>
 *
 * Test approach mirrors {@link RLSEnforcementIT}: applies the V59-equivalent policy
 * programmatically because kc-core test profile disables Flyway.
 *
 * @since Wave 85 Bucket B / GAP-466 / GAP-577-prep
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestContainersConfiguration.class, TestSecurityConfig.class})
@ContextConfiguration(initializers = TestContainersConfiguration.Initializer.class)
class RLSHardeningIT {

    private static final String TABLE = "students";
    private static final String AUDIT_TABLE = "admin_audit_logs";
    private static final String RLS_TEST_ROLE = "kite_rls_hardening_test_role";

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeAll
    static void provisionHardenedRls(@Autowired DataSource ds) {
        try (Connection raw = ds.getConnection()) {
            raw.setAutoCommit(true);

            // V58 baseline equivalent
            execIgnoreError(raw, "ALTER TABLE " + TABLE + " ENABLE ROW LEVEL SECURITY");
            execIgnoreError(raw, "ALTER TABLE " + TABLE + " FORCE ROW LEVEL SECURITY");
            execIgnoreError(raw, "DROP POLICY IF EXISTS tenant_isolation ON " + TABLE);

            // V59-equivalent policy with admin-bypass + NULL force-fail
            execIgnoreError(raw,
                "CREATE POLICY tenant_isolation ON " + TABLE + " " +
                "USING (" +
                "    COALESCE(current_setting('app.is_platform_admin', true)::boolean, false) " +
                "    OR instance_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid" +
                ") " +
                "WITH CHECK (" +
                "    COALESCE(current_setting('app.is_platform_admin', true)::boolean, false) " +
                "    OR instance_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid" +
                ")"
            );

            // V60-equivalent admin_audit_logs table + immutability policies
            execIgnoreError(raw,
                "CREATE TABLE IF NOT EXISTS " + AUDIT_TABLE + " (" +
                "  id UUID PRIMARY KEY, " +
                "  admin_id UUID NOT NULL, " +
                "  admin_email VARCHAR(255) NOT NULL, " +
                "  action VARCHAR(64) NOT NULL, " +
                "  target_tenant_id UUID, " +
                "  target_resource VARCHAR(512), " +
                "  payload_jsonb JSONB, " +
                "  client_ip VARCHAR(64), " +
                "  user_agent TEXT, " +
                "  created_at TIMESTAMP NOT NULL DEFAULT NOW()" +
                ")"
            );
            execIgnoreError(raw, "ALTER TABLE " + AUDIT_TABLE + " ENABLE ROW LEVEL SECURITY");
            execIgnoreError(raw, "ALTER TABLE " + AUDIT_TABLE + " FORCE ROW LEVEL SECURITY");
            execIgnoreError(raw, "DROP POLICY IF EXISTS admin_audit_select ON " + AUDIT_TABLE);
            execIgnoreError(raw, "CREATE POLICY admin_audit_select ON " + AUDIT_TABLE + " FOR SELECT USING (true)");
            execIgnoreError(raw, "DROP POLICY IF EXISTS admin_audit_insert ON " + AUDIT_TABLE);
            execIgnoreError(raw, "CREATE POLICY admin_audit_insert ON " + AUDIT_TABLE + " FOR INSERT WITH CHECK (true)");
            execIgnoreError(raw, "DROP POLICY IF EXISTS admin_audit_no_update ON " + AUDIT_TABLE);
            execIgnoreError(raw, "CREATE POLICY admin_audit_no_update ON " + AUDIT_TABLE + " FOR UPDATE USING (false) WITH CHECK (false)");
            execIgnoreError(raw, "DROP POLICY IF EXISTS admin_audit_no_delete ON " + AUDIT_TABLE);
            execIgnoreError(raw, "CREATE POLICY admin_audit_no_delete ON " + AUDIT_TABLE + " FOR DELETE USING (false)");

            // NOSUPERUSER role to actually trigger RLS enforcement
            execIgnoreError(raw,
                "DO $$ BEGIN " +
                "  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '" + RLS_TEST_ROLE + "') THEN " +
                "    CREATE ROLE " + RLS_TEST_ROLE + " NOSUPERUSER NOBYPASSRLS NOINHERIT; " +
                "  END IF; " +
                "END $$;"
            );
            execIgnoreError(raw, "GRANT SELECT, INSERT, UPDATE, DELETE ON " + TABLE + " TO " + RLS_TEST_ROLE);
            execIgnoreError(raw, "GRANT SELECT, INSERT, UPDATE, DELETE ON " + AUDIT_TABLE + " TO " + RLS_TEST_ROLE);
            execIgnoreError(raw, "GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO " + RLS_TEST_ROLE);

        } catch (Exception e) {
            throw new IllegalStateException("Failed to apply hardened RLS in test setup", e);
        }
    }

    private static void execIgnoreError(Connection raw, String sql) {
        try (PreparedStatement ps = raw.prepareStatement(sql)) {
            ps.execute();
        } catch (Exception e) {
            // Defensive: some statements may fail if table doesn't exist; surface to log via stderr.
            System.err.println("[RLSHardeningIT] Setup statement failed (continuing): " + sql + " — " + e.getMessage());
        }
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ========== B-AC8: NULL force-fail — no silent fallback ==========

    @Test
    @DisplayName("B-AC8: query with NULL GUC → 0 rows (no fallback to default tenant)")
    void bAc8_nullGucForcesDefaultDeny() {
        UUID tenantA = UUID.randomUUID();
        seedStudent(tenantA, "ForceFail-Probe");

        // Explicitly DO NOT set tenant context. With strengthened V59 policy,
        // NULLIF('','')::uuid returns NULL, and `instance_id = NULL` returns NULL (not TRUE).
        // No rows visible. Default-deny preserved.
        Long count = transactionTemplate.execute(status -> {
            // Mirror connection-init-sql RESET behavior — set GUC to empty string
            entityManager.createNativeQuery("SELECT set_config('app.current_tenant_id', '', true)")
                .getSingleResult();
            entityManager.createNativeQuery("SET LOCAL ROLE " + RLS_TEST_ROLE).executeUpdate();
            return ((Number) entityManager
                .createNativeQuery("SELECT count(*) FROM " + TABLE)
                .getSingleResult()
            ).longValue();
        });

        assertThat(count)
            .as("Empty GUC must NOT be coalesced to a default tenant — must surface as zero rows")
            .isZero();
    }

    // ========== B-AC7: Admin-bypass clause ==========

    @Test
    @DisplayName("B-AC7: admin-bypass GUC=true → sees rows across tenants")
    void bAc7_adminBypassSeesAllTenants() {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        seedStudent(tenantA, "A-only");
        seedStudent(tenantB, "B-only");

        Long visibleToAdmin = transactionTemplate.execute(status -> {
            // Admin bypass: set is_platform_admin=true, leave tenant_id empty
            entityManager.createNativeQuery("SELECT set_config('app.is_platform_admin', 'true', true)")
                .getSingleResult();
            entityManager.createNativeQuery("SELECT set_config('app.current_tenant_id', '', true)")
                .getSingleResult();
            entityManager.createNativeQuery("SET LOCAL ROLE " + RLS_TEST_ROLE).executeUpdate();
            return ((Number) entityManager
                .createNativeQuery("SELECT count(*) FROM " + TABLE +
                    " WHERE instance_id IN (:a, :b)")
                .setParameter("a", tenantA)
                .setParameter("b", tenantB)
                .getSingleResult()
            ).longValue();
        });

        assertThat(visibleToAdmin)
            .as("Platform admin with bypass GUC should see both tenant rows")
            .isEqualTo(2L);
    }

    @Test
    @DisplayName("B-AC7: admin-bypass GUC=false (default) → does NOT see other-tenant rows")
    void bAc7_nonAdminCannotBypass() {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        seedStudent(tenantB, "B-only");

        Long visibleAsTenantA = transactionTemplate.execute(status -> {
            entityManager.createNativeQuery("SELECT set_config('app.is_platform_admin', 'false', true)")
                .getSingleResult();
            entityManager.createNativeQuery("SELECT set_config('app.current_tenant_id', :tid, true)")
                .setParameter("tid", tenantA.toString())
                .getSingleResult();
            entityManager.createNativeQuery("SET LOCAL ROLE " + RLS_TEST_ROLE).executeUpdate();
            return ((Number) entityManager
                .createNativeQuery("SELECT count(*) FROM " + TABLE +
                    " WHERE instance_id = :b")
                .setParameter("b", tenantB)
                .getSingleResult()
            ).longValue();
        });

        assertThat(visibleAsTenantA)
            .as("Non-admin user as tenant A must not see tenant B's rows")
            .isZero();
    }

    // ========== B-AC6: HikariCP connection-init-sql RESET ==========

    @Test
    @DisplayName("B-AC6: fresh pool connection starts with empty GUC (no leak from prior tenant)")
    void bAc6_freshConnectionStartsWithEmptyGuc() throws Exception {
        UUID tenantA = UUID.randomUUID();
        TenantContext.setCurrentTenant(tenantA);

        // Run a tenant-A transaction so SET LOCAL fires
        transactionTemplate.execute(status -> {
            entityManager.createNativeQuery("SELECT set_config('app.current_tenant_id', :tid, true)")
                .setParameter("tid", tenantA.toString())
                .getSingleResult();
            entityManager.createNativeQuery("SELECT 1").getSingleResult();
            return null;
        });

        TenantContext.clear();

        // Pop a fresh connection and verify GUC is empty/null (HikariCP connection-init-sql
        // ran SELECT set_config(...,'',false) when the physical connection was created;
        // SET LOCAL of prior transaction cleared at commit; therefore fresh checkout = clean state)
        try (Connection raw = dataSource.getConnection();
             PreparedStatement ps = raw.prepareStatement(
                 "SELECT current_setting('app.current_tenant_id', true), " +
                 "       current_setting('app.is_platform_admin', true)")) {
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).isTrue();
                String tenantGuc = rs.getString(1);
                String adminGuc = rs.getString(2);
                assertThat(tenantGuc)
                    .as("Tenant GUC must not leak — fresh checkout should be empty/null")
                    .satisfiesAnyOf(
                        v -> assertThat(v).isNull(),
                        v -> assertThat(v).isEmpty()
                    );
                assertThat(adminGuc)
                    .as("Admin-bypass GUC must default to false/empty on fresh checkout")
                    .satisfiesAnyOf(
                        v -> assertThat(v).isNull(),
                        v -> assertThat(v).isEmpty(),
                        v -> assertThat(v).isEqualTo("false")
                    );
            }
        }
    }

    // ========== B-AC2: Immutable admin_audit_logs ==========

    @Test
    @DisplayName("B-AC2: admin_audit_logs INSERT succeeds, UPDATE rejected, DELETE rejected (PDPL Art 11)")
    void bAc2_adminAuditLogsImmutable() {
        UUID rowId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        // INSERT should succeed
        transactionTemplate.executeWithoutResult(status -> {
            entityManager.createNativeQuery("SET LOCAL ROLE " + RLS_TEST_ROLE).executeUpdate();
            int inserted = entityManager.createNativeQuery(
                "INSERT INTO " + AUDIT_TABLE + " (id, admin_id, admin_email, action, target_tenant_id, target_resource, created_at) " +
                "VALUES (:id, :aid, :em, :act, :tt, :tr, NOW())"
            )
                .setParameter("id", rowId)
                .setParameter("aid", adminId)
                .setParameter("em", "admin@kitehub.me")
                .setParameter("act", "READ_TENANT")
                .setParameter("tt", UUID.randomUUID())
                .setParameter("tr", "students/test-resource")
                .executeUpdate();
            assertThat(inserted).as("Admin audit INSERT should succeed").isEqualTo(1);
        });

        // UPDATE should be blocked by RLS policy (predicate false → 0 rows updated)
        Integer updated = transactionTemplate.execute(status -> {
            entityManager.createNativeQuery("SET LOCAL ROLE " + RLS_TEST_ROLE).executeUpdate();
            return entityManager.createNativeQuery(
                "UPDATE " + AUDIT_TABLE + " SET action = 'TAMPERED' WHERE id = :id"
            ).setParameter("id", rowId).executeUpdate();
        });
        assertThat(updated)
            .as("Admin audit UPDATE must be blocked by RLS policy (PDPL Art 11 immutability)")
            .isZero();

        // Verify row still has original action value (no tampering succeeded)
        String storedAction = transactionTemplate.execute(status -> {
            entityManager.createNativeQuery("SET LOCAL ROLE " + RLS_TEST_ROLE).executeUpdate();
            return (String) entityManager.createNativeQuery(
                "SELECT action FROM " + AUDIT_TABLE + " WHERE id = :id"
            ).setParameter("id", rowId).getSingleResult();
        });
        assertThat(storedAction)
            .as("Original action value must persist after blocked UPDATE")
            .isEqualTo("READ_TENANT");

        // DELETE should be blocked by RLS policy (predicate false → 0 rows deleted)
        Integer deleted = transactionTemplate.execute(status -> {
            entityManager.createNativeQuery("SET LOCAL ROLE " + RLS_TEST_ROLE).executeUpdate();
            return entityManager.createNativeQuery(
                "DELETE FROM " + AUDIT_TABLE + " WHERE id = :id"
            ).setParameter("id", rowId).executeUpdate();
        });
        assertThat(deleted)
            .as("Admin audit DELETE must be blocked by RLS policy (PDPL Art 11 immutability)")
            .isZero();

        // Verify row still exists
        Long rowsRemaining = transactionTemplate.execute(status -> {
            entityManager.createNativeQuery("SET LOCAL ROLE " + RLS_TEST_ROLE).executeUpdate();
            return ((Number) entityManager.createNativeQuery(
                "SELECT count(*) FROM " + AUDIT_TABLE + " WHERE id = :id"
            ).setParameter("id", rowId).getSingleResult()).longValue();
        });
        assertThat(rowsRemaining)
            .as("Row must persist after blocked DELETE")
            .isEqualTo(1L);
    }

    // ========== Helper: seed student row bypassing RLS for fixture setup ==========

    private Long seedStudent(UUID tenantId, String name) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement setGuc = conn.prepareStatement(
                "SELECT set_config('app.current_tenant_id', ?, true)")) {
                setGuc.setString(1, tenantId.toString());
                setGuc.executeQuery().close();
            }
            try (PreparedStatement insert = conn.prepareStatement(
                "INSERT INTO " + TABLE + " (instance_id, name, email, phone, status, deleted, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, 'ACTIVE', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) RETURNING id"
            )) {
                insert.setObject(1, tenantId);
                insert.setString(2, name);
                insert.setString(3, "rls-hardening-" + UUID.randomUUID() + "@example.test");
                insert.setString(4, "0900000000");
                try (ResultSet rs = insert.executeQuery()) {
                    if (rs.next()) {
                        Long id = rs.getLong(1);
                        conn.commit();
                        return id;
                    }
                }
            }
            conn.rollback();
            fail("Failed to seed student row for tenant " + tenantId);
            return null;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to seed student for tenant " + tenantId, e);
        }
    }
}
