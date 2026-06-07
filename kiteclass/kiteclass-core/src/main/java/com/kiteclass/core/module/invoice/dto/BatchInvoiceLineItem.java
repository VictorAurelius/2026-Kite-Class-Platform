package com.kiteclass.core.module.invoice.dto;

import java.math.BigDecimal;

/**
 * One enrollment's projected invoice line in a batch monthly invoice run (GAP-297).
 *
 * <p>Returned by both batch-generate (preview) and batch-confirm (persist) so the
 * Phase 2 frontend can render the per-enrollment breakdown before and after
 * persistence. Amounts are in VND.
 *
 * @param enrollmentId     the enrollment being billed
 * @param studentId        the student the invoice is for
 * @param classId          the class the enrollment belongs to
 * @param classNameVi      class display name (nullable if the class was not found)
 * @param tuitionAmount    original monthly tuition (before pro-rata + discount)
 * @param discountPercent  enrollment discount percentage (0–100)
 * @param proratedTuition  tuition after pro-rata for mid-month enrollments
 * @param discountAmount   discount applied on the prorated tuition (positive value)
 * @param total            final billable amount = proratedTuition − discountAmount
 * @param prorated         true when the enrollment started mid-month (pro-rata applied)
 * @param billableDays     number of billed days within the month
 * @param daysInMonth      total days in the billed month
 * @author KiteClass Team
 * @since GAP-297
 */
public record BatchInvoiceLineItem(
        Long enrollmentId,
        Long studentId,
        Long classId,
        String classNameVi,
        BigDecimal tuitionAmount,
        BigDecimal discountPercent,
        BigDecimal proratedTuition,
        BigDecimal discountAmount,
        BigDecimal total,
        boolean prorated,
        int billableDays,
        int daysInMonth
) {
}
