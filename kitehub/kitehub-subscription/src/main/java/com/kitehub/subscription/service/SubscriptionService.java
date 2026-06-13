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
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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
    // GAP-1256 (SUB-21): the single instance.tier sync point. applyPendingUpgrade
    // (create-flow activation + upgrade apply) routes through this helper so the
    // denormalized instances.tier never drifts from the active subscription tier.
    private final InstanceTierSyncService instanceTierSyncService;

    // Phase 1 BETA (SUB-20 create + upgrade): when enabled, the PENDING payment + VietQR charge the
    // symbolic override (default 10.000đ) via a real bank transfer instead of the full tier price.
    // subscription.priceVnd keeps the real contractual amount. Mirrors PaymentService beta gate so the
    // create-subscription path no longer bypasses the override (KH-3 G2 walk fix — QR was charging 1.5M).
    @org.springframework.beans.factory.annotation.Value("${kitehub.payment.beta-mode.enabled:false}")
    private boolean betaModeEnabled;

    @org.springframework.beans.factory.annotation.Value("${kitehub.payment.beta-mode.override-amount-vnd:10000}")
    private long betaOverrideAmountVnd;

    /**
     * Create a new subscription for instance — Phase 1 BETA manual VietQR gate (SUB-20).
     *
     * <p>Pre-rule code marked {@code status=ACTIVE} immediately, allowing Owner
     * to self-grant any paid tier without payment. This was caught at Wave
     * flow-kh3 G1 walk 2026-06-04 (UC-SUB-01 walkthrough).</p>
     *
     * <p>New behavior mirrors {@link #upgradeSubscription} manual VietQR pattern:
     * subscription is persisted with {@code status=PENDING, tier=FREE,
     * pendingTier=<requested>, billingCycle=<requested>, priceVnd=<calculated>},
     * a {@code Payment PENDING} for the full tier price is created via
     * {@code VietQRService}, and the response carries {@code pendingPaymentId}
     * for the FE to redirect to {@code /billing/payment/{pendingPaymentId}}.</p>
     *
     * <p>Instance activation + subscription-created email are deferred to
     * {@link #applyPendingUpgrade} which runs after the admin confirms payment
     * via {@code PaymentService.confirmPayment} (UC-SUB-07).</p>
     *
     * @param request Create subscription request
     * @return Created subscription response carrying {@code pendingPaymentId}
     */
    @Transactional
    public SubscriptionResponse createSubscription(CreateSubscriptionRequest request) {
        log.info("Creating PENDING subscription (SUB-20 manual VietQR gate) for instance: {}",
            request.getInstanceId());

        // Validate instance exists — body not used in createSubscription per SUB-20
        // (instance activation deferred to applyPendingUpgrade). Existence check only.
        if (!instanceRepository.existsById(request.getInstanceId())) {
            throw new IllegalArgumentException("Instance not found: " + request.getInstanceId());
        }

        // Check if instance already has active subscription
        subscriptionRepository.findActiveByInstanceId(request.getInstanceId())
            .ifPresent(sub -> {
                throw new IllegalArgumentException("Instance already has active subscription: " + sub.getId());
            });

        // Validate tier (FREE cannot have paid subscription)
        if (request.getTier() == PricingTier.FREE) {
            throw new IllegalArgumentException("Cannot create subscription for FREE tier");
        }

        // GAP-1080: idempotency — findActiveByInstanceId only matches ACTIVE, so without this
        // guard every retry (FE double-click / curl re-run) spawns a fresh PENDING row + a
        // duplicate VietQR Payment (KH-3 G2 walk 2026-06-09: instance had 3 PENDING rows).
        // Same-tier retry → return the existing PENDING subscription (idempotent).
        // Different-tier request while a PENDING exists → 409, resolve the in-flight one first.
        java.util.Optional<Subscription> existingPending = subscriptionRepository
            .findByInstanceId(request.getInstanceId())
            .stream()
            .filter(s -> s.getStatus() == SubscriptionStatus.PENDING)
            .findFirst();
        if (existingPending.isPresent()) {
            Subscription pending = existingPending.get();
            if (pending.getPendingTier() == request.getTier()) {
                log.info("Instance {} already has PENDING subscription {} for tier {} — returning existing "
                        + "(idempotent, GAP-1080)", request.getInstanceId(), pending.getId(), request.getTier());
                return SubscriptionResponse.fromEntity(pending);
            }
            throw new com.kitehub.subscription.exception.SubscriptionConflictException(
                "Instance already has a pending subscription (" + pending.getId() + ", tier "
                    + pending.getPendingTier() + "); resolve or cancel it before creating another");
        }

        // Calculate price based on requested tier and billing cycle
        long price = request.getTier().getPrice(request.getBillingCycle());

        // SUB-20: persist subscription with PENDING/FREE state, requested tier in pendingTier.
        // tier flips to requested + status flips to ACTIVE only after applyPendingUpgrade runs
        // (driven by PaymentService.confirmPayment per UC-SUB-07).
        Subscription subscription = new Subscription();
        subscription.setInstanceId(request.getInstanceId());
        subscription.setTier(PricingTier.FREE);
        subscription.setPendingTier(request.getTier());
        subscription.setBillingCycle(request.getBillingCycle());
        subscription.setPriceVnd(price);
        subscription.setStatus(SubscriptionStatus.PENDING);
        // startedAt + expiresAt remain null until activation; computed in applyPendingUpgrade.
        subscription.setAutoRenew(request.getAutoRenew() != null ? request.getAutoRenew() : true);

        Subscription saved = subscriptionRepository.save(subscription);

        // Spawn Payment PENDING for the full tier price (reuses upgrade-path helper for VietQR setup).
        Payment savedPayment = paymentRepository.save(createPendingPayment(saved, price));
        saved.setPendingPaymentId(savedPayment.getId());
        saved = subscriptionRepository.save(saved);

        log.info("Created PENDING subscription: {} (pendingTier={}, pendingPaymentId={}) for instance: {}",
            saved.getId(), request.getTier(), savedPayment.getId(), request.getInstanceId());

        // NOTE: instance activation + subscription-created email deferred to applyPendingUpgrade
        // after admin confirms payment (SUB-20).

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
        // No active subscription = not-found (bình thường cho TRIAL tenant chưa nâng cấp) → 404,
        // KHÔNG phải 400. EntityNotFoundException map 404 qua GlobalExceptionHandler; FE treat
        // 404 = "no active sub → tier FREE" (GAP-1079).
        Subscription subscription = subscriptionRepository.findActiveByInstanceId(instanceId)
            .orElseThrow(() -> new EntityNotFoundException("No active subscription found for instance: " + instanceId));

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
            .orElseGet(() -> paymentRepository.save(createPendingPayment(subscription, proratedCharge)));

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

        // GAP-1018 bug 4 (FM-10b): downgrade to FREE is inconsistent with createSubscription which
        // forbids FREE per SUB-01 (a paid subscription never holds tier FREE). Allowing it would let
        // auto-renew later mint a 0₫ payment for a FREE "subscription". Cancel ends the subscription;
        // downgrade only moves between paid tiers.
        if (newTier == PricingTier.FREE) {
            throw new IllegalArgumentException(
                "Cannot downgrade to FREE tier (consistent with create per SUB-01). "
                    + "Cancel the subscription to drop to FREE.");
        }

        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new IllegalArgumentException("Can only downgrade active subscriptions");
        }

        // KH-5 FM-5: refuse to schedule a downgrade while a paid tier change (upgrade) is
        // pending. Overwriting pendingTier here would leave pendingPaymentId pointing at the
        // upgrade's payment — admin confirms it and the owner pays an upgrade price for a
        // recorded downgrade. Mirror the guard in upgradeSubscription().
        if (subscription.getPendingPaymentId() != null) {
            throw new IllegalArgumentException(
                "Subscription has a pending tier change payment; resolve or cancel it before downgrading");
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

        // GAP-1017: cancellation must propagate to the instance, otherwise the owner
        // keeps using the service indefinitely after cancelling.
        // - immediate=true  → suspend the instance now.
        // - immediate=false → the expiry scheduler suspends it when expiresAt passes
        //   (see SubscriptionRenewalService.suspendCancelledExpired).
        if (immediate) {
            instanceRepository.findById(subscription.getInstanceId()).ifPresent(instance -> {
                if (instance.getStatus() != InstanceStatus.SUSPENDED) {
                    instance.setStatus(InstanceStatus.SUSPENDED);
                    instanceRepository.save(instance);
                    log.info("Instance suspended on immediate cancellation: {}", instance.getId());
                }
            });
        }

        log.info("Cancelled subscription: {}", subscriptionId);
    }

    // GAP-1096: removed dead-code activateSubscription(UUID). It had 0 production callers
    // (superseded by applyPendingUpgrade create-flow per SUB-20) and set status without
    // syncing instances.tier — a latent split-brain (same class as GAP-1090) if ever
    // re-wired. Activation now flows exclusively through applyPendingUpgrade (tier synced
    // via InstanceTierSyncService per SUB-21).

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
     * Create a PENDING Payment record for a manual VietQR transfer.
     *
     * <p>Used by both:</p>
     * <ul>
     *   <li>{@link #createSubscription} — first-time paid creation (SUB-20),
     *       amount = full requested-tier price.</li>
     *   <li>{@link #upgradeSubscription} — upgrade to higher tier (UC-SUB-02),
     *       amount = prorated charge.</li>
     * </ul>
     *
     * @param subscription Subscription owning the payment
     * @param amount Amount in VND (full price for create, prorated for upgrade)
     * @return Created payment record (not saved)
     */
    private Payment createPendingPayment(Subscription subscription, long amount) {
        // Phase 1 BETA: charge the symbolic override (10.000đ) via a real transfer when beta-mode is
        // enabled, instead of the full tier price. subscription.priceVnd keeps the real amount (set by
        // the caller); only the PENDING Payment + VietQR QR use the beta amount.
        long effectiveAmount = betaModeEnabled ? betaOverrideAmountVnd : amount;
        Payment payment = new Payment();
        payment.setSubscriptionId(subscription.getId());
        payment.setInstanceId(subscription.getInstanceId()); // V58 RLS: instance_id NOT NULL
        payment.setAmountVnd(effectiveAmount);
        payment.setCurrency("VND");
        payment.setPaymentMethod(PaymentMethod.VIETQR);
        payment.setStatus(PaymentStatus.PENDING);

        // Bug D fix (KH-3 G2 SePay walk): the QR memo (addInfo) MUST equal payment.txnRef
        // (KH3SUB<8hex>) so the bank-transfer description SePay forwards carries the token
        // PaymentService.processSepayWebhook matches on. The create path previously used a
        // "KITECLASS <subId>" memo + never set txnRef → SePay could never confirm the payment.
        String txnRef = PaymentService.generateTxnRef(UUID.randomUUID());
        payment.setTxnRef(txnRef);
        payment.setPaymentContent(txnRef);
        payment.setQrCodeUrl(vietQRService.generateQRCode(UUID.randomUUID(), effectiveAmount, txnRef));
        payment.setBankCode(vietQRService.getBankCode());
        payment.setAccountNumber(vietQRService.getAccountNumber());
        payment.setAccountName(vietQRService.getAccountName());

        log.info("Created pending payment record: {} VNĐ (beta-mode={}, txnRef={}) for subscription {}",
            effectiveAmount, betaModeEnabled, txnRef, subscription.getId());

        return payment;
    }

    /**
     * Apply a pending tier change after its payment is confirmed.
     *
     * <p>Handles two flow variants — both gated by the same admin-confirm step:</p>
     * <ul>
     *   <li><strong>Create-flow (SUB-20):</strong> subscription was created with
     *       {@code status=PENDING, tier=FREE, pendingTier=<requested>} by
     *       {@link #createSubscription}. On confirm: flip to ACTIVE, set
     *       {@code startedAt}/{@code expiresAt}, activate instance, send
     *       subscription-created email.</li>
     *   <li><strong>Upgrade-flow (UC-SUB-02):</strong> subscription was already
     *       ACTIVE at a lower tier; only {@code pendingTier} + {@code priceVnd}
     *       need to flip. Instance is already ACTIVE; no email.</li>
     * </ul>
     *
     * @param subscriptionId subscription UUID from the confirmed payment
     * @param paymentId confirmed payment UUID
     */
    // GAP-1062: REQUIRES_NEW isolates this best-effort downstream step from the
    // caller's payment-capture transaction. All 3 callers (PaymentService verify /
    // SePay webhook / manual confirm) do `payment.complete()` then call this in a
    // try/catch with the documented intent "payment captured; upgrade retried by
    // admin/job". With default REQUIRED this method joined the caller's txn, so any
    // throw here (soft-deleted subscription, optimistic lock, instance missing) set
    // the shared txn rollback-only → the caller's commit threw UnexpectedRollbackException
    // → the payment.complete() write was rolled back despite the try/catch. Same class
    // as the 2026-05-16 admin-login 500 (audit-service-isolation.md / design-patterns §3.11).
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void applyPendingUpgrade(UUID subscriptionId, UUID paymentId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
            .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + subscriptionId));

        if (subscription.getPendingPaymentId() == null || !subscription.getPendingPaymentId().equals(paymentId)) {
            log.info("Payment {} is not the pending payment for subscription {}; skipping tier apply",
                paymentId, subscriptionId);
            return;
        }

        if (subscription.getPendingTier() == null) {
            // GAP-1016: a confirmed pending payment with no tier change = manual renewal.
            // Extend the cycle + reactivate a suspended instance now that payment cleared,
            // instead of manualRenewal having extended for free at request time.
            applyConfirmedRenewal(subscription, paymentId);
            return;
        }

        PricingTier targetTier = subscription.getPendingTier();
        boolean isCreateFlow = subscription.getStatus() == SubscriptionStatus.PENDING;

        // GAP-1018 bug 2 (FM-7): a confirmed payment with a pendingTier that is NOT an upgrade
        // (lower-or-equal ordinal, never a PENDING create-flow) means a manual-renewal payment
        // cleared while a downgrade was scheduled (downgradeSubscription sets pendingTier but no
        // payment; only renewal/upgrade create payments, and upgrade is higher-ordinal). Route to
        // the renewal path so the cycle is extended AND the scheduled downgrade is applied — the
        // upgrade branch below would change tier without extending the cycle (owner pays renewal,
        // gets no extra time + downgrade left half-applied).
        if (!isCreateFlow && targetTier.ordinal() <= subscription.getTier().ordinal()) {
            applyConfirmedRenewal(subscription, paymentId);
            return;
        }

        subscription.setTier(targetTier);
        subscription.setPriceVnd(targetTier.getPrice(subscription.getBillingCycle()));
        subscription.setPendingTier(null);
        subscription.setPendingPaymentId(null);

        if (isCreateFlow) {
            // SUB-20 create-flow: flip to ACTIVE + initialize lifecycle dates.
            LocalDateTime now = LocalDateTime.now();
            subscription.setStatus(SubscriptionStatus.ACTIVE);
            subscription.setStartedAt(now);
            subscription.setExpiresAt(calculateExpiryDate(now, subscription.getBillingCycle()));
        }

        Subscription saved = subscriptionRepository.save(subscription);

        if (isCreateFlow) {
            // Activate instance + send subscription-created email (deferred from createSubscription).
            Instance instance = instanceRepository.findById(saved.getInstanceId())
                .orElseThrow(() -> new IllegalArgumentException(
                    "Instance not found: " + saved.getInstanceId()));
            instance.setStatus(InstanceStatus.ACTIVE);
            // GAP-1256 (SUB-21): sync instances.tier to the activated paid tier via the single
            // sync point. instance.tier is load-bearing (connection-pool size
            // MultiTenantDataSourceConfig, custom-domain eligibility DomainService, data-retention
            // window DataRetentionService); the create flow previously only flipped
            // subscriptions.tier, leaving instances.tier stuck at FREE (GAP-1090).
            instanceTierSyncService.syncInstanceTier(instance, targetTier);
            instance.setSubscriptionId(saved.getId());
            instance.setSubscriptionExpiresAt(saved.getExpiresAt());
            instanceRepository.save(instance);

            try {
                emailServiceClient.sendSubscriptionCreatedEmail(
                    instance.getId(),
                    instance.getContactEmail(),
                    instance.getOrganizationName(),
                    targetTier.name(),
                    saved.getBillingCycle().name()
                );
            } catch (Exception e) {
                log.error("Failed to send subscription created email for instance: {}",
                    instance.getId(), e);
            }

            log.info("Activated PENDING subscription {} to tier {} (create-flow SUB-20)",
                subscriptionId, targetTier);
        } else {
            // GAP-1256 (SUB-21): upgrade-flow syncs instances.tier to the new active tier via the
            // single sync point. instance.tier is load-bearing (connection-pool size, custom-domain
            // eligibility, data-retention window); previously only subscriptions.tier flipped on
            // upgrade, leaving instances.tier stuck at the pre-upgrade tier (GAP-1090). Load + sync
            // + save runs OUTSIDE the best-effort email try/catch so a tier-sync failure is never
            // silently swallowed.
            Instance instance = instanceRepository.findById(saved.getInstanceId())
                .orElseThrow(() -> new IllegalArgumentException(
                    "Instance not found: " + saved.getInstanceId()));
            instanceTierSyncService.syncInstanceTier(instance, targetTier);
            instanceRepository.save(instance);

            // GAP-974: notify the owner that the paid tier upgrade is now active.
            // The create flow above already sends subscription-created; this closes
            // the previously-silent upgrade-flow activation.
            try {
                emailServiceClient.sendSubscriptionActivatedEmail(
                    instance.getId(),
                    instance.getContactEmail(),
                    instance.getOrganizationName(),
                    targetTier.name(),
                    saved.getExpiresAt() == null ? null : saved.getExpiresAt().toString()
                );
            } catch (Exception e) {
                log.error("Failed to send subscription activated email for subscription: {}",
                    subscriptionId, e);
            }
            log.info("Applied pending upgrade for subscription {} to tier {}", subscriptionId, targetTier);
        }
    }

    /**
     * GAP-1016: apply a confirmed manual-renewal payment.
     *
     * <p>Manual renewal ({@code SubscriptionRenewalService.manualRenewal}) no longer
     * extends for free — it creates a PENDING VietQR payment and sets
     * {@code pendingPaymentId} with no tier change. When the admin confirms that
     * payment, {@link #applyPendingUpgrade} routes here to extend the cycle and
     * reactivate a suspended instance.</p>
     *
     * @param subscription subscription whose renewal payment was confirmed
     * @param paymentId confirmed payment UUID (for logging)
     */
    private void applyConfirmedRenewal(Subscription subscription, UUID paymentId) {
        // GAP-1018 bug 2 (FM-7): apply a scheduled downgrade (pendingTier set, ≤ current ordinal)
        // on renewal-confirm so it is not silently stuck. priceVnd follows the new (lower) tier.
        boolean downgradeApplied = false;
        if (subscription.getPendingTier() != null) {
            PricingTier downgraded = subscription.getPendingTier();
            subscription.setTier(downgraded);
            subscription.setPriceVnd(downgraded.getPrice(subscription.getBillingCycle()));
            subscription.setPendingTier(null);
            downgradeApplied = true;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime base = subscription.getExpiresAt() != null
            && subscription.getExpiresAt().isAfter(now)
            ? subscription.getExpiresAt() : now;
        subscription.setExpiresAt(calculateExpiryDate(base, subscription.getBillingCycle()));
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setPendingPaymentId(null);
        Subscription saved = subscriptionRepository.save(subscription);

        Instance instance = instanceRepository.findById(saved.getInstanceId())
            .orElseThrow(() -> new IllegalArgumentException(
                "Instance not found: " + saved.getInstanceId()));
        instance.setSubscriptionExpiresAt(saved.getExpiresAt());
        if (downgradeApplied) {
            // GAP-1256 (SUB-21): a renewal that applied a downgrade must sync instances.tier.
            instanceTierSyncService.syncInstanceTier(instance, saved.getTier());
        }
        if (instance.getStatus() == InstanceStatus.SUSPENDED) {
            instance.setStatus(InstanceStatus.ACTIVE);
            log.info("Instance reactivated on renewal-payment confirm: {}", instance.getId());
        }
        instanceRepository.save(instance);

        log.info("Confirmed manual renewal for subscription {} (payment {}); new expiry {} "
                + "(downgrade applied: {})", saved.getId(), paymentId, saved.getExpiresAt(), downgradeApplied);
    }

    /**
     * Clear pending state after payment rejection.
     *
     * <p>For upgrade-flow (subscription was ACTIVE), this only clears
     * {@code pendingTier}/{@code pendingPaymentId} so the owner can submit a
     * new upgrade request; current tier stays ACTIVE.</p>
     *
     * <p>For create-flow (SUB-20, subscription was PENDING with no prior
     * ACTIVE state), there is no tier to fall back to — the subscription is
     * marked CANCELLED so the owner can create a fresh paid subscription
     * cleanly per Phase 1 BETA reject policy (UC-SUB-07).</p>
     *
     * @param subscriptionId subscription UUID from the rejected payment
     * @param paymentId rejected payment UUID
     */
    @Transactional
    public void clearPendingUpgrade(UUID subscriptionId, UUID paymentId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
            .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + subscriptionId));

        if (subscription.getPendingPaymentId() == null || !subscription.getPendingPaymentId().equals(paymentId)) {
            log.info("Payment {} is not the pending payment for subscription {}; skipping pending clear",
                paymentId, subscriptionId);
            return;
        }

        boolean isCreateFlow = subscription.getStatus() == SubscriptionStatus.PENDING;

        subscription.setPendingTier(null);
        subscription.setPendingPaymentId(null);

        if (isCreateFlow) {
            // SUB-20 create-flow reject: no prior ACTIVE state to fall back to → cancel
            // so owner can submit a fresh paid subscription cleanly (Phase 1 BETA policy).
            subscription.setStatus(SubscriptionStatus.CANCELLED);
            subscription.setAutoRenew(false);
        }

        subscriptionRepository.save(subscription);
        log.info("Cleared pending state for subscription {} after payment {} (create-flow={})",
            subscriptionId, paymentId, isCreateFlow);
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
