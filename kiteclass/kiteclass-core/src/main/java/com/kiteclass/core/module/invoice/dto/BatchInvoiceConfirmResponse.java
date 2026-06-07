package com.kiteclass.core.module.invoice.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Result of confirming a batch monthly invoice run (GAP-297) — invoices persisted.
 *
 * <p>Returned by {@code POST /api/v1/invoices/batch-confirm}. Idempotent: enrollments
 * that already have an invoice for {@code month} are counted in {@code skippedCount}
 * instead of producing duplicates.
 *
 * @param month             billed month in {@code yyyy-MM} form
 * @param createdCount      number of invoices newly persisted in this run
 * @param skippedCount      number of enrollments already invoiced for the month (idempotency)
 * @param totalRevenue      sum of all CREATED invoice totals in VND
 * @param createdInvoiceIds ids of the invoices created in this run
 * @author KiteClass Team
 * @since GAP-297
 */
public record BatchInvoiceConfirmResponse(
        String month,
        int createdCount,
        int skippedCount,
        BigDecimal totalRevenue,
        List<Long> createdInvoiceIds
) {
}
