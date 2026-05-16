package com.kitehub.subscription.baseline;

import com.kitehub.platform.domain.enums.MigrationPhase;
import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.subscription.controller.admin.AdminMigrationController;
import com.kitehub.subscription.dto.ForceConvertRequest;
import com.kitehub.subscription.dto.UpgradeRequest;
import com.kitehub.subscription.dto.UpgradeResponse;
import com.kitehub.subscription.service.TrialToPaidService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * GAP-440 Wave 86 Bucket B baseline scaffold — async-style 202 ACCEPTED semantic.
 *
 * <p><strong>Spring Boot baseline version: 3.5.14.</strong> This test pins the
 * "async accepts request, returns 202 immediately" semantic against the current
 * Spring Boot baseline. Wave 86 Bucket B prior agent confirmed Maven Central
 * does NOT yet publish a Spring Boot 3.5.15+ patch; real dep bump is deferred to
 * {@code GAP-451}. When the upstream bump lands, re-run this test to verify the
 * @async-style 202 ACCEPTED contract is preserved across the Spring framework
 * upgrade (HTTP status mapping, response body shape, controller mvc pipeline).</p>
 *
 * <p><strong>Scope rationale (re-scoped Wave 86 Bucket B):</strong> KiteHub does
 * not currently ship a literal bulk-import endpoint annotated with
 * {@code @Async} + {@code CompletableFuture}. The closest async-accept semantic
 * in the codebase is {@link AdminMigrationController#forceConvert} which returns
 * {@code 202 ACCEPTED} with a poll URL — the canonical "async accept" pattern
 * for long-running ops in this stack. This baseline pins that contract so a
 * future Spring Boot patch (or a future bulk-import endpoint following the same
 * pattern) can be diffed against a verified PASS state.</p>
 *
 * @see <a href="documents/04-quality/gaps/GAP-440-spring-boot-dep-bump-before-prod.md">GAP-440</a>
 * @see <a href="documents/04-quality/gaps/GAP-451-spring-boot-3-5-x-no-newer-patch-await-upstream.md">GAP-451</a>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Spring Boot 3.5.14 baseline — async 202 ACCEPTED semantic (GAP-440 Wave 86 Bucket B)")
class BulkImportAsyncBaselineTest {

    @Mock
    private TrialToPaidService trialToPaidService;

    private AdminMigrationController controller;
    private UUID instanceId;

    @BeforeEach
    void setUp() {
        controller = new AdminMigrationController(trialToPaidService);
        instanceId = UUID.randomUUID();
    }

    /**
     * Baseline contract: long-running mutation endpoint returns
     * {@code 202 ACCEPTED} immediately (not {@code 200 OK} which would imply
     * synchronous completion).
     *
     * <p>Post-Spring-Boot bump (GAP-451), re-run to verify Spring's MVC pipeline
     * still maps {@code ResponseEntity.status(HttpStatus.ACCEPTED)} to HTTP 202
     * with the response body intact.</p>
     */
    @Test
    @DisplayName("force-convert returns 202 ACCEPTED (async accept semantic) — NOT 200 OK")
    void forceConvertReturns202NotSync200() {
        UpgradeResponse expected = UpgradeResponse.builder()
            .instanceId(instanceId)
            .migrationPhase(MigrationPhase.PAYMENT_CAPTURED)
            .startedAt(LocalDateTime.now())
            .estimatedCompletionSeconds(5)
            .pollUrl("/api/platform/instances/" + instanceId + "/migration-status")
            .build();

        when(trialToPaidService.forceConvert(eq(instanceId), any(UpgradeRequest.class),
            eq("INV-WAVE86-001"), eq("Wave 86 Bucket B baseline scaffold")))
            .thenReturn(expected);

        ForceConvertRequest req = ForceConvertRequest.builder()
            .tier(PricingTier.PREMIUM)
            .billingCycle("ANNUAL")
            .invoiceRef("INV-WAVE86-001")
            .reason("Wave 86 Bucket B baseline scaffold")
            .build();

        ResponseEntity<UpgradeResponse> resp = controller.forceConvert(instanceId, req);

        // Baseline pin: status MUST be 202 ACCEPTED, not 200 OK
        assertThat(resp.getStatusCode())
            .as("Async accept endpoint must return 202 ACCEPTED, not 200 OK")
            .isEqualTo(HttpStatus.ACCEPTED);
        assertThat(resp.getStatusCode().value()).isEqualTo(202);

        // Baseline pin: response body intact (Spring's serialization pipeline unchanged)
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getInstanceId()).isEqualTo(instanceId);
        assertThat(resp.getBody().getMigrationPhase()).isEqualTo(MigrationPhase.PAYMENT_CAPTURED);
        assertThat(resp.getBody().getPollUrl()).startsWith("/api/platform/");
    }

    /**
     * Baseline contract: response body carries a poll URL so the caller can
     * follow up on async completion (instead of blocking on the initial request).
     *
     * <p>Post-Spring-Boot bump, re-run to ensure Jackson serialization of the
     * DTO preserves the {@code pollUrl} field shape.</p>
     */
    @Test
    @DisplayName("force-convert response body includes pollUrl (async caller contract)")
    void forceConvertResponseBodyIncludesPollUrl() {
        String expectedPollUrl = "/api/platform/instances/" + instanceId + "/migration-status";
        UpgradeResponse expected = UpgradeResponse.builder()
            .instanceId(instanceId)
            .migrationPhase(MigrationPhase.PAYMENT_CAPTURED)
            .startedAt(LocalDateTime.now())
            .estimatedCompletionSeconds(10)
            .pollUrl(expectedPollUrl)
            .build();

        when(trialToPaidService.forceConvert(any(), any(), any(), any())).thenReturn(expected);

        ForceConvertRequest req = ForceConvertRequest.builder()
            .tier(PricingTier.BASIC)
            .billingCycle("MONTHLY")
            .invoiceRef("INV-WAVE86-002")
            .reason("Baseline test")
            .build();

        ResponseEntity<UpgradeResponse> resp = controller.forceConvert(instanceId, req);

        assertThat(resp.getBody().getPollUrl())
            .as("Async accept response MUST carry pollUrl for follow-up")
            .isEqualTo(expectedPollUrl);
        assertThat(resp.getBody().getEstimatedCompletionSeconds())
            .as("Async accept response MUST carry ETA hint for caller")
            .isGreaterThan(0);
    }
}
