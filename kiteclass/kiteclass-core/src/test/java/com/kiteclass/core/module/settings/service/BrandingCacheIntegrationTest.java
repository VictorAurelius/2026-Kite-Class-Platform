package com.kiteclass.core.module.settings.service;

import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.config.TestContainersConfiguration;
import com.kiteclass.core.module.settings.dto.request.UpdateBrandingRequest;
import com.kiteclass.core.module.settings.dto.response.BrandingResponse;
import com.kiteclass.core.module.settings.storage.BrandingAssetStorage;
import com.kiteclass.core.module.settings.versioning.BrandingVersionService;
import java.util.Objects;
import java.util.UUID;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link BrandingService} caching — closes <strong>GAP-215</strong>.
 *
 * <p>Wave 5 audit (`performance-audit-2026-04-25-wave5.md` finding P0-1) flagged that
 * {@code BrandingService.getBranding()} hit PostgreSQL on every document render. Sub-PR 5.6b
 * adds {@code @Cacheable("branding-by-tenant")} + {@code @CacheEvict} on mutators. This test
 * verifies the wiring works end-to-end with the real Redis cache + real repository.
 *
 * <p>Pattern mirrors {@code StudentCacheIntegrationTest} — TestContainers Postgres + Redis,
 * clear caches per test, exercise hit / miss / eviction.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestContainersConfiguration.class})
@ContextConfiguration(initializers = TestContainersConfiguration.Initializer.class)
@Transactional
class BrandingCacheIntegrationTest {

    private static final String CACHE_NAME = "branding-by-tenant";

    @Autowired
    private BrandingService brandingService;

    @Autowired
    private CacheManager cacheManager;

    /**
     * BrandingVersionService is mocked so the {@code updateBranding} path doesn't trigger a
     * JSONB snapshot insert. The original reason for this isolation was GAP-220 (now FIXED via
     * {@code @JdbcTypeCode(SqlTypes.JSON)} on {@code BrandingVersion.snapshotJson}); the mock
     * is kept as defensive isolation so this cache test never depends on the snapshot pipeline
     * staying healthy.
     *
     * <p>Uses {@link MockitoBean} (Spring Framework 6.2 / Spring Boot 3.4+) instead of the
     * deprecated {@code @MockitoBean} from {@code spring-boot-test} — same semantics, new package.
     */
    @MockitoBean
    private BrandingVersionService brandingVersionService;

    /**
     * BrandingAssetStorage is mocked so {@code uploadLogo}/{@code uploadFavicon} don't hit a
     * real MinIO endpoint in the cache test (GAP-804). The returned URL is irrelevant here — the
     * test only asserts cache eviction, not the stored URL.
     */
    @MockitoBean
    private BrandingAssetStorage brandingAssetStorage;

    private UUID tenant1;
    private UUID tenant2;

    @BeforeEach
    void setUp() {
        tenant1 = UUID.randomUUID();
        tenant2 = UUID.randomUUID();
        // CRITICAL: clear all caches to avoid cross-test contamination + serializer mismatches.
        cacheManager.getCacheNames().forEach(name ->
                Objects.requireNonNull(cacheManager.getCache(name)).clear());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("First getBranding() warms cache; second call hits cache (same tenant)")
    void getBranding_warms_then_hits_cache_for_same_tenant() {
        TenantContext.setCurrentTenant(tenant1);
        Cache cache = cacheManager.getCache(CACHE_NAME);
        assertThat(cache).as("cache '%s' must be registered in CacheConfig", CACHE_NAME).isNotNull();

        // First call: miss — populates cache as a side effect of @Cacheable.
        BrandingResponse first = brandingService.getBranding();
        Cache.ValueWrapper afterFirst = cache.get(tenant1);
        assertThat(afterFirst).as("cache entry should exist after first call").isNotNull();

        // Second call: hit — must return the same logical content.
        BrandingResponse second = brandingService.getBranding();
        assertThat(second).isNotNull();
        assertThat(second.getDisplayName()).isEqualTo(first.getDisplayName());
        assertThat(second.getPrimaryColor()).isEqualTo(first.getPrimaryColor());
    }

    @Test
    @DisplayName("Cache is keyed per tenant — separate entries for t1 vs t2")
    void getBranding_isolates_cache_per_tenant() {
        TenantContext.setCurrentTenant(tenant1);
        brandingService.getBranding();
        TenantContext.clear();

        TenantContext.setCurrentTenant(tenant2);
        brandingService.getBranding();

        Cache cache = cacheManager.getCache(CACHE_NAME);
        assertThat(cache).isNotNull();
        // Both entries must exist under their tenant-keyed slots — that IS the isolation proof.
        // Payloads are intentionally NOT compared: when neither tenant has customised branding,
        // both calls return the default Branding shape (identical bytes), which is correct
        // behaviour. The cache key separation is what guarantees no cross-tenant leakage.
        assertThat(cache.get(tenant1)).as("t1 entry exists in cache").isNotNull();
        assertThat(cache.get(tenant2)).as("t2 entry exists in cache").isNotNull();
    }

    @Test
    @DisplayName("updateBranding evicts the cache entry for the calling tenant")
    void updateBranding_evicts_cache_for_tenant() {
        TenantContext.setCurrentTenant(tenant1);
        brandingService.getBranding(); // warm
        Cache cache = cacheManager.getCache(CACHE_NAME);
        assertThat(cache).isNotNull();
        assertThat(cache.get(tenant1)).as("warm before update").isNotNull();

        UpdateBrandingRequest req = UpdateBrandingRequest.builder()
                .displayName("Updated School")
                .primaryColor("#FF0000")
                .secondaryColor("#00FF00")
                .accentColor("#0000FF")
                .build();
        brandingService.updateBranding(req);

        assertThat(cache.get(tenant1))
                .as("@CacheEvict on updateBranding should clear the tenant entry")
                .isNull();

        // Subsequent read repopulates with the new value.
        BrandingResponse afterUpdate = brandingService.getBranding();
        assertThat(afterUpdate.getDisplayName()).isEqualTo("Updated School");
        assertThat(afterUpdate.getPrimaryColor()).isEqualToIgnoringCase("#FF0000");
    }

    @Test
    @DisplayName("uploadLogo evicts the cache entry for the calling tenant")
    void uploadLogo_evicts_cache_for_tenant() {
        TenantContext.setCurrentTenant(tenant1);
        brandingService.getBranding(); // warm
        Cache cache = cacheManager.getCache(CACHE_NAME);
        assertThat(cache).isNotNull();
        assertThat(cache.get(tenant1)).isNotNull();

        // GAP-1037: bytes must pass the magic-byte content sniff (PNG signature).
        MultipartFile logo = new MockMultipartFile(
                "logo", "logo.png", "image/png",
                new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D});
        brandingService.uploadLogo(logo);

        assertThat(cache.get(tenant1))
                .as("@CacheEvict on uploadLogo should clear the tenant entry")
                .isNull();
    }

    @Test
    @DisplayName("uploadFavicon evicts the cache entry for the calling tenant")
    void uploadFavicon_evicts_cache_for_tenant() {
        TenantContext.setCurrentTenant(tenant1);
        brandingService.getBranding(); // warm
        Cache cache = cacheManager.getCache(CACHE_NAME);
        assertThat(cache).isNotNull();
        assertThat(cache.get(tenant1)).isNotNull();

        // GAP-1037: bytes must pass the magic-byte content sniff (ICO signature).
        MultipartFile favicon = new MockMultipartFile(
                "favicon", "favicon.ico", "image/x-icon",
                new byte[] {0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x10, 0x10});
        brandingService.uploadFavicon(favicon);

        assertThat(cache.get(tenant1))
                .as("@CacheEvict on uploadFavicon should clear the tenant entry")
                .isNull();
    }
}
