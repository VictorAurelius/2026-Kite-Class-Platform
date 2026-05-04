package com.kiteclass.core.module.parent.dto;

/**
 * Read-only conduct (hạnh kiểm) projection exposed to a parent for one of
 * their linked children.
 *
 * <p>Phase 1B v1 stub: backing schema for hạnh kiểm rating is not yet in
 * place — current K-12 conduct data lives in legacy paper records, no
 * digital column exists in {@code grades} or {@code report_cards}.
 * Endpoint returns an empty list until GAP-321b.1 ships the schema +
 * entity. Until then this DTO documents the contract the FE will rely on.
 *
 * @param studentId    child's id (always matches the path parameter)
 * @param period       e.g. "Học kỳ 1 — 2025-2026" / "Cả năm 2025-2026"
 * @param rating       hạnh kiểm rating: TỐT / KHÁ / TRUNG_BÌNH / YẾU
 * @param remark       teacher remark (nullable; minimum projection per
 *                     BR-PARENT-PORTAL-006)
 * @author KiteClass Team
 * @since 2.18.1 (Wave 18b2 — GAP-321b Phase 1B foundation)
 */
public record ParentConductFacetResponse(
        Long studentId,
        String period,
        String rating,
        String remark
) {
}
