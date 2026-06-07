package com.kitehub.subscription.scheduler;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.entity.Subscription;
import com.kitehub.platform.domain.enums.SubscriptionStatus;
import com.kitehub.subscription.client.EmailServiceClient;
import com.kitehub.subscription.config.SubscriptionConfig;
import com.kitehub.subscription.repository.InstanceRepository;
import com.kitehub.subscription.repository.SubscriptionRepository;
import com.kitehub.subscription.service.SubscriptionRenewalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduler for checking subscription expiration and sending renewal reminders.
 * Runs daily at 9:00 AM to process expiring subscriptions.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionExpirationChecker {

    private final SubscriptionRepository subscriptionRepository;
    private final InstanceRepository instanceRepository;
    private final SubscriptionRenewalService renewalService;
    private final EmailServiceClient emailServiceClient;
    private final SubscriptionConfig subscriptionConfig;

    /**
     * Daily job to check expiring subscriptions and send reminders.
     * Runs at 9:00 AM every day.
     *
     * Reminder schedule:
     * - 7 days before expiration
     * - 3 days before expiration
     * - 1 day before expiration
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void checkExpiringSubscriptions() {
        log.info("Starting daily subscription expiration check");

        LocalDateTime now = LocalDateTime.now();

        // Find subscriptions expiring in 7 days
        LocalDateTime sevenDaysFromNow = now.plusDays(7);
        List<Subscription> expiringSoon = subscriptionRepository.findExpiringBetween(
            now,
            sevenDaysFromNow,
            SubscriptionStatus.ACTIVE
        );

        int reminders7Days = 0;
        int reminders3Days = 0;
        int reminders1Day = 0;

        for (Subscription subscription : expiringSoon) {
            long daysUntilExpiration = renewalService.getDaysUntilExpiration(subscription);

            // Send reminder at configurable days before expiration
            if (subscriptionConfig.getWarningDays().contains((int) daysUntilExpiration)) {
                sendRenewalReminder(subscription, daysUntilExpiration);

                if (daysUntilExpiration == 7) reminders7Days++;
                else if (daysUntilExpiration == 3) reminders3Days++;
                else if (daysUntilExpiration == 1) reminders1Day++;
            }
        }

        log.info("Sent renewal reminders: 7-day={}, 3-day={}, 1-day={}",
            reminders7Days, reminders3Days, reminders1Day);
    }

    /**
     * Daily job to process expired subscriptions.
     * Runs at 10:00 AM every day (after expiration check).
     *
     * - Marks subscriptions as EXPIRED if past expiration date
     * - Suspends instances after grace period ends
     */
    @Scheduled(cron = "0 0 10 * * *")
    public void processExpiredSubscriptions() {
        log.info("Starting expired subscription processing");

        LocalDateTime now = LocalDateTime.now();

        // Find expired subscriptions (past expiration date)
        List<Subscription> expiredSubscriptions = subscriptionRepository.findExpiredSubscriptions(now);
        log.info("Found {} expired subscriptions", expiredSubscriptions.size());

        int markedExpired = 0;
        int suspended = 0;

        for (Subscription subscription : expiredSubscriptions) {
            try {
                // Mark as expired if still active
                if (subscription.getStatus() == SubscriptionStatus.ACTIVE) {
                    subscription.setStatus(SubscriptionStatus.EXPIRED);
                    subscriptionRepository.save(subscription);
                    markedExpired++;
                    log.info("Marked subscription as expired: {}", subscription.getId());
                }

                // Check if grace period has ended
                if (!renewalService.isInGracePeriod(subscription)) {
                    renewalService.suspendExpiredSubscription(subscription.getId());
                    suspended++;
                }

            } catch (Exception e) {
                log.error("Failed to process expired subscription: {}", subscription.getId(), e);
            }
        }

        // GAP-1017: suspend instances of end-of-cycle (immediate=false) cancellations
        // once their expiry has passed — findExpiredSubscriptions skips CANCELLED.
        List<Subscription> cancelledExpired = subscriptionRepository.findCancelledExpiredSubscriptions(now);
        int cancelledSuspended = 0;
        for (Subscription subscription : cancelledExpired) {
            try {
                renewalService.suspendCancelledExpired(subscription.getId());
                cancelledSuspended++;
            } catch (Exception e) {
                log.error("Failed to suspend cancelled-expired subscription: {}", subscription.getId(), e);
            }
        }

        log.info("Expired subscription processing complete. Marked expired: {}, Suspended: {}, "
                + "Cancelled-expired suspended: {}",
            markedExpired, suspended, cancelledSuspended);
    }

    /**
     * Send renewal reminder email.
     *
     * @param subscription Subscription to send reminder for
     * @param daysUntilExpiration Days until expiration
     */
    private void sendRenewalReminder(Subscription subscription, long daysUntilExpiration) {
        try {
            // Get instance for contact email
            Instance instance = instanceRepository.findById(subscription.getInstanceId())
                .orElse(null);

            if (instance != null && instance.getContactEmail() != null) {
                emailServiceClient.sendRenewalReminder(
                    instance.getContactEmail(),
                    instance.getOrganizationName(),
                    daysUntilExpiration,
                    subscription.getTier().name(),
                    subscription.getPriceVnd()
                );
                log.info("Renewal reminder sent for subscription: {} ({} days until expiration)",
                    subscription.getId(), daysUntilExpiration);
            } else {
                log.warn("Cannot send renewal reminder - instance or contact email not found for subscription: {}",
                    subscription.getId());
            }
        } catch (Exception e) {
            log.error("Failed to send renewal reminder for subscription: {}", subscription.getId(), e);
        }
    }

    /**
     * Manual trigger for testing purposes.
     * Can be called via admin API to trigger expiration check manually.
     */
    public void triggerManualCheck() {
        log.info("Manual expiration check triggered");
        checkExpiringSubscriptions();
        processExpiredSubscriptions();
    }
}
