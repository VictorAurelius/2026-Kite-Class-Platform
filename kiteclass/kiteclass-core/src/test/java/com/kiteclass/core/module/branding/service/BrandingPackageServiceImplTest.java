package com.kiteclass.core.module.branding.service;

import com.kiteclass.core.module.branding.dto.BrandingPackage;
import com.kiteclass.core.module.branding.entity.BrandingResource;
import com.kiteclass.core.module.branding.entity.ResourceCategory;
import com.kiteclass.core.module.branding.entity.ResourceType;
import com.kiteclass.core.module.branding.repository.BrandingResourceRepository;
import com.kiteclass.core.module.instance.entity.FrontendInstance;
import com.kiteclass.core.module.instance.repository.FrontendInstanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link BrandingPackageServiceImpl} — covers GAP-129
 * (multi-tenant isolation: must NOT load resources from other tenants).
 *
 * @since 4.5.0 (GAP-129 fix)
 */
@ExtendWith(MockitoExtension.class)
class BrandingPackageServiceImplTest {

    @Mock
    private FrontendInstanceRepository instanceRepository;

    @Mock
    private BrandingResourceRepository resourceRepository;

    @InjectMocks
    private BrandingPackageServiceImpl service;

    private static final Long INSTANCE_PK = 10L;
    private FrontendInstance tenantA;
    private UUID tenantAInstanceId;

    @BeforeEach
    void setUp() {
        tenantAInstanceId = UUID.randomUUID();
        tenantA = FrontendInstance.builder()
                .tenantSlug("tenant-a")
                .slug("tenant-a")
                .frontendUrl("https://tenant-a.kiteclass.com")
                .brandingVersion(2)
                .deployedAt(Instant.now())
                .build();
        tenantA.setId(INSTANCE_PK);
        tenantA.setInstanceId(tenantAInstanceId);
    }

    private BrandingResource resource(UUID instanceId, ResourceType type, ResourceCategory category, String url) {
        BrandingResource r = BrandingResource.builder()
                .type(type)
                .category(category)
                .storageUrl(url)
                .build();
        r.setInstanceId(instanceId);
        r.setDeleted(false);
        return r;
    }

    @Test
    @DisplayName("should call tenant-scoped query (NOT findAll) — regression guard for GAP-129")
    void getByInstanceId_usesTenantScopedQuery_notFindAll() {
        when(instanceRepository.findById(INSTANCE_PK)).thenReturn(Optional.of(tenantA));
        when(resourceRepository.findByInstanceIdAndDeletedFalse(tenantAInstanceId))
                .thenReturn(List.of(
                        resource(tenantAInstanceId, ResourceType.LOGO, ResourceCategory.STATIC,
                                "https://cdn/a/logo.png")));

        BrandingPackage pkg = service.getByInstanceId(INSTANCE_PK);

        assertThat(pkg).isNotNull();
        // Tenant-scoped lookup
        verify(resourceRepository).findByInstanceIdAndDeletedFalse(tenantAInstanceId);
        // Regression guard — full table scan must NOT be used
        verify(resourceRepository, never()).findAll();
    }

    @Test
    @DisplayName("should return only tenant A resources, never tenant B (multi-tenancy isolation)")
    void getByInstanceId_doesNotReturnOtherTenantResources() {
        when(instanceRepository.findById(INSTANCE_PK)).thenReturn(Optional.of(tenantA));
        // Repository (correctly stubbed) returns ONLY tenant-A resources.
        when(resourceRepository.findByInstanceIdAndDeletedFalse(tenantAInstanceId))
                .thenReturn(List.of(
                        resource(tenantAInstanceId, ResourceType.LOGO, ResourceCategory.STATIC,
                                "https://cdn/a/logo.png"),
                        resource(tenantAInstanceId, ResourceType.BANNER, ResourceCategory.TEMPLATE,
                                "https://cdn/a/banner.png")));

        BrandingPackage pkg = service.getByInstanceId(INSTANCE_PK);

        assertThat(pkg.assets()).hasSize(2);
        assertThat(pkg.assets()).allSatisfy(asset ->
                assertThat(asset.url()).contains("/a/"));
        // Repository must be called with tenant A's UUID exactly once — and never with another tenant's id.
        verify(resourceRepository, times(1)).findByInstanceIdAndDeletedFalse(tenantAInstanceId);
        verifyNoMoreInteractions(resourceRepository);
    }

    @Test
    @DisplayName("should return empty asset list when tenant has no branding resources")
    void getByInstanceId_emptyResources() {
        when(instanceRepository.findById(INSTANCE_PK)).thenReturn(Optional.of(tenantA));
        when(resourceRepository.findByInstanceIdAndDeletedFalse(tenantAInstanceId))
                .thenReturn(List.of());

        BrandingPackage pkg = service.getByInstanceId(INSTANCE_PK);

        assertThat(pkg.assets()).isEmpty();
        verify(resourceRepository, never()).findAll();
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when frontend instance not found")
    void getByInstanceId_instanceNotFound_throws() {
        when(instanceRepository.findById(INSTANCE_PK)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByInstanceId(INSTANCE_PK))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(String.valueOf(INSTANCE_PK));

        verify(resourceRepository, never()).findAll();
        verify(resourceRepository, never()).findByInstanceIdAndDeletedFalse(any(UUID.class));
    }
}
