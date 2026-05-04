package com.kiteclass.core.module.parent.service.impl;

import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.module.attendance.dto.AttendancePeriodResponse;
import com.kiteclass.core.module.attendance.entity.AttendancePeriod;
import com.kiteclass.core.module.attendance.repository.AttendancePeriodRepository;
import com.kiteclass.core.module.parent.audit.ParentFacet;
import com.kiteclass.core.module.parent.audit.ParentReadAuditLogService;
import com.kiteclass.core.module.parent.repository.ParentStudentLinkRepository;
import com.kiteclass.core.module.parent.service.ParentAttendanceFacetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * JPA-backed read-only attendance facet scoped to the parent's linked
 * children.
 *
 * <p>Mirrors the {@code ParentTranscriptServiceImpl} scope-guard pattern:
 * <ol>
 *   <li>Reject null parent (401).</li>
 *   <li>Reject null/inverted range (400).</li>
 *   <li>Verify {@code ParentStudentLink} edge (403 — leak-free).</li>
 *   <li>Only then query the upstream attendance-period repo (read-only API
 *       shipped Wave 18b1 GAP-323 Phase 1A — never duplicated here).</li>
 *   <li>Best-effort emit one audit row.</li>
 * </ol>
 *
 * @author KiteClass Team
 * @since 2.18.1 (Wave 18b2 — GAP-321b Phase 1B foundation)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParentAttendanceFacetServiceImpl implements ParentAttendanceFacetService {

    private final ParentStudentLinkRepository linkRepository;
    private final AttendancePeriodRepository attendancePeriodRepository;
    private final ParentReadAuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public Page<AttendancePeriodResponse> getAttendanceForChild(Long parentId,
                                                                Long childId,
                                                                LocalDate from,
                                                                LocalDate to,
                                                                Pageable pageable) {
        if (parentId == null) {
            throw new BusinessException("AUTH_REQUIRED", HttpStatus.UNAUTHORIZED);
        }
        if (childId == null || from == null || to == null || from.isAfter(to)) {
            throw new BusinessException("BAD_REQUEST", HttpStatus.BAD_REQUEST);
        }
        if (!linkRepository.existsByParentIdAndStudentIdAndDeletedFalse(parentId, childId)) {
            log.warn("Parent {} attempted attendance read for unlinked child {} — denied",
                    parentId, childId);
            throw new BusinessException("PARENT_FACET_FORBIDDEN", HttpStatus.FORBIDDEN);
        }

        Page<AttendancePeriod> page = attendancePeriodRepository
                .findByStudentIdAndDateBetweenAndDeletedFalse(childId, from, to, pageable);

        auditLogService.logRead(parentId, childId, ParentFacet.ATTENDANCE);

        return page.map(this::toResponse);
    }

    private AttendancePeriodResponse toResponse(AttendancePeriod p) {
        return AttendancePeriodResponse.builder()
                .id(p.getId())
                .studentId(p.getStudentId())
                .classId(p.getClassId())
                .subjectSectionId(p.getSubjectSectionId())
                .periodNo(p.getPeriodNo())
                .date(p.getDate())
                .status(p.getStatus())
                .recordedBy(p.getRecordedBy())
                .recordedAt(p.getRecordedAt())
                .notes(p.getNotes())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
