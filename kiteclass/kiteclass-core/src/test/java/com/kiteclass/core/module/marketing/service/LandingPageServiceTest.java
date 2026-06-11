package com.kiteclass.core.module.marketing.service;

import com.kiteclass.core.module.marketing.dto.request.UpdateLandingPageRequest;
import com.kiteclass.core.module.marketing.dto.response.LandingPageResponse;
import com.kiteclass.core.module.marketing.entity.LandingPage;
import com.kiteclass.core.module.marketing.mapper.LandingPageMapper;
import com.kiteclass.core.module.marketing.repository.LandingPageRepository;
import com.kiteclass.core.module.marketing.service.impl.LandingPageServiceImpl;
import com.kiteclass.core.module.settings.repository.BrandingRepository;
import com.kiteclass.core.module.settings.storage.BrandingAssetUrlResolver;
import com.kiteclass.core.testutil.LandingPageTestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
 * Unit tests for {@link LandingPageServiceImpl}.
 *
 * @since 2.10
 */
@ExtendWith(MockitoExtension.class)
class LandingPageServiceTest {

    @Mock
    private LandingPageRepository landingPageRepository;

    @Mock
    private LandingPageMapper landingPageMapper;

    @Mock
    private BrandingRepository brandingRepository;

    @Mock
    private LandingPageContentSanitizer contentSanitizer;

    @Mock
    private BrandingAssetUrlResolver assetUrlResolver;

    @InjectMocks
    private LandingPageServiceImpl landingPageService;

    private LandingPage landingPage;
    private LandingPageResponse landingPageResponse;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        landingPage = LandingPageTestDataBuilder.createDefaultLandingPage();
        landingPage.setInstanceId(tenantId);
        landingPageResponse = LandingPageResponse.builder()
                .id(landingPage.getId())
                .heroTitle(landingPage.getHeroTitle())
                .primaryColor(landingPage.getPrimaryColor())
                .build();
    }

    @Test
    void getLandingPage_notExists_createsDefault() {
        // Given
        when(landingPageRepository.findByInstanceIdAndDeletedFalse(tenantId)).thenReturn(Optional.empty());
        when(landingPageRepository.save(any(LandingPage.class))).thenReturn(landingPage);
        when(landingPageMapper.toResponse(any(LandingPage.class))).thenReturn(landingPageResponse);

        // When
        LandingPageResponse result = landingPageService.getLandingPage(tenantId);

        // Then
        assertThat(result).isNotNull();
        verify(landingPageRepository).save(any(LandingPage.class));
        verify(landingPageMapper).toResponse(any(LandingPage.class));
    }

    @Test
    void getLandingPage_exists_returnsExisting() {
        // Given
        when(landingPageRepository.findByInstanceIdAndDeletedFalse(tenantId)).thenReturn(Optional.of(landingPage));
        when(landingPageMapper.toResponse(any(LandingPage.class))).thenReturn(landingPageResponse);

        // When
        LandingPageResponse result = landingPageService.getLandingPage(tenantId);

        // Then
        assertThat(result).isNotNull();
        verify(landingPageRepository, never()).save(any());
        verify(landingPageMapper).toResponse(landingPage);
    }

    @Test
    void updateLandingPage_shouldUpdateSuccessfully() {
        // Given
        UpdateLandingPageRequest request = LandingPageTestDataBuilder.createDefaultUpdateRequest();
        when(landingPageRepository.findByInstanceIdAndDeletedFalse(tenantId)).thenReturn(Optional.of(landingPage));
        when(landingPageRepository.save(any(LandingPage.class))).thenReturn(landingPage);
        when(landingPageMapper.toResponse(any(LandingPage.class))).thenReturn(landingPageResponse);

        // When
        LandingPageResponse result = landingPageService.updateLandingPage(tenantId, request);

        // Then
        assertThat(result).isNotNull();
        verify(landingPageMapper).updateEntity(landingPage, request);
        // GAP-827: sanitize-on-write runs after mapper, before persist.
        verify(contentSanitizer).sanitize(landingPage);
        verify(landingPageRepository).save(landingPage);
    }

    @Test
    void getLandingPage_regeneratesPresignedAssetUrlsOnRead() {
        // GAP-1204: a persisted presigned logo URL would 403 after its 7-day TTL; the
        // service re-derives a fresh URL on read (heroImageUrl swept too).
        String expiredLogo = "http://localhost:9100/kite-branding-assets/static/abc/LOGO/x.png?X-Amz-Signature=stale";
        String freshLogo = "http://localhost:9100/kite-branding-assets/static/abc/LOGO/x.png?X-Amz-Signature=fresh";
        String staticHero = "/demo-banners/co-ha-toan.webp";
        landingPageResponse.setLogoUrl(expiredLogo);
        landingPageResponse.setHeroImageUrl(staticHero);
        when(landingPageRepository.findByInstanceIdAndDeletedFalse(tenantId)).thenReturn(Optional.of(landingPage));
        when(landingPageMapper.toResponse(any(LandingPage.class))).thenReturn(landingPageResponse);
        when(assetUrlResolver.regenerate(expiredLogo)).thenReturn(freshLogo);
        when(assetUrlResolver.regenerate(staticHero)).thenReturn(staticHero); // non-presigned → unchanged

        // When
        LandingPageResponse result = landingPageService.getLandingPage(tenantId);

        // Then — stale presigned URL replaced with a fresh one; static hero untouched.
        assertThat(result.getLogoUrl()).isEqualTo(freshLogo);
        assertThat(result.getHeroImageUrl()).isEqualTo(staticHero);
        verify(assetUrlResolver).regenerate(expiredLogo);
    }
}
