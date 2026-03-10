package com.kitehub.subscription.service;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.entity.Subscription;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.platform.domain.enums.SubscriptionStatus;
import com.kitehub.subscription.repository.InstanceRepository;
import com.kitehub.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for handling subscription renewal logic.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionRenewalService {

    private final SubscriptionRepository subscriptionRepository;
    private final InstanceRepository instanceRepository;
    private static final int GRACE_PERIOD_DAYS = 3;

    /**
     * Process subscription renewal.
     * If autoRenew is enabled, creates payment invoice and sends email.
     * If not renewed within grace period, suspends instance.
     *
     * @param subscriptionId UUID of the subscription
     * @return true if renewal initiated, false if manual payment required
     * @throws IllegalArgumentException if subscription not found
     */
    @Transactional
    public boolean processRenewal(UUID subscriptionId) {
        log.info("Processing renewal for subscription: {}", subscriptionId);

        Subscription subscription = subscriptionRepository.findById(subscriptionId)
            .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + subscriptionId));

        if (subscription.getStatus() != SubscriptionStatus.ACTIVE &&
            subscription.getStatus() != SubscriptionStatus.EXPIRED) {
            log.warn("Cannot renew subscription in status: {}", subscription.getStatus());
            return false;
        }

        if (!subscription.getAutoRenew()) {
            log.info("Auto-renew disabled for subscription: {}", subscriptionId);
            return false;
        }

        // TODO: Create payment invoice (integrate with Payment Service in PR 4.6)
        // For now, just extend the subscription
        LocalDateTime newExpiresAt = LocalDateTime.now().plusMonths(1);
        subscription.setExpiresAt(newExpiresAt);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscriptionRepository.save(subscription);

        // TODO: Send payment reminder email (integrate with Email Service in PR 4.12)
        log.info("Renewal email would be sent for subscription: {}", subscriptionId);

        log.info("Subscription renewed successfully: {} (expires: {})", subscriptionId, newExpiresAt);
        return true;
    }

    /**
     * Manually renew subscription (called from API).
     *
     * @param subscriptionId UUID of the subscription
     * @throws IllegalArgumentException if subscription not found
     */
    @Transactional
    public void manualRenewal(UUID subscriptionId) {
        log.info("Manual renewal requested for subscription: {}", subscriptionId);

        Subscription subscription = subscriptionRepository.findById(subscriptionId)
            .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + subscriptionId));

        if (subscription.getStatus() == SubscriptionStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot renew cancelled subscription: " + subscriptionId);
        }

        // Extend subscription by billing cycle
        LocalDateTime newExpiresAt = subscription.getExpiresAt().plusMonths(1);
        subscription.setExpiresAt(newExpiresAt);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscriptionRepository.save(subscription);

        // Update instance status if it was suspended
        Instance instance = instanceRepository.findById(subscription.getInstanceId())
            .orElseThrow(() -> new IllegalArgumentException("Instance not found: " + subscription.getInstanceId()));

        if (instance.getStatus() == InstanceStatus.SUSPENDED) {
            instance.setStatus(InstanceStatus.ACTIVE);
            instanceRepository.save(instance);
            log.info("Instance reactivated: {}", instance.getId());
        }

        log.info("Subscription renewed manually: {} (new expiry: {})", subscriptionId, newExpiresAt);
    }

    /**
     * Check if subscription is in grace period.
     * Grace period: 3 days after expiration where instance is still accessible (read-only).
     *
     * @param subscription Subscription to check
     * @return true if in grace period
     */
    public boolean isInGracePeriod(Subscription subscription) {
        if (subscription.getStatus() != SubscriptionStatus.EXPIRED) {
            return false;
        }

        LocalDateTime gracePeriodEnd = subscription.getExpiresAt().plusDays(GRACE_PERIOD_DAYS);
        return LocalDateTime.now().isBefore(gracePeriodEnd);
    }

    /**
     * Suspend instance when subscription expires and grace period ends.
     *
     * @param subscriptionId UUID of the subscription
     * @throws IllegalArgumentException if subscription not found
     */
    @Transactional
    public void suspendExpiredSubscription(UUID subscriptionId) {
        log.info("Suspending expired subscription: {}", subscriptionId);

        Subscription subscription = subscriptionRepository.findById(subscriptionId)
            .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + subscriptionId));

        if (subscription.getStatus() != SubscriptionStatus.EXPIRED) {
            log.warn("Cannot suspend non-expired subscription: {}", subscriptionId);
            return;
        }

        // Check if grace period has ended
        if (isInGracePeriod(subscription)) {
            log.info("Subscription still in grace period: {}", subscriptionId);
            return;
        }

        // Suspend instance
        Instance instance = instanceRepository.findById(subscription.getInstanceId())
            .orElseThrow(() -> new IllegalArgumentException("Instance not found: " + subscription.getInstanceId()));

        if (instance.getStatus() != InstanceStatus.SUSPENDED) {
            instance.suspend();
            instanceRepository.save(instance);
            log.info("Instance suspended due to expired subscription: {}", instance.getId());
        }

        // TODO: Send suspension notification email
        log.info("Suspension email would be sent for instance: {}", instance.getId());
    }

    /**
     * Get days until subscription expires.
     *
     * @param subscription Subscription to check
     * @return days until expiration (negative if already expired)
     */
    public long getDaysUntilExpiration(Subscription subscription) {
        if (subscription.getExpiresAt() == null) {
            return Long.MAX_VALUE;
        }

        return java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), subscription.getExpiresAt());
    }
}
