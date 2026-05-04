package com.kiteclass.core.module.childprotection.repository;

import com.kiteclass.core.module.childprotection.entity.Incident;
import com.kiteclass.core.module.childprotection.enums.IncidentCategory;
import com.kiteclass.core.module.childprotection.enums.IncidentSeverity;
import com.kiteclass.core.module.childprotection.enums.IncidentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for {@link Incident} — child-protection ticket persistence.
 *
 * <p>All queries assume tenant filter is active (BaseEntity-level filter
 * "tenantFilter" enabled by TenantFilterInterceptor for API requests).
 *
 * <p>Encrypted columns (description, evidence_paths) are decrypted by
 * {@code AesGcmAttributeConverter} on read — repository methods return
 * plaintext to callers (which then enforce RBAC at the service layer in
 * Phase 1B; Phase 1A leaves this to integration tests).
 *
 * @since 5.x (Wave 18b1 Bucket E — GAP-322 Phase 1A)
 */
@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {

    /**
     * Find non-deleted incident by ID.
     */
    Optional<Incident> findByIdAndDeletedFalse(Long id);

    /**
     * Page of non-deleted incidents, optionally filtered by severity / category
     * / status. Phase 1A read-only listing.
     */
    @Query(
            "SELECT i FROM Incident i WHERE i.deleted = false "
                    + "AND (:severity IS NULL OR i.severity = :severity) "
                    + "AND (:category IS NULL OR i.category = :category) "
                    + "AND (:status IS NULL OR i.status = :status)"
    )
    Page<Incident> findByFilters(
            @Param("severity") IncidentSeverity severity,
            @Param("category") IncidentCategory category,
            @Param("status") IncidentStatus status,
            Pageable pageable
    );
}
