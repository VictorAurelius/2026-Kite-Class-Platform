package com.kiteclass.core.module.parent.service.impl;

import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.module.parent.audit.ParentFacet;
import com.kiteclass.core.module.parent.audit.ParentReadAuditLogService;
import com.kiteclass.core.module.parent.dto.ParentNotificationFacetResponse;
import com.kiteclass.core.module.parent.repository.ParentStudentLinkRepository;
import com.kiteclass.core.module.parent.service.ConsentService;
import com.kiteclass.core.module.parent.service.ParentNotificationsFacetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * JPA-backed read-only notifications facet scoped to the parent's linked
 * children.
 *
 * <p>Phase 1B v1 stub: returns an empty page after the scope guard
 * succeeds. There is no parent-targeted notifications table yet; the
 * cross-cutting notification engine ships in Wave 18a Bucket B
 * (GAP-063b). The endpoint exists now so the FE notification drawer is
 * wired against a stable contract. Scope guard + audit row are NOT
 * stubbed; they are Phase 1B foundation.
 *
 * @author KiteClass Team
 * @since 2.18.1 (Wave 18b2 — GAP-321b Phase 1B foundation)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParentNotificationsFacetServiceImpl implements ParentNotificationsFacetService {

    /**
     * BR-PARENT-PORTAL-014 — facet name used for the per-field consent
     * lookup.
     */
    public static final String CONSENT_FIELD_NOTIFICATIONS = "notifications";

    private final ParentStudentLinkRepository linkRepository;
    private final ParentReadAuditLogService auditLogService;
    private final ConsentService consentService;

    @Override
    @Transactional(readOnly = true)
    public Page<ParentNotificationFacetResponse> getNotificationsForChild(Long parentId,
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
            log.warn("Parent {} attempted notifications read for unlinked child {} — denied",
                    parentId, childId);
            throw new BusinessException("PARENT_FACET_FORBIDDEN", HttpStatus.FORBIDDEN);
        }

        // BR-PARENT-PORTAL-014 — PDPL granular consent gate (uniform with
        // other 4 facets per Wave 24 GAP-361 v1.5).
        if (!consentService.checkConsent(parentId, childId, CONSENT_FIELD_NOTIFICATIONS)) {
            log.warn("Parent {} attempted notifications read for child {} without consent — denied",
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

        auditLogService.logRead(parentId, childId, ParentFacet.NOTIFICATIONS);

        // TODO (GAP-063b / GAP-321b.1): join the cross-cutting notifications
        // engine when it ships. The v1 stub is intentional.
        return new PageImpl<>(List.of(), pageable, 0);
    }
}
