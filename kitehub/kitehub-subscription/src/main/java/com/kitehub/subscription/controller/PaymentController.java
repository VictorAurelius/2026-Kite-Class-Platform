package com.kitehub.subscription.controller;

import com.kitehub.platform.domain.enums.PaymentStatus;
import com.kitehub.subscription.billing.dto.ReceiptResponse;
import com.kitehub.subscription.billing.service.ReceiptService;
import com.kitehub.subscription.dto.CreatePaymentRequest;
import com.kitehub.subscription.dto.CursorPage;
import com.kitehub.subscription.dto.PaymentResponse;
import com.kitehub.subscription.service.PaymentService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for payment operations.
 *
 * <p>SLO Tier C (writes have side effects: payment record + outbox event).
 * See {@code documents/05-guides/api-performance-slo.md}.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/platform/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment creation, QR code generation, and payment history")
@Timed(value = "http.server.requests", percentiles = {0.5, 0.95, 0.99},
       extraTags = {"slo", "tier-c", "controller", "payment"})
public class PaymentController {

    /**
     * GAP-562b (Wave 80 Bucket C): mutation endpoints require OWNER role.
     *
     * <p>Wave 79 introduces OWNER / STAFF role separation; legacy
     * {@code PLATFORM_ADMIN} and {@code ADMIN} aliases stay accepted until
     * Wave 81 cutoff (2026-06-14) per
     * {@code com.kitehub.subscription.auth.role.PlatformRole}.</p>
     */
    static final String OWNER_AUTHZ =
            "hasAnyRole('OWNER','PLATFORM_ADMIN','ADMIN')";

    /**
     * Read endpoints accessible to OWNER + STAFF + legacy admin aliases.
     * STAFF needs read access for operational visibility per
     * {@code documents/01-business/roles/use-cases.md}.
     */
    static final String OWNER_OR_STAFF_AUTHZ =
            "hasAnyRole('OWNER','STAFF','PLATFORM_ADMIN','ADMIN')";

    private final PaymentService paymentService;
    private final ReceiptService receiptService;

    /**
     * Create a new payment.
     *
     * @param request Create payment request
     * @return Created payment response
     */
    @PostMapping
    @PreAuthorize(OWNER_AUTHZ)
    public ResponseEntity<PaymentResponse> createPayment(
        @Valid @RequestBody CreatePaymentRequest request
    ) {
        PaymentResponse response = paymentService.createPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** Default page size for the admin payment list (GAP-432). */
    private static final int DEFAULT_PAGE_SIZE = 50;
    /** Hard cap on page size to prevent clients re-introducing unbounded scans. */
    private static final int MAX_PAGE_SIZE = 200;

    /**
     * Get all payments with optional filters, paginated.
     *
     * <p><strong>GAP-432:</strong> previously this endpoint called
     * {@code paymentRepository.findAll()} under the hood. The endpoint now
     * accepts {@code page} + {@code size} query parameters and returns a
     * {@link Page} envelope so the response is bounded.</p>
     *
     * @param status Payment status filter (optional)
     * @param page   Zero-based page index (default 0)
     * @param size   Page size (default 50, capped at 200)
     * @return Page of payment responses
     */
    @GetMapping
    @PreAuthorize(OWNER_OR_STAFF_AUTHZ)
    public ResponseEntity<Page<PaymentResponse>> getAllPayments(
        @RequestParam(required = false) PaymentStatus status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size
    ) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(safePage, safeSize,
            Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PaymentResponse> responses = paymentService.getAllPayments(status, pageable);
        return ResponseEntity.ok(responses);
    }

    /**
     * Cursor (keyset) variant for very large payment ledgers — Wave 85 Bucket D D-AC1.
     *
     * <p>When the payment table grows beyond ~1M rows, OFFSET pagination starts
     * to pay a linear skip-cost. This endpoint accepts an opaque base64 cursor
     * (decoded into the last-seen {@code id}) and returns the next page using a
     * keyset query (no OFFSET). Order is fixed {@code id ASC}.</p>
     *
     * <p>Clients should NOT mix {@code page} and {@code cursor}; the controller
     * returns 400 if both are supplied.</p>
     *
     * @param status optional status filter
     * @param cursor opaque cursor from prior response (null/blank for first page)
     * @param size   page size (defaults 50, capped at 200)
     * @return CursorPage envelope
     */
    @GetMapping(params = "cursor")
    @PreAuthorize(OWNER_OR_STAFF_AUTHZ)
    public ResponseEntity<CursorPage<PaymentResponse>> getPaymentsByCursor(
        @RequestParam(required = false) PaymentStatus status,
        @RequestParam(required = false) String cursor,
        @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size
    ) {
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        UUID cursorId = CursorPage.decodeCursor(cursor);
        CursorPage<PaymentResponse> page = paymentService.getPaymentsByCursor(status, cursorId, safeSize);
        return ResponseEntity.ok(page);
    }

    /**
     * Get payment by ID.
     *
     * @param id Payment UUID
     * @return Payment response
     */
    @GetMapping("/{id}")
    @PreAuthorize(OWNER_OR_STAFF_AUTHZ)
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable UUID id) {
        PaymentResponse response = paymentService.getPayment(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all payments for subscription.
     *
     * @param subscriptionId Subscription UUID
     * @return List of payment responses
     */
    @GetMapping("/subscription/{subscriptionId}")
    @PreAuthorize(OWNER_OR_STAFF_AUTHZ)
    public ResponseEntity<List<PaymentResponse>> getPaymentsBySubscription(@PathVariable UUID subscriptionId) {
        List<PaymentResponse> responses = paymentService.getPaymentsBySubscription(subscriptionId);
        return ResponseEntity.ok(responses);
    }

    /**
     * Get QR code URL for payment.
     *
     * @param id Payment UUID
     * @return QR code URL
     */
    @GetMapping("/{id}/qr-code")
    @PreAuthorize(OWNER_OR_STAFF_AUTHZ)
    public ResponseEntity<Map<String, String>> getQRCode(@PathVariable UUID id) {
        String qrCodeUrl = paymentService.getQRCode(id);
        return ResponseEntity.ok(Map.of("qrCodeUrl", qrCodeUrl));
    }

    /**
     * Non-VAT receipt (biên nhận) for a confirmed payment (GAP-1266). Available only after the
     * payment is COMPLETED (400 otherwise); derived on-demand from the payment row.
     *
     * @param id payment UUID
     * @return receipt representation
     */
    @GetMapping("/{id}/receipt")
    @PreAuthorize(OWNER_OR_STAFF_AUTHZ)
    public ResponseEntity<ReceiptResponse> getReceipt(@PathVariable UUID id) {
        return ResponseEntity.ok(receiptService.generateReceipt(id));
    }
}
