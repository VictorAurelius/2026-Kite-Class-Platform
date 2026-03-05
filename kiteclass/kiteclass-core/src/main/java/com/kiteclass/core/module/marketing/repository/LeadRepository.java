package com.kiteclass.core.module.marketing.repository;

import com.kiteclass.core.module.marketing.entity.Lead;
import com.kiteclass.core.module.marketing.enums.LeadStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Lead entity.
 * Business Rule: BR-MKT-002 - Lead email must be unique per tenant.
 *
 * @since 2.10
 */
@Repository
public interface LeadRepository extends JpaRepository<Lead, Long> {

    /**
     * Find lead by email within tenant.
     *
     * @param email      lead email
     * @param instanceId tenant instance ID
     * @return lead if exists
     */
    @Query("SELECT l FROM Lead l WHERE l.email = :email AND l.instanceId = :instanceId AND l.deleted = false")
    Optional<Lead> findByEmailAndInstanceIdAndDeletedFalse(@Param("email") String email,
                                                             @Param("instanceId") UUID instanceId);

    /**
     * Find all leads by status with pagination.
     *
     * @param instanceId tenant instance ID
     * @param status     lead status
     * @param pageable   pagination parameters
     * @return page of leads
     */
    @Query("SELECT l FROM Lead l WHERE l.instanceId = :instanceId AND l.status = :status AND l.deleted = false")
    Page<Lead> findByInstanceIdAndStatusAndDeletedFalse(@Param("instanceId") UUID instanceId,
                                                          @Param("status") LeadStatus status,
                                                          Pageable pageable);

    /**
     * Find all leads for tenant with pagination.
     *
     * @param instanceId tenant instance ID
     * @param pageable   pagination parameters
     * @return page of leads
     */
    @Query("SELECT l FROM Lead l WHERE l.instanceId = :instanceId AND l.deleted = false")
    Page<Lead> findByInstanceIdAndDeletedFalse(@Param("instanceId") UUID instanceId, Pageable pageable);

    /**
     * Find lead by ID (bypass Hibernate filter issue).
     *
     * @param id         lead ID
     * @param instanceId tenant instance ID
     * @return lead if exists
     */
    @Query("SELECT l FROM Lead l WHERE l.id = :id AND l.instanceId = :instanceId AND l.deleted = false")
    Optional<Lead> findByIdAndInstanceIdAndDeletedFalse(@Param("id") Long id, @Param("instanceId") UUID instanceId);
}
