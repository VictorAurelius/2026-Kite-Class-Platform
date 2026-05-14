package com.kitehub.subscription.idempotency.scheduler;

import com.kitehub.subscription.idempotency.repository.IdempotencyKeyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Daily cleanup of expired idempotency rows (GAP-536 Wave 77 Bucket D).
 *
 * <p>Runs at 04:00 every day to delete rows whose {@code expires_at} is
 * in the past. The 24h TTL means table size stays bounded at ~1 day worth
 * of mutation requests, well within Postgres comfort for Phase 1 BETA
 * volume.</p>
 *
 * @since Wave 77 — GAP-536
 */
@Component
@Slf4j
public class IdempotencyCleanupJob {

    private final IdempotencyKeyRepository repository;

    public IdempotencyCleanupJob(IdempotencyKeyRepository repository) {
        this.repository = repository;
    }

    /** Daily 04:00 server-time. {@code @Scheduled} runs at fixed cron. */
    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void purgeExpired() {
        int deleted = repository.deleteExpired(OffsetDateTime.now());
        if (deleted > 0) {
            log.info("IdempotencyCleanupJob purged {} expired rows", deleted);
        }
    }
}
