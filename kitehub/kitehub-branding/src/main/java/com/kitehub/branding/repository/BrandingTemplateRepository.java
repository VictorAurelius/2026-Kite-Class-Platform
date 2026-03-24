package com.kitehub.branding.repository;

import com.kitehub.branding.domain.entity.BrandingTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for BrandingTemplate entity.
 *
 * @since 1.0
 */
@Repository
public interface BrandingTemplateRepository extends JpaRepository<BrandingTemplate, UUID> {

    /**
     * Find all active templates ordered by name.
     *
     * @return list of active templates
     */
    List<BrandingTemplate> findByActiveTrueOrderByNameAsc();

    /**
     * Find active templates by category.
     *
     * @param category template category
     * @return list of active templates in category
     */
    List<BrandingTemplate> findByCategoryAndActiveTrue(String category);
}
