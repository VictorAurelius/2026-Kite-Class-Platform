package com.kitehub.admin.controller;

import com.kitehub.admin.dto.ConfirmPaymentRequest;
import com.kitehub.admin.dto.DashboardStats;
import com.kitehub.admin.dto.InstanceSummary;
import com.kitehub.admin.dto.RejectPaymentRequest;
import com.kitehub.admin.dto.RevenueReport;
import com.kitehub.admin.event.SubscriptionDataChangedEvent;
import com.kitehub.admin.service.AnalyticsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.entity.Subscription;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.subscription.dto.PaymentResponse;
import com.kitehub.subscription.repository.InstanceRepository;
import com.kitehub.subscription.repository.SubscriptionRepository;
import com.kitehub.subscription.service.PaymentService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Admin portal REST APIs (LEGACY {@code /api/platform/admin} prefix).
 *
 * <p><strong>Deprecated (GAP-654):</strong> the canonical admin API surface is the
 * {@code /api/v1/admin/*} prefix served by {@link AdminInstancesController},
 * {@link AdminPaymentsController}, {@link AdminRevenueController} +
 * {@link AdminAuditLogController}. This legacy controller is retained during Phase 1 BETA for
 * backward compatibility; consumers MUST migrate to the v1 paths before the sunset date
 * (2026-09-30). Responses carry RFC 8594 {@code Sunset} + {@code Link: rel="successor-version"}
 * headers via {@link com.kitehub.admin.config.SunsetHeaderInterceptor}.</p>
 *
 * <p>Class-level {@code @PreAuthorize("hasRole('PLATFORM_ADMIN')")} enforces
 * platform-admin role on every endpoint — closes GAP-637 RBAC backfill (local-verify
 * path Wave 103 Bucket A). SecurityConfig already requires authentication on
 * {@code /api/platform/admin/**}; this annotation adds the explicit role gate
 * per OWASP A01 defense-in-depth (per
 * {@code .claude/rules/pre-launch-owasp-rest-hardening-checklist.md} §2.1).</p>
 *
 * @since 1.0
 * @deprecated since v1 (GAP-654) — migrate to the {@code /api/v1/admin/*} controllers;
 *             this legacy {@code /api/platform/admin} surface targets removal 2026-09-30.
 */
@Deprecated(since = "v1", forRemoval = true)
@Slf4j
@RestController
@RequestMapping("/api/platform/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
@Tag(name = "Admin", description = "Platform administration, analytics, instance management, and payment operations")
public class AdminController {

    private final AnalyticsService analyticsService;
    private final InstanceRepository instanceRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentService paymentService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Default page size for list endpoints (GAP-126).
     */
    static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * Hard cap for page size on list endpoints (GAP-126) — prevents callers
     * requesting size=10000 and re-creating the unbounded scan we just fixed.
     */
    static final int MAX_PAGE_SIZE = 100;

    /**
     * Get dashboard statistics.
     *
     * @return dashboard stats
     */
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardStats> getDashboard() {
        log.info("Admin requested dashboard stats");
        DashboardStats stats = analyticsService.getDashboardStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Get instances with summary information (paginated, GAP-126).
     *
     * <p>Defaults to {@value #DEFAULT_PAGE_SIZE} per page; caller-supplied
     * {@code size} is clamped to {@value #MAX_PAGE_SIZE}. Replaces the prior
     * unbounded {@code findAll()} that would scan the full Instance table on
     * every admin page load.</p>
     *
     * @param pageable pagination + sort (default size 20, max 100)
     * @return page of instance summaries
     */
    @GetMapping("/instances")
    public ResponseEntity<Page<InstanceSummary>> getAllInstances(
            @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
        Pageable safe = clampPageable(pageable);
        log.info("Admin requested instances page={} size={}", safe.getPageNumber(), safe.getPageSize());

        Page<Instance> page = instanceRepository.findAll(safe);
        Page<InstanceSummary> summaries = page.map(this::convertToSummary);

        return ResponseEntity.ok(summaries);
    }

    /**
     * Suspend an instance (admin action).
     *
     * @param id instance ID
     * @return updated instance summary
     */
    /**
     * Get instance detail by ID.
     *
     * @param id instance ID
     * @return instance summary
     */
    @GetMapping("/instances/{id}")
    public ResponseEntity<InstanceSummary> getInstanceById(@PathVariable UUID id) {
        log.info("Admin requested instance detail: {}", id);

        Instance instance = instanceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Instance not found: " + id));

        return ResponseEntity.ok(convertToSummary(instance));
    }

    @PatchMapping("/instances/{id}/suspend")
    public ResponseEntity<InstanceSummary> suspendInstance(@PathVariable UUID id) {
        log.info("Admin suspending instance: {}", id);

        Instance instance = instanceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Instance not found: " + id));

        instance.setStatus(InstanceStatus.SUSPENDED);
        instance = instanceRepository.save(instance);

        // GAP-126 — invalidate dashboard cache after status change
        eventPublisher.publishEvent(
                new SubscriptionDataChangedEvent(this, "instance.suspended", id));

        return ResponseEntity.ok(convertToSummary(instance));
    }

    /**
     * Activate an instance (admin action).
     *
     * @param id instance ID
     * @return updated instance summary
     */
    @PatchMapping("/instances/{id}/activate")
    public ResponseEntity<InstanceSummary> activateInstance(@PathVariable UUID id) {
        log.info("Admin activating instance: {}", id);

        Instance instance = instanceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Instance not found: " + id));

        instance.setStatus(InstanceStatus.ACTIVE);
        instance = instanceRepository.save(instance);

        // GAP-126 — invalidate dashboard cache after status change
        eventPublisher.publishEvent(
                new SubscriptionDataChangedEvent(this, "instance.activated", id));

        return ResponseEntity.ok(convertToSummary(instance));
    }

    /**
     * Get revenue analytics.
     *
     * @param period    report period (DAILY, MONTHLY, YEARLY)
     * @param startDate start date (yyyy-MM-dd)
     * @param endDate   end date (yyyy-MM-dd)
     * @return revenue report
     */
    @GetMapping("/revenue")
    public ResponseEntity<RevenueReport> getRevenue(
            @RequestParam(defaultValue = "MONTHLY") String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        // Default to current month if not provided
        if (startDate == null) {
            startDate = LocalDate.now().withDayOfMonth(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        log.info("Admin requested revenue report: {} from {} to {}", period, startDate, endDate);

        RevenueReport report = analyticsService.getRevenueReport(period, startDate, endDate);
        return ResponseEntity.ok(report);
    }

    /**
     * Get subscriptions (paginated, GAP-126).
     *
     * <p>Defaults to {@value #DEFAULT_PAGE_SIZE} per page; caller-supplied
     * {@code size} is clamped to {@value #MAX_PAGE_SIZE}.</p>
     *
     * @param pageable pagination + sort (default size 20, max 100)
     * @return page of subscriptions
     */
    @GetMapping("/subscriptions")
    public ResponseEntity<Page<Subscription>> getAllSubscriptions(
            @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
        Pageable safe = clampPageable(pageable);
        log.info("Admin requested subscriptions page={} size={}", safe.getPageNumber(), safe.getPageSize());

        return ResponseEntity.ok(subscriptionRepository.findAll(safe));
    }

    // ==================== PAYMENT ADMIN APIs ====================

    /**
     * Get all pending payments (Admin).
     *
     * @return list of pending payments
     */
    @GetMapping("/payments/pending")
    public ResponseEntity<List<PaymentResponse>> getPendingPayments() {
        log.info("Admin requested pending payments");
        List<PaymentResponse> payments = paymentService.getPendingPayments();
        return ResponseEntity.ok(payments);
    }

    /**
     * Confirm a payment manually (Admin).
     *
     * @param id payment ID
     * @param request confirm request with transaction ID
     * @return updated payment
     */
    @PostMapping("/payments/{id}/confirm")
    public ResponseEntity<PaymentResponse> confirmPayment(
            @PathVariable UUID id,
            @Valid @RequestBody ConfirmPaymentRequest request
    ) {
        log.info("Admin confirming payment: {} with transactionId: {}", id, request.getTransactionId());
        PaymentResponse payment = paymentService.confirmPayment(id, request.getTransactionId());

        // GAP-126 — payment confirmation drives subscription state; refresh dashboard
        eventPublisher.publishEvent(
                new SubscriptionDataChangedEvent(this, "payment.confirmed", id));

        return ResponseEntity.ok(payment);
    }

    /**
     * Reject a payment manually (Admin).
     *
     * @param id payment ID
     * @param request reject request with reason
     * @return updated payment
     */
    @PostMapping("/payments/{id}/reject")
    public ResponseEntity<PaymentResponse> rejectPayment(
            @PathVariable UUID id,
            @Valid @RequestBody RejectPaymentRequest request
    ) {
        log.info("Admin rejecting payment: {} with reason: {}", id, request.getReason());
        PaymentResponse payment = paymentService.rejectPayment(id, request.getReason());

        // GAP-126 — rejected payment may flip subscription state
        eventPublisher.publishEvent(
                new SubscriptionDataChangedEvent(this, "payment.rejected", id));

        return ResponseEntity.ok(payment);
    }

    // ==================== HELPER METHODS ====================

    /**
     * Enforce {@value #MAX_PAGE_SIZE} ceiling on caller-supplied page size
     * (GAP-126). Spring 3.4 added {@code spring.data.web.pageable.max-page-size}
     * but applying it via a helper keeps behavior consistent across servlet
     * versions and is unit-testable without a full {@code @WebMvcTest} context.
     */
    static Pageable clampPageable(Pageable pageable) {
        if (pageable == null) {
            return PageRequest.of(0, DEFAULT_PAGE_SIZE);
        }
        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            return PageRequest.of(pageable.getPageNumber(), MAX_PAGE_SIZE, pageable.getSort());
        }
        return pageable;
    }

    /**
     * Convert Instance entity to InstanceSummary DTO.
     */
    private InstanceSummary convertToSummary(Instance instance) {
        // Find active subscription for this instance
        Subscription subscription = subscriptionRepository.findActiveByInstanceId(instance.getId())
                .orElse(null);

        return InstanceSummary.builder()
                .id(instance.getId())
                .organizationName(instance.getOrganizationName())
                .subdomain(instance.getSubdomain())
                .status(instance.getStatus().name())
                .tier(subscription != null ? subscription.getTier().name() : "TRIAL")
                .ownerEmail(null) // Not available in Instance entity (only ownerId)
                .ownerPhone(null) // Not available in Instance entity
                .trialEndDate(instance.getTrialExpiresAt())
                .subscriptionEndDate(subscription != null ? subscription.getExpiresAt() : null)
                .databaseUrl(instance.getDatabaseUrl())
                .totalUsers(0L) // Mock data - would query from instance database
                .totalStudents(0L)
                .totalCourses(0L)
                .createdAt(instance.getCreatedAt())
                .updatedAt(instance.getUpdatedAt())
                .build();
    }
}
