package com.kiteclass.core.module.invoice.controller;

import com.kiteclass.core.common.dto.ApiResponse;
import com.kiteclass.core.module.invoice.dto.ApplyAdjustmentRequest;
import com.kiteclass.core.module.invoice.dto.InvoiceResponse;
import com.kiteclass.core.module.invoice.service.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    /**
     * Gets invoice by ID.
     *
     * @param id the invoice ID
     * @return invoice response DTO
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoiceById(@PathVariable Long id) {
        log.info("GET /api/v1/invoices/{}", id);
        InvoiceResponse invoice = invoiceService.getInvoiceById(id);
        return ResponseEntity.ok(ApiResponse.success(invoice));
    }

    /**
     * Gets invoices by student ID, paginated.
     *
     * @param studentId the student ID (path variable)
     * @param pageable pagination parameters
     * @return page of invoice response DTOs
     */
    @GetMapping("/student/{studentId}")
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
    public ResponseEntity<ApiResponse<Page<InvoiceResponse>>> getUnpaidInvoicesByStudent(
            @PathVariable Long studentId,
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("GET /api/v1/invoices/student/{}/unpaid", studentId);
        // TODO: PR-2.14 - Filter by paymentStatus != PAID in service layer
        Page<InvoiceResponse> invoices = invoiceService.getInvoicesByStudent(studentId, pageable);
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
    public ResponseEntity<ApiResponse<Page<InvoiceResponse>>> getOverdueInvoicesByStudent(
            @PathVariable Long studentId,
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("GET /api/v1/invoices/student/{}/overdue", studentId);
        // TODO: PR-2.14 - Filter by dueDate < today AND paymentStatus != PAID
        Page<InvoiceResponse> invoices = invoiceService.getInvoicesByStudent(studentId, pageable);
        return ResponseEntity.ok(ApiResponse.success(invoices));
    }

    /**
     * Marks invoice as paid (manual payment recording).
     *
     * @param id the invoice ID
     * @return updated invoice response DTO
     */
    @PostMapping("/{id}/mark-paid")
    public ResponseEntity<ApiResponse<InvoiceResponse>> markInvoiceAsPaid(@PathVariable Long id) {
        log.info("POST /api/v1/invoices/{}/mark-paid", id);
        // TODO: PR-2.14 - Implement markAsPaid in service layer (update paymentStatus)
        InvoiceResponse invoice = invoiceService.getInvoiceById(id);
        return ResponseEntity.ok(ApiResponse.success(invoice));
    }

    /**
     * Cancels an invoice.
     *
     * @param id the invoice ID
     * @return cancelled invoice response DTO
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<InvoiceResponse>> cancelInvoice(@PathVariable Long id) {
        log.info("PUT /api/v1/invoices/{}/cancel", id);
        InvoiceResponse invoice = invoiceService.cancelInvoice(id);
        return ResponseEntity.ok(ApiResponse.success(invoice));
    }
}
