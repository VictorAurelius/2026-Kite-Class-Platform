package com.kiteclass.core.module.childprotection.service;

import com.kiteclass.core.module.childprotection.repository.ChildProtectionAuditLogRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuditChainVerificationCron} — covers daily
 * hash-chain integrity verification (Phase 1C v1.5, GAP-359 sub-task 359.5).
 *
 * <p>Verifies Micrometer counter wiring (PASS path emits
 * {@code child_protection.audit.chain.verified{result=pass}}; FAIL path
 * additionally emits {@code child_protection.audit.chain.break}) and
 * per-chain isolation (one chain failure does not abort verification of
 * the rest).
 *
 * @since Wave 24 Bucket A — GAP-359 sub-task 359.5
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuditChainVerificationCron — daily integrity verification")
class AuditChainVerificationCronTest {

    private static final UUID TENANT_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TENANT_B = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String INCIDENT = "Incident";

    @Mock
    private ChildProtectionAuditLogRepository repository;

    @Mock
    private ChildProtectionAuditService auditService;

    private MeterRegistry meterRegistry;
    private AuditChainVerificationCron cron;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        cron = new AuditChainVerificationCron(repository, auditService, meterRegistry);
    }

    @Test
    @DisplayName("no chains → 0 breaks, no verification calls")
    void emptyChains_zeroBreaks() {
        when(repository.findDistinctChains()).thenReturn(List.of());

        int breaks = cron.verifyAllChains();

        assertThat(breaks).isZero();
        verify(auditService, never()).verifyChain(any(), any());
    }

    @Test
    @DisplayName("all chains pass → break counter zero, verified counter pass=N")
    void allChainsPass_zeroBreakCounter() {
        when(repository.findDistinctChains()).thenReturn(java.util.Arrays.<Object[]>asList(
                new Object[]{TENANT_A, INCIDENT},
                new Object[]{TENANT_B, INCIDENT}));
        when(auditService.verifyChain(any(UUID.class), eq(INCIDENT))).thenReturn(true);

        int breaks = cron.verifyAllChains();

        assertThat(breaks).isZero();
        // Break counter must NOT have been incremented
        double breakTotal = meterRegistry.find(AuditChainVerificationCron.METRIC_CHAIN_BREAK)
                .counters().stream().mapToDouble(c -> c.count()).sum();
        assertThat(breakTotal).isZero();
        // Verified counter PASS rows: 2 (one per tenant)
        double passTotal = meterRegistry.find(AuditChainVerificationCron.METRIC_CHAIN_VERIFIED)
                .tag("result", "pass")
                .counters().stream().mapToDouble(c -> c.count()).sum();
        assertThat(passTotal).isEqualTo(2.0);
    }

    @Test
    @DisplayName("tampered chain → break counter incremented + WARN logged")
    void tamperedChain_incrementsBreakCounter() {
        when(repository.findDistinctChains()).thenReturn(java.util.Arrays.<Object[]>asList(
                new Object[]{TENANT_A, INCIDENT}));
        when(auditService.verifyChain(TENANT_A, INCIDENT)).thenReturn(false);

        int breaks = cron.verifyAllChains();

        assertThat(breaks).isEqualTo(1);
        double breakCount = meterRegistry.find(AuditChainVerificationCron.METRIC_CHAIN_BREAK)
                .tag("instance", TENANT_A.toString())
                .tag("entityType", INCIDENT)
                .counter()
                .count();
        assertThat(breakCount).isEqualTo(1.0);
        // verified counter records FAIL result for this chain
        double failCount = meterRegistry.find(AuditChainVerificationCron.METRIC_CHAIN_VERIFIED)
                .tag("result", "fail")
                .counter()
                .count();
        assertThat(failCount).isEqualTo(1.0);
    }

    @Test
    @DisplayName("per-chain isolation — one failure does not abort the rest")
    void perChainIsolation_failureDoesNotAbort() {
        when(repository.findDistinctChains()).thenReturn(java.util.Arrays.<Object[]>asList(
                new Object[]{TENANT_A, INCIDENT},
                new Object[]{TENANT_B, INCIDENT}));
        when(auditService.verifyChain(TENANT_A, INCIDENT))
                .thenThrow(new RuntimeException("DB read fail"));
        when(auditService.verifyChain(TENANT_B, INCIDENT)).thenReturn(true);

        int breaks = cron.verifyAllChains();

        // Tenant A counted as a break (catch + ok=false), tenant B passed
        assertThat(breaks).isEqualTo(1);
        verify(auditService).verifyChain(TENANT_B, INCIDENT);
    }
}
