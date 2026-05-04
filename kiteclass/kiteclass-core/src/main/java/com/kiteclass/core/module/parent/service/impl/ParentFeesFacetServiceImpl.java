package com.kiteclass.core.module.parent.service.impl;

import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.module.invoice.entity.Invoice;
import com.kiteclass.core.module.invoice.repository.InvoiceRepository;
import com.kiteclass.core.module.parent.audit.ParentFacet;
import com.kiteclass.core.module.parent.audit.ParentReadAuditLogService;
import com.kiteclass.core.module.parent.dto.ParentFeeFacetResponse;
import com.kiteclass.core.module.parent.repository.ParentStudentLinkRepository;
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
 * <p>Phase 1B v1 stub: maps from {@code Invoice} via the existing
 * {@code findByStudentIdAndDeletedFalse}. The {@code from}/{@code to}
 * range arguments are accepted in the API surface for forward compat but
 * the v1 query does not yet narrow on issue date — concrete narrowing
 * lands in GAP-321b.1. The api-contract.md flags this explicitly. Scope
 * guard + audit row are NOT stubbed; they are Phase 1B foundation.
 *
 * @author KiteClass Team
 * @since 2.18.1 (Wave 18b2 — GAP-321b Phase 1B foundation)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParentFeesFacetServiceImpl implements ParentFeesFacetService {

    private final ParentStudentLinkRepository linkRepository;
    private final InvoiceRepository invoiceRepository;
    private final ParentReadAuditLogService auditLogService;

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

        // TODO (GAP-321b.1): replace with a date-range-narrowing JPQL that
        // also joins instalments + payment history. v1 ships the
        // unfiltered child-scoped page so the FE drill-down can wire
        // against the contract immediately.
        Page<Invoice> page = invoiceRepository.findByStudentIdAndDeletedFalse(childId, pageable);

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
