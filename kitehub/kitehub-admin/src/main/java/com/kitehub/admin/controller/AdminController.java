package com.kitehub.admin.controller;

import com.kitehub.admin.dto.ConfirmPaymentRequest;
import com.kitehub.admin.dto.DashboardStats;
import com.kitehub.admin.dto.InstanceSummary;
import com.kitehub.admin.dto.RejectPaymentRequest;
import com.kitehub.admin.dto.RevenueReport;
import com.kitehub.admin.service.AnalyticsService;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
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
import java.util.stream.Collectors;

/**
 * Admin portal REST APIs.
 *
 * @since 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/platform/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AnalyticsService analyticsService;
    private final InstanceRepository instanceRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentService paymentService;

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
     * Get all instances with summary information.
     *
     * @return list of instance summaries
     */
    @GetMapping("/instances")
    public ResponseEntity<List<InstanceSummary>> getAllInstances() {
        log.info("Admin requested all instances");

        List<Instance> instances = instanceRepository.findAll();

        List<InstanceSummary> summaries = instances.stream()
                .map(this::convertToSummary)
                .collect(Collectors.toList());

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
     * Get all subscriptions.
     *
     * @return list of subscriptions
     */
    @GetMapping("/subscriptions")
    public ResponseEntity<List<Subscription>> getAllSubscriptions() {
        log.info("Admin requested all subscriptions");

        List<Subscription> subscriptions = subscriptionRepository.findAll();
        return ResponseEntity.ok(subscriptions);
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
        return ResponseEntity.ok(payment);
    }

    // ==================== HELPER METHODS ====================

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
