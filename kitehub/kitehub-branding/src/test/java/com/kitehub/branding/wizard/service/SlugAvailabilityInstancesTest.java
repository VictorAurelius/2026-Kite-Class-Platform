package com.kitehub.branding.wizard.service;

import com.kitehub.branding.repository.BrandingJobRepository;
import com.kitehub.branding.wizard.dto.SlugAvailabilityResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * GAP-1111 — the wizard slug-availability check must reflect the CANONICAL
 * subdomain registry ({@code instances.subdomain}, owned by kitehub-subscription),
 * not just {@code branding_jobs.organization_name}.
 *
 * <p>kitehub-branding shares the {@code /kitehub} datasource with kitehub-subscription
 * (see {@code application.yml} {@code DATABASE_URL=jdbc:postgresql://.../kitehub}), so
 * {@link SlugAvailabilityService} reads the {@code instances} table directly via a
 * read-only native query (the minimal clean cross-service read — no cross-module JPA
 * entity import per {@code design-patterns.md}). H2 cannot model this shared table, so
 * the check is proven against a real Postgres via Testcontainers.</p>
 *
 * <p>Named {@code *Test} (not {@code *IT}) so it runs under the {@code ./mvnw test}
 * surefire phase — Docker/Testcontainers is available in CI per the convention of
 * {@code AIRateLimitServiceRedisTest}.</p>
 */
@Testcontainers
@DisplayName("SlugAvailabilityService — instances.subdomain canonical check (GAP-1111)")
class SlugAvailabilityInstancesTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine");

    private JdbcTemplate jdbcTemplate;
    private SlugAvailabilityService service;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource ds = new DriverManagerDataSource(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        ds.setDriverClassName("org.postgresql.Driver");
        jdbcTemplate = new JdbcTemplate(ds);

        // Minimal slice of the canonical instances table (kitehub-subscription
        // V1__create_instances_table.sql) — only the columns this check reads.
        jdbcTemplate.execute("DROP TABLE IF EXISTS instances");
        jdbcTemplate.execute(
            "CREATE TABLE instances ("
                + "id UUID PRIMARY KEY,"
                + "subdomain VARCHAR(50) UNIQUE NOT NULL,"
                + "deleted BOOLEAN NOT NULL DEFAULT FALSE)");

        BrandingJobRepository jobRepo = mock(BrandingJobRepository.class);
        // Force the secondary branding_jobs.organization_name proxy to "free" so the
        // test isolates the primary instances.subdomain signal.
        when(jobRepo.existsByOrganizationNameLowercased(anyString())).thenReturn(false);

        service = new SlugAvailabilityService(jobRepo, jdbcTemplate);
    }

    private void claimSubdomain(String subdomain, boolean deleted) {
        jdbcTemplate.update(
            "INSERT INTO instances (id, subdomain, deleted) VALUES (?, ?, ?)",
            UUID.randomUUID(), subdomain, deleted);
    }

    @Test
    @DisplayName("Subdomain already claimed in instances -> slug unavailable + suggestions")
    void subdomainClaimedInInstancesIsUnavailable() {
        claimSubdomain("acme-school", false);

        SlugAvailabilityResponse resp = service.check("acme-school");

        assertThat(resp.available()).isFalse();
        assertThat(resp.suggestions()).isNotEmpty();
    }

    @Test
    @DisplayName("Subdomain not in instances -> available")
    void freeSubdomainIsAvailable() {
        SlugAvailabilityResponse resp = service.check("brand-new-school");

        assertThat(resp.available()).isTrue();
        assertThat(resp.suggestions()).isEmpty();
    }

    @Test
    @DisplayName("Soft-deleted subdomain (deleted=true) -> available again")
    void softDeletedSubdomainIsAvailable() {
        claimSubdomain("reopened-school", true);

        SlugAvailabilityResponse resp = service.check("reopened-school");

        assertThat(resp.available()).isTrue();
    }

    @Test
    @DisplayName("Match against instances.subdomain is case-insensitive")
    void caseInsensitiveMatch() {
        // Stored mixed-case; queried lowercase — LOWER(subdomain) match still hits.
        claimSubdomain("MixedCase", false);

        SlugAvailabilityResponse resp = service.check("mixedcase");

        assertThat(resp.available()).isFalse();
    }

    @Test
    @DisplayName("Reserved word stays unavailable (short-circuits before instances query)")
    void reservedWordStillUnavailable() {
        SlugAvailabilityResponse resp = service.check("admin");

        assertThat(resp.available()).isFalse();
    }
}
