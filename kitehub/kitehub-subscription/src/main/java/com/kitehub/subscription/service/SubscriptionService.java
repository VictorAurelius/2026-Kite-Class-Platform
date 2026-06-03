package com.kitehub.subscription.service;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.entity.Payment;
import com.kitehub.platform.domain.entity.Subscription;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.platform.domain.enums.PaymentMethod;
import com.kitehub.platform.domain.enums.PaymentStatus;
import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.platform.domain.enums.SubscriptionStatus;
import com.kitehub.subscription.dto.CreateSubscriptionRequest;
import com.kitehub.subscription.dto.SubscriptionResponse;
import com.kitehub.subscription.repository.InstanceRepository;
import com.kitehub.subscription.repository.PaymentRepository;
import com.kitehub.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing subscriptions.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final InstanceRepository instanceRepository;
    private final PaymentRepository paymentRepository;
    private final VietQRService vietQRService;
    private final com.kitehub.subscription.client.EmailServiceClient emailServiceClient;

    /**
     * Create a new subscription for instance.
     *
     * @param request Create subscription request
     * @return Created subscription response
     */
    @Transactional
    public SubscriptionResponse createSubscription(CreateSubscriptionRequest request) {
        log.info("Creating subscription for instance: {}", request.getInstanceId());

        // Validate instance exists
        Instance instance = instanceRepository.findById(request.getInstanceId())
            .orElseThrow(() -> new IllegalArgumentException("Instance not found: " + request.getInstanceId()));

        // Check if instance already has active subscription
        subscriptionRepository.findActiveByInstanceId(request.getInstanceId())
            .ifPresent(sub -> {
                throw new IllegalArgumentException("Instance already has active subscription: " + sub.getId());
            });

        // Validate tier (FREE cannot have paid subscription)
        if (request.getTier() == PricingTier.FREE) {
            throw new IllegalArgumentException("Cannot create subscription for FREE tier");
        }

        // Calculate price based on tier and billing cycle
        long price = request.getTier().getPrice(request.getBillingCycle());

        // Create subscription
        Subscription subscription = new Subscription();
        subscription.setInstanceId(request.getInstanceId());
        subscription.setTier(request.getTier());
        subscription.setBillingCycle(request.getBillingCycle());
        subscription.setPriceVnd(price);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setStartedAt(LocalDateTime.now());
        subscription.setExpiresAt(calculateExpiryDate(LocalDateTime.now(), request.getBillingCycle()));
        subscription.setAutoRenew(request.getAutoRenew() != null ? request.getAutoRenew() : true);

        Subscription saved = subscriptionRepository.save(subscription);

        // Update instance status to ACTIVE
        instance.setStatus(InstanceStatus.ACTIVE);
        instance.setSubscriptionId(saved.getId());
        instance.setSubscriptionExpiresAt(saved.getExpiresAt());
        instanceRepository.save(instance);

        log.info("Created subscription: {} for instance: {}", saved.getId(), request.getInstanceId());

        // Send subscription created email
        try {
            emailServiceClient.sendSubscriptionCreatedEmail(
                instance.getId(),
                instance.getContactEmail(),
                instance.getOrganizationName(),
                request.getTier().name(),
                request.getBillingCycle().name()
            );
        } catch (Exception e) {
            log.error("Failed to send subscription created email for instance: {}", instance.getId(), e);
        }

        return SubscriptionResponse.fromEntity(saved);
    }

    /**
     * Get subscription by ID.
     *
     * @param subscriptionId Subscription UUID
     * @return Subscription response
     */
    @Transactional(readOnly = true)
    public SubscriptionResponse getSubscription(UUID subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
            .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + subscriptionId));

        return SubscriptionResponse.fromEntity(subscription);
    }

    /**
     * Get active subscription for instance.
     *
     * @param instanceId Instance UUID
     * @return Subscription response
     */
    @Transactional(readOnly = true)
    public SubscriptionResponse getActiveSubscription(UUID instanceId) {
        Subscription subscription = subscriptionRepository.findActiveByInstanceId(instanceId)
            .orElseThrow(() -> new IllegalArgumentException("No active subscription found for instance: " + instanceId));

        return SubscriptionResponse.fromEntity(subscription);
    }

    /**
     * Get all subscriptions for instance.
     *
     * @param instanceId Instance UUID
     * @return List of subscription responses
     */
    @Transactional(readOnly = true)
    public List<SubscriptionResponse> getSubscriptionsByInstance(UUID instanceId) {
        return subscriptionRepository.findByInstanceId(instanceId)
            .stream()
            .map(SubscriptionResponse::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Upgrade subscription to higher tier.
     *
     * <p>Phase 1 BETA uses manual bank transfer/VietQR. The new tier is NOT
     * applied immediately; it is stored as pending state and applied only after
     * the pending payment is confirmed by a platform admin.</p>
     *
     * @param subscriptionId Subscription UUID
     * @param newTier New pricing tier
     * @return Updated subscription response
     */
    @Transactional
    public SubscriptionResponse upgradeSubscription(UUID subscriptionId, PricingTier newTier) {
        log.info("Creating pending upgrade for subscription {} to tier {}", subscriptionId, newTier);

        Subscription subscription = subscriptionRepository.findById(subscriptionId)
            .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + subscriptionId));

        // Validate upgrade
        if (newTier.ordinal() <= subscription.getTier().ordinal()) {
            throw new IllegalArgumentException("Can only upgrade to higher tier. Use downgrade for lower tiers.");
        }

        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new IllegalArgumentException("Can only upgrade active subscriptions");
        }

        if (subscription.getPendingPaymentId() != null && subscription.getPendingTier() != null) {
            if (subscription.getPendingTier() == newTier) {
                return SubscriptionResponse.fromEntity(subscription);
            }
            throw new IllegalArgumentException("Subscription already has a pending upgrade payment");
        }

        // Calculate prorated charge
        long daysLeft = Math.max(0, ChronoUnit.DAYS.between(LocalDateTime.now(), subscription.getExpiresAt()));
        long proratedCharge = calculateProratedCharge(subscription.getTier(), newTier, daysLeft, subscription.getBillingCycle());

        log.info("Prorated charge for upgrade: {} VNĐ ({} days left)", proratedCharge, daysLeft);

        Payment savedPayment = paymentRepository.findLatestPendingBySubscriptionId(subscriptionId)
            .orElseGet(() -> paymentRepository.save(createProratedPayment(subscription, proratedCharge)));

        subscription.setPendingTier(newTier);
        subscription.setPendingPaymentId(savedPayment.getId());

        Subscription updated = subscriptionRepository.save(subscription);

        log.info("Pending upgrade created: subscription={} targetTier={} payment={}",
            subscriptionId, newTier, savedPayment.getId());

        return SubscriptionResponse.fromEntity(updated);
    }

    /**
     * Downgrade subscription to lower tier.
     * Downgrade happens at end of current billing cycle.
     *
     * @param subscriptionId Subscription UUID
     * @param newTier New pricing tier
     * @return Updated subscription response
     */
    @Transactional
    public SubscriptionResponse downgradeSubscription(UUID subscriptionId, PricingTier newTier) {
        log.info("Downgrading subscription {} to tier {}", subscriptionId, newTier);

        Subscription subscription = subscriptionRepository.findById(subscriptionId)
            .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + subscriptionId));

        // Validate downgrade
        if (newTier.ordinal() >= subscription.getTier().ordinal()) {
            throw new IllegalArgumentException("Can only downgrade to lower tier. Use upgrade for higher tiers.");
        }

        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new IllegalArgumentException("Can only downgrade active subscriptions");
        }

        // Downgrade happens at end of cycle - store as pending change
        subscription.setPendingTier(newTier);

        Subscription updated = subscriptionRepository.save(subscription);

        log.info("Scheduled downgrade for subscription: {} to tier {} (will apply at {})",
            subscriptionId, newTier, subscription.getExpiresAt());

        return SubscriptionResponse.fromEntity(updated);
    }

    /**
     * Cancel subscription.
     *
     * @param subscriptionId Subscription UUID
     * @param immediate If true, cancel immediately. If false, cancel at end of cycle.
     */
    @Transactional
    public void cancelSubscription(UUID subscriptionId, boolean immediate) {
        log.info("Cancelling subscription {} (immediate: {})", subscriptionId, immediate);

        Subscription subscription = subscriptionRepository.findById(subscriptionId)
            .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + subscriptionId));

        if (subscription.getStatus() == SubscriptionStatus.CANCELLED) {
            log.warn("Subscription already cancelled: {}", subscriptionId);
            return;
        }

        if (immediate) {
            // Immediate cancellation
            subscription.cancel();
            subscription.setExpiresAt(LocalDateTime.now());
            subscription.setAutoRenew(false);
        } else {
            // Cancel at end of cycle
            subscription.cancel();
            subscription.setAutoRenew(false);
            // Expiry date stays same, but won't renew
        }

        subscriptionRepository.save(subscription);

        log.info("Cancelled subscription: {}", subscriptionId);
    }

    /**
     * Activate subscription after payment completed.
     * Changes subscription status to ACTIVE and updates expiry date.
     *
     * @param subscriptionId Subscription UUID
     * @return Activated subscription response
     */
    @Transactional
    public SubscriptionResponse activateSubscription(UUID subscriptionId) {
        log.info("Activating subscription: {}", subscriptionId);

        Subscription subscription = subscriptionRepository.findById(subscriptionId)
            .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + subscriptionId));

        // Verify subscription is not already active
        if (subscription.getStatus() == SubscriptionStatus.ACTIVE) {
            log.warn("Subscription already active: {}", subscriptionId);
            return SubscriptionResponse.fromEntity(subscription);
        }

        // Activate subscription
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setStartedAt(LocalDateTime.now());
        subscription.setExpiresAt(calculateExpiryDate(LocalDateTime.now(), subscription.getBillingCycle()));

        Subscription activated = subscriptionRepository.save(subscription);

        // Update instance status to ACTIVE
        Instance instance = instanceRepository.findById(subscription.getInstanceId())
            .orElseThrow(() -> new IllegalArgumentException("Instance not found: " + subscription.getInstanceId()));

        instance.setStatus(InstanceStatus.ACTIVE);
        instance.setSubscriptionExpiresAt(activated.getExpiresAt());
        instanceRepository.save(instance);

        log.info("Activated subscription: {} for instance: {}", subscriptionId, instance.getId());

        return SubscriptionResponse.fromEntity(activated);
    }

    /**
     * Get subscriptions expiring within the next 30 days.
     *
     * @return List of expiring subscriptions
     */
    @Transactional(readOnly = true)
    public List<SubscriptionResponse> getExpiringSubscriptions() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thirtyDaysFromNow = now.plusDays(30);

        List<Subscription> expiringSubscriptions = subscriptionRepository.findExpiringBetween(
            now,
            thirtyDaysFromNow,
            SubscriptionStatus.ACTIVE
        );

        return expiringSubscriptions.stream()
            .map(SubscriptionResponse::fromEntity)
            .toList();
    }

    /**
     * Calculate prorated charge for tier upgrade.
     *
     * @param oldTier Current tier
     * @param newTier New tier
     * @param daysLeft Days left in current cycle
     * @param billingCycle Billing cycle
     * @return Prorated charge in VNĐ
     */
    public long calculateProratedCharge(PricingTier oldTier, PricingTier newTier, long daysLeft, com.kitehub.platform.domain.enums.BillingCycle billingCycle) {
        long oldPrice = oldTier.getPrice(billingCycle);
        long newPrice = newTier.getPrice(billingCycle);
        long priceDiff = newPrice - oldPrice;

        // Calculate daily rate
        long cycleDays = billingCycle == com.kitehub.platform.domain.enums.BillingCycle.ANNUALLY ? 365 : 30;
        double dailyRate = (double) priceDiff / cycleDays;

        // Prorated charge = daily rate * days left
        return Math.round(dailyRate * daysLeft);
    }

    /**
     * Create payment record for prorated tier upgrade charge.
     *
     * @param subscription Subscription being upgraded
     * @param amount Prorated amount to charge
     * @return Created payment record (not saved)
     */
    private Payment createProratedPayment(Subscription subscription, long amount) {
        Payment payment = new Payment();
        payment.setSubscriptionId(subscription.getId());
        payment.setAmountVnd(amount);
        payment.setCurrency("VND");
        payment.setPaymentMethod(PaymentMethod.VIETQR);
        payment.setStatus(PaymentStatus.PENDING);

        String paymentContent = vietQRService.generatePaymentContent(subscription.getId());
        payment.setPaymentContent(paymentContent);
        payment.setQrCodeUrl(vietQRService.generateQRCode(UUID.randomUUID(), amount, subscription.getId()));
        payment.setBankCode(vietQRService.getBankCode());
        payment.setAccountNumber(vietQRService.getAccountNumber());
        payment.setAccountName(vietQRService.getAccountName());

        log.info("Created pending upgrade payment record: {} VNĐ for subscription {}",
            amount, subscription.getId());

        return payment;
    }

    /**
     * Apply a pending upgrade after its payment is confirmed.
     *
     * @param subscriptionId subscription UUID from the confirmed payment
     * @param paymentId confirmed payment UUID
     */
    @Transactional
    public void applyPendingUpgrade(UUID subscriptionId, UUID paymentId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
            .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + subscriptionId));

        if (subscription.getPendingPaymentId() == null || !subscription.getPendingPaymentId().equals(paymentId)) {
            log.info("Payment {} is not the pending upgrade payment for subscription {}; skipping tier apply",
                paymentId, subscriptionId);
            return;
        }

        if (subscription.getPendingTier() == null) {
            log.warn("Subscription {} has pending payment {} but no pending tier", subscriptionId, paymentId);
            subscription.setPendingPaymentId(null);
            subscriptionRepository.save(subscription);
            return;
        }

        PricingTier targetTier = subscription.getPendingTier();
        subscription.setTier(targetTier);
        subscription.setPriceVnd(targetTier.getPrice(subscription.getBillingCycle()));
        subscription.setPendingTier(null);
        subscription.setPendingPaymentId(null);
        subscriptionRepository.save(subscription);

        log.info("Applied pending upgrade for subscription {} to tier {}", subscriptionId, targetTier);
    }

    /**
     * Clear pending upgrade state after payment rejection.
     *
     * @param subscriptionId subscription UUID from the rejected payment
     * @param paymentId rejected payment UUID
     */
    @Transactional
    public void clearPendingUpgrade(UUID subscriptionId, UUID paymentId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
            .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + subscriptionId));

        if (subscription.getPendingPaymentId() == null || !subscription.getPendingPaymentId().equals(paymentId)) {
            log.info("Payment {} is not the pending upgrade payment for subscription {}; skipping pending clear",
                paymentId, subscriptionId);
            return;
        }

        subscription.setPendingTier(null);
        subscription.setPendingPaymentId(null);
        subscriptionRepository.save(subscription);
        log.info("Cleared pending upgrade state for subscription {} after payment {}", subscriptionId, paymentId);
    }

    /**
     * Calculate expiry date based on billing cycle.
     *
     * @param startDate Start date
     * @param billingCycle Billing cycle
     * @return Expiry date
     */
    private LocalDateTime calculateExpiryDate(LocalDateTime startDate, com.kitehub.platform.domain.enums.BillingCycle billingCycle) {
        return billingCycle == com.kitehub.platform.domain.enums.BillingCycle.ANNUALLY
            ? startDate.plusYears(1)
            : startDate.plusMonths(1);
    }
}
