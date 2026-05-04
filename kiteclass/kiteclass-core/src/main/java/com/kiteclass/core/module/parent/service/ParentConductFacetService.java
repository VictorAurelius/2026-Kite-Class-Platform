package com.kiteclass.core.module.parent.service;

import com.kiteclass.core.module.parent.dto.ParentConductFacetResponse;

import java.util.List;

/**
 * Read-only conduct (hạnh kiểm) queries scoped to the authenticated parent's
 * children.
 *
 * <p>Phase 1B v1 stub: backing schema not yet present. Returns an empty
 * list after the scope guard succeeds. Concrete query lands in GAP-321b.1.
 *
 * @author KiteClass Team
 * @since 2.18.1 (Wave 18b2 — GAP-321b Phase 1B foundation)
 */
public interface ParentConductFacetService {

    /**
     * Returns the conduct ratings for one of the parent's linked children,
     * filtered by period (e.g., "HK1-2025-2026"). v1 returns an empty list.
     *
     * @throws com.kiteclass.core.common.exception.BusinessException with code
     *         {@code AUTH_REQUIRED} (401) if {@code parentId} is null
     * @throws com.kiteclass.core.common.exception.BusinessException with code
     *         {@code BAD_REQUEST} (400) if {@code childId} is null
     * @throws com.kiteclass.core.common.exception.BusinessException with code
     *         {@code PARENT_FACET_FORBIDDEN} (403) if no active
     *         {@code ParentStudentLink} edge exists between parent and child
     */
    List<ParentConductFacetResponse> getConductForChild(Long parentId, Long childId, String period);
}
