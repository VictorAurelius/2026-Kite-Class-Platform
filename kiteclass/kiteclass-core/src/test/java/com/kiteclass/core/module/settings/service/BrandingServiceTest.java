package com.kiteclass.core.module.settings.service;

import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.module.settings.dto.request.UpdateBrandingRequest;
import com.kiteclass.core.module.settings.dto.response.BrandingResponse;
import com.kiteclass.core.module.settings.entity.Branding;
import com.kiteclass.core.module.settings.mapper.BrandingMapper;
import com.kiteclass.core.module.settings.repository.BrandingRepository;
import com.kiteclass.core.testutil.BrandingTestDataBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for BrandingService.
 *
 * @since 2.9
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BrandingService Tests")
class BrandingServiceTest {

    @Mock
    private BrandingRepository brandingRepository;

    @Mock
    private BrandingMapper brandingMapper;

    @InjectMocks
    private BrandingServiceImpl brandingService;

    private UUID testInstanceId;

    @BeforeEach
    void setUp() {
        testInstanceId = UUID.randomUUID();
        TenantContext.setCurrentTenant(testInstanceId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Should get existing branding")
    void shouldGetExistingBranding() {
        // Given
        Branding branding = BrandingTestDataBuilder.createDefaultBranding(testInstanceId);
        BrandingResponse expectedResponse = BrandingResponse.builder()
                .id(1L)
                .displayName("Test Center")
                .build();

        when(brandingRepository.findByInstanceIdAndDeletedFalse(testInstanceId))
                .thenReturn(Optional.of(branding));
        when(brandingMapper.toResponse(branding)).thenReturn(expectedResponse);

        // When
        BrandingResponse result = brandingService.getBranding();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDisplayName()).isEqualTo("Test Center");
        verify(brandingRepository).findByInstanceIdAndDeletedFalse(testInstanceId);
        verify(brandingMapper).toResponse(branding);
    }

    @Test
    @DisplayName("Should return default branding when not exists")
    void shouldReturnDefaultBrandingWhenNotExists() {
        // Given
        BrandingResponse expectedResponse = BrandingResponse.builder()
                .displayName("KiteClass")
                .build();

        when(brandingRepository.findByInstanceIdAndDeletedFalse(testInstanceId))
                .thenReturn(Optional.empty());
        when(brandingMapper.toResponse(any(Branding.class))).thenReturn(expectedResponse);

        // When
        BrandingResponse result = brandingService.getBranding();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDisplayName()).isEqualTo("KiteClass");
        verify(brandingRepository).findByInstanceIdAndDeletedFalse(testInstanceId);
    }

    @Test
    @DisplayName("Should update existing branding")
    void shouldUpdateExistingBranding() {
        // Given
        Branding existingBranding = BrandingTestDataBuilder.createDefaultBranding(testInstanceId);
        UpdateBrandingRequest request = UpdateBrandingRequest.builder()
                .displayName("Updated Center")
                .primaryColor("#FF0000")
                .secondaryColor("#00FF00")
                .accentColor("#0000FF")
                .build();

        BrandingResponse expectedResponse = BrandingResponse.builder()
                .id(1L)
                .displayName("Updated Center")
                .build();

        when(brandingRepository.findByInstanceIdAndDeletedFalse(testInstanceId))
                .thenReturn(Optional.of(existingBranding));
        when(brandingRepository.save(any(Branding.class))).thenReturn(existingBranding);
        when(brandingMapper.toResponse(existingBranding)).thenReturn(expectedResponse);

        // When
        BrandingResponse result = brandingService.updateBranding(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDisplayName()).isEqualTo("Updated Center");
        verify(brandingMapper).updateFromRequest(request, existingBranding);
        verify(brandingRepository).save(existingBranding);
    }

    @Test
    @DisplayName("Should create new branding when updating non-existent")
    void shouldCreateNewBrandingWhenUpdatingNonExistent() {
        // Given
        UpdateBrandingRequest request = UpdateBrandingRequest.builder()
                .displayName("New Center")
                .primaryColor("#3B82F6")
                .secondaryColor("#8B5CF6")
                .accentColor("#10B981")
                .build();

        Branding savedBranding = new Branding();
        savedBranding.setDisplayName("New Center");

        BrandingResponse expectedResponse = BrandingResponse.builder()
                .displayName("New Center")
                .build();

        when(brandingRepository.findByInstanceIdAndDeletedFalse(testInstanceId))
                .thenReturn(Optional.empty());
        when(brandingRepository.save(any(Branding.class))).thenReturn(savedBranding);
        when(brandingMapper.toResponse(savedBranding)).thenReturn(expectedResponse);

        // When
        BrandingResponse result = brandingService.updateBranding(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDisplayName()).isEqualTo("New Center");
        verify(brandingRepository, times(2)).save(any(Branding.class));
    }

    @Test
    @DisplayName("Should upload logo")
    void shouldUploadLogo() {
        // Given
        String logoUrl = "https://s3.amazonaws.com/bucket/logo.png";
        Branding branding = BrandingTestDataBuilder.createDefaultBranding(testInstanceId);

        BrandingResponse expectedResponse = BrandingResponse.builder()
                .logoUrl(logoUrl)
                .build();

        when(brandingRepository.findByInstanceIdAndDeletedFalse(testInstanceId))
                .thenReturn(Optional.of(branding));
        when(brandingRepository.save(branding)).thenReturn(branding);
        when(brandingMapper.toResponse(branding)).thenReturn(expectedResponse);

        // When
        BrandingResponse result = brandingService.uploadLogo(logoUrl);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getLogoUrl()).isEqualTo(logoUrl);
        verify(brandingRepository).save(branding);
    }

    @Test
    @DisplayName("Should upload favicon")
    void shouldUploadFavicon() {
        // Given
        String faviconUrl = "https://s3.amazonaws.com/bucket/favicon.ico";
        Branding branding = BrandingTestDataBuilder.createDefaultBranding(testInstanceId);

        BrandingResponse expectedResponse = BrandingResponse.builder()
                .faviconUrl(faviconUrl)
                .build();

        when(brandingRepository.findByInstanceIdAndDeletedFalse(testInstanceId))
                .thenReturn(Optional.of(branding));
        when(brandingRepository.save(branding)).thenReturn(branding);
        when(brandingMapper.toResponse(branding)).thenReturn(expectedResponse);

        // When
        BrandingResponse result = brandingService.uploadFavicon(faviconUrl);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFaviconUrl()).isEqualTo(faviconUrl);
        verify(brandingRepository).save(branding);
    }
}
