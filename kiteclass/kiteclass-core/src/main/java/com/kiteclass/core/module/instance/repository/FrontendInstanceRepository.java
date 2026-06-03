package com.kiteclass.core.module.instance.repository;

import com.kiteclass.core.module.instance.entity.FrontendInstance;
import com.kiteclass.core.module.instance.entity.FrontendInstanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @since 3.15.0 (GAP-009)
 */
@Repository
public interface FrontendInstanceRepository extends JpaRepository<FrontendInstance, Long> {

    Optional<FrontendInstance> findBySlugAndDeletedFalse(String slug);

    Optional<FrontendInstance> findByTenantSlugAndDeletedFalse(String tenantSlug);

    List<FrontendInstance> findByStatusAndDeletedFalse(FrontendInstanceStatus status);

    boolean existsBySlugAndDeletedFalse(String slug);
}
