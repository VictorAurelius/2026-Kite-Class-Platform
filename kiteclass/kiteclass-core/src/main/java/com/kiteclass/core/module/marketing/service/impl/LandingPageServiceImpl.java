package com.kiteclass.core.module.marketing.service.impl;

import com.kiteclass.core.module.marketing.dto.request.UpdateLandingPageRequest;
import com.kiteclass.core.module.marketing.dto.response.LandingPageResponse;
import com.kiteclass.core.module.marketing.entity.LandingPage;
import com.kiteclass.core.module.marketing.mapper.LandingPageMapper;
import com.kiteclass.core.module.marketing.repository.LandingPageRepository;
import com.kiteclass.core.module.marketing.service.LandingPageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Implementation of LandingPageService interface.
 *
 * <p>Business Rule: BR-MKT-001 - Each tenant has ONE landing page.
 * Landing pages are auto-created with default values if not exists.
 *
 * @since 2.10
 */
@Slf4j
@Service
@RequiredArgsConstructor
@org.springframework.validation.annotation.Validated
public class LandingPageServiceImpl implements LandingPageService {

    private final LandingPageRepository landingPageRepository;
    private final LandingPageMapper landingPageMapper;

    /**
     * Gets landing page for tenant, creates default if not exists.
     *
     * <p>Implements BR-MKT-001: Each tenant has ONE landing page.
     * Result is cached with key "landingPage::{tenantId}".
     *
     * @param tenantId the tenant ID
     * @return LandingPageResponse with landing page content
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "landingPages", key = "#tenantId")
    public LandingPageResponse getLandingPage(UUID tenantId) {
        log.debug("Fetching landing page for tenant: {}", tenantId);

        LandingPage landingPage = getOrCreateDefault(tenantId);

        return landingPageMapper.toResponse(landingPage);
    }

    /**
     * Updates landing page content for tenant.
     *
     * <p>Creates landing page with defaults if not exists.
     * Only updates non-null fields from request.
     *
     * @param tenantId the tenant ID
     * @param request  the update request
     * @return LandingPageResponse with updated content
     */
    @Override
    @Transactional
    @CacheEvict(value = "landingPages", key = "#tenantId")
    public LandingPageResponse updateLandingPage(UUID tenantId, UpdateLandingPageRequest request) {
        log.info("Updating landing page for tenant: {}", tenantId);

        LandingPage landingPage = getOrCreateDefault(tenantId);

        landingPageMapper.updateEntity(landingPage, request);
        LandingPage updated = landingPageRepository.save(landingPage);

        log.info("Updated landing page for tenant: {}", tenantId);
        return landingPageMapper.toResponse(updated);
    }

    /**
     * Gets existing landing page or creates default for tenant.
     *
     * <p>Private helper method implementing BR-MKT-001.
     * Default values are set in LandingPage entity fields.
     *
     * @param tenantId the tenant ID
     * @return landing page (existing or newly created)
     */
    private LandingPage getOrCreateDefault(UUID tenantId) {
        return landingPageRepository.findByInstanceIdAndDeletedFalse(tenantId)
                .orElseGet(() -> {
                    log.info("Creating default landing page for tenant: {}", tenantId);

                    LandingPage newLandingPage = new LandingPage();
                    newLandingPage.setInstanceId(tenantId);
                    // Default values are already set in entity @Column annotations

                    return landingPageRepository.save(newLandingPage);
                });
    }
}
