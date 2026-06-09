package com.kiteclass.core.module.lms;

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
 * Verifies Postgres Row-Level Security (RLS) tenant isolation on the four LMS tables
 * ({@code course_modules}, {@code lessons}, {@code learning_resources},
 * {@code lesson_progress}) created by {@code V79__entity_schema_sync.sql}.
 *
 * <p>Context (GAP-1112): the four LMS tables carry {@code instance_id UUID NOT NULL} and are
 * protected by the Hibernate {@code tenantFilter} (BaseEntity) at the ORM layer. V79 ALSO
 * ships DB-level RLS for them — its trailing {@code DO $$} block (V79 lines 577-613) iterates
 * {@code course_modules / lessons / learning_resources / lesson_progress} and applies
 * {@code ENABLE} + {@code FORCE ROW LEVEL SECURITY} + the V59-hardened {@code tenant_isolation}
 * policy (admin-bypass via {@code app.is_platform_admin} + NULL force-fail). The earlier
 * sweep V78 ran before V79 so it does not cover these tables, but V79 closes the gap itself.
 *
 * <p>Before this test, the RLS on these four tables had no regression guard — the kc-core test
 * profile ({@code application-test.yml}) disables Flyway and uses {@code ddl-auto: create-drop},
 * so V79's RLS never runs in tests. This IT mirrors {@link
 * com.kiteclass.core.common.datasource.RLSEnforcementIT}: it applies the same hardened policy
 * programmatically and exercises a CRUD round-trip so the defense-in-depth backstop stays proven.
 *
 * <p>Tests cover, for every LMS table:
 * <ul>
 *   <li>Cross-tenant read isolation — tenant A cannot see tenant B's rows (count + by-id).</li>
 *   <li>Default-deny — with no tenant context (GUC unset) every row is invisible (NULL force-fail).</li>
 * </ul>
 *
 * @since GAP-1112
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestContainersConfiguration.class, TestSecurityConfig.class})
@ContextConfiguration(initializers = TestContainersConfiguration.Initializer.class)
class LmsRlsIsolationIT {

    private static final String[] LMS_TABLES = {
        "course_modules",
        "lessons",
        "learning_resources",
        "lesson_progress"
    };

    private static final String RLS_TEST_ROLE = "kite_lms_rls_test_role";

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private TransactionTemplate transactionTemplate;

    /**
     * Applies the V79-equivalent hardened RLS policy to each LMS table and provisions a
     * NOSUPERUSER + NOBYPASSRLS role for the test transactions.
     *
     * <p>kc-core test profile disables Flyway ({@code ddl-auto: create-drop}), so V79's RLS
     * block does NOT run automatically. We re-apply the same SQL pattern the migration ships
     * (admin-bypass + NULL force-fail) so the test is self-contained AND validates the policy
     * shape. The Testcontainers DB user is a superuser (bypasses RLS even under FORCE), so the
     * test transactions {@code SET LOCAL ROLE} into a stripped-down role — mirroring production
     * where the app's DB role is NOSUPERUSER + NOBYPASSRLS.
     */
    @BeforeAll
    static void provisionLmsRls(@Autowired DataSource ds) {
        try (Connection raw = ds.getConnection()) {
            raw.setAutoCommit(true);
            for (String table : LMS_TABLES) {
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
            throw new IllegalStateException("Failed to apply LMS RLS policies in test setup", e);
        }

        try (Connection raw = ds.getConnection()) {
            raw.setAutoCommit(true);
            exec(raw,
                "DO $$ BEGIN "
                + "  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '" + RLS_TEST_ROLE + "') THEN "
                + "    CREATE ROLE " + RLS_TEST_ROLE + " NOSUPERUSER NOBYPASSRLS NOINHERIT; "
                + "  END IF; "
                + "END $$;");
            for (String table : LMS_TABLES) {
                exec(raw, "GRANT SELECT, INSERT, UPDATE, DELETE ON " + table + " TO " + RLS_TEST_ROLE);
            }
            exec(raw, "GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO " + RLS_TEST_ROLE);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to provision LMS RLS test role", e);
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
    // Cross-tenant read isolation — one test per LMS table.
    // ----------------------------------------------------------------------------------------

    @Test
    @DisplayName("course_modules: tenant A cannot see tenant B's rows (RLS cross-tenant isolation)")
    void courseModules_shouldNotLeakCrossTenant() {
        assertCrossTenantIsolation("course_modules", this::seedCourseModule);
    }

    @Test
    @DisplayName("lessons: tenant A cannot see tenant B's rows (RLS cross-tenant isolation)")
    void lessons_shouldNotLeakCrossTenant() {
        assertCrossTenantIsolation("lessons", this::seedLesson);
    }

    @Test
    @DisplayName("learning_resources: tenant A cannot see tenant B's rows (RLS cross-tenant isolation)")
    void learningResources_shouldNotLeakCrossTenant() {
        assertCrossTenantIsolation("learning_resources", this::seedLearningResource);
    }

    @Test
    @DisplayName("lesson_progress: tenant A cannot see tenant B's rows (RLS cross-tenant isolation)")
    void lessonProgress_shouldNotLeakCrossTenant() {
        assertCrossTenantIsolation("lesson_progress", this::seedLessonProgress);
    }

    // ----------------------------------------------------------------------------------------
    // Default-deny — no tenant context → every LMS row invisible (NULL force-fail).
    // ----------------------------------------------------------------------------------------

    @Test
    @DisplayName("All LMS tables hide every row when tenant context is unset (NULL force-fail)")
    void allLmsTables_shouldRejectQueryWithoutTenantContext() {
        UUID tenantA = UUID.randomUUID();
        seedCourseModule(tenantA);
        seedLesson(tenantA);
        seedLearningResource(tenantA);
        seedLessonProgress(tenantA);

        // No TenantContext → aspect would not SET LOCAL → GUC NULL → policy filters everything.
        TenantContext.clear();

        for (String table : LMS_TABLES) {
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
    // Shared assertion + helpers.
    // ----------------------------------------------------------------------------------------

    /**
     * Seeds one tenant-A row + one tenant-B row, then asserts (as tenant A) that:
     * (1) a raw {@code SELECT *} count returns exactly the single tenant-A row, and
     * (2) an explicit by-id lookup of tenant B's row returns zero rows.
     */
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

    /**
     * Mirrors {@code TenantAwareDataSourceInterceptor} — the aspect only fires on
     * {@code @Transactional} methods, not inside a raw {@link TransactionTemplate} lambda, so we
     * manually issue the same {@code SET LOCAL app.current_tenant_id} it would have set.
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

    /**
     * Switches the current transaction's role to the NOSUPERUSER + NOBYPASSRLS test role so RLS
     * actually applies. {@code SET LOCAL} reverts the role at commit/rollback.
     */
    private void setLocalRoleToRlsTestRole() {
        entityManager.createNativeQuery("SET LOCAL ROLE " + RLS_TEST_ROLE).executeUpdate();
    }

    // --- Per-table JDBC seeders (bypass JPA so the tenantFilter aspect does not intercept) ---

    private Long seedCourseModule(UUID tenantId) {
        return seed(tenantId,
            "INSERT INTO course_modules (instance_id, course_id, title, order_number, deleted, created_at) "
            + "VALUES (?, ?, ?, ?, false, CURRENT_TIMESTAMP) RETURNING id",
            ps -> {
                ps.setObject(1, tenantId);
                ps.setLong(2, randomId());
                ps.setString(3, "Module " + UUID.randomUUID());
                ps.setInt(4, 1);
            });
    }

    private Long seedLesson(UUID tenantId) {
        return seed(tenantId,
            "INSERT INTO lessons (instance_id, module_id, title, is_trial, order_number, deleted, created_at) "
            + "VALUES (?, ?, ?, false, ?, false, CURRENT_TIMESTAMP) RETURNING id",
            ps -> {
                ps.setObject(1, tenantId);
                ps.setLong(2, randomId());
                ps.setString(3, "Lesson " + UUID.randomUUID());
                ps.setInt(4, 1);
            });
    }

    private Long seedLearningResource(UUID tenantId) {
        return seed(tenantId,
            "INSERT INTO learning_resources (instance_id, lesson_id, type, url, title, deleted, created_at) "
            + "VALUES (?, ?, 'VIDEO', ?, ?, false, CURRENT_TIMESTAMP) RETURNING id",
            ps -> {
                ps.setObject(1, tenantId);
                ps.setLong(2, randomId());
                ps.setString(3, "https://cdn.example.test/" + UUID.randomUUID());
                ps.setString(4, "Resource " + UUID.randomUUID());
            });
    }

    private Long seedLessonProgress(UUID tenantId) {
        return seed(tenantId,
            "INSERT INTO lesson_progress (instance_id, user_id, lesson_id, completed, progress_percent, deleted, created_at) "
            + "VALUES (?, ?, ?, false, 0, false, CURRENT_TIMESTAMP) RETURNING id",
            ps -> {
                ps.setObject(1, tenantId);
                ps.setLong(2, randomId());
                ps.setLong(3, randomId());
            });
    }

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement ps) throws Exception;
    }

    /**
     * Inserts a row directly via JDBC, binding the GUC to the target tenant so the policy's
     * {@code WITH CHECK} clause is satisfied. The seed connection is the superuser test user
     * (bypasses RLS) — setting the GUC keeps intent explicit and matches {@code RLSEnforcementIT}.
     */
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
