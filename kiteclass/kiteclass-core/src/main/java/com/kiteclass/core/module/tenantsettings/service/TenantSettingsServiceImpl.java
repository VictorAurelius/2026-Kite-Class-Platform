package com.kiteclass.core.module.tenantsettings.service;

import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.module.tenantsettings.dto.request.UpdateTenantSettingsRequest;
import com.kiteclass.core.module.tenantsettings.dto.response.TenantSettingsResponse;
import com.kiteclass.core.module.tenantsettings.entity.SchoolType;
import com.kiteclass.core.module.tenantsettings.entity.TenantSettings;
import com.kiteclass.core.module.tenantsettings.mapper.TenantSettingsMapper;
import com.kiteclass.core.module.tenantsettings.repository.TenantSettingsRepository;
import com.kiteclass.core.module.tenantsettings.util.AcademicYearCalculator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;

/**
 * Default implementation of {@link TenantSettingsService}.
 *
 * @since Wave provisioning-1 (GAP-947)
 */
@Service
@Validated
@RequiredArgsConstructor
@Slf4j
public class TenantSettingsServiceImpl implements TenantSettingsService {

    private final TenantSettingsRepository tenantSettingsRepository;
    private final TenantSettingsMapper tenantSettingsMapper;

    @Override
    @Transactional
    public TenantSettingsResponse getSettings() {
        UUID instanceId = TenantContext.getCurrentTenant();

        TenantSettings settings = tenantSettingsRepository
                .findByInstanceIdAndDeletedFalse(instanceId)
                .orElseGet(() -> tenantSettingsRepository.save(createDefault(instanceId)));

        return tenantSettingsMapper.toResponse(settings);
    }

    @Override
    @Transactional
    public TenantSettingsResponse updateSettings(@Valid UpdateTenantSettingsRequest request) {
        UUID instanceId = TenantContext.getCurrentTenant();

        TenantSettings settings = tenantSettingsRepository
                .findByInstanceIdAndDeletedFalse(instanceId)
                .orElseGet(() -> tenantSettingsRepository.save(createDefault(instanceId)));

        // Provided-field-wins merge (null fields ignored per mapper NullValuePropertyMappingStrategy.IGNORE)
        tenantSettingsMapper.updateFromRequest(request, settings);

        settings = tenantSettingsRepository.save(settings);

        log.info("Updated tenant settings for instance {}", instanceId);

        return tenantSettingsMapper.toResponse(settings);
    }

    /**
     * Build a default settings row for a tenant with Năm học auto-filled.
     *
     * @param instanceId tenant instance ID
     * @return new (un-persisted) default settings
     */
    private TenantSettings createDefault(UUID instanceId) {
        TenantSettings settings = TenantSettings.builder()
                .timezone(TenantSettings.DEFAULT_TIMEZONE)
                .locale(TenantSettings.DEFAULT_LOCALE)
                .academicYear(AcademicYearCalculator.currentAcademicYear())
                .schoolType(SchoolType.CENTER)
                .build();
        settings.setInstanceId(instanceId);
        return settings;
    }
}
