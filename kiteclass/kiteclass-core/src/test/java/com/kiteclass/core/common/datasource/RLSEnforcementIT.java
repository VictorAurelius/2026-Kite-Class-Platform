package com.kiteclass.core.common.datasource;

import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.config.TestContainersConfiguration;
import com.kiteclass.core.config.TestSecurityConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that Postgres Row-Level Security (RLS) policies created by
 * {@code V58__enable_rls_tenant_scoped_tables.sql} actually filter rows at the DB layer,
 * even for query paths that bypass the Hibernate {@code tenantFilter} interceptor
 * (e.g. native SQL via {@link EntityManager#createNativeQuery}).
 *
 * <p>The {@link TenantAwareDataSourceInterceptor} aspect issues
 * {@code SET LOCAL app.current_tenant_id} at every {@code @Transactional} boundary;
 * combined with the RLS policy {@code USING (instance_id = current_setting(...)::uuid)},
 * any row whose tenant does not match is invisible to the current transaction.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>{@link #shouldRejectQueryWithoutTenantContext()}</li>
 *   <li>{@link #shouldNotLeakCrossTenant()}</li>
 *   <li>{@link #shouldEnforceOnNativeSql()}</li>
 *   <li>{@link #shouldClearTenantOnConnectionRelease()}</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since GAP-466 / Wave 56
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestContainersConfiguration.class, TestSecurityConfig.class})
@ContextConfiguration(initializers = TestContainersConfiguration.Initializer.class)
class RLSEnforcementIT {

    private static final String TABLE = "students";

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private static final String RLS_TEST_ROLE = "kite_rls_test_role";

    @org.junit.jupiter.api.BeforeAll
    static void provisionRlsTestRole(@Autowired DataSource ds) {
        // NOTE: kc-core test profile (`application-test.yml`) disables Flyway and uses
        // `ddl-auto: create-drop`, so V58__enable_rls_tenant_scoped_tables.sql does NOT
        // run automatically. We apply the policy programmatically here so the test is
        // self-contained AND verifies the same SQL pattern that the migration ships.
        try (Connection raw = ds.getConnection()) {
            raw.setAutoCommit(true);
            try (PreparedStatement enableRls = raw.prepareStatement(
                "ALTER TABLE " + TABLE + " ENABLE ROW LEVEL SECURITY")) {
                enableRls.execute();
            }
            try (PreparedStatement forceRls = raw.prepareStatement(
                "ALTER TABLE " + TABLE + " FORCE ROW LEVEL SECURITY")) {
                forceRls.execute();
            }
            try (PreparedStatement drop = raw.prepareStatement(
                "DROP POLICY IF EXISTS tenant_isolation ON " + TABLE)) {
                drop.execute();
            }
            try (PreparedStatement create = raw.prepareStatement(
                "CREATE POLICY tenant_isolation ON " + TABLE + " " +
                "USING (instance_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid) " +
                "WITH CHECK (instance_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid)")) {
                create.execute();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to apply RLS policy in test setup", e);
        }

        // Postgres superusers (and roles with BYPASSRLS) bypass RLS even under FORCE ROW
        // LEVEL SECURITY (per PG docs). The Testcontainers test user is a superuser, so we
        // create a stripped-down `kite_rls_test_role` that the test transactions then
        // `SET ROLE` into. This mirrors production where the app's DB role is
        // NOSUPERUSER + NOBYPASSRLS.
        try (Connection raw = ds.getConnection()) {
            raw.setAutoCommit(true);
            try (PreparedStatement create = raw.prepareStatement(
                "DO $$ BEGIN " +
                "  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '" + RLS_TEST_ROLE + "') THEN " +
                "    CREATE ROLE " + RLS_TEST_ROLE + " NOSUPERUSER NOBYPASSRLS NOINHERIT; " +
                "  END IF; " +
                "END $$;")) {
                create.execute();
            }
            // Grant the minimum DML privileges the tests need on the students table.
            try (PreparedStatement grant = raw.prepareStatement(
                "GRANT SELECT, INSERT, UPDATE, DELETE ON " + TABLE + " TO " + RLS_TEST_ROLE)) {
                grant.execute();
            }
            try (PreparedStatement grantSeq = raw.prepareStatement(
                "GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO " + RLS_TEST_ROLE)) {
                grantSeq.execute();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to provision RLS test role", e);
        }
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Query without tenant context returns zero rows (RLS default-deny)")
    void shouldRejectQueryWithoutTenantContext() {
        UUID tenantA = UUID.randomUUID();
        seedStudent(tenantA, "Default-Deny Probe");

        // No TenantContext set → aspect skips SET LOCAL → GUC is NULL → policy filters everything.
        TenantContext.clear();

        Long count = transactionTemplate.execute(status -> {
            mirrorAspectGucSet();
            setLocalRoleToRlsTestRole();
            return ((Number) entityManager
                .createNativeQuery("SELECT count(*) FROM " + TABLE)
                .getSingleResult()
            ).longValue();
        });

        assertThat(count)
            .as("With no tenant context, RLS should hide every row")
            .isZero();
    }

    private void setLocalRoleToRlsTestRole() {
        // Switch the current transaction's role to a NOSUPERUSER + NOBYPASSRLS role so RLS
        // actually applies. SET LOCAL means the role reverts at commit/rollback.
        entityManager.createNativeQuery("SET LOCAL ROLE " + RLS_TEST_ROLE).executeUpdate();
    }

    /**
     * The {@link TenantAwareDataSourceInterceptor} aspect fires only on
     * {@code @Transactional}-annotated methods, NOT inside a raw {@link TransactionTemplate}
     * lambda. To exercise RLS with realistic semantics from these tests we manually invoke
     * the same {@code SET LOCAL app.current_tenant_id} that the aspect would have issued.
     */
    private void mirrorAspectGucSet() {
        if (!TenantContext.isSet()) {
            return; // Default-deny path; leave GUC NULL.
        }
        entityManager
            .createNativeQuery("SELECT set_config('app.current_tenant_id', :tid, true)")
            .setParameter("tid", TenantContext.getCurrentTenant().toString())
            .getSingleResult();
    }

    @Test
    @DisplayName("Tenant A query for Tenant B's data returns zero rows (cross-tenant leak prevented)")
    void shouldNotLeakCrossTenant() {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        Long studentBId = seedStudent(tenantB, "Tenant-B Student");
        assertThat(studentBId).isNotNull();

        TenantContext.setCurrentTenant(tenantA);
        Long visibleBRowsForA = transactionTemplate.execute(status -> {
            mirrorAspectGucSet();
            setLocalRoleToRlsTestRole();
            return ((Number) entityManager
                .createNativeQuery("SELECT count(*) FROM " + TABLE + " WHERE id = :id")
                .setParameter("id", studentBId)
                .getSingleResult()
            ).longValue();
        });
        TenantContext.clear();

        assertThat(visibleBRowsForA)
            .as("Tenant A must not see Tenant B's row even by explicit id lookup")
            .isZero();
    }

    @Test
    @DisplayName("Native SQL select sees only current-tenant rows")
    void shouldEnforceOnNativeSql() {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        seedStudent(tenantA, "A1");
        seedStudent(tenantA, "A2");
        seedStudent(tenantB, "B1");

        TenantContext.setCurrentTenant(tenantA);
        Long countForA = transactionTemplate.execute(status -> {
            mirrorAspectGucSet();
            setLocalRoleToRlsTestRole();
            return ((Number) entityManager
                .createNativeQuery("SELECT count(*) FROM " + TABLE)
                .getSingleResult()
            ).longValue();
        });
        TenantContext.clear();

        assertThat(countForA)
            .as("Tenant A should see exactly its own 2 rows via raw SELECT *")
            .isEqualTo(2L);
    }

    @Test
    @DisplayName("Connection returned to pool does not leak app.current_tenant_id to next checkout")
    void shouldClearTenantOnConnectionRelease() throws Exception {
        UUID tenantA = UUID.randomUUID();
        TenantContext.setCurrentTenant(tenantA);

        // Run a tenant-A transaction so that SET LOCAL fires on the underlying connection.
        transactionTemplate.execute(status -> {
            mirrorAspectGucSet();
            entityManager.createNativeQuery("SELECT 1").getSingleResult();
            return null;
        });

        TenantContext.clear();

        // Now pop a fresh connection straight off the pool and check the GUC. `SET LOCAL` is
        // transaction-scoped, so once the prior transaction committed the value MUST be NULL
        // when read outside of any transaction on a freshly-checked-out (or recycled) connection.
        try (Connection raw = dataSource.getConnection();
             PreparedStatement ps = raw.prepareStatement(
                 "SELECT current_setting('app.current_tenant_id', true)")) {
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).isTrue();
                String leaked = rs.getString(1);
                assertThat(leaked)
                    .as("Tenant GUC must not leak across pool checkouts")
                    .satisfiesAnyOf(
                        v -> assertThat(v).isNull(),
                        v -> assertThat(v).isEmpty()
                    );
            }
        }
    }

    /**
     * Inserts a minimal student row for the given tenant directly via JDBC, bypassing JPA so
     * the RLS aspect does not intercept this fixture-setup path. We temporarily disable
     * row_security on the seed connection so the WITH CHECK clause does not reject the insert
     * when we are operating outside the aspect's SET LOCAL flow.
     */
    private Long seedStudent(UUID tenantId, String name) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            // Satisfy RLS's WITH CHECK clause by binding the GUC to the seed-target tenant.
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
                insert.setString(3, "rls-it-" + UUID.randomUUID() + "@example.test");
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
            return null;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to seed student for tenant " + tenantId, e);
        }
    }
}
