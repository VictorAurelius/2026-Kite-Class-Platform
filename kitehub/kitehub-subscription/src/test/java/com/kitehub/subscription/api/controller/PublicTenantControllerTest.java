package com.kitehub.subscription.api.controller;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.subscription.api.dto.TenantResolveDto;
import com.kitehub.subscription.module.tenant.service.TenantLookupService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PublicTenantController} (Wave tenant-domain-1 Bucket B — GAP-813).
 *
 * <p>Verifies the controller's HTTP-status + body shape mapping per
 * {@code documents/01-business/kitehub/marketing/api-contract.md} §9 across
 * 200 / 400 / 404 / 410 paths.</p>
 *
 * @since Wave tenant-domain-1 Bucket B (GAP-813)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PublicTenantController — anonymous tenant resolve (GAP-813)")
class PublicTenantControllerTest {

    @Mock
    private TenantLookupService tenantLookupService;

    @InjectMocks
    private PublicTenantController controller;

    private Instance buildInstance(InstanceStatus status, String slug, String name) {
        Instance instance = new Instance();
        instance.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        instance.setSubdomain(slug);
        instance.setOrganizationName(name);
        instance.setStatus(status);
        return instance;
    }

    @Test
    @DisplayName("ACTIVE tenant → 200 + DTO with id/subdomain/name/status=ACTIVE")
    void resolveBySubdomain_active_returns200() {
        Instance instance = buildInstance(InstanceStatus.ACTIVE, "sky", "Trung tâm Anh ngữ Sky Education");
        when(tenantLookupService.findBySubdomain("sky")).thenReturn(Optional.of(instance));
        when(tenantLookupService.toDto(instance)).thenReturn(TenantResolveDto.builder()
                .id(instance.getId())
                .subdomain(instance.getSubdomain())
                .name(instance.getOrganizationName())
                .status(InstanceStatus.ACTIVE.name())
                .build());

        ResponseEntity<?> response = controller.resolveBySubdomain("sky");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(TenantResolveDto.class);
        TenantResolveDto body = (TenantResolveDto) response.getBody();
        assertThat(body.getId()).isEqualTo(instance.getId());
        assertThat(body.getSubdomain()).isEqualTo("sky");
        assertThat(body.getName()).isEqualTo("Trung tâm Anh ngữ Sky Education");
        assertThat(body.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("TRIAL tenant → 200 + DTO with status=ACTIVE (public projection collapses TRIAL→ACTIVE)")
    void resolveBySubdomain_trial_returns200WithActiveStatus() {
        Instance instance = buildInstance(InstanceStatus.TRIAL, "pioneer", "Trung tâm Pioneer");
        when(tenantLookupService.findBySubdomain("pioneer")).thenReturn(Optional.of(instance));
        when(tenantLookupService.toDto(instance)).thenReturn(TenantResolveDto.builder()
                .id(instance.getId())
                .subdomain(instance.getSubdomain())
                .name(instance.getOrganizationName())
                .status(InstanceStatus.TRIAL.name())
                .build());

        ResponseEntity<?> response = controller.resolveBySubdomain("pioneer");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        TenantResolveDto body = (TenantResolveDto) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus())
                .as("public projection collapses TRIAL → ACTIVE so FE only learns 1 OK state")
                .isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("Unknown slug → 404 + TENANT_NOT_FOUND")
    void resolveBySubdomain_notFound_returns404() {
        when(tenantLookupService.findBySubdomain("ghost")).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.resolveBySubdomain("ghost");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("error")).isEqualTo("TENANT_NOT_FOUND");
        assertThat(body.get("message")).asString().contains("ghost");
    }

    @Test
    @DisplayName("SUSPENDED tenant → 410 + TENANT_SUSPENDED + status field")
    void resolveBySubdomain_suspended_returns410() {
        Instance instance = buildInstance(InstanceStatus.SUSPENDED, "lapsed", "Lapsed Center");
        when(tenantLookupService.findBySubdomain("lapsed")).thenReturn(Optional.of(instance));

        ResponseEntity<?> response = controller.resolveBySubdomain("lapsed");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.GONE);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("error")).isEqualTo("TENANT_SUSPENDED");
        assertThat(body.get("status")).isEqualTo("SUSPENDED");
        // DTO mapping NOT invoked on non-active paths (lighter; no extra projection).
        verify(tenantLookupService, never()).toDto(any());
    }

    @Test
    @DisplayName("DELETED tenant → 410 + TENANT_DELETED")
    void resolveBySubdomain_deleted_returns410() {
        Instance instance = buildInstance(InstanceStatus.DELETED, "wiped", "Wiped Center");
        when(tenantLookupService.findBySubdomain("wiped")).thenReturn(Optional.of(instance));

        ResponseEntity<?> response = controller.resolveBySubdomain("wiped");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.GONE);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("error")).isEqualTo("TENANT_DELETED");
        assertThat(body.get("status")).isEqualTo("DELETED");
    }

    @Test
    @DisplayName("PENDING tenant (pre-provisioning) → 404 (treat as not-found)")
    void resolveBySubdomain_pending_returns404() {
        Instance instance = buildInstance(InstanceStatus.PENDING, "newbie", "Newbie Center");
        when(tenantLookupService.findBySubdomain("newbie")).thenReturn(Optional.of(instance));

        ResponseEntity<?> response = controller.resolveBySubdomain("newbie");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ---------------- 400 validation paths ----------------

    @Test
    @DisplayName("UPPERCASE slug → 400 INVALID_SLUG_FORMAT (no DB lookup)")
    void resolveBySubdomain_uppercase_returns400() {
        ResponseEntity<?> response = controller.resolveBySubdomain("Sky");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body.get("error")).isEqualTo("INVALID_SLUG_FORMAT");
        verify(tenantLookupService, never()).findBySubdomain(any());
    }

    @Test
    @DisplayName("Leading hyphen → 400 INVALID_SLUG_FORMAT")
    void resolveBySubdomain_leadingHyphen_returns400() {
        ResponseEntity<?> response = controller.resolveBySubdomain("-sky");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(tenantLookupService, never()).findBySubdomain(any());
    }

    @Test
    @DisplayName("Trailing hyphen → 400 INVALID_SLUG_FORMAT")
    void resolveBySubdomain_trailingHyphen_returns400() {
        ResponseEntity<?> response = controller.resolveBySubdomain("sky-");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Slug > 50 chars → 400 INVALID_SLUG_FORMAT")
    void resolveBySubdomain_tooLong_returns400() {
        String tooLong = "a".repeat(51);

        ResponseEntity<?> response = controller.resolveBySubdomain(tooLong);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(tenantLookupService, never()).findBySubdomain(any());
    }

    @Test
    @DisplayName("Empty slug → 400 INVALID_SLUG_FORMAT")
    void resolveBySubdomain_empty_returns400() {
        ResponseEntity<?> response = controller.resolveBySubdomain("");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Slug with special chars → 400 INVALID_SLUG_FORMAT")
    void resolveBySubdomain_specialChars_returns400() {
        ResponseEntity<?> response = controller.resolveBySubdomain("sky_center");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Single character slug → valid format (delegates to lookup)")
    void resolveBySubdomain_singleChar_validatesPasses() {
        when(tenantLookupService.findBySubdomain("a")).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.resolveBySubdomain("a");

        // not 400 — format valid; 404 because no row.
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(tenantLookupService).findBySubdomain("a");
    }

    @Test
    @DisplayName("Slug exactly 50 chars → valid format")
    void resolveBySubdomain_50chars_validatesPasses() {
        String at50 = "a".repeat(50);
        when(tenantLookupService.findBySubdomain(at50)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.resolveBySubdomain(at50);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
