package com.kiteclass.core.module.parent.service;

import com.kiteclass.core.module.parent.dto.ParentFeeFacetResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

/**
 * Read-only fee/invoice queries scoped to the authenticated parent's
 * children.
 *
 * <p>Phase 1B v1 stub: maps from {@code Invoice} where data exists. The
 * underlying invoice schema covers the contract; richer breakdown
 * (instalments, payment history) is deferred to GAP-321b.1.
 *
 * @author KiteClass Team
 * @since 2.18.1 (Wave 18b2 — GAP-321b Phase 1B foundation)
 */
public interface ParentFeesFacetService {

    /**
     * Returns a page of invoices for one of the parent's linked children,
     * filtered by issue date.
     *
     * @throws com.kiteclass.core.common.exception.BusinessException with code
     *         {@code AUTH_REQUIRED} (401) if {@code parentId} is null
     * @throws com.kiteclass.core.common.exception.BusinessException with code
     *         {@code BAD_REQUEST} (400) if {@code childId} or range argument
     *         is null or the range is inverted
     * @throws com.kiteclass.core.common.exception.BusinessException with code
     *         {@code PARENT_FACET_FORBIDDEN} (403) if no active
     *         {@code ParentStudentLink} edge exists between parent and child
     */
    Page<ParentFeeFacetResponse> getFeesForChild(Long parentId,
                                                 Long childId,
                                                 LocalDate from,
                                                 LocalDate to,
                                                 Pageable pageable);
}
