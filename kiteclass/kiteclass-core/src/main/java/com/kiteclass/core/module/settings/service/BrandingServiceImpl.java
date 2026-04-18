package com.kiteclass.core.module.settings.service;

import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.module.branding.events.BrandingEventPublisher;
import com.kiteclass.core.module.branding.events.BrandingUpdatedEvent;
import com.kiteclass.core.module.settings.dto.request.UpdateBrandingRequest;
import com.kiteclass.core.module.settings.dto.response.BrandingResponse;
import com.kiteclass.core.module.settings.entity.Branding;
import com.kiteclass.core.module.settings.mapper.BrandingMapper;
import com.kiteclass.core.module.settings.repository.BrandingRepository;
import com.kiteclass.core.module.settings.versioning.BrandingVersionService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;
import java.util.UUID;

/**
 * Service implementation for Branding management.
 *
 * @since 2.9
 */
@Service
@Validated
@Slf4j
public class BrandingServiceImpl implements BrandingService {

    private final BrandingRepository brandingRepository;
    private final BrandingMapper brandingMapper;
    private final BrandingEventPublisher brandingEventPublisher;
    private final BrandingVersionService brandingVersionService;

    public BrandingServiceImpl(
            BrandingRepository brandingRepository,
            BrandingMapper brandingMapper,
            @Autowired(required = false) BrandingEventPublisher brandingEventPublisher,
            @Autowired(required = false) BrandingVersionService brandingVersionService) {
        this.brandingRepository = brandingRepository;
        this.brandingMapper = brandingMapper;
        this.brandingEventPublisher = brandingEventPublisher;
        this.brandingVersionService = brandingVersionService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public BrandingResponse getBranding() {
        UUID instanceId = TenantContext.getCurrentTenant();

        Branding branding = brandingRepository.findByInstanceIdAndDeletedFalse(instanceId)
                .orElseGet(() -> createDefaultBranding(instanceId));

        return brandingMapper.toResponse(branding);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public BrandingResponse updateBranding(@Valid UpdateBrandingRequest request) {
        UUID instanceId = TenantContext.getCurrentTenant();

        Branding branding = brandingRepository.findByInstanceIdAndDeletedFalse(instanceId)
                .orElseGet(() -> {
                    Branding newBranding = new Branding();
                    newBranding.setInstanceId(instanceId);
                    newBranding.setDisplayName(request.getDisplayName());
                    newBranding.setPrimaryColor("#3B82F6");
                    newBranding.setSecondaryColor("#8B5CF6");
                    newBranding.setAccentColor("#10B981");
                    return brandingRepository.save(newBranding);
                });

        // Update fields from request (PATCH semantics)
        brandingMapper.updateFromRequest(request, branding);

        branding = brandingRepository.save(branding);

        // Wave 4 (GAP-033p): snapshot the new state into version history.
        if (brandingVersionService != null) {
            brandingVersionService.snapshot(branding, /*rollbackOf*/ null);
        }

        // Wave 4 (GAP-021): publish branding.updated so downstream caches evict.
        if (brandingEventPublisher != null) {
            brandingEventPublisher.publishUpdated(new BrandingUpdatedEvent(
                    branding.getId(),
                    instanceId.toString(),
                    branding.getVersion() == null ? 0 : branding.getVersion().intValue(),
                    Instant.now()));
        }

        log.info("Updated branding for instance {}", instanceId);

        return brandingMapper.toResponse(branding);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public BrandingResponse uploadLogo(String fileUrl) {
        UUID instanceId = TenantContext.getCurrentTenant();

        Branding branding = brandingRepository.findByInstanceIdAndDeletedFalse(instanceId)
                .orElseGet(() -> createDefaultBranding(instanceId));

        branding.setLogoUrl(fileUrl);
        branding = brandingRepository.save(branding);

        log.info("Uploaded logo for instance {}: {}", instanceId, fileUrl);

        return brandingMapper.toResponse(branding);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public BrandingResponse uploadFavicon(String fileUrl) {
        UUID instanceId = TenantContext.getCurrentTenant();

        Branding branding = brandingRepository.findByInstanceIdAndDeletedFalse(instanceId)
                .orElseGet(() -> createDefaultBranding(instanceId));

        branding.setFaviconUrl(fileUrl);
        branding = brandingRepository.save(branding);

        log.info("Uploaded favicon for instance {}: {}", instanceId, fileUrl);

        return brandingMapper.toResponse(branding);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public String getThemeConfig() {
        UUID instanceId = TenantContext.getCurrentTenant();

        return brandingRepository.findByInstanceIdAndDeletedFalse(instanceId)
                .map(Branding::getThemeConfigJson)
                .orElse(null);
    }

    /**
     * Create default branding for tenant.
     *
     * @param instanceId tenant instance ID
     * @return default branding (not persisted)
     */
    private Branding createDefaultBranding(UUID instanceId) {
        Branding branding = new Branding();
        branding.setInstanceId(instanceId);
        branding.setDisplayName("KiteClass");
        branding.setTagline("Nền tảng quản lý trung tâm đào tạo");
        branding.setPrimaryColor("#3B82F6");
        branding.setSecondaryColor("#8B5CF6");
        branding.setAccentColor("#10B981");
        return branding;
    }
}
