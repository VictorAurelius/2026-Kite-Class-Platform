package com.kiteclass.core.module.parent.service.impl;

import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.module.grade.entity.Transcript;
import com.kiteclass.core.module.grade.repository.TranscriptRepository;
import com.kiteclass.core.module.parent.dto.TranscriptResponse;
import com.kiteclass.core.module.parent.repository.ParentStudentLinkRepository;
import com.kiteclass.core.module.parent.service.ConsentService;
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

    /**
     * BR-PARENT-PORTAL-014 — facet name used for the per-field consent
     * lookup. Exposed as a constant so the matching FE settings page +
     * tests reference one symbol.
     */
    public static final String CONSENT_FIELD_TRANSCRIPT = "transcript";

    private final ParentStudentLinkRepository linkRepository;
    private final TranscriptRepository transcriptRepository;
    private final ConsentService consentService;

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

        // BR-PARENT-PORTAL-014 — PDPL Decree 13/2023 Art 16 granular consent
        // gate (uniform across all 5 facets per Wave 24 GAP-361 v1.5).
        if (!consentService.checkConsent(parentId, childId, CONSENT_FIELD_TRANSCRIPT)) {
            log.warn("Parent {} attempted transcript read for child {} without consent — denied",
                    parentId, childId);
            throw new BusinessException("PARENT_CONSENT_REQUIRED", HttpStatus.FORBIDDEN);
        }

        // BR-PARENT-PORTAL-015 — re-consent gate. If the parent's stored
        // consent version is below the current required policy version, the
        // facet returns 403 RECONSENT_REQUIRED (FE prompts re-confirmation).
        if (consentService.getConsentVersion(parentId, childId)
                < consentService.getRequiredVersion()) {
            log.warn("Parent {} consent version stale for child {} — re-consent required",
                    parentId, childId);
            throw new BusinessException("RECONSENT_REQUIRED", HttpStatus.FORBIDDEN);
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
