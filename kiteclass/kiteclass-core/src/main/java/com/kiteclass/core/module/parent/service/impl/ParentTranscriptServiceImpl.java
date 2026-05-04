package com.kiteclass.core.module.parent.service.impl;

import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.module.grade.entity.Transcript;
import com.kiteclass.core.module.grade.repository.TranscriptRepository;
import com.kiteclass.core.module.parent.dto.TranscriptResponse;
import com.kiteclass.core.module.parent.repository.ParentStudentLinkRepository;
import com.kiteclass.core.module.parent.service.ParentTranscriptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * JPA-backed read-only transcript view scoped to the parent's linked children.
 *
 * <p>Scope guard order:
 * <ol>
 *   <li>Reject null {@code parentId} (401 — happens when Gateway forwards a
 *       request without resolving {@code X-User-Reference-Id}).</li>
 *   <li>Reject null {@code childId} (400).</li>
 *   <li>Verify {@code ParentStudentLinkRepository.existsByParentIdAndStudentIdAndDeletedFalse}
 *       (403 if missing). The boolean form is used (not the join-fetch list)
 *       so we leak nothing about the child's identity to a non-linked parent.</li>
 *   <li>Only after all guards pass, query
 *       {@link TranscriptRepository#findByStudentIdAndDeletedFalseOrderBySemesterDesc(Long)}.</li>
 * </ol>
 *
 * <p>{@code @Transactional(readOnly = true)} ensures the Hibernate
 * {@code tenantFilter} is auto-applied on each repository call (consistent with
 * the rest of {@code module/parent}).
 *
 * @author KiteClass Team
 * @since 2.18.0 (Wave 18b1 — GAP-321 Phase 1A)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParentTranscriptServiceImpl implements ParentTranscriptService {

    private final ParentStudentLinkRepository linkRepository;
    private final TranscriptRepository transcriptRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TranscriptResponse> getTranscriptsForChild(Long parentId, Long childId) {
        if (parentId == null) {
            throw new BusinessException("AUTH_REQUIRED", HttpStatus.UNAUTHORIZED);
        }
        if (childId == null) {
            throw new BusinessException("BAD_REQUEST", HttpStatus.BAD_REQUEST);
        }

        // BR-PARENT-PORTAL-001: parent may only read transcripts for children
        // they are linked to via a non-deleted ParentStudentLink. We use the
        // boolean exists query (not the fetch query) so that a non-linked
        // caller never reaches the Transcript table.
        if (!linkRepository.existsByParentIdAndStudentIdAndDeletedFalse(parentId, childId)) {
            log.warn("Parent {} attempted transcript read for unlinked child {} — denied", parentId, childId);
            throw new BusinessException("PARENT_NOT_LINKED", HttpStatus.FORBIDDEN);
        }

        List<Transcript> transcripts =
                transcriptRepository.findByStudentIdAndDeletedFalseOrderBySemesterDesc(childId);
        return transcripts.stream()
                .map(this::toResponse)
                .toList();
    }

    private TranscriptResponse toResponse(Transcript t) {
        return new TranscriptResponse(
                t.getId(),
                t.getStudentId(),
                t.getSemester(),
                t.getAcademicYear(),
                t.getTotalCredits(),
                t.getSemesterGpa(),
                t.getCumulativeGpa(),
                t.getTotalCourses(),
                t.getPassedCourses(),
                t.getFailedCourses()
        );
    }
}
