package com.kiteclass.core.module.invoice.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Preview of a batch monthly invoice run (GAP-297) — NO persistence.
 *
 * <p>Returned by {@code POST /api/v1/invoices/batch-generate}. Lets a center Owner
 * review the projected invoices (count, total revenue, per-enrollment breakdown)
 * before confirming the batch.
 *
 * @param month            billed month in {@code yyyy-MM} form (e.g. {@code 2026-05})
 * @param invoiceCount     number of invoices that would be generated
 * @param totalRevenue     sum of all line totals in VND
 * @param invoices         per-enrollment projected line items
 * @author KiteClass Team
 * @since GAP-297
 */
public record BatchInvoicePreviewResponse(
        String month,
        int invoiceCount,
        BigDecimal totalRevenue,
        List<BatchInvoiceLineItem> invoices
) {
}
