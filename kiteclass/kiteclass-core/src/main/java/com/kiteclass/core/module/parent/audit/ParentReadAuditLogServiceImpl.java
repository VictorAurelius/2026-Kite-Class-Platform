package com.kiteclass.core.module.parent.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * JPA-backed best-effort writer for {@link ParentReadAuditLog}.
 *
 * <p>{@link Propagation#REQUIRES_NEW} on {@link #logRead} keeps the audit
 * row's commit independent of the surrounding read transaction — if the
 * surrounding read commits, the audit row stays even if a later write in
 * the same outer txn rolls back. The write itself runs read-write so the
 * tenantFilter inherited from {@code BaseEntity} attaches the active
 * instance id automatically.
 *
 * @since 2.18.1 (Wave 18b2 — GAP-321b Phase 1B foundation)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParentReadAuditLogServiceImpl implements ParentReadAuditLogService {

    private final ParentReadAuditLogRepository repository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logRead(Long parentId, Long childId, ParentFacet facet) {
        try {
            ParentReadAuditLog entry = ParentReadAuditLog.builder()
                    .parentId(parentId)
                    .childId(childId)
                    .facet(facet)
                    .readAt(LocalDateTime.now())
                    .build();
            repository.save(entry);
        } catch (RuntimeException ex) {
            // Best-effort: never let an audit-store hiccup propagate up to
            // the parent's facet read. Errors here are operational, not
            // user-visible.
            log.warn("ParentReadAuditLog write failed (parent={}, child={}, facet={}): {}",
                    parentId, childId, facet, ex.getMessage());
        }
    }
}
