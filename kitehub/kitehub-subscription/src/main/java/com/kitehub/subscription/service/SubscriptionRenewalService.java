package com.kitehub.subscription.service;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.entity.Payment;
import com.kitehub.platform.domain.entity.Subscription;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.platform.domain.enums.PaymentMethod;
import com.kitehub.platform.domain.enums.PaymentStatus;
import com.kitehub.platform.domain.enums.SubscriptionStatus;
import com.kitehub.subscription.client.EmailServiceClient;
import com.kitehub.subscription.config.SubscriptionConfig;
import com.kitehub.subscription.repository.InstanceRepository;
import com.kitehub.subscription.repository.PaymentRepository;
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
    private final PaymentRepository paymentRepository;
    private final EmailServiceClient emailServiceClient;
    private final SubscriptionConfig subscriptionConfig;
    private final VietQRService vietQRService;

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

        // Apply pending tier change if exists (downgrade scheduled at end of cycle)
        if (subscription.getPendingTier() != null) {
            log.info("Applying pending tier change from {} to {} for subscription {}",
                subscription.getTier(), subscription.getPendingTier(), subscriptionId);
            subscription.setTier(subscription.getPendingTier());
            subscription.setPriceVnd(subscription.getPendingTier().getPrice(subscription.getBillingCycle()));
            subscription.setPendingTier(null);  // Clear pending tier
        }

        // Create payment invoice for renewal
        Payment renewalPayment = createRenewalPayment(subscription);
        Payment savedPayment = paymentRepository.save(renewalPayment);

        // Extend subscription (will be activated when payment completes)
        LocalDateTime newExpiresAt = subscription.getExpiresAt().plusMonths(1);
        subscription.setExpiresAt(newExpiresAt);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setPendingPaymentId(savedPayment.getId());
        subscriptionRepository.save(subscription);

        log.info("Created renewal payment invoice: {} (amount: {} VNĐ)",
            savedPayment.getId(), savedPayment.getAmountVnd());

        // Payment reminder emails are sent by SubscriptionExpirationChecker scheduler
        // (7, 3, and 1 days before expiration)

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

        // KH-5 FM-2: a PENDING subscription was never activated (expiresAt is null until
        // applyPendingUpgrade runs). Renewing it would NPE on getExpiresAt().plusMonths().
        // Surface a 400 instead of a 500 — renewal only makes sense for an activated cycle.
        if (subscription.getStatus() == SubscriptionStatus.PENDING || subscription.getExpiresAt() == null) {
            throw new IllegalArgumentException(
                "Cannot renew a subscription that has not been activated: " + subscriptionId);
        }

        // GAP-1016: manual renewal must go through the payment gate. Previously this
        // method extended expiresAt + reactivated a suspended instance for free,
        // bypassing VietQR — a revenue leak. Now it creates a PENDING renewal payment
        // and records it as the pending payment; the actual cycle extension + instance
        // reactivation happen on payment confirm (PaymentService.confirmPayment →
        // SubscriptionService.applyPendingUpgrade renewal branch).
        if (subscription.getPendingPaymentId() != null) {
            log.info("Subscription {} already has a pending renewal payment {}; skipping duplicate",
                subscriptionId, subscription.getPendingPaymentId());
            return;
        }

        Payment renewalPayment = paymentRepository.save(createRenewalPayment(subscription));
        subscription.setPendingPaymentId(renewalPayment.getId());
        subscriptionRepository.save(subscription);

        log.info("Manual renewal payment invoice created: {} ({} VNĐ) for subscription {} — "
                + "cycle extension deferred until payment confirmed",
            renewalPayment.getId(), renewalPayment.getAmountVnd(), subscriptionId);
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

        LocalDateTime gracePeriodEnd = subscription.getExpiresAt().plusDays(subscriptionConfig.getGracePeriodDays());
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

        // Send suspension notification email
        if (instance.getContactEmail() != null) {
            emailServiceClient.sendSuspensionNotification(
                instance.getContactEmail(),
                instance.getOrganizationName()
            );
            log.info("Suspension notification sent to instance: {}", instance.getId());
        }
    }

    /**
     * GAP-1017: suspend the instance of an end-of-cycle cancellation past its expiry.
     *
     * <p>{@code immediate=true} cancellations suspend synchronously in
     * {@code SubscriptionService.cancelSubscription}; {@code immediate=false}
     * cancellations keep service until {@code expiresAt}, then this scheduler pass
     * suspends the instance.</p>
     *
     * @param subscriptionId UUID of the cancelled subscription
     */
    @Transactional
    public void suspendCancelledExpired(UUID subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
            .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + subscriptionId));

        if (subscription.getStatus() != SubscriptionStatus.CANCELLED) {
            return;
        }

        instanceRepository.findById(subscription.getInstanceId()).ifPresent(instance -> {
            if (instance.getStatus() != InstanceStatus.SUSPENDED) {
                instance.setStatus(InstanceStatus.SUSPENDED);
                instanceRepository.save(instance);
                log.info("Instance suspended for end-of-cycle cancelled subscription: {}", instance.getId());
            }
        });
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

    /**
     * Create payment invoice for subscription renewal.
     *
     * @param subscription Subscription to renew
     * @return Created payment record (not saved)
     */
    private Payment createRenewalPayment(Subscription subscription) {
        Payment payment = new Payment();
        payment.setSubscriptionId(subscription.getId());
        payment.setInstanceId(subscription.getInstanceId()); // V58 RLS: instance_id NOT NULL
        payment.setAmountVnd(subscription.getPriceVnd());
        payment.setCurrency("VND");
        payment.setPaymentMethod(PaymentMethod.VIETQR); // Default to VietQR for subscription payments
        payment.setStatus(PaymentStatus.PENDING);
        payment.setPaymentContent(String.format(
            "Subscription renewal for %s tier - %s (Instance: %s)",
            subscription.getTier(),
            subscription.getBillingCycle(),
            subscription.getInstanceId()
        ));
        // GAP-939: snapshot bank account info from VietQRService defaults so Owner
        // sees full transfer details on renewal payment page (parity with prorated
        // upgrade flow already fixed in SubscriptionService.createProratedPayment).
        payment.setQrCodeUrl(vietQRService.generateQRCode(
            UUID.randomUUID(), subscription.getPriceVnd(), subscription.getId()));
        payment.setBankCode(vietQRService.getBankCode());
        payment.setAccountNumber(vietQRService.getAccountNumber());
        payment.setAccountName(vietQRService.getAccountName());

        log.info("Created renewal payment invoice: {} VNĐ for subscription {}",
            subscription.getPriceVnd(), subscription.getId());

        return payment;
    }
}
