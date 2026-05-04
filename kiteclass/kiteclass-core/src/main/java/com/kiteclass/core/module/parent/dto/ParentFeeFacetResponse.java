package com.kiteclass.core.module.parent.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Read-only fee/invoice projection exposed to a parent for one of their
 * linked children.
 *
 * <p>Phase 1B v1 stub: maps from {@code Invoice} where backing data exists.
 * Returns whatever subset the existing payment/invoice tables already
 * contain — concrete query specialisation (instalment breakdown, payment
 * history join) is deferred to GAP-321b.1 follow-up. The minimum viable
 * projection enumerated below mirrors the parent-portal use-case
 * UC-PARENT-FACET-FEES-001 acceptance criteria (parent sees what they owe
 * + due date).
 *
 * <p>Design rationale: even when the source schema evolves (e.g., addition
 * of partial-payment ledger), this DTO stays stable so the FE doesn't
 * thrash. Adding fields requires reviewer sign-off per
 * {@code rules.md} BR-PARENT-FACET-FEES-001.
 *
 * @param invoiceId       primary key (opaque to UI; useful for cache key)
 * @param studentId       child's id (always matches the path parameter)
 * @param invoiceNumber   human-readable invoice number (nullable for legacy)
 * @param status          {@code DRAFT / SENT / PARTIAL / PAID / OVERDUE / CANCELLED}
 * @param totalAmount     total invoice amount (VND, scale 2)
 * @param balanceDue      outstanding balance after partial payments
 * @param dueDate         due date; nullable if not yet scheduled
 * @author KiteClass Team
 * @since 2.18.1 (Wave 18b2 — GAP-321b Phase 1B foundation)
 */
public record ParentFeeFacetResponse(
        Long invoiceId,
        Long studentId,
        String invoiceNumber,
        String status,
        BigDecimal totalAmount,
        BigDecimal balanceDue,
        LocalDate dueDate
) {
}
