package com.kiteclass.core.module.parent.service.impl;

import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.module.invoice.entity.Invoice;
import com.kiteclass.core.module.invoice.repository.InvoiceRepository;
import com.kiteclass.core.module.parent.audit.ParentFacet;
import com.kiteclass.core.module.parent.audit.ParentReadAuditLogService;
import com.kiteclass.core.module.parent.dto.ParentFeeFacetResponse;
import com.kiteclass.core.module.parent.repository.ParentStudentLinkRepository;
import com.kiteclass.core.module.parent.service.ConsentService;
import com.kiteclass.core.module.parent.service.ParentFeesFacetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * JPA-backed read-only fees facet scoped to the parent's linked children.
 *
 * <p>Phase 1B remainder (Wave 18b3 — BR-PARENT-FACET-FEES-002): the query
 * narrows by {@code dueDate BETWEEN :from AND :to} and prefetches
 * {@code items} + {@code adjustments} via {@link
 * com.kiteclass.core.module.invoice.repository.InvoiceRepository#findByStudentIdAndDueDateRange}
 * to avoid N+1. Hibernate Statistics test ({@link
 * com.kiteclass.core.module.parent.repository.ParentFeesFacetEntityGraphIT})
 * asserts ≤3 prepared statements per facet call. Scope guard + audit row
 * unchanged from Phase 1B foundation.
 *
 * <p>Out of scope (tracked in <b>GAP-321b.1-fees-instalment-payment-history</b>):
 * instalment-plan join, payment-history projection.
 *
 * @author KiteClass Team
 * @since 2.18.2 (Wave 18b3 — GAP-321b Phase 1B remainder)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParentFeesFacetServiceImpl implements ParentFeesFacetService {

    private final ParentStudentLinkRepository linkRepository;
    private final InvoiceRepository invoiceRepository;
    private final ParentReadAuditLogService auditLogService;
    private final ConsentService consentService;

    /**
     * BR-PARENT-PORTAL-011 — facet name used for the per-field consent
     * lookup. Exposed as a constant so the matching FE settings page +
     * tests reference one symbol.
     */
    public static final String CONSENT_FIELD_FEES = "fees";

    @Override
    @Transactional(readOnly = true)
    public Page<ParentFeeFacetResponse> getFeesForChild(Long parentId,
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
            log.warn("Parent {} attempted fees read for unlinked child {} — denied",
                    parentId, childId);
            throw new BusinessException("PARENT_FACET_FORBIDDEN", HttpStatus.FORBIDDEN);
        }

        // BR-PARENT-PORTAL-011 — PDPL Decree 13/2023 Art 16 granular consent
        // gate. Scope guard above proves the link exists; this gate proves the
        // parent has explicitly granted the `fees` field consent. Default
        // consent (V56 migration) has no fields granted → 403 until parent
        // toggles via PUT /api/v1/parent/consent.
        if (!consentService.checkConsent(parentId, childId, CONSENT_FIELD_FEES)) {
            log.warn("Parent {} attempted fees read for child {} without consent — denied",
                    parentId, childId);
            throw new BusinessException(
                    "PARENT_CONSENT_REQUIRED", HttpStatus.FORBIDDEN);
        }

        // BR-PARENT-PORTAL-015 (Wave 24 GAP-361 v1.5) — re-consent gate.
        // If the parent's stored consent version is below the current
        // required policy version, the facet returns 403 RECONSENT_REQUIRED
        // (FE prompts re-confirmation). Idempotent for parents already at
        // the required version.
        if (consentService.getConsentVersion(parentId, childId)
                < consentService.getRequiredVersion()) {
            log.warn("Parent {} consent version stale for child {} — re-consent required",
                    parentId, childId);
            throw new BusinessException("RECONSENT_REQUIRED", HttpStatus.FORBIDDEN);
        }

        // BR-PARENT-FACET-FEES-002 — date-range narrowing + EntityGraph
        // (items + adjustments prefetched in single round-trip).
        Page<Invoice> page = invoiceRepository
                .findByStudentIdAndDueDateRange(childId, from, to, pageable);

        auditLogService.logRead(parentId, childId, ParentFacet.FEES);

        return page.map(this::toResponse);
    }

    private ParentFeeFacetResponse toResponse(Invoice i) {
        return new ParentFeeFacetResponse(
                i.getId(),
                i.getStudentId(),
                i.getInvoiceNumber(),
                i.getStatus() != null ? i.getStatus().name() : null,
                i.getTotal(),
                i.getBalanceDue(),
                i.getDueDate()
        );
    }
}
