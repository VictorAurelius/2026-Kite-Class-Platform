package com.kiteclass.core.module.childprotection.repository;

import com.kiteclass.core.module.childprotection.entity.Vetting;
import com.kiteclass.core.module.childprotection.enums.VettingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
public interface VettingRepository extends JpaRepository<Vetting, Long> {

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
     */
    @Query(
            "SELECT v FROM Vetting v WHERE v.deleted = false "
                    + "AND (:status IS NULL OR v.status = :status)"
    )
    Page<Vetting> findByFilters(
            @Param("status") VettingStatus status,
            Pageable pageable
    );
}
