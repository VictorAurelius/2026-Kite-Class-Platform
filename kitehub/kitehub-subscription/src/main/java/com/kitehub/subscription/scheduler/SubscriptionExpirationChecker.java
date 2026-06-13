package com.kitehub.subscription.scheduler;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.entity.Payment;
import com.kitehub.platform.domain.entity.Subscription;
import com.kitehub.platform.domain.enums.PaymentStatus;
import com.kitehub.platform.domain.enums.SubscriptionStatus;
import com.kitehub.subscription.client.EmailServiceClient;
import com.kitehub.subscription.config.SubscriptionConfig;
import com.kitehub.subscription.notification.channel.OwnerNotificationDispatcher;
import com.kitehub.subscription.repository.InstanceRepository;
import com.kitehub.subscription.repository.PaymentRepository;
import com.kitehub.subscription.repository.SubscriptionRepository;
import com.kitehub.subscription.service.SubscriptionRenewalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

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
    private final PaymentRepository paymentRepository;
    private final SubscriptionRenewalService renewalService;
    private final EmailServiceClient emailServiceClient;
    private final SubscriptionConfig subscriptionConfig;
    private final OwnerNotificationDispatcher ownerNotificationDispatcher;

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
        int graceReminders = 0;

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
                    // GAP-1263: involuntary churn (non-payment lapse) → win-back outreach.
                    sendWinBackBestEffort(subscription.getInstanceId(), false);
                } else {
                    // GAP-1259 (SUB-23): still within the SUB-04 grace window → emit a
                    // dunning reminder ("còn X ngày trước suspend") instead of doing
                    // nothing. EmailServiceClient.alreadySentToday() de-dups per day.
                    if (sendGraceDunningReminder(subscription)) {
                        graceReminders++;
                    }
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
                // GAP-1263: voluntary cancel that has now lapsed → win-back outreach.
                sendWinBackBestEffort(subscription.getInstanceId(), true);
            } catch (Exception e) {
                log.error("Failed to suspend cancelled-expired subscription: {}", subscription.getId(), e);
            }
        }

        log.info("Expired subscription processing complete. Marked expired: {}, Suspended: {}, "
                + "Cancelled-expired suspended: {}, Grace dunning reminders: {}",
            markedExpired, suspended, cancelledSuspended, graceReminders);
    }

    /**
     * GAP-1259 (SUB-23): expire {@code Payment} rows stuck in PENDING past the configured
     * TTL and release the holding subscription's {@code pendingPaymentId}.
     *
     * <p>A VietQR/bank-transfer payment the owner never completes would otherwise pin
     * {@code pendingPaymentId} forever — {@code SubscriptionService} skips creating a new
     * payment while one is pending, so a fresh renewal/upgrade attempt is blocked. After
     * {@code kitehub.subscription.pending-payment-ttl-days} the payment is marked FAILED
     * and the subscription freed.</p>
     *
     * <p>PaymentStatus has no EXPIRED value (payments.status CHECK allows only
     * PENDING/COMPLETED/FAILED/REFUNDED/CANCELLED), so a timed-out PENDING payment is
     * recorded as FAILED — semantically the documented "timeout" failure.</p>
     *
     * <p>Runs daily at 10:30 AM (after expired-subscription processing).</p>
     */
    @Scheduled(cron = "0 30 10 * * *")
    public void processStalePendingPayments() {
        int ttlDays = subscriptionConfig.getPendingPaymentTtlDays();
        LocalDateTime cutoff = LocalDateTime.now().minusDays(ttlDays);
        log.info("Processing stale PENDING payments older than {} days (cutoff {})", ttlDays, cutoff);

        List<Payment> pendingPayments = paymentRepository.findPendingPayments();
        int expired = 0;
        int released = 0;

        for (Payment payment : pendingPayments) {
            try {
                if (payment.getCreatedAt() == null || !payment.getCreatedAt().isBefore(cutoff)) {
                    continue;  // still within TTL
                }

                payment.fail();  // PaymentStatus has no EXPIRED — FAILED = documented timeout
                paymentRepository.save(payment);
                expired++;
                log.info("Expired stale PENDING payment {} (created {}, ttl {}d)",
                    payment.getId(), payment.getCreatedAt(), ttlDays);

                // Release the subscription's pendingPaymentId so a fresh attempt is possible.
                if (payment.getSubscriptionId() != null) {
                    Subscription sub = subscriptionRepository.findById(payment.getSubscriptionId())
                        .orElse(null);
                    if (sub != null && payment.getId().equals(sub.getPendingPaymentId())) {
                        sub.setPendingPaymentId(null);
                        subscriptionRepository.save(sub);
                        released++;
                    }
                }
            } catch (Exception e) {
                log.error("Failed to expire stale PENDING payment: {}", payment.getId(), e);
            }
        }

        log.info("Stale PENDING payment processing complete. Expired: {}, pendingPaymentId released: {}",
            expired, released);
    }

    /**
     * GAP-1080 AC#2: sweep pre-existing orphan PENDING subscriptions.
     *
     * <p>A subscription created for the create-first-paid VietQR gate stays PENDING until
     * payment confirm (SUB-20). The dup-prevention guard stops NEW orphans, but legacy /
     * abandoned signup attempts can linger PENDING forever (instance never activated).
     * After {@code kitehub.subscription.orphan-pending-subscription-ttl-days} an
     * un-activated PENDING subscription is soft-deleted — its instance was never activated,
     * so no tenant data is affected.</p>
     *
     * <p>Runs daily at 10:45 AM.</p>
     */
    @Scheduled(cron = "0 45 10 * * *")
    public void processOrphanPendingSubscriptions() {
        int ttlDays = subscriptionConfig.getOrphanPendingSubscriptionTtlDays();
        LocalDateTime cutoff = LocalDateTime.now().minusDays(ttlDays);
        log.info("Sweeping orphan PENDING subscriptions older than {} days (cutoff {})", ttlDays, cutoff);

        List<Subscription> pendingSubs = subscriptionRepository.findByStatus(SubscriptionStatus.PENDING);
        int cleaned = 0;

        for (Subscription sub : pendingSubs) {
            try {
                if (sub.getCreatedAt() == null || !sub.getCreatedAt().isBefore(cutoff)) {
                    continue;  // still within grace; payment may yet complete
                }
                sub.softDelete();
                subscriptionRepository.save(sub);
                cleaned++;
                log.info("Soft-deleted orphan PENDING subscription {} (created {}, ttl {}d)",
                    sub.getId(), sub.getCreatedAt(), ttlDays);
            } catch (Exception e) {
                log.error("Failed to clean orphan PENDING subscription: {}", sub.getId(), e);
            }
        }

        log.info("Orphan PENDING subscription sweep complete. Cleaned: {}", cleaned);
    }

    /**
     * GAP-1263: best-effort win-back outreach after an instance is suspended (CTA → reactivate).
     *
     * <p>Delegates to {@link OwnerNotificationDispatcher#sendWinBack} — the IN_APP + EMAIL channels
     * each persist/deliver under {@code Propagation.REQUIRES_NEW} (per {@code design-patterns.md}
     * §3.11), so the notification side-effect can never roll back the suspend transaction. The
     * dispatcher already swallows per-channel failures; this extra try/catch guards the instance
     * lookup so a notification miss never aborts the scheduler sweep.</p>
     *
     * @param instanceId owning instance (may be {@code null} for malformed rows)
     * @param voluntary  {@code true} = owner cancelled; {@code false} = non-payment lapse
     */
    private void sendWinBackBestEffort(UUID instanceId, boolean voluntary) {
        if (instanceId == null) {
            return;
        }
        try {
            Instance instance = instanceRepository.findById(instanceId).orElse(null);
            if (instance != null) {
                ownerNotificationDispatcher.sendWinBack(instance, voluntary);
            }
        } catch (Exception e) {
            log.warn("Win-back notification failed for instance {}: {}", instanceId, e.getMessage());
        }
    }

    /**
     * GAP-1259 (SUB-23): emit a grace-period dunning reminder for an EXPIRED subscription
     * still within the SUB-04 grace window. Reuses the existing renewal-reminder email
     * (no new template — BE-4 owns templates) carrying the days remaining before suspend.
     *
     * @param subscription EXPIRED subscription within the grace window
     * @return true if a reminder was emitted
     */
    private boolean sendGraceDunningReminder(Subscription subscription) {
        Instance instance = instanceRepository.findById(subscription.getInstanceId()).orElse(null);
        if (instance == null || instance.getContactEmail() == null) {
            return false;
        }

        long daysUntilSuspend = daysUntilGraceEnd(subscription);
        try {
            emailServiceClient.sendRenewalReminder(
                instance.getId(),
                instance.getContactEmail(),
                instance.getOrganizationName(),
                daysUntilSuspend,
                subscription.getTier().name(),
                subscription.getPriceVnd()
            );
            log.info("Grace dunning reminder sent for subscription {} ({} day(s) before suspend)",
                subscription.getId(), daysUntilSuspend);
            return true;
        } catch (Exception e) {
            log.error("Failed to send grace dunning reminder for subscription: {}",
                subscription.getId(), e);
            return false;
        }
    }

    /**
     * Days remaining in the SUB-04 grace window before the instance is suspended.
     *
     * @param subscription EXPIRED subscription
     * @return non-negative days until grace end (0 on the final grace day)
     */
    private long daysUntilGraceEnd(Subscription subscription) {
        LocalDateTime graceEnd = subscription.getExpiresAt()
            .plusDays(subscriptionConfig.getGracePeriodDays());
        long days = ChronoUnit.DAYS.between(LocalDateTime.now(), graceEnd);
        return Math.max(0, days);
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
