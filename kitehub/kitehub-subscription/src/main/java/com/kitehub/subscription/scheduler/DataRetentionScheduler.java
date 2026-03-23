package com.kitehub.subscription.scheduler;

import com.kitehub.subscription.service.DataRetentionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job for data retention processing.
 * Runs daily at 3:00 AM to check retention warnings and expired data.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataRetentionScheduler {

    private final DataRetentionService retentionService;

    /**
     * Daily data retention check.
     * Sends warnings for instances nearing retention expiry,
     * and deletes data for instances past retention period.
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void checkRetention() {
        log.info("Starting daily data retention check");

        int warningsSent = retentionService.processRetentionWarnings();
        int deletedCount = retentionService.processExpiredRetention();

        log.info("Data retention check completed: {} warnings sent, {} instances deleted",
            warningsSent, deletedCount);
    }
}
