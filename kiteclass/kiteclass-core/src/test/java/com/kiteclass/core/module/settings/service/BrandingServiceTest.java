package com.kiteclass.core.module.settings.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.module.branding.entity.ResourceType;
import com.kiteclass.core.module.marketing.config.LandingPageSafetyProperties;
import com.kiteclass.core.module.marketing.service.LandingPageContentSanitizer;
import com.kiteclass.core.module.marketing.service.impl.LandingPageContentSanitizerImpl;
import com.kiteclass.core.module.settings.dto.request.UpdateBrandingRequest;
import com.kiteclass.core.module.settings.dto.response.BrandingResponse;
import com.kiteclass.core.module.settings.entity.Branding;
import com.kiteclass.core.module.settings.mapper.BrandingMapper;
import com.kiteclass.core.module.settings.repository.BrandingRepository;
import com.kiteclass.core.module.settings.storage.BrandingAssetStorage;
import com.kiteclass.core.testutil.BrandingTestDataBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    @Mock
    private BrandingAssetStorage brandingAssetStorage;

    private BrandingServiceImpl brandingService;

    private UUID testInstanceId;

    @BeforeEach
    void setUp() {
        testInstanceId = UUID.randomUUID();
        TenantContext.setCurrentTenant(testInstanceId);
        // Real content sanitizer (GAP-829) — no DB needed; only ObjectMapper +
        // properties. Identity for clean text (existing tests unaffected) but lets
        // shouldSanitizeFreeTextAndPreserveVnDiacritics verify the write-path wiring.
        LandingPageContentSanitizer contentSanitizer =
                new LandingPageContentSanitizerImpl(new ObjectMapper(), new LandingPageSafetyProperties());
        // Pass nulls for branding event publisher and version service —
        // they're optional and unrelated to the behavior under test.
        brandingService = new BrandingServiceImpl(
                brandingRepository, brandingMapper, null, null, brandingAssetStorage, contentSanitizer);
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
    @DisplayName("Should sanitize free-text on write and preserve Vietnamese diacritics (GAP-829)")
    void shouldSanitizeFreeTextAndPreserveVnDiacritics() {
        // Given — tenant free-text with HTML/script markup + Vietnamese diacritics.
        // brandingMapper.updateFromRequest is mocked (no-op) so these entity values are
        // what the write-path sanitizer (GAP-829) operates on.
        Branding existingBranding = BrandingTestDataBuilder.createDefaultBranding(testInstanceId);
        existingBranding.setDisplayName("Trần Thị Hồng <b>Sky</b>");
        existingBranding.setTagline("Học tiếng Anh <script>alert(1)</script> chất lượng");
        existingBranding.setAddress("123 Lê Lợi <img src=x onerror=alert(1)> Q.1");

        UpdateBrandingRequest request = UpdateBrandingRequest.builder()
                .displayName("Trần Thị Hồng <b>Sky</b>")
                .build();

        when(brandingRepository.findByInstanceIdAndDeletedFalse(testInstanceId))
                .thenReturn(Optional.of(existingBranding));
        when(brandingRepository.save(any(Branding.class))).thenReturn(existingBranding);
        when(brandingMapper.toResponse(existingBranding))
                .thenReturn(BrandingResponse.builder().build());

        // When — sanitize mutates the entity in place before save
        brandingService.updateBranding(request);

        // Then — markup stripped, VN diacritics preserved (NFC)
        assertThat(existingBranding.getDisplayName()).doesNotContain("<", ">");
        assertThat(existingBranding.getDisplayName()).contains("Trần Thị Hồng").contains("Sky");
        assertThat(existingBranding.getTagline()).doesNotContain("<script", "</script");
        assertThat(existingBranding.getTagline()).contains("Học tiếng Anh").contains("chất lượng");
        assertThat(existingBranding.getAddress()).doesNotContain("<img", "onerror");
        assertThat(existingBranding.getAddress()).contains("123 Lê Lợi").contains("Q.1");
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
    @DisplayName("Should upload logo via multipart and store to MinIO")
    void shouldUploadLogo() {
        // Given
        String logoUrl = "https://minio.local/kite-branding-assets/static/t/logo/logo.png?sig=x";
        Branding branding = BrandingTestDataBuilder.createDefaultBranding(testInstanceId);
        MultipartFile file = new MockMultipartFile(
                "logo", "logo.png", "image/png", "fake-png-bytes".getBytes());

        BrandingResponse expectedResponse = BrandingResponse.builder()
                .logoUrl(logoUrl)
                .build();

        when(brandingAssetStorage.store(eq(testInstanceId), eq(ResourceType.LOGO),
                eq("logo.png"), eq("image/png"), any(byte[].class))).thenReturn(logoUrl);
        when(brandingRepository.findByInstanceIdAndDeletedFalse(testInstanceId))
                .thenReturn(Optional.of(branding));
        when(brandingRepository.save(branding)).thenReturn(branding);
        when(brandingMapper.toResponse(branding)).thenReturn(expectedResponse);

        // When
        BrandingResponse result = brandingService.uploadLogo(file);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getLogoUrl()).isEqualTo(logoUrl);
        assertThat(branding.getLogoUrl()).isEqualTo(logoUrl);
        verify(brandingAssetStorage).store(eq(testInstanceId), eq(ResourceType.LOGO),
                eq("logo.png"), eq("image/png"), any(byte[].class));
        verify(brandingRepository).save(branding);
    }

    @Test
    @DisplayName("Should upload favicon via multipart and store to MinIO")
    void shouldUploadFavicon() {
        // Given
        String faviconUrl = "https://minio.local/kite-branding-assets/static/t/favicon/favicon.ico?sig=x";
        Branding branding = BrandingTestDataBuilder.createDefaultBranding(testInstanceId);
        MultipartFile file = new MockMultipartFile(
                "favicon", "favicon.ico", "image/x-icon", "fake-ico-bytes".getBytes());

        BrandingResponse expectedResponse = BrandingResponse.builder()
                .faviconUrl(faviconUrl)
                .build();

        when(brandingAssetStorage.store(eq(testInstanceId), eq(ResourceType.FAVICON),
                eq("favicon.ico"), eq("image/x-icon"), any(byte[].class))).thenReturn(faviconUrl);
        when(brandingRepository.findByInstanceIdAndDeletedFalse(testInstanceId))
                .thenReturn(Optional.of(branding));
        when(brandingRepository.save(branding)).thenReturn(branding);
        when(brandingMapper.toResponse(branding)).thenReturn(expectedResponse);

        // When
        BrandingResponse result = brandingService.uploadFavicon(file);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFaviconUrl()).isEqualTo(faviconUrl);
        assertThat(branding.getFaviconUrl()).isEqualTo(faviconUrl);
        verify(brandingAssetStorage).store(eq(testInstanceId), eq(ResourceType.FAVICON),
                eq("favicon.ico"), eq("image/x-icon"), any(byte[].class));
        verify(brandingRepository).save(branding);
    }

    @Test
    @DisplayName("Should reject upload with unsupported content type")
    void shouldRejectUnsupportedContentType() {
        // Given
        MultipartFile file = new MockMultipartFile(
                "logo", "logo.txt", "text/plain", "not-an-image".getBytes());

        // When & Then — validation fails before any repository/storage call
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> brandingService.uploadLogo(file))
                .isInstanceOf(com.kiteclass.core.common.exception.ValidationException.class);
        verify(brandingAssetStorage, times(0)).store(any(), any(), any(), any(), any());
    }
}
