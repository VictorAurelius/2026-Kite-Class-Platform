package com.kiteclass.core.module.parent.service;

import com.kiteclass.core.module.attendance.dto.AttendancePeriodResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

/**
 * Read-only attendance-period queries scoped to the authenticated parent's
 * children.
 *
 * <p>Wave 18b2 Bucket C (GAP-321b Phase 1B foundation) — sister facet of
 * the existing {@link ParentTranscriptService}. Reuses the {@code
 * ParentStudentLink} scope guard pattern + the AttendancePeriod read-only
 * API shipped Wave 18b1 (GAP-323 Phase 1A) — never duplicates the upstream
 * data; never invents writes.
 *
 * @author KiteClass Team
 * @since 2.18.1 (Wave 18b2 — GAP-321b Phase 1B foundation)
 */
public interface ParentAttendanceFacetService {

    /**
     * Returns a page of period attendance records for one of the parent's
     * linked children, oldest-first, restricted to the supplied date range.
     *
     * @throws com.kiteclass.core.common.exception.BusinessException with code
     *         {@code AUTH_REQUIRED} (401) if {@code parentId} is null
     * @throws com.kiteclass.core.common.exception.BusinessException with code
     *         {@code BAD_REQUEST} (400) if {@code childId} or any range
     *         argument is null or the range is inverted
     * @throws com.kiteclass.core.common.exception.BusinessException with code
     *         {@code PARENT_FACET_FORBIDDEN} (403) if no active
     *         {@code ParentStudentLink} edge exists between parent and child
     */
    Page<AttendancePeriodResponse> getAttendanceForChild(Long parentId,
                                                        Long childId,
                                                        LocalDate from,
                                                        LocalDate to,
                                                        Pageable pageable);
}
