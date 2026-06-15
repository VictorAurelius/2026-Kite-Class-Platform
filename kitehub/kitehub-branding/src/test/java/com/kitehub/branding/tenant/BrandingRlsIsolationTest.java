package com.kitehub.branding.tenant;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * GAP-1020 (Part 1) — proves Row-Level Security on {@code branding_jobs} actually isolates tenants
 * once the {@code app.current_tenant_id} GUC is set (the value
 * {@link TenantAwareDataSourceInterceptor} issues per transaction), under a NON-superuser DB role.
 *
 * <p>This is a pure-JDBC Testcontainers IT (no Spring context) that reconstructs the V34 + V75 RLS
 * policy shape on a real PostgreSQL instance, then drives it as a non-owner role — the production
 * posture the {@code kitehub} owner role masks locally. It directly verifies the gap's acceptance
 * criterion: "non-superuser role → cross-tenant branding_jobs query returns empty + WITH CHECK
 * enforced".</p>
 */
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Branding RLS tenant isolation (non-superuser, GUC-driven)")
class BrandingRlsIsolationTest {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    /** Non-owner, non-superuser role — the production-equivalent role RLS actually filters. */
    private static final String APP_ROLE = "rls_app_role";

    private static final UUID TENANT_A = UUID.randomUUID();
    private static final UUID TENANT_B = UUID.randomUUID();

    @BeforeAll
    void provisionSchema() throws SQLException {
        try (Connection owner = ownerConnection(); Statement st = owner.createStatement()) {
            // Minimal FK target + tenant-scoped table (only instance_id matters for the RLS policy).
            st.execute("CREATE TABLE instances (id UUID PRIMARY KEY, tier VARCHAR(20) NOT NULL)");
            st.execute("CREATE TABLE branding_jobs ("
                    + "id UUID PRIMARY KEY, "
                    + "instance_id UUID NOT NULL REFERENCES instances(id), "
                    + "organization_name VARCHAR(200))");

            // RLS policy mirroring V34 (tenant predicate) + V75 (platform-admin bypass), NON-forced.
            st.execute("ALTER TABLE branding_jobs ENABLE ROW LEVEL SECURITY");
            st.execute("CREATE POLICY tenant_isolation ON branding_jobs "
                    + "USING ("
                    + "  COALESCE(current_setting('app.is_platform_admin', true)::boolean, false) "
                    + "  OR instance_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid"
                    + ") "
                    + "WITH CHECK ("
                    + "  COALESCE(current_setting('app.is_platform_admin', true)::boolean, false) "
                    + "  OR instance_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid"
                    + ")");

            // Seed FK targets (as owner — instances has no RLS, matching production). Distinct
            // tiers so the tier-from-subscription read assertion is meaningful (GAP-1020 Part 2).
            st.execute("INSERT INTO instances (id, tier) VALUES "
                    + "('" + TENANT_A + "', 'PREMIUM'), ('" + TENANT_B + "', 'FREE')");

            // Non-superuser app role — gets DML grants but NOT ownership/BYPASSRLS → RLS applies.
            st.execute("CREATE ROLE " + APP_ROLE + " NOSUPERUSER NOBYPASSRLS");
            st.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON branding_jobs TO " + APP_ROLE);
            st.execute("GRANT SELECT ON instances TO " + APP_ROLE);
        }
    }

    @org.junit.jupiter.api.BeforeEach
    void cleanTable() throws SQLException {
        // Tests share one table; truncate (as owner — bypasses RLS) so each starts isolated
        // regardless of JUnit execution order.
        try (Connection owner = ownerConnection(); Statement st = owner.createStatement()) {
            st.execute("TRUNCATE branding_jobs");
        }
    }

    @AfterAll
    void dropRole() throws SQLException {
        try (Connection owner = ownerConnection(); Statement st = owner.createStatement()) {
            st.execute("REVOKE ALL ON branding_jobs FROM " + APP_ROLE);
            st.execute("REVOKE ALL ON instances FROM " + APP_ROLE);
            st.execute("DROP ROLE IF EXISTS " + APP_ROLE);
        }
    }

    @Test
    @DisplayName("With GUC set, insert succeeds for own tenant and cross-tenant SELECT is empty")
    void crossTenantSelectIsEmptyUnderRls() throws SQLException {
        try (Connection app = appRoleConnection()) {
            setTenant(app, TENANT_A);
            insertJob(app, UUID.randomUUID(), TENANT_A, "Org A");

            // Same connection, switch GUC to tenant B → tenant A's row is invisible.
            setTenant(app, TENANT_B);
            assertThat(countJobs(app)).as("cross-tenant rows leak under RLS").isZero();

            // Switch back to tenant A → its own row is visible again.
            setTenant(app, TENANT_A);
            assertThat(countJobs(app)).as("own-tenant row must be visible").isEqualTo(1);
        }
    }

    @Test
    @DisplayName("WITH CHECK rejects inserting a row for a different tenant than the GUC")
    void withCheckRejectsCrossTenantInsert() throws SQLException {
        try (Connection app = appRoleConnection()) {
            setTenant(app, TENANT_B);
            // GUC = tenant B but row claims tenant A → policy WITH CHECK violation.
            assertThatThrownBy(() -> insertJob(app, UUID.randomUUID(), TENANT_A, "Spoofed"))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("row-level security");
        }
    }

    @Test
    @DisplayName("Unset GUC yields default-deny (zero rows) — not a full-table scan")
    void unsetGucDefaultDeny() throws SQLException {
        try (Connection app = appRoleConnection()) {
            // Seed a row for tenant A via a properly-scoped connection.
            try (Connection seed = appRoleConnection()) {
                setTenant(seed, TENANT_A);
                insertJob(seed, UUID.randomUUID(), TENANT_A, "Org A unset-test");
            }
            // No GUC set on this connection → current_setting(...) NULL → policy denies all.
            assertThat(countJobs(app)).as("unset GUC must default-deny").isZero();
        }
    }

    @Test
    @DisplayName("Platform-admin GUC bypasses RLS (sees all tenants)")
    void platformAdminBypass() throws SQLException {
        try (Connection seed = appRoleConnection()) {
            setTenant(seed, TENANT_A);
            insertJob(seed, UUID.randomUUID(), TENANT_A, "Org A admin-test");
        }
        try (Connection seed = appRoleConnection()) {
            setTenant(seed, TENANT_B);
            insertJob(seed, UUID.randomUUID(), TENANT_B, "Org B admin-test");
        }
        try (Connection admin = appRoleConnection(); Statement st = admin.createStatement()) {
            st.execute("SELECT set_config('app.is_platform_admin', 'true', false)");
            assertThat(countJobs(admin)).as("admin must see >=2 tenants' rows")
                    .isGreaterThanOrEqualTo(2);
        }
    }

    @Test
    @DisplayName("Tier-from-subscription: resolver query reads instances.tier from real Postgres")
    void tierResolvedFromInstancesTableOnRealPostgres() throws SQLException {
        // Verifies the exact read SubscriptionTierResolver issues (SELECT tier FROM instances
        // WHERE id = ?) returns the authoritative server-side tier on real Postgres — GAP-1020
        // Part 2 (entitlement tier comes from the DB, not the client header).
        try (Connection app = appRoleConnection();
             PreparedStatement ps = app.prepareStatement("SELECT tier FROM instances WHERE id = ?")) {
            ps.setObject(1, TENANT_A);
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString(1)).isEqualTo("PREMIUM");
            }
        }
    }

    // --- helpers ---------------------------------------------------------------------------------

    private Connection ownerConnection() throws SQLException {
        return DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    /** Connection that immediately downgrades to the non-superuser app role (RLS applies). */
    private Connection appRoleConnection() throws SQLException {
        Connection c = ownerConnection();
        try (Statement st = c.createStatement()) {
            st.execute("SET ROLE " + APP_ROLE);
        }
        return c;
    }

    private void setTenant(Connection c, UUID tenantId) throws SQLException {
        try (PreparedStatement ps =
                     c.prepareStatement("SELECT set_config('app.current_tenant_id', ?, false)")) {
            ps.setString(1, tenantId.toString());
            ps.execute();
        }
    }

    private void insertJob(Connection c, UUID id, UUID instanceId, String org) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO branding_jobs (id, instance_id, organization_name) VALUES (?, ?, ?)")) {
            ps.setObject(1, id);
            ps.setObject(2, instanceId);
            ps.setString(3, org);
            ps.executeUpdate();
        }
    }

    private int countJobs(Connection c) throws SQLException {
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM branding_jobs")) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
