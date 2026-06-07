package com.kitehub.subscription.audit;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Repository for {@link AdminAuditLog} (GAP-521).
 *
 * @since 1.0.0 (Wave 72a)
 */
@Repository
public interface AdminAuditLogRepository
        extends JpaRepository<AdminAuditLog, Long>, JpaSpecificationExecutor<AdminAuditLog> {

    Page<AdminAuditLog> findByAdminUserIdOrderByCreatedAtDesc(UUID adminUserId, Pageable pageable);

    Page<AdminAuditLog> findByActionOrderByCreatedAtDesc(String action, Pageable pageable);

    /**
     * Filtered audit-log search backing the admin read API (GAP-774).
     *
     * <p>Every filter is optional (null = no constraint on that dimension).
     * Results ordered {@code created_at DESC} (newest first) for the admin
     * audit-log viewer. Backs {@code GET /api/v1/admin/audit-logs}.</p>
     *
     * <p>GAP-1028: previously a single JPQL with {@code (:param IS NULL OR ...)}
     * for every filter. On a default (all-null) load Postgres could not infer the
     * bind type of the null {@code LocalDateTime} params ({@code :from}/{@code :to})
     * and returned {@code ERROR: could not determine data type of parameter $5} →
     * HTTP 500. (Testcontainers happened to type-infer it, hiding the bug in IT.)
     * Rebuilt as a dynamic {@link Specification} that only adds a predicate for the
     * non-null filters — no null parameter is ever bound, so the error cannot occur
     * regardless of Postgres version / sort / count-query restructuring.</p>
     *
     * @param action      exact action match (e.g. {@code BETA_REQUEST_APPROVE}); null = any
     * @param adminUserId admin who performed the action; null = any
     * @param from        inclusive lower bound on {@code created_at}; null = unbounded
     * @param to          inclusive upper bound on {@code created_at}; null = unbounded
     * @param pageable    pagination (sort forced to created_at DESC)
     * @return page of matching audit-log rows, newest first
     */
    default Page<AdminAuditLog> search(
            String action,
            UUID adminUserId,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable) {

        Specification<AdminAuditLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (action != null) {
                predicates.add(cb.equal(root.get("action"), action));
            }
            if (adminUserId != null) {
                predicates.add(cb.equal(root.get("adminUserId"), adminUserId));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };

        Pageable sorted = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        return findAll(spec, sorted);
    }
}
