package com.kiteclass.core.module.invoice.service;

import com.kiteclass.core.common.dto.PageResponse;
import com.kiteclass.core.module.invoice.dto.ApplyAdjustmentRequest;
import com.kiteclass.core.module.invoice.dto.InvoiceItemResponse;
import com.kiteclass.core.module.invoice.dto.InvoiceResponse;
import com.kiteclass.core.module.invoice.entity.Invoice;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for invoice management.
 *
 * @author KiteClass Team
 * @since 2.8.0
 */
public interface InvoiceService {

    /**
     * Auto-creates invoice for enrollment (event-driven).
     *
     * <p>Business Rules:
     * <ul>
     *   <li>BR-INV-001: Auto-generate invoice from enrollment</li>
     *   <li>BR-INV-002: Calculate total with items + adjustments</li>
     *   <li>BR-INV-005: One invoice per enrollment</li>
     * </ul>
     *
     * @param enrollmentId the enrollment ID
     * @return created invoice entity
     */
    Invoice createInvoiceForEnrollment(Long enrollmentId);

    /**
     * Gets invoice by ID.
     *
     * @param id the invoice ID
     * @return invoice response DTO
     */
    InvoiceResponse getInvoiceById(Long id);

    /**
     * Gets invoice line items by invoice ID.
     *
     * @param invoiceId the invoice ID
     * @return list of invoice item response DTOs
     */
    List<InvoiceItemResponse> getInvoiceItems(Long invoiceId);

    /**
     * Gets all invoices for a student, paginated.
     *
     * @param studentId the student ID
     * @param pageable pagination parameters
     * @return page of invoice response DTOs
     */
    Page<InvoiceResponse> getInvoicesByStudent(Long studentId, Pageable pageable);

    /**
     * Lists all invoices for the current tenant, paginated (flat list).
     *
     * <p>Backs the Owner dashboard {@code GET /api/v1/invoices}. Tenant-scoped via
     * the Hibernate {@code tenantFilter} — no cross-tenant leak. Supports sorting
     * (e.g. {@code createdAt,desc}) through the {@link Pageable}.
     *
     * @param pageable pagination + sort params
     * @return paginated invoice list for the current tenant
     */
    PageResponse<InvoiceResponse> getInvoices(Pageable pageable);

    /**
     * Applies adjustment to invoice (discount, late fee, etc.).
     *
     * @param invoiceId the invoice ID
     * @param request adjustment details
     * @return updated invoice response DTO
     */
    InvoiceResponse applyAdjustment(Long invoiceId, @Valid ApplyAdjustmentRequest request);

    /**
     * Calculates and applies late fees for overdue invoice.
     *
     * <p>Late fee calculation: 0.1% per day overdue.
     *
     * @param invoiceId the invoice ID
     * @return updated invoice response DTO
     */
    InvoiceResponse calculateLateFees(Long invoiceId);

    /**
     * Gets all overdue invoices, paginated.
     *
     * @param pageable pagination parameters
     * @return page of overdue invoice response DTOs
     */
    Page<InvoiceResponse> getOverdueInvoices(Pageable pageable);

    /**
     * Cancels an invoice.
     *
     * <p>Only DRAFT or SENT invoices can be cancelled.
     *
     * @param id the invoice ID
     * @return cancelled invoice response DTO
     */
    InvoiceResponse cancelInvoice(Long id);

    /**
     * Gets unpaid invoices for a student, paginated.
     * Unpaid = status not in (PAID, CANCELLED, REFUNDED).
     *
     * @param studentId the student ID
     * @param pageable pagination parameters
     * @return page of unpaid invoice response DTOs
     * @since 2.14
     */
    Page<InvoiceResponse> getUnpaidInvoicesByStudent(Long studentId, Pageable pageable);

    /**
     * Gets overdue unpaid invoices for a student, paginated.
     * Overdue = dueDate < today AND status not in (PAID, CANCELLED, REFUNDED).
     *
     * @param studentId the student ID
     * @param pageable pagination parameters
     * @return page of overdue invoice response DTOs
     * @since 2.14
     */
    Page<InvoiceResponse> getOverdueInvoicesByStudent(Long studentId, Pageable pageable);

    /**
     * Marks invoice as paid (manual payment recording).
     *
     * @param id the invoice ID
     * @return updated invoice response DTO
     * @since 2.14
     */
    InvoiceResponse markInvoiceAsPaid(Long id);
}
