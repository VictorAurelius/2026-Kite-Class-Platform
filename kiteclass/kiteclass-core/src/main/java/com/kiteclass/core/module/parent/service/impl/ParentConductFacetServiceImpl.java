package com.kiteclass.core.module.parent.service.impl;

import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.module.parent.audit.ParentFacet;
import com.kiteclass.core.module.parent.audit.ParentReadAuditLogService;
import com.kiteclass.core.module.parent.dto.ParentConductFacetResponse;
import com.kiteclass.core.module.parent.repository.ParentStudentLinkRepository;
import com.kiteclass.core.module.parent.service.ParentConductFacetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * JPA-backed read-only conduct (hạnh kiểm) facet scoped to the parent's
 * linked children.
 *
 * <p>Phase 1B v1 stub: backing schema for digital hạnh kiểm rating is not
 * yet present — current K-12 deployments record conduct on paper. Endpoint
 * always returns an empty list after the scope guard succeeds. Concrete
 * source-of-truth lands in GAP-321b.1. Scope guard + audit row are NOT
 * stubbed; they are Phase 1B foundation.
 *
 * @author KiteClass Team
 * @since 2.18.1 (Wave 18b2 — GAP-321b Phase 1B foundation)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParentConductFacetServiceImpl implements ParentConductFacetService {

    private final ParentStudentLinkRepository linkRepository;
    private final ParentReadAuditLogService auditLogService;

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

        auditLogService.logRead(parentId, childId, ParentFacet.CONDUCT);

        // TODO (GAP-321b.1): replace stub with the digital hạnh kiểm
        // rating store once shipped. The v1 stub is intentional — the
        // scope guard + audit row are the foundation contract.
        return List.of();
    }
}
