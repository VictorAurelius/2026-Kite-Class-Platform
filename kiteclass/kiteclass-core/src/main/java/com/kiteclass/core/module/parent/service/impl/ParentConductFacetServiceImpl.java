package com.kiteclass.core.module.parent.service.impl;

import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.module.childprotection.entity.Incident;
import com.kiteclass.core.module.childprotection.enums.IncidentVisibilityScope;
import com.kiteclass.core.module.childprotection.repository.IncidentRepository;
import com.kiteclass.core.module.parent.audit.ParentFacet;
import com.kiteclass.core.module.parent.audit.ParentReadAuditLogService;
import com.kiteclass.core.module.parent.dto.ParentConductFacetResponse;
import com.kiteclass.core.module.parent.repository.ParentStudentLinkRepository;
import com.kiteclass.core.module.parent.service.ConsentService;
import com.kiteclass.core.module.parent.service.ParentConductFacetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * JPA-backed read-only conduct (hạnh kiểm) facet scoped to the parent's
 * linked children.
 *
 * <p>Phase 1C v1 (Wave 19 Bucket D — GAP-321b-1-conduct): replaces the
 * Phase 1B v1 stub with a real query against {@link Incident} filtered to
 * {@link IncidentVisibilityScope#PARENT_VISIBLE} and
 * {@link IncidentVisibilityScope#PUBLIC} per BR-CHILD-PROTECT-005 (Bucket A).
 * Records defaulting to {@link IncidentVisibilityScope#STAFF_ONLY} (legacy
 * + new abuse / grooming / CSAM tickets) NEVER reach the parent portal
 * through this surface.
 *
 * <p>Hạnh kiểm rating (TỐT / KHÁ / TRUNG_BÌNH / YẾU) is not yet a digital
 * column in {@code grades} or {@code report_cards} — current K-12
 * deployments still record it on paper. Until the rating store ships,
 * this facet projects the {@code Incident.severity} into a coarse rating
 * label so parents see a meaningful signal:
 * <ul>
 *   <li>{@code CRITICAL} → {@code "YẾU"} (rare; only PARENT_VISIBLE/PUBLIC
 *       will ever flow through here, so this would be a downgraded officer
 *       choice)</li>
 *   <li>{@code HIGH} → {@code "TRUNG_BÌNH"}</li>
 *   <li>{@code MEDIUM} → {@code "KHÁ"}</li>
 *   <li>{@code LOW} → {@code "TỐT"}</li>
 * </ul>
 *
 * <p>The remark is the {@link Incident#getTitle()} (kept plaintext for
 * indexing / list views). The encrypted {@code description} field is NOT
 * surfaced — even for {@code PARENT_VISIBLE} rows the narrative may name
 * other minors and is gated behind the safeguarding-officer surface
 * (Phase 1C remainder).
 *
 * @author KiteClass Team
 * @since 2.19.0 (Wave 19 Bucket D — GAP-321b-1-conduct real wiring)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParentConductFacetServiceImpl implements ParentConductFacetService {

    /**
     * Visibility scopes the parent caller may see — per BR-CHILD-PROTECT-005
     * the staff-restricted scopes ({@code STAFF_ONLY}, {@code RESTRICTED})
     * are excluded.
     */
    private static final Set<IncidentVisibilityScope> PARENT_VISIBLE_SCOPES =
            EnumSet.of(IncidentVisibilityScope.PARENT_VISIBLE, IncidentVisibilityScope.PUBLIC);

    /**
     * BR-PARENT-PORTAL-014 — facet name used for the per-field consent
     * lookup.
     */
    public static final String CONSENT_FIELD_CONDUCT = "conduct";

    private final ParentStudentLinkRepository linkRepository;
    private final IncidentRepository incidentRepository;
    private final ParentReadAuditLogService auditLogService;
    private final ConsentService consentService;

    @Override
    @Transactional(readOnly = true)
    public List<ParentConductFacetResponse> getConductForChild(Long parentId, Long childId, String period) {
        if (parentId == null) {
            throw new BusinessException("AUTH_REQUIRED", HttpStatus.UNAUTHORIZED);
        }
        if (childId == null) {
            throw new BusinessException("BAD_REQUEST", HttpStatus.BAD_REQUEST);
        }
        if (!linkRepository.existsByParentIdAndStudentIdAndDeletedFalse(parentId, childId)) {
            log.warn("Parent {} attempted conduct read for unlinked child {} — denied",
                    parentId, childId);
            throw new BusinessException("PARENT_FACET_FORBIDDEN", HttpStatus.FORBIDDEN);
        }

        // BR-PARENT-PORTAL-014 — PDPL granular consent gate (uniform with
        // other 4 facets per Wave 24 GAP-361 v1.5).
        if (!consentService.checkConsent(parentId, childId, CONSENT_FIELD_CONDUCT)) {
            log.warn("Parent {} attempted conduct read for child {} without consent — denied",
                    parentId, childId);
            throw new BusinessException("PARENT_CONSENT_REQUIRED", HttpStatus.FORBIDDEN);
        }

        // BR-PARENT-PORTAL-015 — re-consent gate.
        if (consentService.getConsentVersion(parentId, childId)
                < consentService.getRequiredVersion()) {
            log.warn("Parent {} consent version stale for child {} — re-consent required",
                    parentId, childId);
            throw new BusinessException("RECONSENT_REQUIRED", HttpStatus.FORBIDDEN);
        }

        // BR-CHILD-PROTECT-005 + BR-PARENT-FACET-CONDUCT-002: real wiring —
        // only PARENT_VISIBLE + PUBLIC scopes surface to parents. The default
        // STAFF_ONLY (set on every legacy + newly-created Incident per V54)
        // ensures abuse/grooming/CSAM records cannot leak.
        List<Incident> visible = incidentRepository.findVisibleForParentList(
                childId, PARENT_VISIBLE_SCOPES);

        auditLogService.logRead(parentId, childId, ParentFacet.CONDUCT);

        return visible.stream()
                .map(i -> toResponse(i, period))
                .toList();
    }

    private ParentConductFacetResponse toResponse(Incident i, String period) {
        return new ParentConductFacetResponse(
                i.getSubjectStudentId(),
                period,
                ratingFromSeverity(i),
                i.getTitle());
    }

    /**
     * Project the {@code Incident.severity} into a coarse hạnh kiểm rating
     * label until the digital rating store ships. Returns {@code null}
     * when severity is missing rather than guessing a rating — defensive
     * against schema drift.
     */
    private String ratingFromSeverity(Incident i) {
        if (i.getSeverity() == null) {
            return null;
        }
        return switch (i.getSeverity()) {
            case CRITICAL -> "YẾU";
            case HIGH -> "TRUNG_BÌNH";
            case MEDIUM -> "KHÁ";
            case LOW -> "TỐT";
        };
    }
}
