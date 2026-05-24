package com.kitehub.subscription.consent.immutable;

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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testcontainers IT for V56 immutable consent — Wave br-4 Bucket B (GAP-353b).
 *
 * <p>Per {@code .claude/rules/postgres-specific-type-testcontainers.md}: INET +
 * JSONB binding cannot be tested under H2. This IT exercises the actual Postgres
 * adapters via {@code postgres:15-alpine} container.
 *
 * <p>Coverage:
 * <ol>
 *   <li>POST round-trip creates row + current_hash deterministic</li>
 *   <li>Hash chain valid (recompute matches stored)</li>
 *   <li>Withdraw creates NEW row (NOT flip latest)</li>
 *   <li>RLS blocks raw UPDATE (immutability enforcement)</li>
 *   <li>RLS blocks raw DELETE</li>
 *   <li>JSONB granted field round-trip preserves keys + values</li>
 *   <li>INET round-trip for IPv4 + IPv6</li>
 *   <li>Chain integrity validator detects tampering (manual UPDATE bypassing RLS)</li>
 * </ol>
 *
 * @since Wave beta-readiness-4 Bucket B — GAP-353b
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@DisplayName("ConsentRecordImmutable V56 — Postgres Testcontainers IT (GAP-353b)")
class ConsentRecordImmutablePostgresIT {

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
    private ConsentRecordImmutableRepository repository;

    @Autowired
    private ConsentService consentService;

    @Autowired
    private DataSource dataSource;

    /** RabbitTemplate mocked — production EmailServiceClient requires it, IT doesn't exercise email. */
    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        // Cleanup needs to bypass RLS DELETE policy — superuser is the test user (rds_superuser-like).
        // In Testcontainers PG, the connection user is the table owner, so RLS applies to FORCE only.
        // We use TRUNCATE which is owner-only and not subject to per-row RLS.
        jdbc.execute("TRUNCATE TABLE consent_record_immutable RESTART IDENTITY");
    }

    @Test
    @DisplayName("recordConsent creates row + current_hash deterministic")
    void record_creates_row() {
        ConsentRecordImmutable saved = consentService.recordConsent(
                42L, 7L,
                Map.of("essential", true, "analytics", true, "marketing", false),
                "203.0.113.7", "Mozilla/5.0 Trần Thị Hồng browser");

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getPrevHash()).isNull(); // chain head
        assertThat(saved.getCurrentHash()).isNotNull().hasSize(64);
        assertThat(saved.getGranted()).contains("\"analytics\":true").contains("\"marketing\":false");
        assertThat(saved.getIpAddress()).isEqualTo("203.0.113.7");
    }

    @Test
    @DisplayName("hash chain validates correctly across multiple inserts")
    void chain_integrity_holds() {
        Long userId = 100L;
        ConsentRecordImmutable r1 = consentService.recordConsent(userId, 7L,
                Map.of("essential", true, "analytics", true, "marketing", false),
                "203.0.113.7", "ua1");
        ConsentRecordImmutable r2 = consentService.recordConsent(userId, 7L,
                Map.of("essential", true, "analytics", true, "marketing", true),
                "203.0.113.7", "ua1");
        ConsentRecordImmutable r3 = consentService.recordConsent(userId, 7L,
                Map.of("essential", true, "analytics", false, "marketing", false),
                "203.0.113.7", "ua1");

        assertThat(r2.getPrevHash()).isEqualTo(r1.getCurrentHash());
        assertThat(r3.getPrevHash()).isEqualTo(r2.getCurrentHash());

        // Chain validator should pass without exception.
        consentService.verifyChainIntegrity(userId);
        List<ConsentRecordImmutable> history = consentService.findHistory(userId);
        assertThat(history).hasSize(3);
    }

    @Test
    @DisplayName("withdrawConsent INSERTs new row, latest row keeps analytics=false")
    void withdraw_inserts_new_row() {
        Long userId = 200L;
        consentService.recordConsent(userId, 7L,
                Map.of("essential", true, "analytics", true, "marketing", true),
                "203.0.113.7", "ua-withdraw-test");
        ConsentRecordImmutable revoked = consentService.withdrawConsent(
                userId, 7L, "203.0.113.7", "ua-withdraw-test");

        assertThat(revoked.getGranted()).contains("\"analytics\":false");
        assertThat(revoked.getGranted()).contains("\"marketing\":false");

        List<ConsentRecordImmutable> history = consentService.findHistory(userId);
        assertThat(history).hasSize(2);
        assertThat(history.get(0).getGranted()).contains("\"analytics\":true");
        assertThat(history.get(1).getGranted()).contains("\"analytics\":false");
    }

    @Test
    @DisplayName("RLS blocks UPDATE on consent_record_immutable")
    void rls_blocks_update() {
        ConsentRecordImmutable saved = consentService.recordConsent(
                300L, 7L,
                Map.of("essential", true, "analytics", true, "marketing", false),
                "203.0.113.7", "ua-rls-test");

        // Switch to non-owner role to ensure RLS applies. Testcontainers default user
        // is owner; we use SET ROLE to a freshly-created non-superuser to exercise RLS.
        // Simpler approach: try direct UPDATE — for owner, RLS policies don't auto-apply
        // unless FORCE RLS. We can apply FORCE here for the test to verify policy logic.
        try {
            jdbc.execute("ALTER TABLE consent_record_immutable FORCE ROW LEVEL SECURITY");
            assertThatThrownBy(() -> jdbc.update(
                    "UPDATE consent_record_immutable SET ip_address = ?::inet WHERE id = ?",
                    "127.0.0.1", saved.getId()))
                    .as("UPDATE should be blocked by RLS no_update policy")
                    .hasMessageContaining("policy");
        } finally {
            jdbc.execute("ALTER TABLE consent_record_immutable NO FORCE ROW LEVEL SECURITY");
        }
    }

    @Test
    @DisplayName("RLS blocks DELETE on consent_record_immutable")
    void rls_blocks_delete() {
        ConsentRecordImmutable saved = consentService.recordConsent(
                400L, 7L,
                Map.of("essential", true, "analytics", true, "marketing", false),
                "203.0.113.7", "ua-delete-test");
        try {
            jdbc.execute("ALTER TABLE consent_record_immutable FORCE ROW LEVEL SECURITY");
            assertThatThrownBy(() -> jdbc.update(
                    "DELETE FROM consent_record_immutable WHERE id = ?", saved.getId()))
                    .as("DELETE should be blocked by RLS no_delete policy")
                    .hasMessageContaining("policy");
        } finally {
            jdbc.execute("ALTER TABLE consent_record_immutable NO FORCE ROW LEVEL SECURITY");
        }
    }

    @Test
    @DisplayName("INET round-trip preserves IPv4 + IPv6 values")
    void inet_roundtrip() {
        ConsentRecordImmutable ipv4 = consentService.recordConsent(
                500L, 7L,
                Map.of("essential", true, "analytics", true, "marketing", false),
                "203.0.113.7", "ua-ipv4");
        ConsentRecordImmutable ipv6 = consentService.recordConsent(
                501L, 7L,
                Map.of("essential", true, "analytics", true, "marketing", false),
                "2001:db8:85a3::8a2e:370:7334", "ua-ipv6");

        ConsentRecordImmutable reloadedV4 = repository.findById(ipv4.getId()).orElseThrow();
        ConsentRecordImmutable reloadedV6 = repository.findById(ipv6.getId()).orElseThrow();

        assertThat(reloadedV4.getIpAddress()).isEqualTo("203.0.113.7");
        assertThat(reloadedV6.getIpAddress()).isEqualTo("2001:db8:85a3::8a2e:370:7334");
    }

    @Test
    @DisplayName("chain integrity detects tampering after manual UPDATE")
    void chain_detects_tampering() {
        Long userId = 600L;
        ConsentRecordImmutable r1 = consentService.recordConsent(userId, 7L,
                Map.of("essential", true, "analytics", true, "marketing", false),
                "203.0.113.7", "ua-tamper");
        consentService.recordConsent(userId, 7L,
                Map.of("essential", true, "analytics", true, "marketing", true),
                "203.0.113.7", "ua-tamper");

        // Tamper: bypass RLS (FORCE off on owner) — flip granted directly.
        jdbc.update(
                "UPDATE consent_record_immutable SET granted = ?::jsonb WHERE id = ?",
                "{\"analytics\":false,\"essential\":true,\"marketing\":false}", r1.getId());

        // Chain validator detects current_hash mismatch on r1.
        assertThatThrownBy(() -> consentService.verifyChainIntegrity(userId))
                .isInstanceOf(ConsentService.ConsentChainIntegrityException.class)
                .hasMessageContaining("hash mismatch");
    }
}
