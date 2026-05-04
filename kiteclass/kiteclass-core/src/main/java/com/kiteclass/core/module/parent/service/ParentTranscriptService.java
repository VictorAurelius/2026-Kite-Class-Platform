package com.kiteclass.core.module.parent.service;

import com.kiteclass.core.module.parent.dto.TranscriptResponse;

import java.util.List;

/**
 * Read-only transcript queries scoped to the authenticated parent's children.
 *
 * <p>Phase 1A — single facet (transcript) of the K-12 LEGAL parent portal
 * (GAP-321). Five sister facets — điểm danh, học phí, hạnh kiểm, notifications,
 * kỷ luật — are deferred to GAP-321b (Phase 1B).
 *
 * <p>The implementation MUST verify a non-deleted {@code ParentStudentLink}
 * edge between the calling parent and the requested child BEFORE any data
 * fetch (BR-PARENT-PORTAL-001 in {@code parent-portal/rules.md}). Without this
 * scope guard, a parent could enumerate other students' transcripts by
 * incrementing the path id.
 *
 * @author KiteClass Team
 * @since 2.18.0 (Wave 18b1 — GAP-321 Phase 1A)
 */
public interface ParentTranscriptService {

    /**
     * Lists all transcripts (newest semester first) for one of the parent's
     * linked children.
     *
     * @param parentId authenticated parent id (from {@code X-User-Reference-Id})
     * @param childId  requested child's student id (from path)
     * @return list of transcripts, possibly empty
     * @throws com.kiteclass.core.common.exception.BusinessException with code
     *         {@code AUTH_REQUIRED} (401) if {@code parentId} is null
     * @throws com.kiteclass.core.common.exception.BusinessException with code
     *         {@code BAD_REQUEST} (400) if {@code childId} is null
     * @throws com.kiteclass.core.common.exception.BusinessException with code
     *         {@code PARENT_NOT_LINKED} (403) if no active
     *         {@code ParentStudentLink} edge exists between parent and child
     */
    List<TranscriptResponse> getTranscriptsForChild(Long parentId, Long childId);
}
