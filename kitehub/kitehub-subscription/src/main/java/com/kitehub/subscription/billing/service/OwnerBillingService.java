package com.kitehub.subscription.billing.service;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.entity.Payment;
import com.kitehub.platform.domain.entity.Subscription;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.platform.domain.enums.PaymentMethod;
import com.kitehub.platform.domain.enums.PaymentStatus;
import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.platform.domain.enums.SubscriptionStatus;
import com.kitehub.subscription.billing.dto.DowngradePreviewResponse;
import com.kitehub.subscription.billing.dto.PendingPaymentStatusResponse;
import com.kitehub.subscription.billing.dto.ReactivateResponse;
import com.kitehub.subscription.exception.SubscriptionConflictException;
import com.kitehub.subscription.repository.InstanceRepository;
import com.kitehub.subscription.repository.PaymentRepository;
import com.kitehub.subscription.repository.SubscriptionRepository;
import com.kitehub.subscription.service.VietQRService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Owner-facing billing-portal queries + win-back reactivation (FE-1 contract).
 *
 * <p>Holds the read-only status/preview lookups and the manual-VietQR-gated reactivation so the
 * shared {@code SubscriptionService} core stays untouched. Tenant isolation is enforced by the
 * controller via {@code TenantOwnershipGuard} before these methods run.</p>
 *
 * <ul>
 *   <li>{@link #getPendingPaymentStatus} — GAP-1257-BE "đang chờ xác nhận" poll.</li>
 *   <li>{@link #getDowngradePreview} — GAP-1261 over-cap impact preview.</li>
 *   <li>{@link #reactivate} — GAP-1263-BE win-back (idempotent; fraud tombstone vs voluntary).</li>
 * </ul>
 *
 * @author KiteHub Team
 * @since wave-kitehub-biz-100
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OwnerBillingService {

    private final SubscriptionRepository subscriptionRepository;
    private final InstanceRepository instanceRepository;
    private final PaymentRepository paymentRepository;
    private final VietQRService vietQRService;

    /** Admin-confirm SLA window — how long the owner should expect the admin to reconcile (SUB-19). */
    @Value("${kitehub.payment.admin-confirm-sla-hours:24}")
    private long adminConfirmSlaHours;

    // Phase 1 BETA symbolic-amount override (mirrors PaymentService/SubscriptionService GAP-975):
    // the reactivation Payment charges the override instead of the full tier price when enabled.
    @Value("${kitehub.payment.beta-mode.enabled:false}")
    private boolean betaModeEnabled;

    @Value("${kitehub.payment.beta-mode.override-amount-vnd:10000}")
    private long betaOverrideAmountVnd;

    private static final String USAGE_DATA_NOTE =
        "Số liệu sử dụng thực tế (học sinh, dung lượng) nằm ở cơ sở dữ liệu riêng của trung tâm; "
            + "bản xem trước này so sánh giới hạn (cap) của gói hiện tại với gói đích. "
            + "Hãy đối chiếu với số liệu thực tế của bạn trước khi hạ gói.";

    /**
     * Pending-payment status for the instance's in-flight VietQR payment (GAP-1257-BE).
     *
     * @param instanceId instance UUID
     * @return status (with {@code hasPendingPayment=false} when none is in flight)
     */
    @Transactional(readOnly = true)
    public PendingPaymentStatusResponse getPendingPaymentStatus(UUID instanceId) {
        Optional<Subscription> withPending = subscriptionRepository.findByInstanceId(instanceId).stream()
            .filter(s -> s.getPendingPaymentId() != null)
            .max(Comparator.comparing(Subscription::getCreatedAt,
                Comparator.nullsFirst(Comparator.naturalOrder())));

        if (withPending.isEmpty()) {
            return PendingPaymentStatusResponse.builder()
                .hasPendingPayment(false)
                .adminConfirmSlaHours(adminConfirmSlaHours)
                .build();
        }

        Subscription sub = withPending.get();
        Payment payment = paymentRepository.findById(sub.getPendingPaymentId()).orElse(null);
        if (payment == null) {
            // Dangling pointer (payment purged) — report no in-flight payment rather than 500.
            log.warn("Subscription {} references missing pending payment {}",
                sub.getId(), sub.getPendingPaymentId());
            return PendingPaymentStatusResponse.builder()
                .hasPendingPayment(false)
                .subscriptionId(sub.getId())
                .adminConfirmSlaHours(adminConfirmSlaHours)
                .build();
        }

        PricingTier tier = sub.getPendingTier() != null ? sub.getPendingTier() : sub.getTier();
        var createdAt = payment.getCreatedAt();
        var expiresAt = createdAt != null ? createdAt.plusHours(adminConfirmSlaHours) : null;

        return PendingPaymentStatusResponse.builder()
            .hasPendingPayment(true)
            .subscriptionId(sub.getId())
            .pendingPaymentId(payment.getId())
            .amount(payment.getAmountVnd())
            .currency(payment.getCurrency())
            .status(payment.getStatus())
            .tier(tier != null ? tier.name() : null)
            .createdAt(createdAt)
            .expiresAt(expiresAt)
            .adminConfirmSlaHours(adminConfirmSlaHours)
            .build();
    }

    /**
     * Over-cap impact preview for a tier downgrade (GAP-1261).
     *
     * @param instanceId instance UUID
     * @param targetTier the lower tier the owner is considering
     * @return preview of shrunk entitlement caps + feature loss + Vietnamese warnings
     * @throws EntityNotFoundException  if the instance does not exist (→ 404)
     * @throws IllegalArgumentException if {@code targetTier} is not strictly lower than current (→ 400)
     */
    @Transactional(readOnly = true)
    public DowngradePreviewResponse getDowngradePreview(UUID instanceId, PricingTier targetTier) {
        Instance instance = instanceRepository.findById(instanceId)
            .orElseThrow(() -> new EntityNotFoundException("Instance not found: " + instanceId));

        // Current effective tier = active subscription tier when present, else the denormalized
        // instances.tier (kept in sync per SUB-21).
        PricingTier current = subscriptionRepository.findActiveByInstanceId(instanceId)
            .map(Subscription::getTier)
            .orElseGet(instance::getTier);
        if (current == null) {
            current = PricingTier.FREE;
        }

        if (targetTier == null) {
            throw new IllegalArgumentException("targetTier is required");
        }
        if (targetTier.ordinal() >= current.ordinal()) {
            throw new IllegalArgumentException(
                "targetTier (" + targetTier + ") must be lower than the current tier (" + current
                    + "). Use upgrade for higher tiers.");
        }

        boolean currentDomain = current.allowsCustomDomain();
        boolean targetDomain = targetTier.allowsCustomDomain();
        boolean willDisableDomain = currentDomain && !targetDomain;
        boolean hasActiveDomain = instance.getCustomDomain() != null
            && !instance.getCustomDomain().isBlank();

        List<String> warnings = new ArrayList<>();
        if (targetTier.getMaxStudents() < current.getMaxStudents()) {
            warnings.add(String.format(
                "Gói %s giới hạn %d học sinh (gói hiện tại %s cho %d).",
                targetTier, targetTier.getMaxStudents(), current, current.getMaxStudents()));
        }
        if (targetTier.getMaxTeachers() < current.getMaxTeachers()) {
            warnings.add(String.format(
                "Gói %s giới hạn %d giáo viên (gói hiện tại %s cho %d).",
                targetTier, targetTier.getMaxTeachers(), current, current.getMaxTeachers()));
        }
        if (targetTier.getStorageLimitMB() < current.getStorageLimitMB()) {
            warnings.add(String.format(
                "Dung lượng lưu trữ giảm còn %d MB (gói hiện tại %d MB).",
                targetTier.getStorageLimitMB(), current.getStorageLimitMB()));
        }
        if (willDisableDomain) {
            warnings.add(hasActiveDomain
                ? "Tên miền riêng đang dùng (" + instance.getCustomDomain()
                    + ") sẽ bị vô hiệu hóa — khách sẽ truy cập qua subdomain mặc định."
                : "Tính năng tên miền riêng sẽ không còn khả dụng ở gói này.");
        }

        return DowngradePreviewResponse.builder()
            .currentTier(current)
            .targetTier(targetTier)
            .currentMaxStudents(current.getMaxStudents())
            .targetMaxStudents(targetTier.getMaxStudents())
            .currentMaxTeachers(current.getMaxTeachers())
            .targetMaxTeachers(targetTier.getMaxTeachers())
            .currentStorageMb(current.getStorageLimitMB())
            .targetStorageMb(targetTier.getStorageLimitMB())
            .customDomainCurrentlyAllowed(currentDomain)
            .customDomainTargetAllowed(targetDomain)
            .customDomainWillBeDisabled(willDisableDomain)
            .hasActiveCustomDomain(hasActiveDomain)
            .warnings(warnings)
            .usageDataNote(USAGE_DATA_NOTE)
            .build();
    }

    /**
     * Win-back reactivation for a SUSPENDED instance (GAP-1263-BE).
     *
     * <p>Phase 1 BETA manual-VietQR gate (mirrors GAP-1016 manual renewal): creates a PENDING
     * reactivation payment + sets {@code subscription.pendingPaymentId}; the instance flips back to
     * ACTIVE only after the admin confirms (existing {@code applyConfirmedRenewal} path). Idempotent.
     * PURGED/DELETED instances are fraud/admin tombstones — NOT self-reactivatable (→ 409, contact
     * support); a voluntary cancel leaves the instance merely SUSPENDED and IS reactivatable.</p>
     *
     * @param instanceId instance UUID
     * @return reactivation outcome
     * @throws EntityNotFoundException     if the instance does not exist (→ 404)
     * @throws SubscriptionConflictException if the instance is a tombstone (PURGED/DELETED) or
     *                                       never activated (→ 409)
     */
    @Transactional
    public ReactivateResponse reactivate(UUID instanceId) {
        Instance instance = instanceRepository.findById(instanceId)
            .orElseThrow(() -> new EntityNotFoundException("Instance not found: " + instanceId));

        InstanceStatus status = instance.getStatus();
        if (status == InstanceStatus.PURGED) {
            throw new SubscriptionConflictException(
                "Trung tâm đã bị xóa vĩnh viễn, dữ liệu không thể khôi phục. Vui lòng tạo trung tâm mới.");
        }
        if (status == InstanceStatus.DELETED) {
            // Fraud-block / admin-deleted tombstone — cannot self-reactivate; route to support.
            throw new SubscriptionConflictException(
                "Trung tâm đang bị khóa hoặc đã bị xóa. Vui lòng liên hệ hỗ trợ để được khôi phục.");
        }
        if (status == InstanceStatus.ACTIVE || status == InstanceStatus.TRIAL) {
            return ReactivateResponse.builder()
                .instanceId(instanceId)
                .outcome(ReactivateResponse.Outcome.ALREADY_ACTIVE)
                .churnType(ReactivateResponse.ChurnType.NONE)
                .message("Trung tâm đang hoạt động — không cần kích hoạt lại.")
                .build();
        }
        if (status == InstanceStatus.PENDING) {
            throw new SubscriptionConflictException(
                "Trung tâm chưa từng được kích hoạt lần đầu. Hoàn tất đăng ký trước khi kích hoạt lại.");
        }

        // status == SUSPENDED → win-back path.
        Optional<Subscription> latest = subscriptionRepository.findByInstanceId(instanceId).stream()
            .max(Comparator.comparing(Subscription::getCreatedAt,
                Comparator.nullsFirst(Comparator.naturalOrder())));

        if (latest.isEmpty()) {
            return ReactivateResponse.builder()
                .instanceId(instanceId)
                .outcome(ReactivateResponse.Outcome.NO_SUBSCRIPTION)
                .churnType(ReactivateResponse.ChurnType.NONE)
                .message("Không tìm thấy gói đăng ký để kích hoạt lại — vui lòng tạo gói mới.")
                .build();
        }

        Subscription sub = latest.get();
        ReactivateResponse.ChurnType churnType = sub.getStatus() == SubscriptionStatus.CANCELLED
            ? ReactivateResponse.ChurnType.VOLUNTARY
            : ReactivateResponse.ChurnType.INVOLUNTARY;

        // Idempotent: a reactivation/renewal payment already in flight → return it.
        if (sub.getPendingPaymentId() != null) {
            Payment existing = paymentRepository.findById(sub.getPendingPaymentId()).orElse(null);
            if (existing != null && existing.getStatus() == PaymentStatus.PENDING) {
                log.info("Reactivation idempotent — instance {} already has pending payment {}",
                    instanceId, existing.getId());
                return paymentResponse(instanceId, sub, existing, churnType);
            }
        }

        long amount = sub.getPriceVnd() != null ? sub.getPriceVnd()
            : (sub.getTier() != null ? sub.getTier().getPrice(sub.getBillingCycle()) : 0L);
        Payment payment = paymentRepository.save(createReactivationPayment(sub, amount));
        sub.setPendingPaymentId(payment.getId());
        subscriptionRepository.save(sub);

        log.info("Win-back reactivation payment {} created for instance {} (subscription {}, churn {})",
            payment.getId(), instanceId, sub.getId(), churnType);
        return paymentResponse(instanceId, sub, payment, churnType);
    }

    private ReactivateResponse paymentResponse(UUID instanceId, Subscription sub, Payment payment,
                                               ReactivateResponse.ChurnType churnType) {
        return ReactivateResponse.builder()
            .instanceId(instanceId)
            .outcome(ReactivateResponse.Outcome.PAYMENT_REQUIRED)
            .churnType(churnType)
            .subscriptionId(sub.getId())
            .pendingPaymentId(payment.getId())
            .amount(payment.getAmountVnd())
            .currency(payment.getCurrency())
            .message("Vui lòng thanh toán để kích hoạt lại trung tâm. "
                + "Trung tâm sẽ hoạt động trở lại sau khi admin xác nhận chuyển khoản.")
            .build();
    }

    /**
     * Create a PENDING VietQR reactivation payment (mirrors
     * {@code SubscriptionRenewalService.createRenewalPayment}) — on confirm the existing
     * {@code applyConfirmedRenewal} branch extends the cycle + flips the instance to ACTIVE.
     */
    private Payment createReactivationPayment(Subscription subscription, long amount) {
        long effectiveAmount = betaModeEnabled ? betaOverrideAmountVnd : amount;
        Payment payment = new Payment();
        payment.setSubscriptionId(subscription.getId());
        payment.setInstanceId(subscription.getInstanceId()); // V58 RLS: instance_id NOT NULL
        payment.setAmountVnd(effectiveAmount);
        payment.setCurrency("VND");
        payment.setPaymentMethod(PaymentMethod.VIETQR);
        payment.setStatus(PaymentStatus.PENDING);

        // SePay matching reference KH3SUB<8 hex> — mirrors PaymentService.generateTxnRef format
        // (package-private there; inlined here to avoid widening its visibility for one caller).
        String txnRef = "KH3SUB" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        payment.setTxnRef(txnRef);
        payment.setPaymentContent(txnRef);
        payment.setQrCodeUrl(vietQRService.generateQRCode(UUID.randomUUID(), effectiveAmount, txnRef));
        payment.setBankCode(vietQRService.getBankCode());
        payment.setAccountNumber(vietQRService.getAccountNumber());
        payment.setAccountName(vietQRService.getAccountName());
        return payment;
    }
}
