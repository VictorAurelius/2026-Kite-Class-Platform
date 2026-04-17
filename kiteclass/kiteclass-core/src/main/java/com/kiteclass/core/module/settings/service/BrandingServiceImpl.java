package com.kiteclass.core.module.settings.service;

import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.module.settings.dto.request.UpdateBrandingRequest;
import com.kiteclass.core.module.settings.dto.response.BrandingResponse;
import com.kiteclass.core.module.settings.entity.Branding;
import com.kiteclass.core.module.settings.mapper.BrandingMapper;
import com.kiteclass.core.module.settings.repository.BrandingRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;

/**
 * Service implementation for Branding management.
 *
 * @since 2.9
 */
@Service
@Validated
@RequiredArgsConstructor
@Slf4j
public class BrandingServiceImpl implements BrandingService {

    private final BrandingRepository brandingRepository;
    private final BrandingMapper brandingMapper;

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
