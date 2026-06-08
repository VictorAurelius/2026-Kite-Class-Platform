package com.kiteclass.core.module.invoice.controller;

import com.kiteclass.core.common.dto.ApiResponse;
import com.kiteclass.core.common.dto.PageResponse;
import com.kiteclass.core.module.invoice.dto.ApplyAdjustmentRequest;
import com.kiteclass.core.module.invoice.dto.BatchInvoiceConfirmResponse;
import com.kiteclass.core.module.invoice.dto.BatchInvoicePreviewResponse;
import com.kiteclass.core.module.invoice.dto.InvoiceItemResponse;
import com.kiteclass.core.module.invoice.dto.InvoiceResponse;
import com.kiteclass.core.module.invoice.service.InvoiceBatchService;
import com.kiteclass.core.module.invoice.service.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for invoice management.
 *
 * @author KiteClass Team
 * @since 2.8.0
 */
@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
@Slf4j
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final InvoiceBatchService invoiceBatchService;

    /**
     * Previews batch monthly invoices for a month (GAP-297) — NO persistence.
     *
     * <p>Enumerates active enrollments for the current tenant × class tuition,
     * applies mid-month pro-rata, and returns the projected invoice count, total
     * revenue (VND) and per-enrollment line items so the Owner can review before
     * confirming.
     *
     * @param month billed month in {@code yyyy-MM} form (e.g. {@code 2026-05})
     * @return preview of the invoices that would be generated
     */
    @PostMapping("/batch-generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<BatchInvoicePreviewResponse>> batchGenerate(
            @RequestParam("month") String month) {
        log.info("POST /api/v1/invoices/batch-generate?month={}", month);
        BatchInvoicePreviewResponse preview = invoiceBatchService.generatePreview(month);
        return ResponseEntity.ok(ApiResponse.success(preview));
    }

    /**
     * Confirms (persists) batch monthly invoices for a month (GAP-297).
     *
     * <p>Persists one invoice per active enrollment and emits an
     * {@code InvoiceCreatedEvent} per created invoice. Idempotent — re-running for
     * the same month skips enrollments already invoiced (no duplicates).
     *
     * @param month billed month in {@code yyyy-MM} form
     * @return created/skipped counts + created invoice ids
     */
    @PostMapping("/batch-confirm")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<BatchInvoiceConfirmResponse>> batchConfirm(
            @RequestParam("month") String month) {
        log.info("POST /api/v1/invoices/batch-confirm?month={}", month);
        BatchInvoiceConfirmResponse result = invoiceBatchService.confirm(month);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Lists all invoices for the current tenant, paginated (flat list).
     *
     * <p>Backs the Owner dashboard. Tenant-scoped via the Hibernate
     * {@code tenantFilter} — no cross-tenant leak. Supports {@code sort=createdAt,desc}.
     * Authorization mirrors the sibling invoice read endpoints.
     *
     * @param pageable pagination + sort params (default sort {@code createdAt,desc})
     * @return page of invoice response DTOs scoped to the current tenant
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'OWNER', 'PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<InvoiceResponse>>> getInvoices(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("GET /api/v1/invoices");
        PageResponse<InvoiceResponse> invoices = invoiceService.getInvoices(pageable);
        return ResponseEntity.ok(ApiResponse.success(invoices));
    }

    /**
     * Gets invoice by ID.
     *
     * @param id the invoice ID
     * @return invoice response DTO
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'OWNER', 'PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoiceById(@PathVariable Long id) {
        log.info("GET /api/v1/invoices/{}", id);
        InvoiceResponse invoice = invoiceService.getInvoiceById(id);
        return ResponseEntity.ok(ApiResponse.success(invoice));
    }

    /**
     * Gets invoice line items by invoice ID.
     *
     * @param id the invoice ID
     * @return list of invoice item response DTOs
     */
    @GetMapping("/{id}/items")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'OWNER', 'PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<List<InvoiceItemResponse>>> getInvoiceItems(@PathVariable Long id) {
        log.info("GET /api/v1/invoices/{}/items", id);
        List<InvoiceItemResponse> items = invoiceService.getInvoiceItems(id);
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    /**
     * Gets invoices by student ID, paginated.
     *
     * @param studentId the student ID (path variable)
     * @param pageable pagination parameters
     * @return page of invoice response DTOs
     */
    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'OWNER', 'PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<Page<InvoiceResponse>>> getInvoicesByStudent(
            @PathVariable Long studentId,
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("GET /api/v1/invoices/student/{}", studentId);
        Page<InvoiceResponse> invoices = invoiceService.getInvoicesByStudent(studentId, pageable);
        return ResponseEntity.ok(ApiResponse.success(invoices));
    }

    /**
     * Applies adjustment to invoice.
     *
     * @param id the invoice ID
     * @param request adjustment details
     * @return updated invoice response DTO
     */
    @PostMapping("/{id}/adjustments")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<InvoiceResponse>> applyAdjustment(
            @PathVariable Long id,
            @Valid @RequestBody ApplyAdjustmentRequest request) {

        log.info("POST /api/v1/invoices/{}/adjustments: {}", id, request.getType());
        InvoiceResponse invoice = invoiceService.applyAdjustment(id, request);
        return ResponseEntity.ok(ApiResponse.success(invoice));
    }

    /**
     * Calculates and applies late fees for invoice.
     *
     * @param id the invoice ID
     * @return updated invoice response DTO
     */
    @PostMapping("/{id}/late-fees")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<InvoiceResponse>> calculateLateFees(@PathVariable Long id) {
        log.info("POST /api/v1/invoices/{}/late-fees", id);
        InvoiceResponse invoice = invoiceService.calculateLateFees(id);
        return ResponseEntity.ok(ApiResponse.success(invoice));
    }

    /**
     * Gets overdue invoices, paginated.
     *
     * @param pageable pagination parameters
     * @return page of overdue invoice response DTOs
     */
    @GetMapping("/overdue")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'OWNER', 'PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<Page<InvoiceResponse>>> getOverdueInvoices(
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("GET /api/v1/invoices/overdue");
        Page<InvoiceResponse> invoices = invoiceService.getOverdueInvoices(pageable);
        return ResponseEntity.ok(ApiResponse.success(invoices));
    }

    /**
     * Gets unpaid invoices for a student, paginated.
     *
     * @param studentId the student ID
     * @param pageable pagination parameters
     * @return page of unpaid invoice response DTOs
     */
    @GetMapping("/student/{studentId}/unpaid")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'OWNER', 'PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<Page<InvoiceResponse>>> getUnpaidInvoicesByStudent(
            @PathVariable Long studentId,
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("GET /api/v1/invoices/student/{}/unpaid", studentId);
        Page<InvoiceResponse> invoices = invoiceService.getUnpaidInvoicesByStudent(studentId, pageable);
        return ResponseEntity.ok(ApiResponse.success(invoices));
    }

    /**
     * Gets overdue invoices for a student, paginated.
     *
     * @param studentId the student ID
     * @param pageable pagination parameters
     * @return page of overdue invoice response DTOs
     */
    @GetMapping("/student/{studentId}/overdue")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'OWNER', 'PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<Page<InvoiceResponse>>> getOverdueInvoicesByStudent(
            @PathVariable Long studentId,
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("GET /api/v1/invoices/student/{}/overdue", studentId);
        Page<InvoiceResponse> invoices = invoiceService.getOverdueInvoicesByStudent(studentId, pageable);
        return ResponseEntity.ok(ApiResponse.success(invoices));
    }

    /**
     * Marks invoice as paid (manual payment recording).
     *
     * @param id the invoice ID
     * @return updated invoice response DTO
     */
    @PostMapping("/{id}/mark-paid")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<InvoiceResponse>> markInvoiceAsPaid(@PathVariable Long id) {
        log.info("POST /api/v1/invoices/{}/mark-paid", id);
        InvoiceResponse invoice = invoiceService.markInvoiceAsPaid(id);
        return ResponseEntity.ok(ApiResponse.success(invoice));
    }

    /**
     * Cancels an invoice.
     *
     * @param id the invoice ID
     * @return cancelled invoice response DTO
     */
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<InvoiceResponse>> cancelInvoice(@PathVariable Long id) {
        log.info("PUT /api/v1/invoices/{}/cancel", id);
        InvoiceResponse invoice = invoiceService.cancelInvoice(id);
        return ResponseEntity.ok(ApiResponse.success(invoice));
    }
}
