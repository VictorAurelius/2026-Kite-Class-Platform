package com.kiteclass.core.module.tenantsettings.service;

import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.module.tenantsettings.dto.request.UpdateTenantSettingsRequest;
import com.kiteclass.core.module.tenantsettings.dto.response.TenantSettingsResponse;
import com.kiteclass.core.module.tenantsettings.entity.SchoolType;
import com.kiteclass.core.module.tenantsettings.entity.TenantSettings;
import com.kiteclass.core.module.tenantsettings.mapper.TenantSettingsMapper;
import com.kiteclass.core.module.tenantsettings.repository.TenantSettingsRepository;
import com.kiteclass.core.module.tenantsettings.util.AcademicYearCalculator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TenantSettingsServiceImpl}.
 *
 * @since Wave provisioning-1 (GAP-947)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TenantSettingsService Tests")
class TenantSettingsServiceTest {

    @Mock
    private TenantSettingsRepository tenantSettingsRepository;

    @Mock
    private TenantSettingsMapper tenantSettingsMapper;

    @InjectMocks
    private TenantSettingsServiceImpl service;

    private UUID instanceId;

    @BeforeEach
    void setUp() {
        instanceId = UUID.randomUUID();
        TenantContext.setCurrentTenant(instanceId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("getSettings returns existing row without creating a new one")
    void getSettings_existing() {
        TenantSettings existing = TenantSettings.builder()
                .timezone("Asia/Ho_Chi_Minh").locale("vi")
                .academicYear("2026-2027").schoolType(SchoolType.CENTER)
                .build();
        existing.setInstanceId(instanceId);
        TenantSettingsResponse response = TenantSettingsResponse.builder()
                .academicYear("2026-2027").locale("vi").timezone("Asia/Ho_Chi_Minh")
                .schoolType("CENTER").build();

        when(tenantSettingsRepository.findByInstanceIdAndDeletedFalse(instanceId))
                .thenReturn(Optional.of(existing));
        when(tenantSettingsMapper.toResponse(existing)).thenReturn(response);

        TenantSettingsResponse result = service.getSettings();

        assertThat(result.getAcademicYear()).isEqualTo("2026-2027");
        verify(tenantSettingsRepository, never()).save(any(TenantSettings.class));
    }

    @Test
    @DisplayName("getSettings auto-creates default with Năm học auto-filled when absent")
    void getSettings_autoCreatesDefault() {
        when(tenantSettingsRepository.findByInstanceIdAndDeletedFalse(instanceId))
                .thenReturn(Optional.empty());
        when(tenantSettingsRepository.save(any(TenantSettings.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(tenantSettingsMapper.toResponse(any(TenantSettings.class)))
                .thenReturn(TenantSettingsResponse.builder().build());

        service.getSettings();

        ArgumentCaptor<TenantSettings> captor = ArgumentCaptor.forClass(TenantSettings.class);
        verify(tenantSettingsRepository).save(captor.capture());
        TenantSettings saved = captor.getValue();
        assertThat(saved.getInstanceId()).isEqualTo(instanceId);
        assertThat(saved.getTimezone()).isEqualTo(TenantSettings.DEFAULT_TIMEZONE);
        assertThat(saved.getLocale()).isEqualTo(TenantSettings.DEFAULT_LOCALE);
        assertThat(saved.getSchoolType()).isEqualTo(SchoolType.CENTER);
        assertThat(saved.getAcademicYear()).isEqualTo(AcademicYearCalculator.currentAcademicYear());
    }

    @Test
    @DisplayName("updateSettings merges request into existing row (provided-field-wins)")
    void updateSettings_existing() {
        TenantSettings existing = TenantSettings.builder()
                .timezone("Asia/Ho_Chi_Minh").locale("vi")
                .academicYear("2026-2027").schoolType(SchoolType.CENTER)
                .build();
        existing.setInstanceId(instanceId);
        UpdateTenantSettingsRequest request = UpdateTenantSettingsRequest.builder()
                .schoolType("K12").address("123 Đường Láng, Hà Nội").build();

        when(tenantSettingsRepository.findByInstanceIdAndDeletedFalse(instanceId))
                .thenReturn(Optional.of(existing));
        when(tenantSettingsRepository.save(existing)).thenReturn(existing);
        when(tenantSettingsMapper.toResponse(existing))
                .thenReturn(TenantSettingsResponse.builder().schoolType("K12").build());

        TenantSettingsResponse result = service.updateSettings(request);

        assertThat(result.getSchoolType()).isEqualTo("K12");
        verify(tenantSettingsMapper).updateFromRequest(request, existing);
        verify(tenantSettingsRepository).save(existing);
    }

    @Test
    @DisplayName("updateSettings creates default first when no row exists, then applies request")
    void updateSettings_createsDefaultThenApplies() {
        UpdateTenantSettingsRequest request = UpdateTenantSettingsRequest.builder()
                .locale("en").build();

        when(tenantSettingsRepository.findByInstanceIdAndDeletedFalse(instanceId))
                .thenReturn(Optional.empty());
        when(tenantSettingsRepository.save(any(TenantSettings.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(tenantSettingsMapper.toResponse(any(TenantSettings.class)))
                .thenReturn(TenantSettingsResponse.builder().locale("en").build());

        TenantSettingsResponse result = service.updateSettings(request);

        assertThat(result.getLocale()).isEqualTo("en");
        // save called once for default-create + once for the merged update
        verify(tenantSettingsRepository, org.mockito.Mockito.times(2)).save(any(TenantSettings.class));
        verify(tenantSettingsMapper).updateFromRequest(any(UpdateTenantSettingsRequest.class), any(TenantSettings.class));
    }
}
