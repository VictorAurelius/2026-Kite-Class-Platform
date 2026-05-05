package com.kiteclass.core.module.parent.service.impl;

import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.module.parent.dto.FileComplaintRequest;
import com.kiteclass.core.module.parent.dto.ParentComplaintResponse;
import com.kiteclass.core.module.parent.entity.ParentComplaint;
import com.kiteclass.core.module.parent.repository.ParentComplaintRepository;
import com.kiteclass.core.module.parent.repository.ParentStudentLinkRepository;
import com.kiteclass.core.module.parent.service.ParentComplaintService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * v1 INSERT-only complaint writer (Wave 19 — GAP-321c Phase 1C v1).
 *
 * <p>BR-PARENT-PORTAL-013: scope guard reuses the
 * {@link ParentStudentLinkRepository#existsByParentIdAndStudentIdAndDeletedFalse}
 * boolean form (same pattern as Phase 1B facet services); 403 if no link.
 *
 * <p>Audit row not emitted here — write actions ship their own audit
 * skeleton in GAP-321c follow-up. The {@link com.kiteclass.core.common.entity.BaseEntity}
 * createdBy/createdAt fields cover the minimum traceability for v1.
 *
 * @since 2.19.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParentComplaintServiceImpl implements ParentComplaintService {

    private final ParentStudentLinkRepository linkRepository;
    private final ParentComplaintRepository complaintRepository;

    @Override
    @Transactional
    public ParentComplaintResponse fileComplaint(Long parentId, FileComplaintRequest request) {
        if (parentId == null) {
            throw new BusinessException("AUTH_REQUIRED", HttpStatus.UNAUTHORIZED);
        }
        if (request == null
                || request.studentId() == null
                || request.complaintText() == null
                || request.complaintText().isBlank()) {
            throw new BusinessException("BAD_REQUEST", HttpStatus.BAD_REQUEST);
        }
        if (!linkRepository.existsByParentIdAndStudentIdAndDeletedFalse(
                parentId, request.studentId())) {
            log.warn("Parent {} attempted complaint for unlinked student {} — denied",
                    parentId, request.studentId());
            throw new BusinessException("PARENT_FACET_FORBIDDEN", HttpStatus.FORBIDDEN);
        }
        ParentComplaint entity = ParentComplaint.builder()
                .parentId(parentId)
                .studentId(request.studentId())
                .complaintText(request.complaintText())
                .status(ParentComplaint.Status.PENDING)
                .build();
        ParentComplaint saved = complaintRepository.save(entity);
        log.info("Parent {} filed complaint {} for student {}",
                parentId, saved.getId(), request.studentId());
        return ParentComplaintResponse.from(saved);
    }
}
