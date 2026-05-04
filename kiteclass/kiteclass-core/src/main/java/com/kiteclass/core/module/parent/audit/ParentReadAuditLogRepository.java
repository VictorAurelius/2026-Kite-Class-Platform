package com.kiteclass.core.module.parent.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data-access for {@link ParentReadAuditLog}.
 *
 * <p>Phase 1B foundation only carries the write path — admin/safeguarding
 * read queries (paged listing, range filters, IP filtering) are deferred to
 * GAP-321b.4. The {@code findAll}/{@code save} operations inherited from
 * {@link JpaRepository} are sufficient for v1 unit + integration tests.
 *
 * @since 2.18.1 (Wave 18b2 — GAP-321b Phase 1B foundation)
 */
@Repository
public interface ParentReadAuditLogRepository extends JpaRepository<ParentReadAuditLog, Long> {

    /**
     * Returns every audit row recorded for a (parent, child) pair, oldest
     * first. Used by tests + future safeguarding views; the deleted flag is
     * NOT filtered here because audit rows are never soft-deleted in normal
     * operation (legal-hold path may need them all).
     */
    List<ParentReadAuditLog> findByParentIdAndChildIdOrderByReadAtAsc(Long parentId, Long childId);
}
