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
import com.kiteclass.core.module.settings.storage.BrandingAssetUrlResolver;
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

    /** Valid PNG magic bytes (89 50 4E 47 0D 0A 1A 0A) — passes GAP-1037 content sniff. */
    private static final byte[] PNG_BYTES = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D};

    /** Valid ICO magic bytes (00 00 01 00) — passes GAP-1037 content sniff. */
    private static final byte[] ICO_BYTES = {0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x10, 0x10};

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
        // GAP-1204: the regenerate-on-read logic now lives in BrandingAssetUrlResolver
        // (shared with the LandingPage surface). Wrap the same storage mock so the
        // GAP-1072 presigned-regen assertions keep verifying renderableUrl(...) calls.
        brandingService = new BrandingServiceImpl(
                brandingRepository, brandingMapper, null, null, brandingAssetStorage, contentSanitizer,
                new BrandingAssetUrlResolver(brandingAssetStorage));
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
                "logo", "logo.png", "image/png", PNG_BYTES);

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
                "favicon", "favicon.ico", "image/x-icon", ICO_BYTES);

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
    @DisplayName("GAP-1072: should regenerate fresh presigned logo URL on read")
    void shouldRegenerateLogoUrlOnRead() {
        // Given — stored logo URL is a presigned URL whose key is static/<tenant>/logo/logo.png
        String storedUrl = "http://localhost:9100/kite-branding-assets/"
                + "static/11111111-1111-1111-1111-111111111111/logo/logo.png"
                + "?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20260101T000000Z"
                + "&X-Amz-Expires=604800&X-Amz-Signature=stale";
        String expectedKey = "static/11111111-1111-1111-1111-111111111111/logo/logo.png";
        String freshUrl = "http://localhost:9100/kite-branding-assets/"
                + expectedKey + "?X-Amz-Date=20260601T000000Z&X-Amz-Signature=fresh";

        Branding branding = BrandingTestDataBuilder.createDefaultBranding(testInstanceId);
        BrandingResponse mapped = BrandingResponse.builder().id(1L).logoUrl(storedUrl).build();

        when(brandingRepository.findByInstanceIdAndDeletedFalse(testInstanceId))
                .thenReturn(Optional.of(branding));
        when(brandingMapper.toResponse(branding)).thenReturn(mapped);
        when(brandingAssetStorage.renderableUrl(expectedKey)).thenReturn(freshUrl);

        // When
        BrandingResponse result = brandingService.getBranding();

        // Then — response carries the freshly regenerated URL (not the stale stored one)
        assertThat(result.getLogoUrl()).isEqualTo(freshUrl);
        verify(brandingAssetStorage).renderableUrl(expectedKey);
    }

    @Test
    @DisplayName("GAP-1072: should regenerate fresh presigned favicon URL on read")
    void shouldRegenerateFaviconUrlOnRead() {
        // Given
        String storedUrl = "http://localhost:9100/kite-branding-assets/"
                + "static/22222222-2222-2222-2222-222222222222/favicon/favicon.ico"
                + "?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Signature=stale";
        String expectedKey = "static/22222222-2222-2222-2222-222222222222/favicon/favicon.ico";
        String freshUrl = "http://localhost:9100/kite-branding-assets/" + expectedKey
                + "?X-Amz-Signature=fresh";

        Branding branding = BrandingTestDataBuilder.createDefaultBranding(testInstanceId);
        BrandingResponse mapped = BrandingResponse.builder().id(1L).faviconUrl(storedUrl).build();

        when(brandingRepository.findByInstanceIdAndDeletedFalse(testInstanceId))
                .thenReturn(Optional.of(branding));
        when(brandingMapper.toResponse(branding)).thenReturn(mapped);
        when(brandingAssetStorage.renderableUrl(expectedKey)).thenReturn(freshUrl);

        // When
        BrandingResponse result = brandingService.getBranding();

        // Then
        assertThat(result.getFaviconUrl()).isEqualTo(freshUrl);
        verify(brandingAssetStorage).renderableUrl(expectedKey);
    }

    @Test
    @DisplayName("GAP-1072: should keep stored URL when it is not one of our presigned URLs")
    void shouldKeepStoredUrlWhenNotPresigned() {
        // Given — external / non-presigned URL (no X-Amz query) must be left untouched
        String externalUrl = "https://cdn.example.com/logos/brand-logo.png";
        Branding branding = BrandingTestDataBuilder.createDefaultBranding(testInstanceId);
        BrandingResponse mapped = BrandingResponse.builder().id(1L).logoUrl(externalUrl).build();

        when(brandingRepository.findByInstanceIdAndDeletedFalse(testInstanceId))
                .thenReturn(Optional.of(branding));
        when(brandingMapper.toResponse(branding)).thenReturn(mapped);

        // When
        BrandingResponse result = brandingService.getBranding();

        // Then — unchanged; storage never consulted for non-presigned values
        assertThat(result.getLogoUrl()).isEqualTo(externalUrl);
        verify(brandingAssetStorage, times(0)).renderableUrl(any());
    }

    @Test
    @DisplayName("GAP-1072: should keep stored URL when regeneration fails (graceful fallback)")
    void shouldKeepStoredUrlWhenRegenerationFails() {
        // Given — presigned URL, but storage throws when regenerating
        String storedUrl = "http://localhost:9100/kite-branding-assets/"
                + "static/33333333-3333-3333-3333-333333333333/logo/logo.png"
                + "?X-Amz-Signature=stale";
        String expectedKey = "static/33333333-3333-3333-3333-333333333333/logo/logo.png";

        Branding branding = BrandingTestDataBuilder.createDefaultBranding(testInstanceId);
        BrandingResponse mapped = BrandingResponse.builder().id(1L).logoUrl(storedUrl).build();

        when(brandingRepository.findByInstanceIdAndDeletedFalse(testInstanceId))
                .thenReturn(Optional.of(branding));
        when(brandingMapper.toResponse(branding)).thenReturn(mapped);
        when(brandingAssetStorage.renderableUrl(expectedKey))
                .thenThrow(new RuntimeException("presigner down"));

        // When
        BrandingResponse result = brandingService.getBranding();

        // Then — falls back to the stored URL instead of propagating the failure
        assertThat(result.getLogoUrl()).isEqualTo(storedUrl);
    }

    @Test
    @DisplayName("GAP-1211: should upload banner under a unique key and return its URL")
    void shouldUploadBanner() {
        // Given
        String bannerUrl = "https://minio.local/kite-branding-assets/static/t/banner/abc.png?sig=x";
        MultipartFile file = new MockMultipartFile(
                "banner", "banner.png", "image/png", PNG_BYTES);

        when(brandingAssetStorage.store(eq(testInstanceId), eq(ResourceType.BANNER),
                any(String.class), eq("image/png"), any(byte[].class))).thenReturn(bannerUrl);

        // When
        com.kiteclass.core.module.settings.dto.response.BannerUploadResponse result =
                brandingService.uploadBanner(file);

        // Then — stored as BANNER (not LOGO/FAVICON → no slot clobber), URL returned,
        // branding row never touched.
        assertThat(result).isNotNull();
        assertThat(result.url()).isEqualTo(bannerUrl);
        verify(brandingAssetStorage).store(eq(testInstanceId), eq(ResourceType.BANNER),
                any(String.class), eq("image/png"), any(byte[].class));
        verify(brandingRepository, times(0)).save(any(Branding.class));
    }

    @Test
    @DisplayName("GAP-1211: should reject banner with unsupported content type (415)")
    void shouldRejectBannerUnsupportedType() {
        // Given
        MultipartFile file = new MockMultipartFile(
                "banner", "banner.txt", "text/plain", "not-an-image".getBytes());

        // When & Then — 415, before any storage call
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> brandingService.uploadBanner(file))
                .isInstanceOf(com.kiteclass.core.common.exception.BusinessException.class)
                .extracting("status")
                .isEqualTo(org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        verify(brandingAssetStorage, times(0)).store(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("GAP-1211: should reject banner exceeding the size cap (413)")
    void shouldRejectBannerTooLarge() {
        // Given — content one byte over the 5 MB cap
        byte[] tooBig = new byte[(int) BrandingServiceImpl.MAX_ASSET_BYTES + 1];
        MultipartFile file = new MockMultipartFile(
                "banner", "banner.png", "image/png", tooBig);

        // When & Then — 413, before any storage call
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> brandingService.uploadBanner(file))
                .isInstanceOf(com.kiteclass.core.common.exception.BusinessException.class)
                .extracting("status")
                .isEqualTo(org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE);
        verify(brandingAssetStorage, times(0)).store(any(), any(), any(), any(), any());
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

    @Test
    @DisplayName("GAP-1037: should reject image/svg+xml logo (SVG removed from allowlist)")
    void shouldRejectSvgLogoContentType() {
        // Given — a genuine SVG with an embedded <script> declared as image/svg+xml
        byte[] svg = ("<svg xmlns=\"http://www.w3.org/2000/svg\"><script>"
                + "alert(document.cookie)</script></svg>").getBytes();
        MultipartFile file = new MockMultipartFile("logo", "logo.svg", "image/svg+xml", svg);

        // When & Then — rejected at the allowlist (svg+xml no longer accepted),
        // before any storage call
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> brandingService.uploadLogo(file))
                .isInstanceOf(com.kiteclass.core.common.exception.ValidationException.class);
        verify(brandingAssetStorage, times(0)).store(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("GAP-1037: should reject SVG/script payload spoofed as image/png (content sniff)")
    void shouldRejectSpoofedMimeViaContentSniff() {
        // Given — an SVG/script payload that lies about its type with an image/png header.
        // Passes the client-MIME allowlist but its bytes are not a real PNG.
        byte[] svg = ("<svg xmlns=\"http://www.w3.org/2000/svg\"><script>"
                + "alert(1)</script></svg>").getBytes();
        MultipartFile file = new MockMultipartFile("logo", "logo.png", "image/png", svg);

        // When & Then — magic-byte sniff rejects it; storage never reached
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> brandingService.uploadLogo(file))
                .isInstanceOf(com.kiteclass.core.common.exception.ValidationException.class);
        verify(brandingAssetStorage, times(0)).store(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("GAP-1037: should reject spoofed banner payload via content sniff (415)")
    void shouldRejectSpoofedBannerViaContentSniff() {
        // Given — script payload spoofed as image/png on the banner path
        byte[] payload = "<script>alert(1)</script>".getBytes();
        MultipartFile file = new MockMultipartFile("banner", "banner.png", "image/png", payload);

        // When & Then — 415 from the magic-byte sniff, before any storage call
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> brandingService.uploadBanner(file))
                .isInstanceOf(com.kiteclass.core.common.exception.BusinessException.class)
                .extracting("status")
                .isEqualTo(org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        verify(brandingAssetStorage, times(0)).store(any(), any(), any(), any(), any());
    }
}
