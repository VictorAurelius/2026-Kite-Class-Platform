package com.kiteclass.core.common.security;

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
 * Multi-tenant RLS negative tests — verify V58/V59 Row-Level Security policies
 * BLOCK cross-tenant mutations even when SQL syntax targets foreign-tenant rows
 * explicitly via raw native SQL bypassing Hibernate {@code @Filter}.
 *
 * <p>Wave beta-prep-1 Bucket B item 5 AC: "min 3 negative scenarios via real
 * Postgres + RLS V60: SELECT cross-tenant (empty), UPDATE cross-tenant
 * (0 rows affected), DELETE cross-tenant (0 rows affected)".
 *
 * <p>Sister coverage to {@link com.kiteclass.core.common.datasource.RLSHardeningIT}
 * (NULL force-fail / admin-bypass / GUC reset / immutable audit log). This class
 * focuses negative mutation paths — verify attacker constructing raw UPDATE/DELETE
 * WHERE clause naming foreign tenant_id KHÔNG reach those rows.
 *
 * <p>Setup follows Wave 85 hardened RLS provisioning từ {@link
 * com.kiteclass.core.common.datasource.RLSHardeningIT} — applies V59-equivalent
 * policy programmatically vì test profile disables Flyway.
 *
 * @since Wave beta-prep-1 Bucket B (security-beta-min)
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestContainersConfiguration.class, TestSecurityConfig.class})
@ContextConfiguration(initializers = TestContainersConfiguration.Initializer.class)
@DisplayName("MultiTenantRLSNegativeIT — cross-tenant SELECT/UPDATE/DELETE blocked (Wave beta-prep-1 Bucket B)")
class MultiTenantRLSNegativeIT {

    private static final String TABLE = "students";
    private static final String RLS_TEST_ROLE = "kite_rls_neg_test_role";

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
            execIgnoreError(raw, "ALTER TABLE " + TABLE + " ENABLE ROW LEVEL SECURITY");
            execIgnoreError(raw, "ALTER TABLE " + TABLE + " FORCE ROW LEVEL SECURITY");
            execIgnoreError(raw, "DROP POLICY IF EXISTS tenant_isolation ON " + TABLE);
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
            execIgnoreError(raw,
                "DO $$ BEGIN " +
                "  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '" + RLS_TEST_ROLE + "') THEN " +
                "    CREATE ROLE " + RLS_TEST_ROLE + " NOSUPERUSER NOBYPASSRLS NOINHERIT; " +
                "  END IF; " +
                "END $$;"
            );
            execIgnoreError(raw, "GRANT SELECT, INSERT, UPDATE, DELETE ON " + TABLE + " TO " + RLS_TEST_ROLE);
            execIgnoreError(raw, "GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO " + RLS_TEST_ROLE);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to provision RLS in negative test setup", e);
        }
    }

    private static void execIgnoreError(Connection raw, String sql) {
        try (PreparedStatement ps = raw.prepareStatement(sql)) {
            ps.execute();
        } catch (Exception e) {
            System.err.println("[MultiTenantRLSNegativeIT] Setup statement skipped: " + sql
                    + " — " + e.getMessage());
        }
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("SELECT cross-tenant via raw native SQL -> 0 rows (RLS USING clause blocks)")
    void selectCrossTenant_returnsEmpty() {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        seedStudent(tenantA, "Tran Thi Hong (Tenant A)");
        seedStudent(tenantB, "Nguyen Van An (Tenant B)");

        Long visibleAsTenantA = transactionTemplate.execute(status -> {
            entityManager.createNativeQuery("SELECT set_config('app.current_tenant_id', :tid, true)")
                .setParameter("tid", tenantA.toString())
                .getSingleResult();
            entityManager.createNativeQuery("SET LOCAL ROLE " + RLS_TEST_ROLE).executeUpdate();

            return ((Number) entityManager
                .createNativeQuery("SELECT count(*) FROM " + TABLE + " WHERE instance_id = :b")
                .setParameter("b", tenantB)
                .getSingleResult()
            ).longValue();
        });

        assertThat(visibleAsTenantA)
            .as("RLS policy USING clause should reject SELECT for foreign tenant_id")
            .isZero();
    }

    @Test
    @DisplayName("UPDATE cross-tenant via raw native SQL -> 0 rows affected (USING + WITH CHECK)")
    void updateCrossTenant_affectsZeroRows() {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        Long studentBId = seedStudent(tenantB, "Pham Thi Mai (Tenant B target)");

        Integer rowsAffected = transactionTemplate.execute(status -> {
            entityManager.createNativeQuery("SELECT set_config('app.current_tenant_id', :tid, true)")
                .setParameter("tid", tenantA.toString())
                .getSingleResult();
            entityManager.createNativeQuery("SET LOCAL ROLE " + RLS_TEST_ROLE).executeUpdate();

            return entityManager
                .createNativeQuery("UPDATE " + TABLE + " SET name = 'PWNED' WHERE id = :id")
                .setParameter("id", studentBId)
                .executeUpdate();
        });

        assertThat(rowsAffected)
            .as("RLS USING clause should make foreign-tenant row invisible to UPDATE")
            .isZero();

        String currentName = (String) transactionTemplate.execute(status -> {
            entityManager.createNativeQuery("SELECT set_config('app.current_tenant_id', :tid, true)")
                .setParameter("tid", tenantB.toString())
                .getSingleResult();
            entityManager.createNativeQuery("SET LOCAL ROLE " + RLS_TEST_ROLE).executeUpdate();
            return entityManager
                .createNativeQuery("SELECT name FROM " + TABLE + " WHERE id = :id")
                .setParameter("id", studentBId)
                .getSingleResult();
        });

        assertThat(currentName)
            .as("Tenant B's row must remain pristine after attempted cross-tenant UPDATE")
            .isEqualTo("Pham Thi Mai (Tenant B target)");
    }

    @Test
    @DisplayName("DELETE cross-tenant via raw native SQL -> 0 rows affected (USING clause)")
    void deleteCrossTenant_affectsZeroRows() {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        Long studentBId = seedStudent(tenantB, "Le Van Quang (Tenant B persistent)");

        Integer rowsAffected = transactionTemplate.execute(status -> {
            entityManager.createNativeQuery("SELECT set_config('app.current_tenant_id', :tid, true)")
                .setParameter("tid", tenantA.toString())
                .getSingleResult();
            entityManager.createNativeQuery("SET LOCAL ROLE " + RLS_TEST_ROLE).executeUpdate();

            return entityManager
                .createNativeQuery("DELETE FROM " + TABLE + " WHERE id = :id")
                .setParameter("id", studentBId)
                .executeUpdate();
        });

        assertThat(rowsAffected)
            .as("RLS USING clause should make foreign-tenant row invisible to DELETE")
            .isZero();

        Long stillExists = (Long) transactionTemplate.execute(status -> {
            entityManager.createNativeQuery("SELECT set_config('app.current_tenant_id', :tid, true)")
                .setParameter("tid", tenantB.toString())
                .getSingleResult();
            entityManager.createNativeQuery("SET LOCAL ROLE " + RLS_TEST_ROLE).executeUpdate();
            return ((Number) entityManager
                .createNativeQuery("SELECT count(*) FROM " + TABLE + " WHERE id = :id")
                .setParameter("id", studentBId)
                .getSingleResult()
            ).longValue();
        });

        assertThat(stillExists)
            .as("Tenant B's row must survive attempted cross-tenant DELETE attack")
            .isEqualTo(1L);
    }

    private Long seedStudent(UUID tenantId, String name) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement setGuc = conn.prepareStatement(
                "SELECT set_config('app.current_tenant_id', ?, true)")) {
                setGuc.setString(1, tenantId.toString());
                setGuc.executeQuery().close();
            }
            try (PreparedStatement insert = conn.prepareStatement(
                "INSERT INTO " + TABLE + " (instance_id, name, email, phone, status, " +
                "deleted, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, 'ACTIVE', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) " +
                "RETURNING id"
            )) {
                insert.setObject(1, tenantId);
                insert.setString(2, name);
                insert.setString(3, "rls-neg-" + UUID.randomUUID() + "@example.test");
                insert.setString(4, "0901234567");
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
