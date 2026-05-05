package com.kiteclass.core.module.parent.service;

import com.kiteclass.core.module.parent.dto.ParentalConsent;

import java.util.Map;

/**
 * PDPL Decree 13/2023 Art 16 granular consent gate.
 *
 * <p>Read by every parent-side facet API before returning data:
 * {@link #checkConsent(Long, Long, String)} returns {@code false} when
 * the parent has NOT granted consent for the named field; the calling
 * facet then short-circuits with 403 {@code PARENT_CONSENT_REQUIRED}.
 *
 * <p>Written by the parent-settings endpoints
 * ({@code GET/PUT /api/v1/parent/consent}) via {@link #bumpConsent}.
 * Updates per-field flags + bumps {@code version} + sets
 * {@code updatedAt} in one transactional pass.
 *
 * <p>Wave 19 v1 ships the gate + getters + setter. Re-consent on policy
 * version bump + admin tooling are deferred to the GAP-321c follow-up.
 *
 * @author KiteClass Team
 * @since 2.19.0 (Wave 19 — GAP-321c Phase 1C v1)
 */
public interface ConsentService {

    /**
     * @param parentId authenticated parent id
     * @param childId  linked student id
     * @param field    facet/field name (e.g., {@code "fees"},
     *                 {@code "conduct"})
     * @return {@code true} only if a non-deleted
     *         {@link com.kiteclass.core.module.parent.entity.ParentStudentLink}
     *         exists between parent and child AND the link's
     *         {@code parentalConsent.fields[field] == TRUE}; {@code false}
     *         otherwise (no link / null inputs / missing field).
     */
    boolean checkConsent(Long parentId, Long childId, String field);

    /**
     * @param parentId authenticated parent id
     * @param childId  linked student id
     * @return current consent payload (or {@link ParentalConsent#defaultValue()}
     *         when no link exists)
     */
    ParentalConsent getConsent(Long parentId, Long childId);

    /**
     * Returns the consent version on the parent-student link. Used by FE
     * to detect re-consent prompts when policy versions bump.
     *
     * @return version (default {@code 1} if link not found)
     */
    int getConsentVersion(Long parentId, Long childId);

    /**
     * Updates per-field flags on the link's consent map, bumps the
     * version, and stamps {@code updatedAt}.
     *
     * <p>Throws {@code BusinessException("PARENT_CONSENT_LINK_NOT_FOUND",
     * 404)} if no active link exists between parent and child.
     *
     * @param parentId authenticated parent id
     * @param childId  linked student id
     * @param updates  field → granted/revoked map (key = field name,
     *                 value = consent flag)
     * @return refreshed consent payload after the bump
     */
    ParentalConsent bumpConsent(Long parentId, Long childId, Map<String, Boolean> updates);
}
