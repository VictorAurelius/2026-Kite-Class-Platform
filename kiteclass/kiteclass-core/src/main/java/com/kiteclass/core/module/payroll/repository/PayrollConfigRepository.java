package com.kiteclass.core.module.payroll.repository;

import com.kiteclass.core.module.payroll.entity.PayrollConfig;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for {@link PayrollConfig}.
 *
 * <p>All queries assume tenant filter is active (BaseEntity-level filter
 * "tenantFilter" enabled by TenantFilterInterceptor for API requests).
 *
 * @author KiteClass Team
 * @since 4.x (Wave 18a Bucket C)
 */
@Repository
public interface PayrollConfigRepository extends JpaRepository<PayrollConfig, Long> {

    /**
     * Find non-deleted config by teacher ID (one config per teacher per tenant —
     * BR-PAYROLL-001).
     *
     * @param teacherId teacher FK
     * @return Optional with config if present and not deleted
     */
    Optional<PayrollConfig> findByTeacherIdAndDeletedFalse(Long teacherId);

    /**
     * Page of all non-deleted configs (admin list view).
     *
     * @param pageable pagination + sorting
     * @return page of configs
     */
    Page<PayrollConfig> findAllByDeletedFalse(Pageable pageable);
}
