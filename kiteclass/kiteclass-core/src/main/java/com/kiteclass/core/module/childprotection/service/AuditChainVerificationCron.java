package com.kiteclass.core.module.childprotection.service;

import com.kiteclass.core.module.childprotection.repository.ChildProtectionAuditLogRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * AuditChainVerificationCron — daily hash-chain integrity verifier (Phase 1C
 * v1.5, GAP-359 sub-task 359.5).
 *
 * <p>Runs once per day at 02:30 (after {@link RetentionLifecycleServiceImpl}
 * sweeps at 02:00 so retention-driven appends land in the chain BEFORE
 * verification reads it). Iterates every distinct
 * {@code (instance_id, entity_type)} chain in
 * {@code child_protection_audit_log} and re-computes
 * {@code SHA-256(prev_hash || payload_json)} for each entry. On mismatch:
 * logs a WARN, increments
 * {@code child_protection.audit.chain.break{instance, entityType}} counter
 * (reuses the existing alert pattern that fires on rate-limit breach
 * spikes) and continues to the next chain — one tenant break must not
 * silence verification of the rest.
 *
 * <p>Per BR-CHILD-PROTECT-007 the V54 migration revokes DELETE on the
 * audit table from the typical app role; this cron is the second line of
 * defence detecting tamper that bypasses the GRANT (rogue DBA / direct SQL).
 *
 * <p>Operational runbook: {@code documents/05-guides/operations/audit-chain-break-runbook.md}.
 *
 * @since Wave 24 Bucket A — GAP-359 sub-task 359.5
 */
@Component
@Slf4j
public class AuditChainVerificationCron {

    /** Counter incremented on every chain-break detection. */
    public static final String METRIC_CHAIN_BREAK =
            "child_protection.audit.chain.break";

    /** Counter incremented per chain checked (PASS or FAIL). */
    public static final String METRIC_CHAIN_VERIFIED =
            "child_protection.audit.chain.verified";

    private final ChildProtectionAuditLogRepository repository;
    private final ChildProtectionAuditService auditService;
    private final MeterRegistry meterRegistry;

    public AuditChainVerificationCron(
            ChildProtectionAuditLogRepository repository,
            ChildProtectionAuditService auditService,
            MeterRegistry meterRegistry) {
        this.repository = repository;
        this.auditService = auditService;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Daily cron entry point. Cron expression {@code "0 30 2 * * *"} = 02:30
     * every day. Configurable via {@code childprotection.audit.verify.cron}
     * for dev/staging.
     */
    @Scheduled(cron = "${childprotection.audit.verify.cron:0 30 2 * * *}")
    public void scheduledVerification() {
        try {
            verifyAllChains();
        } catch (Exception ex) {
            // Cron must NEVER propagate — rely on logging + Micrometer alerting.
            log.error("Audit chain verification sweep failed", ex);
        }
    }

    /**
     * Verify every distinct chain currently present in the audit table.
     * Returns the number of chain-breaks detected so callers / tests can
     * assert behaviour deterministically.
     *
     * @return number of chains that failed integrity verification
     */
    public int verifyAllChains() {
        List<Object[]> chains = repository.findDistinctChains();
        if (chains.isEmpty()) {
            log.debug("Audit chain verification: no chains present yet");
            return 0;
        }

        int breaks = 0;
        for (Object[] row : chains) {
            UUID instanceId = (UUID) row[0];
            String entityType = (String) row[1];
            boolean ok = false;
            try {
                ok = auditService.verifyChain(instanceId, entityType);
            } catch (Exception ex) {
                log.error("Verification failed for chain instance={} entityType={}",
                        instanceId, entityType, ex);
            }

            String result = ok ? "pass" : "fail";
            Counter.builder(METRIC_CHAIN_VERIFIED)
                    .description("Number of child-protection audit chains verified per cron run")
                    .tags(Tags.of(
                            "instance", String.valueOf(instanceId),
                            "entityType", entityType,
                            "result", result))
                    .register(meterRegistry)
                    .increment();

            if (!ok) {
                breaks++;
                Counter.builder(METRIC_CHAIN_BREAK)
                        .description("Detected tamper in child-protection audit chain")
                        .tags(Tags.of(
                                "instance", String.valueOf(instanceId),
                                "entityType", entityType))
                        .register(meterRegistry)
                        .increment();
                log.warn("Audit chain BREAK detected: instance={} entityType={} — "
                                + "see audit-chain-break-runbook for remediation",
                        instanceId, entityType);
            }
        }

        log.info("Audit chain verification complete: {} chains checked, {} breaks",
                chains.size(), breaks);
        return breaks;
    }
}
