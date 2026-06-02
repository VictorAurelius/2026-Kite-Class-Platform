package com.kitehub.subscription.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Repository for {@link AdminAuditLog} (GAP-521).
 *
 * @since 1.0.0 (Wave 72a)
 */
@Repository
public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {

    Page<AdminAuditLog> findByAdminUserIdOrderByCreatedAtDesc(UUID adminUserId, Pageable pageable);

    Page<AdminAuditLog> findByActionOrderByCreatedAtDesc(String action, Pageable pageable);

    /**
     * Filtered audit-log search backing the admin read API (GAP-774).
     *
     * <p>Every filter is optional (null = no constraint on that dimension).
     * Results ordered {@code created_at DESC} (newest first) for the admin
     * audit-log viewer. Backs {@code GET /api/v1/admin/audit-logs}.</p>
     *
     * @param action      exact action match (e.g. {@code BETA_REQUEST_APPROVE}); null = any
     * @param adminUserId admin who performed the action; null = any
     * @param from        inclusive lower bound on {@code created_at}; null = unbounded
     * @param to          inclusive upper bound on {@code created_at}; null = unbounded
     * @param pageable    pagination (sort fixed to created_at DESC by the query)
     * @return page of matching audit-log rows, newest first
     */
    @Query("""
            SELECT a FROM AdminAuditLog a
            WHERE (:action IS NULL OR a.action = :action)
              AND (:adminUserId IS NULL OR a.adminUserId = :adminUserId)
              AND (:from IS NULL OR a.createdAt >= :from)
              AND (:to IS NULL OR a.createdAt <= :to)
            ORDER BY a.createdAt DESC
            """)
    Page<AdminAuditLog> search(
            @Param("action") String action,
            @Param("adminUserId") UUID adminUserId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);
}
