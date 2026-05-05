package com.kiteclass.core.module.parent.service;

import com.kiteclass.core.module.parent.dto.FileComplaintRequest;
import com.kiteclass.core.module.parent.dto.ParentComplaintResponse;

/**
 * Parent-side complaint write surface (Wave 19 — GAP-321c Phase 1C v1).
 *
 * <p>v1 ships INSERT-only path: validate scope (linked parent ⇄ child),
 * persist a {@link com.kiteclass.core.module.parent.entity.ParentComplaint}
 * row in {@code PENDING}, return the new id. Workflow / triage UI lands
 * in GAP-339.
 *
 * @since 2.19.0
 */
public interface ParentComplaintService {

    /**
     * @param parentId authenticated parent id
     * @param request  validated request body
     * @return persisted complaint summary
     * @throws com.kiteclass.core.common.exception.BusinessException with
     *         code {@code AUTH_REQUIRED} (401) if parentId is null,
     *         {@code BAD_REQUEST} (400) on missing fields, or
     *         {@code PARENT_FACET_FORBIDDEN} (403) if no link exists.
     */
    ParentComplaintResponse fileComplaint(Long parentId, FileComplaintRequest request);
}
