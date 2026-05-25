package com.kitehub.subscription.beta.repository;

import com.kitehub.subscription.beta.entity.BetaAccessRequest;
import com.kitehub.subscription.beta.entity.BetaAccessRequestStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link BetaAccessRequestRepository} backing GAP-610
 * defensive fix (Wave 91 Bucket D).
 *
 * <p>The bug filed in GAP-610 reported {@code GET /api/v1/auth/beta-signup/validate}
 * returned {@code TOKEN_NOT_FOUND} cho a DB row that explicitly contains the
 * matching {@code invite_token} UUID. Three root-cause hypotheses were
 * proposed:</p>
 *
 * <ol>
 *   <li><b>RLS hides anonymous queries</b> — NOT confirmed by state-check
 *       ({@code V34__enable_rls_tenant_scoped_tables.sql} skips
 *       {@code beta_access_request}; the only other RLS migration {@code V50}
 *       targets {@code admin_audit_logs}).</li>
 *   <li><b>UUID encoding mismatch</b> — NOT confirmed (entity declares
 *       {@code private UUID inviteToken}; PostgreSQL column declared
 *       {@code uuid} per V28; Hibernate binds natively).</li>
 *   <li><b>JPA query inference mismatch</b> — NOT confirmed (method-derived
 *       {@code findByInviteToken(UUID)} is the canonical Spring Data shape).</li>
 * </ol>
 *
 * <p>This test exercises a Testcontainers Postgres round-trip end-to-end so
 * any regression of the three hypotheses (e.g., someone adds RLS without a
 * public bypass policy, or downgrades the column type, or swaps the entity to
 * String) is caught at PR-time instead of in production. Aligns với
 * {@code .claude/rules/postgres-specific-type-testcontainers.md} §1 mandate
 * (Postgres-specific type requires real Postgres, not H2).</p>
 *
 * @since Wave 91 Bucket D — GAP-610
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@DisplayName("BetaAccessRequestRepository — Postgres findByInviteToken (GAP-610)")
class BetaAccessRequestRepositoryPostgresIT {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("kitehub_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.dialect",
                () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Autowired
    private BetaAccessRequestRepository repository;

    /**
     * RabbitMQ is excluded từ test profile (per {@code application-test.yml}
     * {@code spring.autoconfigure.exclude: RabbitAutoConfiguration}) nhưng
     * {@code EmailServiceClient} requires {@code RabbitTemplate} as a non-
     * optional constructor param. Mock the bean so {@code @SpringBootTest}
     * full ApplicationContext loads — repository-level tests don't exercise
     * email queue paths anyway.
     *
     * @since Wave beta-readiness-5 Bucket C — GAP-610 IT unblock
     */
    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    void cleanState() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("findByInviteToken returns row for APPROVED entity (anonymous query)")
    void findByInviteToken_returnsApprovedRow() {
        UUID token = UUID.fromString("98446443-e5cc-43e9-9498-6799d460d2db");
        OffsetDateTime now = OffsetDateTime.now();

        BetaAccessRequest saved = repository.save(BetaAccessRequest.builder()
                .email("mvann1207@gmail.com")
                .name("Test Invitee")
                .orgName("Test Center")
                .persona("P2_CENTER_OWNER")
                .status(BetaAccessRequestStatus.APPROVED)
                .inviteToken(token)
                .inviteTokenExpiry(now.plusHours(24))
                .approvedAt(now)
                .inviteSentAt(now)
                .consentGiven(true)
                .consentAt(now)
                .build());
        repository.flush();

        // Re-query — no transaction context, no `SET LOCAL app.*` settings,
        // mirrors the public anonymous endpoint condition (no JWT, no tenant
        // header → no Spring Security context).
        Optional<BetaAccessRequest> found = repository.findByInviteToken(token);

        assertThat(found).as("APPROVED row with matching invite_token must be visible to anonymous JPA query")
                .isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getEmail()).isEqualTo("mvann1207@gmail.com");
        assertThat(found.get().getStatus()).isEqualTo(BetaAccessRequestStatus.APPROVED);
    }

    @Test
    @DisplayName("findByInviteToken returns empty for unknown token (no false positive)")
    void findByInviteToken_returnsEmptyForUnknown() {
        UUID known = UUID.randomUUID();
        UUID unknown = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        repository.save(BetaAccessRequest.builder()
                .email("seeded@example.com")
                .name("Seeded")
                .orgName("Org")
                .persona("P2_CENTER_OWNER")
                .status(BetaAccessRequestStatus.APPROVED)
                .inviteToken(known)
                .inviteTokenExpiry(now.plusHours(24))
                .approvedAt(now)
                .inviteSentAt(now)
                .consentGiven(true)
                .consentAt(now)
                .build());
        repository.flush();

        assertThat(repository.findByInviteToken(unknown)).isEmpty();
    }

    // -----------------------------------------------------------------------
    // GAP-600 Wave 92 Bucket C — abort cleanup query tests
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("GAP-600: markStaleAsAborted flips PENDING rows older than threshold → ABORTED")
    void markStaleAsAborted_flipsPendingOlderThanThreshold() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime stale = now.minusHours(30); // older than 24h threshold

        BetaAccessRequest staleRow = repository.save(BetaAccessRequest.builder()
                .email("stale@example.com")
                .name("Stale User")
                .orgName("Stale Org")
                .persona("P2_CENTER_OWNER")
                .status(BetaAccessRequestStatus.PENDING)
                .consentGiven(true)
                .consentAt(stale)
                .createdAt(stale)
                .updatedAt(stale)
                .build());
        repository.flush();

        OffsetDateTime threshold = now.minusHours(24);
        int aborted = repository.markStaleAsAborted(threshold, now);

        assertThat(aborted).as("1 stale row should be flipped").isEqualTo(1);

        BetaAccessRequest reloaded = repository.findById(staleRow.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(BetaAccessRequestStatus.ABORTED);
        // Audit trail preserved — row NOT deleted
        assertThat(reloaded.getEmail()).isEqualTo("stale@example.com");
    }

    @Test
    @DisplayName("GAP-600: markStaleAsAborted leaves PENDING rows newer than threshold untouched")
    void markStaleAsAborted_leavesFreshPendingUntouched() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime fresh = now.minusHours(6); // within 24h threshold

        BetaAccessRequest freshRow = repository.save(BetaAccessRequest.builder()
                .email("fresh@example.com")
                .name("Fresh User")
                .orgName("Fresh Org")
                .persona("P2_CENTER_OWNER")
                .status(BetaAccessRequestStatus.PENDING)
                .consentGiven(true)
                .consentAt(fresh)
                .createdAt(fresh)
                .updatedAt(fresh)
                .build());
        repository.flush();

        OffsetDateTime threshold = now.minusHours(24);
        int aborted = repository.markStaleAsAborted(threshold, now);

        assertThat(aborted).as("fresh row should NOT be swept").isZero();
        assertThat(repository.findById(freshRow.getId()).orElseThrow().getStatus())
                .isEqualTo(BetaAccessRequestStatus.PENDING);
    }

    @Test
    @DisplayName("GAP-600: markStaleAsAborted leaves APPROVED/REJECTED/SIGNED_UP untouched (status filter)")
    void markStaleAsAborted_leavesNonPendingUntouched() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime stale = now.minusHours(30);

        // APPROVED row stale — should NOT be swept (only PENDING swept)
        BetaAccessRequest approvedStale = repository.save(BetaAccessRequest.builder()
                .email("approved-stale@example.com")
                .name("Approved Stale")
                .orgName("Org")
                .persona("P2_CENTER_OWNER")
                .status(BetaAccessRequestStatus.APPROVED)
                .inviteToken(UUID.randomUUID())
                .inviteTokenExpiry(stale.plusHours(24))
                .approvedAt(stale)
                .inviteSentAt(stale)
                .consentGiven(true)
                .consentAt(stale)
                .createdAt(stale)
                .updatedAt(stale)
                .build());
        repository.flush();

        OffsetDateTime threshold = now.minusHours(24);
        int aborted = repository.markStaleAsAborted(threshold, now);

        assertThat(aborted).as("APPROVED row must NOT be swept regardless of age").isZero();
        assertThat(repository.findById(approvedStale.getId()).orElseThrow().getStatus())
                .isEqualTo(BetaAccessRequestStatus.APPROVED);
    }

    @Test
    @DisplayName("GAP-600: countStalePending returns accurate count before bulk update")
    void countStalePending_returnsAccurateCount() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime stale = now.minusHours(30);
        OffsetDateTime fresh = now.minusHours(6);

        // 2 stale PENDING
        for (int i = 0; i < 2; i++) {
            repository.save(BetaAccessRequest.builder()
                    .email("stale-" + i + "@example.com")
                    .name("Stale " + i)
                    .orgName("Org")
                    .persona("P2_CENTER_OWNER")
                    .status(BetaAccessRequestStatus.PENDING)
                    .consentGiven(true)
                    .consentAt(stale)
                    .createdAt(stale)
                    .updatedAt(stale)
                    .build());
        }
        // 1 fresh PENDING (should not count)
        repository.save(BetaAccessRequest.builder()
                .email("fresh@example.com")
                .name("Fresh")
                .orgName("Org")
                .persona("P2_CENTER_OWNER")
                .status(BetaAccessRequestStatus.PENDING)
                .consentGiven(true)
                .consentAt(fresh)
                .createdAt(fresh)
                .updatedAt(fresh)
                .build());
        repository.flush();

        long count = repository.countStalePending(now.minusHours(24));

        assertThat(count).as("only the 2 stale PENDING rows should count").isEqualTo(2L);
    }

    @Test
    @DisplayName("GAP-600 unique-constraint regression: ABORTED row → user re-submit cùng email succeeds")
    void aborted_row_allows_resubmit_same_email() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime stale = now.minusHours(30);

        // Step 1: PENDING row stale → swept ABORTED
        BetaAccessRequest aborted = repository.save(BetaAccessRequest.builder()
                .email("resubmit@example.com")
                .name("Resubmit User")
                .orgName("Org")
                .persona("P2_CENTER_OWNER")
                .status(BetaAccessRequestStatus.PENDING)
                .consentGiven(true)
                .consentAt(stale)
                .createdAt(stale)
                .updatedAt(stale)
                .build());
        repository.flush();

        repository.markStaleAsAborted(now.minusHours(24), now);
        repository.flush();

        BetaAccessRequest reloaded = repository.findById(aborted.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(BetaAccessRequestStatus.ABORTED);

        // Step 2: user re-submit cùng email — phải success (NOT 409 unique violation)
        // Unique constraint chỉ on (invite_token) — KHÔNG on email. Service-level guard
        // (findFirstByEmailAndStatusOrderByCreatedAtDesc) chỉ catch active PENDING.
        // Sau khi ABORTED, không còn active PENDING với email đó → re-submit OK.
        BetaAccessRequest resubmit = repository.save(BetaAccessRequest.builder()
                .email("resubmit@example.com")
                .name("Resubmit User Again")
                .orgName("Org")
                .persona("P2_CENTER_OWNER")
                .status(BetaAccessRequestStatus.PENDING)
                .consentGiven(true)
                .consentAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build());
        repository.flush();

        assertThat(resubmit.getId()).as("re-submit must succeed with new row id").isNotEqualTo(aborted.getId());

        // Verify: 2 rows tổng với cùng email; 1 ABORTED + 1 PENDING
        // (Service code chỉ query active PENDING khi check duplicate, nên ABORTED không block)
        assertThat(repository.findFirstByEmailAndStatusOrderByCreatedAtDesc(
                "resubmit@example.com", BetaAccessRequestStatus.PENDING))
                .as("active PENDING row should be the fresh re-submit").isPresent()
                .get()
                .extracting(BetaAccessRequest::getId)
                .isEqualTo(resubmit.getId());
    }

    @Test
    @DisplayName("findByInviteToken finds non-APPROVED rows too (lifecycle filter is service-level)")
    void findByInviteToken_findsRegardlessOfStatus() {
        // Lifecycle-status filter (only APPROVED + non-expired + non-SIGNED_UP is
        // considered valid for signup) lives in BetaAccessService.validateToken,
        // not in the repository query. Repository must return the row regardless
        // of status so the service can produce granular error codes
        // (TOKEN_NOT_FOUND vs ALREADY_USED vs TOKEN_EXPIRED).
        UUID tokenPending = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        repository.save(BetaAccessRequest.builder()
                .email("pending@example.com")
                .name("Pending User")
                .orgName("Pending Org")
                .persona("P2_CENTER_OWNER")
                .status(BetaAccessRequestStatus.PENDING)
                .inviteToken(tokenPending)
                .inviteTokenExpiry(now.plusHours(24))
                .consentGiven(true)
                .consentAt(now)
                .build());
        repository.flush();

        assertThat(repository.findByInviteToken(tokenPending))
                .as("PENDING row with token must still be retrievable so service can emit lifecycle error code")
                .isPresent();
    }
}
