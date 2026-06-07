package com.kitehub.subscription.oauth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testcontainers IT for V66 oauth_attempts RLS — Wave p0-local-1 Bucket B (GAP-885).
 *
 * <p>Per {@code .claude/rules/postgres-specific-type-testcontainers.md} + the KC-5
 * lesson (IT ddl-auto masks migration drift): this IT runs Flyway against a real
 * {@code postgres:15-alpine} container with {@code ddl-auto=validate}, so V66 actually
 * applies on the production-equivalent schema (NOT an entity-derived H2 schema).
 *
 * <p>Coverage:
 * <ol>
 *   <li>RLS enabled on oauth_attempts (pg_class.relrowsecurity = true)</li>
 *   <li>tenant_isolation policy exists (pg_policies WHERE tablename='oauth_attempts')</li>
 *   <li>NULL force-fail: INSERT blocked under FORCE RLS without tenant context / non-admin</li>
 *   <li>Admin-bypass + tenant isolation: admin inserts cross-tenant rows; non-admin
 *       connection only sees rows matching its app.current_tenant_id</li>
 * </ol>
 *
 * @since Wave p0-local-1 Bucket B — GAP-885
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@DisplayName("oauth_attempts V66 RLS — Postgres Testcontainers IT (GAP-885)")
class OauthAttemptsRlsPostgresIT {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("kitehub_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.properties.hibernate.dialect",
                () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private DataSource dataSource;

    /** RabbitTemplate mocked — production beans require it; this IT does not exercise messaging. */
    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        // TRUNCATE is owner-only and bypasses per-row RLS — clean slate per test.
        jdbc.execute("TRUNCATE TABLE oauth_attempts RESTART IDENTITY");
    }

    @Test
    @DisplayName("RLS is enabled on oauth_attempts after V66")
    void rls_enabled() {
        Boolean rlsEnabled = jdbc.queryForObject(
                "SELECT relrowsecurity FROM pg_class WHERE relname = 'oauth_attempts'",
                Boolean.class);
        assertThat(rlsEnabled).as("oauth_attempts should have RLS enabled").isTrue();
    }

    @Test
    @DisplayName("tenant_isolation policy exists on oauth_attempts")
    void tenant_isolation_policy_present() {
        Integer policyCount = jdbc.queryForObject(
                "SELECT count(*) FROM pg_policies "
                        + "WHERE tablename = 'oauth_attempts' AND policyname = 'tenant_isolation'",
                Integer.class);
        assertThat(policyCount).as("tenant_isolation policy must exist").isEqualTo(1);
    }

    // NOTE: the Testcontainers default DB user is a SUPERUSER and table owner, so it
    // bypasses RLS even under FORCE. To exercise the policy we SET ROLE to a freshly
    // created NOSUPERUSER role (RLS applies to non-owner roles without needing FORCE).

    @Test
    @DisplayName("NULL force-fail: INSERT blocked for non-admin role without tenant context")
    void null_force_fail_blocks_insert() throws Exception {
        try (Connection c = dataSource.getConnection(); Statement st = c.createStatement()) {
            st.execute("DROP ROLE IF EXISTS rls_oauth_blk");
            st.execute("CREATE ROLE rls_oauth_blk NOSUPERUSER");
            st.execute("GRANT SELECT, INSERT ON oauth_attempts TO rls_oauth_blk");
            st.execute("GRANT USAGE, SELECT ON SEQUENCE oauth_attempts_id_seq TO rls_oauth_blk");
            try {
                st.execute("SET ROLE rls_oauth_blk");
                // No app.is_platform_admin, no app.current_tenant_id → WITH CHECK fails.
                assertThatThrownBy(() -> st.executeUpdate(
                        "INSERT INTO oauth_attempts(state_token, provider, tenant_id) "
                                + "VALUES ('tok-blocked', 'google', 1001)"))
                        .as("INSERT without tenant context must be blocked by RLS")
                        .hasMessageContaining("policy");
            } finally {
                st.execute("RESET ROLE");
                st.execute("DROP OWNED BY rls_oauth_blk");
                st.execute("DROP ROLE IF EXISTS rls_oauth_blk");
            }
        }
    }

    @Test
    @DisplayName("admin-bypass sees all rows; non-admin role sees only its tenant's rows")
    void admin_bypass_and_tenant_isolation() throws Exception {
        try (Connection c = dataSource.getConnection(); Statement st = c.createStatement()) {
            // Seed two cross-tenant rows as the owner (RLS does not apply to owner w/o FORCE).
            st.executeUpdate("INSERT INTO oauth_attempts(state_token, provider, tenant_id) "
                    + "VALUES ('tok-a', 'google', 1001)");
            st.executeUpdate("INSERT INTO oauth_attempts(state_token, provider, tenant_id) "
                    + "VALUES ('tok-b', 'microsoft', 2002)");

            st.execute("DROP ROLE IF EXISTS rls_oauth_rd");
            st.execute("CREATE ROLE rls_oauth_rd NOSUPERUSER");
            st.execute("GRANT SELECT ON oauth_attempts TO rls_oauth_rd");
            try {
                st.execute("SET ROLE rls_oauth_rd");

                // Non-admin, tenant 1001 context → only tenant 1001 row visible.
                st.execute("SET app.current_tenant_id = '1001'");
                assertThat(scalarCount(st, "SELECT count(*) FROM oauth_attempts")).isEqualTo(1);
                try (ResultSet rs = st.executeQuery("SELECT state_token FROM oauth_attempts")) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getString(1)).isEqualTo("tok-a");
                }

                // Tenant 2002 context → only tenant 2002 row visible (cross-tenant isolation).
                st.execute("SET app.current_tenant_id = '2002'");
                try (ResultSet rs = st.executeQuery("SELECT state_token FROM oauth_attempts")) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getString(1)).isEqualTo("tok-b");
                }

                // Unrelated tenant context → no rows leak.
                st.execute("SET app.current_tenant_id = '9999'");
                assertThat(scalarCount(st, "SELECT count(*) FROM oauth_attempts")).isEqualTo(0);

                // Admin-bypass clause (GUC-driven) → same role now sees ALL rows.
                st.execute("SET app.is_platform_admin = 'true'");
                assertThat(scalarCount(st, "SELECT count(*) FROM oauth_attempts")).isEqualTo(2);
            } finally {
                st.execute("RESET ROLE");
                st.execute("RESET app.current_tenant_id");
                st.execute("RESET app.is_platform_admin");
                st.execute("DROP OWNED BY rls_oauth_rd");
                st.execute("DROP ROLE IF EXISTS rls_oauth_rd");
            }
        }
    }

    private static int scalarCount(Statement st, String sql) throws Exception {
        try (ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
