package com.kitehub.subscription.beta;

import com.kitehub.subscription.beta.controller.BetaAccessController;
import com.kitehub.subscription.beta.dto.BetaTokenValidationResponse;
import com.kitehub.subscription.beta.entity.BetaAccessRequest;
import com.kitehub.subscription.beta.entity.BetaAccessRequestStatus;
import com.kitehub.subscription.beta.repository.BetaAccessRequestRepository;
import com.kitehub.subscription.beta.service.BetaAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GAP-610 root-cause reproduction IT — Wave onboarding-polish-2 Bucket E.
 *
 * <p><b>Investigation scope (per `audit-to-gap-pipeline.md` §2.8 fix-time
 * state-check + `release-fix-retry-budget.md` §3.5 investigation phase):</b>
 * verify all 3 H1/H2/H3 hypotheses empirically AND probe newly-surfaced H4
 * (data-state mismatch / lifecycle-collapse). This IT does NOT fix anything —
 * deliverable is empirical evidence of which layer returns "not found" cho
 * which token state combination.</p>
 *
 * <h3>Hypotheses tested layer-by-layer</h3>
 * <ol>
 *   <li><b>H1 — RLS bypass</b>: anonymous JPA call hides APPROVED rows<br>
 *       Static-analysis verdict (Wave 91 Bucket D PR #1490): REJECTED — V34 only
 *       enables RLS on {@code instance_id_tables} array NOT including
 *       {@code beta_access_request}; V50 only targets {@code admin_audit_logs}.<br>
 *       Empirical re-verification here via {@code findByInviteToken} hit on
 *       APPROVED row in anonymous JPA context (no SecurityContext, no MDC
 *       tenant).</li>
 *   <li><b>H2 — Public endpoint missing / path mismatch</b>: controller maybe
 *       not registered OR path differs.<br>
 *       Static verdict: REJECTED — {@code BetaAccessController.validateToken}
 *       declares {@code @GetMapping("/api/v1/auth/beta-signup/validate")}.<br>
 *       Empirical re-verification here via
 *       {@code RequestMappingHandlerMapping} introspection — list all
 *       handlers + confirm exact path mapped + handler bean exists.</li>
 *   <li><b>H3 — JPA query inference mismatch</b>: method-derived query
 *       binds UUID as varchar.<br>
 *       Static verdict: REJECTED — repository uses explicit
 *       {@code @Query("SELECT b FROM BetaAccessRequest b WHERE b.inviteToken = :token")}
 *       (defensive hardening Wave 91 Bucket D); Postgres {@code uuid} column +
 *       Hibernate native UUID binding.<br>
 *       Empirical re-verification here via real Postgres round-trip with
 *       random UUID + assert row retrievable.</li>
 *   <li><b>H4 (NEW — surfaced by Wave 91 Bucket D Phase 1 investigation 2026-05-18)</b>:
 *       data-state mismatch / lifecycle-collapse. {@code BetaAccessService.validateToken}
 *       returns {@code TOKEN_NOT_FOUND} for BOTH (a) row truly absent AND
 *       (b) row exists but {@code status != APPROVED} — conflates two
 *       semantically distinct failure modes. Production rst-cascade-1 walk
 *       2026-05-26 confirmed valid UUID → HTTP 404 + TOKEN_NOT_FOUND;
 *       hypothesis: target row was consumed (SIGNED_UP → invite_token nulled)
 *       OR pre-APPROVAL (PENDING) → service flattens to TOKEN_NOT_FOUND.</li>
 * </ol>
 *
 * <h3>Test infra notes</h3>
 *
 * <p>Same pattern as {@code BetaAccessRequestRepositoryPostgresIT} — full
 * {@code @SpringBootTest} ApplicationContext on Testcontainers Postgres 16;
 * {@code RabbitTemplate} mocked because
 * {@code application-test.yml} excludes {@code RabbitAutoConfiguration} but
 * {@code EmailServiceClient} requires the bean. Postgres-specific column
 * types ({@code uuid}, {@code timestamptz}) per
 * {@code postgres-specific-type-testcontainers.md} §1 mandate.</p>
 *
 * @since Wave onboarding-polish-2 Bucket E — GAP-610 investigation deliverable
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@DisplayName("GAP-610 root-cause repro — validate_token returns NOT_FOUND for valid token")
class BetaSignupTokenReproIT {

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

    @Autowired
    private BetaAccessService service;

    @Autowired
    private WebApplicationContext webContext;

    /** RabbitTemplate mock — see class javadoc. */
    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    void cleanState() {
        repository.deleteAll();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // H2 — Public endpoint missing / path mismatch
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("H2: BetaAccessController.validateToken IS registered at exact path /api/v1/auth/beta-signup/validate")
    void h2_endpoint_is_registered_at_expected_path() {
        // Use specific bean name — Spring Boot registers TWO RequestMappingHandlerMapping
        // beans (the main one + controllerEndpointHandlerMapping for actuator). Resolve
        // the primary one by name.
        RequestMappingHandlerMapping mapping = (RequestMappingHandlerMapping)
                webContext.getBean("requestMappingHandlerMapping");
        Map<?, HandlerMethod> handlerMap = mapping.getHandlerMethods();

        boolean validateEndpointFound = handlerMap.entrySet().stream()
                .anyMatch(e -> {
                    String key = e.getKey().toString();
                    return key.contains("/api/v1/auth/beta-signup/validate")
                            && e.getValue().getBeanType().equals(BetaAccessController.class)
                            && e.getValue().getMethod().getName().equals("validateToken");
                });

        assertThat(validateEndpointFound)
                .as("H2 hypothesis check — controller registration. "
                        + "Expected: BetaAccessController.validateToken registered at exact path "
                        + "/api/v1/auth/beta-signup/validate. "
                        + "Verdict: H2 %s",
                        validateEndpointFound ? "REJECTED (endpoint exists)" : "CONFIRMED (endpoint missing)")
                .isTrue();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // H1 — RLS bypass (anonymous JPA query)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("H1: APPROVED row visible to anonymous JPA call (no SecurityContext, no MDC) — repo layer")
    void h1_rls_does_not_hide_approved_row() {
        UUID token = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        BetaAccessRequest saved = repository.save(BetaAccessRequest.builder()
                .email("h1-test@example.com")
                .name("H1 Test")
                .orgName("H1 Org")
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

        // Anonymous JPA call — no SET LOCAL app.* (no Spring Security context, no tenant MDC).
        // Mirrors public endpoint condition.
        boolean foundViaRepo = repository.findByInviteToken(token).isPresent();

        assertThat(foundViaRepo)
                .as("H1 hypothesis check — RLS hides anonymous query. "
                        + "Expected if H1 TRUE: row NOT visible (RLS default-deny). "
                        + "Verdict: H1 %s",
                        foundViaRepo ? "REJECTED (anonymous visible)" : "CONFIRMED (RLS hides)")
                .isTrue();

        // Sanity: row truly exists in DB
        assertThat(repository.findById(saved.getId())).isPresent();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // H3 — JPA/UUID type binding mismatch
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("H3: explicit @Query findByInviteToken binds UUID natively (Postgres uuid column)")
    void h3_uuid_binding_round_trip_works() {
        UUID token = UUID.fromString("98446443-e5cc-43e9-9498-6799d460d2db");
        OffsetDateTime now = OffsetDateTime.now();

        BetaAccessRequest saved = repository.save(BetaAccessRequest.builder()
                .email("h3-test@example.com")
                .name("H3 Test")
                .orgName("H3 Org")
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

        BetaAccessRequest reloaded = repository.findByInviteToken(token)
                .orElseThrow(() -> new AssertionError(
                        "H3 CONFIRMED — UUID encoding mismatch (token not retrievable). "
                                + "Expected if H3 TRUE: empty Optional."));

        assertThat(reloaded.getInviteToken())
                .as("H3 hypothesis check — UUID round-trips correctly. "
                        + "Verdict: H3 REJECTED (UUID type binding works)")
                .isEqualTo(token);
        assertThat(reloaded.getId()).isEqualTo(saved.getId());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // H4 — Data-state mismatch / lifecycle-collapse (NEW)
    //
    // Per BetaAccessService.validateToken lines 540-557 — the SERVICE layer
    // converts BOTH "no row" AND "row.status != APPROVED" into TOKEN_NOT_FOUND
    // response. So a stale/consumed/pre-approval token returns the SAME 404 +
    // TOKEN_NOT_FOUND as a truly-absent token — operator/UI cannot distinguish.
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("H4 — repo finds row (regardless of status); service-layer collapses lifecycle to TOKEN_NOT_FOUND for non-APPROVED")
    void h4_service_layer_lifecycle_collapse_for_pending_row() {
        UUID token = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        // Seed row in PENDING (not yet approved) — token-bearing but pre-APPROVAL state.
        repository.save(BetaAccessRequest.builder()
                .email("h4-pending@example.com")
                .name("H4 Pending")
                .orgName("H4 Org")
                .persona("P2_CENTER_OWNER")
                .status(BetaAccessRequestStatus.PENDING)
                .inviteToken(token)             // unusual but valid — testing collapse logic
                .inviteTokenExpiry(now.plusHours(24))
                .consentGiven(true)
                .consentAt(now)
                .build());
        repository.flush();

        // Repo CAN find the row regardless of status (lifecycle filter is service-level)
        assertThat(repository.findByInviteToken(token))
                .as("Repo layer returns row regardless of status — fine for granular diagnostics")
                .isPresent();

        // Service layer collapses PENDING into TOKEN_NOT_FOUND (line 549-551)
        BetaTokenValidationResponse resp = service.validateToken(token);
        assertThat(resp.valid()).isFalse();
        assertThat(resp.errorCode())
                .as("H4 CONFIRMED — service.validateToken() returns TOKEN_NOT_FOUND "
                        + "for PENDING row even though row EXISTS in DB. "
                        + "Production behavior matches: user sees 404 + TOKEN_NOT_FOUND, "
                        + "operator cannot distinguish 'row missing' from 'row wrong-state'.")
                .isEqualTo("TOKEN_NOT_FOUND");
    }

    @Test
    @DisplayName("H4 sub-case: SIGNED_UP row → token cleared → really missing → still TOKEN_NOT_FOUND")
    void h4_signed_up_clears_token_returns_not_found() {
        UUID originalToken = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        // Seed APPROVED row, then simulate SIGNED_UP transition that clears invite_token
        BetaAccessRequest entity = repository.save(BetaAccessRequest.builder()
                .email("h4-signedup@example.com")
                .name("H4 Signedup")
                .orgName("H4 Org")
                .persona("P2_CENTER_OWNER")
                .status(BetaAccessRequestStatus.APPROVED)
                .inviteToken(originalToken)
                .inviteTokenExpiry(now.plusHours(24))
                .approvedAt(now)
                .inviteSentAt(now)
                .consentGiven(true)
                .consentAt(now)
                .build());
        repository.flush();

        // Simulate signup completion — BetaAccessService.completeBetaSignup() sets
        // status=SIGNED_UP + clears inviteToken to null (entity.java + service.java
        // line 581-584).
        entity.setStatus(BetaAccessRequestStatus.SIGNED_UP);
        entity.setInviteToken(null);
        entity.setInviteTokenExpiry(null);
        repository.save(entity);
        repository.flush();

        // Now validate using the ORIGINAL token UUID — operator/UI replays the email link
        BetaTokenValidationResponse resp = service.validateToken(originalToken);

        assertThat(resp.valid()).isFalse();
        assertThat(resp.errorCode())
                .as("H4 CONFIRMED: post-signup, invite_token cleared → "
                        + "repo cannot find row → service returns TOKEN_NOT_FOUND. "
                        + "Same response as 'token never existed' — lifecycle context lost.")
                .isEqualTo("TOKEN_NOT_FOUND");
    }

    @Test
    @DisplayName("H4 sub-case: EXPIRED APPROVED row returns TOKEN_EXPIRED (distinct error code — good signal)")
    void h4_expired_returns_distinct_code() {
        UUID token = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        repository.save(BetaAccessRequest.builder()
                .email("h4-expired@example.com")
                .name("H4 Expired")
                .orgName("H4 Org")
                .persona("P2_CENTER_OWNER")
                .status(BetaAccessRequestStatus.APPROVED)
                .inviteToken(token)
                .inviteTokenExpiry(now.minusHours(1))   // past
                .approvedAt(now.minusHours(25))
                .inviteSentAt(now.minusHours(25))
                .consentGiven(true)
                .consentAt(now.minusHours(25))
                .build());
        repository.flush();

        BetaTokenValidationResponse resp = service.validateToken(token);
        assertThat(resp.valid()).isFalse();
        assertThat(resp.errorCode())
                .as("Expired-token branch correctly differentiated — TOKEN_EXPIRED, NOT TOKEN_NOT_FOUND")
                .isEqualTo("TOKEN_EXPIRED");
    }

    @Test
    @DisplayName("Happy path: APPROVED + non-expired + token present → valid=true")
    void happy_path_approved_not_expired_returns_valid() {
        UUID token = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        repository.save(BetaAccessRequest.builder()
                .email("happy@example.com")
                .name("Happy")
                .orgName("Happy Org")
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

        BetaTokenValidationResponse resp = service.validateToken(token);
        assertThat(resp.valid())
                .as("Happy path baseline — confirms repo+service work when row state is RIGHT. "
                        + "Production failure ≠ infra; data-state mismatch is the only remaining hypothesis.")
                .isTrue();
        assertThat(resp.email()).isEqualTo("happy@example.com");
        assertThat(resp.errorCode()).isNull();
    }
}
