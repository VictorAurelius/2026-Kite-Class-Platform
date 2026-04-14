package com.kiteclass.core.module.branding.repository;

import com.kiteclass.core.module.branding.entity.BrandingResource;
import com.kiteclass.core.module.branding.entity.ResourceCategory;
import com.kiteclass.core.module.branding.entity.ResourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @since 3.16.0 (GAP-007)
 */
@Repository
public interface BrandingResourceRepository extends JpaRepository<BrandingResource, Long> {

    List<BrandingResource> findByTypeAndDeletedFalse(ResourceType type);

    Optional<BrandingResource> findFirstByTypeAndCategoryAndDeletedFalse(
            ResourceType type, ResourceCategory category);

    List<BrandingResource> findByCategoryAndDeletedFalse(ResourceCategory category);
}
