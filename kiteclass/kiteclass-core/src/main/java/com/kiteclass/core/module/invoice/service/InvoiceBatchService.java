package com.kiteclass.core.module.invoice.service;

import com.kiteclass.core.module.invoice.dto.BatchInvoiceConfirmResponse;
import com.kiteclass.core.module.invoice.dto.BatchInvoicePreviewResponse;

/**
 * Batch monthly invoice generation (GAP-297).
 *
 * <p>Lets a center Owner generate one invoice per active enrollment for a given
 * month in a single operation, with mid-month pro-rata and idempotent confirm.
 *
 * <p>Business Rules:
 * <ul>
 *   <li>BR-INV-BATCH-001: One invoice per (tenant, enrollment, billing month).</li>
 *   <li>BR-INV-BATCH-002: Mid-month enrollment tuition is pro-rated by remaining days.</li>
 *   <li>BR-INV-BATCH-003: batch-confirm is idempotent — re-running for the same
 *       (tenant, month) skips enrollments already invoiced.</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since GAP-297
 */
public interface InvoiceBatchService {

    /**
     * Previews the invoices that would be generated for the given month — NO
     * persistence. Tenant-scoped via {@code TenantContext}.
     *
     * @param month billed month in {@code yyyy-MM} form (e.g. {@code 2026-05})
     * @return preview with count, total revenue and per-enrollment line items
     */
    BatchInvoicePreviewResponse generatePreview(String month);

    /**
     * Persists invoices for every active enrollment for the given month and emits
     * an {@code InvoiceCreatedEvent} per created invoice. Idempotent: enrollments
     * already invoiced for the month are skipped, not duplicated.
     *
     * @param month billed month in {@code yyyy-MM} form
     * @return confirmation with created/skipped counts + created invoice ids
     */
    BatchInvoiceConfirmResponse confirm(String month);
}
