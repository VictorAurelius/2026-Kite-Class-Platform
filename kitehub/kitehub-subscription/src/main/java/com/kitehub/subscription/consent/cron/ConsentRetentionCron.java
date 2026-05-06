package com.kitehub.subscription.consent.cron;

import com.kitehub.subscription.consent.repository.ConsentRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Daily cron deleting consent_record rows older than the DR-03 retention
 * window (36 months from creation per
 * {@code documents/01-business/kitehub/marketing/rules.md} BR-PDPL-CONSENT-003).
 *
 * <p>Runs at 03:00 alongside {@code DataRetentionScheduler} so DB load is
 * batched into a single low-traffic window.</p>
 *
 * <p>Rationale for hard-delete (vs soft-delete): the entire purpose of
 * retention is purge — keeping a tombstone defeats the BR. Audit chain (if
 * needed downstream) is provided by event emission at write/revoke, not by
 * row preservation.</p>
 *
 * @since Wave 25 Bucket A — GAP-353b
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ConsentRetentionCron {

    /** DR-03 retention window — see BR-PDPL-CONSENT-003. */
    private static final int RETENTION_MONTHS = 36;

    private final ConsentRecordRepository repository;

    /** Daily 03:00 — same window as {@code DataRetentionScheduler}. */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpired() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusMonths(RETENTION_MONTHS);
        log.info("Starting consent_record retention purge — cutoff={}", cutoff);
        int deleted = repository.deleteByCreatedAtBefore(cutoff);
        log.info("Consent retention purge complete — deleted {} records older than {}",
                deleted, cutoff);
    }
}
