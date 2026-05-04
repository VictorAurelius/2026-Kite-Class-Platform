package com.kiteclass.core.module.parent.service;

import com.kiteclass.core.module.parent.dto.ParentNotificationFacetResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

/**
 * Read-only notification queries scoped to the authenticated parent's
 * children.
 *
 * <p>Phase 1B v1 stub: returns an empty page after the scope guard
 * succeeds. The cross-cutting notification engine lands in Wave 18a Bucket B
 * (GAP-063b); concrete query joins the engine when it ships.
 *
 * @author KiteClass Team
 * @since 2.18.1 (Wave 18b2 — GAP-321b Phase 1B foundation)
 */
public interface ParentNotificationsFacetService {

    /**
     * Returns a page of notifications targeted at one of the parent's
     * linked children, newest-first, restricted to the supplied date range.
     * v1 returns an empty page.
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
    Page<ParentNotificationFacetResponse> getNotificationsForChild(Long parentId,
                                                                   Long childId,
                                                                   LocalDate from,
                                                                   LocalDate to,
                                                                   Pageable pageable);
}
