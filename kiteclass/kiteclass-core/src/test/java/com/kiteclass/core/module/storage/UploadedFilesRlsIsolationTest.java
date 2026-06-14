package com.kiteclass.core.module.storage;

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
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GAP-1311 — verifies the Postgres Row-Level Security (RLS) backstop on the two storage tables
 * ({@code uploaded_files}, {@code storage_quotas}) added by {@code V99__uploaded_files_storage_quotas_rls.sql}.
 *
 * <p>Both tables carry {@code instance_id UUID NOT NULL} (V79) and the Hibernate {@code tenantFilter}
 * at the ORM layer, but were never in an enable-RLS sweep (absent from V58/V59/V78/V81/V83/V84) —
 * single-layer isolation, unlike every tenant-scoped peer. V99 adds the V59-hardened
 * {@code tenant_isolation} policy (admin-bypass + NULL force-fail).
 *
 * <p>The kc-core test profile ({@code application-test.yml}) disables Flyway and uses
 * {@code ddl-auto: create-drop}, so V99 does NOT run automatically in tests. This test mirrors
 * {@link com.kiteclass.core.module.lms.LmsRlsIsolationIT}: it applies the identical policy SQL the
 * migration ships and provisions a NOSUPERUSER + NOBYPASSRLS role (the Testcontainers user is a
 * superuser that bypasses RLS even under FORCE), so the test both validates the policy SHAPE and
 * proves DB-level isolation independent of the Hibernate filter. Named {@code *Test} so it is
 * CI-bound (surefire), per the GAP-1311 acceptance criteria.
 *
 * @since GAP-1311
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestContainersConfiguration.class, TestSecurityConfig.class})
@ContextConfiguration(initializers = TestContainersConfiguration.Initializer.class)
class UploadedFilesRlsIsolationTest {

    private static final String[] STORAGE_TABLES = {
        "uploaded_files",
        "storage_quotas"
    };

    private static final String RLS_TEST_ROLE = "kite_storage_rls_test_role";

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeAll
    static void provisionStorageRls(@Autowired DataSource ds) {
        try (Connection raw = ds.getConnection()) {
            raw.setAutoCommit(true);
            for (String table : STORAGE_TABLES) {
                exec(raw, "ALTER TABLE " + table + " ENABLE ROW LEVEL SECURITY");
                exec(raw, "ALTER TABLE " + table + " FORCE ROW LEVEL SECURITY");
                exec(raw, "DROP POLICY IF EXISTS tenant_isolation ON " + table);
                exec(raw,
                    "CREATE POLICY tenant_isolation ON " + table + " "
                    + "USING ("
                    + "    COALESCE(current_setting('app.is_platform_admin', true)::boolean, false) "
                    + "    OR instance_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid"
                    + ") "
                    + "WITH CHECK ("
                    + "    COALESCE(current_setting('app.is_platform_admin', true)::boolean, false) "
                    + "    OR instance_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid"
                    + ")");
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to apply storage RLS policies in test setup", e);
        }

        try (Connection raw = ds.getConnection()) {
            raw.setAutoCommit(true);
            exec(raw,
                "DO $$ BEGIN "
                + "  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '" + RLS_TEST_ROLE + "') THEN "
                + "    CREATE ROLE " + RLS_TEST_ROLE + " NOSUPERUSER NOBYPASSRLS NOINHERIT; "
                + "  END IF; "
                + "END $$;");
            for (String table : STORAGE_TABLES) {
                exec(raw, "GRANT SELECT, INSERT, UPDATE, DELETE ON " + table + " TO " + RLS_TEST_ROLE);
            }
            exec(raw, "GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO " + RLS_TEST_ROLE);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to provision storage RLS test role", e);
        }
    }

    private static void exec(Connection conn, String sql) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.execute();
        }
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ----------------------------------------------------------------------------------------
    // Cross-tenant read isolation — one test per storage table.
    // ----------------------------------------------------------------------------------------

    @Test
    @DisplayName("uploaded_files: tenant A cannot see tenant B's rows (RLS cross-tenant isolation)")
    void uploadedFiles_shouldNotLeakCrossTenant() {
        assertCrossTenantIsolation("uploaded_files", this::seedUploadedFile);
    }

    @Test
    @DisplayName("storage_quotas: tenant A cannot see tenant B's rows (RLS cross-tenant isolation)")
    void storageQuotas_shouldNotLeakCrossTenant() {
        assertCrossTenantIsolation("storage_quotas", this::seedStorageQuota);
    }

    // ----------------------------------------------------------------------------------------
    // Default-deny — no tenant context → every storage row invisible (NULL force-fail).
    // ----------------------------------------------------------------------------------------

    @Test
    @DisplayName("Storage tables hide every row when tenant context is unset (NULL force-fail)")
    void storageTables_shouldRejectQueryWithoutTenantContext() {
        UUID tenantA = UUID.randomUUID();
        seedUploadedFile(tenantA);
        seedStorageQuota(tenantA);

        // No TenantContext → aspect would not SET LOCAL → GUC NULL → policy filters everything.
        TenantContext.clear();

        for (String table : STORAGE_TABLES) {
            Long count = transactionTemplate.execute(status -> {
                mirrorAspectGucSet();
                setLocalRoleToRlsTestRole();
                return ((Number) entityManager
                    .createNativeQuery("SELECT count(*) FROM " + table)
                    .getSingleResult()
                ).longValue();
            });
            assertThat(count)
                .as("With no tenant context, RLS must hide every row of %s", table)
                .isZero();
        }
    }

    // ----------------------------------------------------------------------------------------
    // Shared assertion + helpers (mirror LmsRlsIsolationIT).
    // ----------------------------------------------------------------------------------------

    private void assertCrossTenantIsolation(String table, java.util.function.Function<UUID, Long> seeder) {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();

        seeder.apply(tenantA);
        Long tenantBRowId = seeder.apply(tenantB);
        assertThat(tenantBRowId).isNotNull();

        TenantContext.setCurrentTenant(tenantA);

        Long visibleForA = transactionTemplate.execute(status -> {
            mirrorAspectGucSet();
            setLocalRoleToRlsTestRole();
            return ((Number) entityManager
                .createNativeQuery("SELECT count(*) FROM " + table)
                .getSingleResult()
            ).longValue();
        });

        Long tenantBVisibleToA = transactionTemplate.execute(status -> {
            mirrorAspectGucSet();
            setLocalRoleToRlsTestRole();
            return ((Number) entityManager
                .createNativeQuery("SELECT count(*) FROM " + table + " WHERE id = :id")
                .setParameter("id", tenantBRowId)
                .getSingleResult()
            ).longValue();
        });

        TenantContext.clear();

        assertThat(visibleForA)
            .as("Tenant A should see exactly its own row in %s via raw SELECT *", table)
            .isEqualTo(1L);
        assertThat(tenantBVisibleToA)
            .as("Tenant A must not see tenant B's %s row even by explicit id lookup", table)
            .isZero();
    }

    private void mirrorAspectGucSet() {
        if (!TenantContext.isSet()) {
            return; // Default-deny path; leave GUC NULL.
        }
        entityManager
            .createNativeQuery("SELECT set_config('app.current_tenant_id', :tid, true)")
            .setParameter("tid", TenantContext.getCurrentTenant().toString())
            .getSingleResult();
    }

    private void setLocalRoleToRlsTestRole() {
        entityManager.createNativeQuery("SET LOCAL ROLE " + RLS_TEST_ROLE).executeUpdate();
    }

    // --- Per-table JDBC seeders (bypass JPA so the tenantFilter aspect does not intercept) ---

    private Long seedUploadedFile(UUID tenantId) {
        return seed(tenantId,
            "INSERT INTO uploaded_files "
            + "(instance_id, uploader_id, file_type, original_name, storage_path, file_size, "
            + " mime_type, access_level, status, deleted, created_at) "
            + "VALUES (?, ?, 'DOCUMENT', ?, ?, ?, 'application/pdf', 'PRIVATE', 'CONFIRMED', false, CURRENT_TIMESTAMP) "
            + "RETURNING id",
            ps -> {
                ps.setObject(1, tenantId);
                ps.setLong(2, randomId());
                ps.setString(3, "file-" + UUID.randomUUID() + ".pdf");
                ps.setString(4, tenantId + "/uploads/2026/06/" + UUID.randomUUID() + ".pdf");
                ps.setLong(5, 1024L);
            });
    }

    private Long seedStorageQuota(UUID tenantId) {
        return seed(tenantId,
            "INSERT INTO storage_quotas "
            + "(instance_id, tier, used_bytes, quota_bytes, last_calculated_at, created_at) "
            + "VALUES (?, 'FREE', 0, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) "
            + "RETURNING id",
            ps -> {
                ps.setObject(1, tenantId);
                ps.setLong(2, 1024L * 1024 * 1024); // 1 GB FREE tier quota
            });
    }

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement ps) throws Exception;
    }

    private Long seed(UUID tenantId, String insertSql, StatementBinder binder) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement setGuc = conn.prepareStatement(
                "SELECT set_config('app.current_tenant_id', ?, true)")) {
                setGuc.setString(1, tenantId.toString());
                setGuc.executeQuery().close();
            }
            try (PreparedStatement insert = conn.prepareStatement(insertSql)) {
                binder.bind(insert);
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
            throw new IllegalStateException("Failed to seed row for tenant " + tenantId, e);
        }
    }

    private static long randomId() {
        return ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
    }
}
