package com.kiteclass.core.module.childprotection.repository;

import com.kiteclass.core.module.childprotection.entity.Vetting;
import com.kiteclass.core.module.childprotection.enums.VettingStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Vetting} — staff vetting record persistence.
 *
 * <p>Tenant filter applied via {@code BaseEntity}'s Hibernate filter
 * {@code tenantFilter} — queries scoped per tenant transparently.
 *
 * <p>Encrypted columns ({@code lltp_number}, {@code police_check_details})
 * are decrypted by {@code AesGcmAttributeConverter} on read.
 *
 * @since Wave 18b2 Bucket B — GAP-322b Phase 1B foundation
 */
@Repository
public interface VettingRepository extends JpaRepository<Vetting, Long>, JpaSpecificationExecutor<Vetting> {

    /**
     * Find non-deleted vetting record by ID.
     */
    Optional<Vetting> findByIdAndDeletedFalse(Long id);

    /**
     * Find the active (non-deleted) vetting record for a teacher.
     * Phase 1B foundation assumes one record per teacher; future iterations
     * may track historical vetting cycles by adding a "current" flag.
     */
    Optional<Vetting> findFirstByTeacherIdAndDeletedFalseOrderByIdDesc(Long teacherId);

    /**
     * Page of non-deleted vetting records, optionally filtered by status.
     *
     * <p><b>42P18 note (GAP-1109):</b> the previous JPQL form
     * {@code (:status IS NULL OR v.status = :status)} bound an UNTYPED null in the
     * {@code IS NULL} position, which PostgreSQL rejects at PREPARE time with
     * {@code 42P18 could not determine data type of parameter} (H2 hides this).
     * This is now built with the Criteria API: the status predicate is only added
     * when the parameter is non-null, so no untyped-null bind is ever emitted. The
     * Hibernate {@code tenantFilter} still applies (Criteria → JPQL-equivalent),
     * unlike a native-SQL rewrite which would silently drop tenant isolation.
     */
    default Page<Vetting> findByFilters(
            VettingStatus status,
            Pageable pageable
    ) {
        Specification<Vetting> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleted"), false));
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return findAll(spec, pageable);
    }
}
