package com.kitehub.branding.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link TenantOwnershipGuard} — GAP-1019 branding X-Instance-Id binding.
 */
@DisplayName("TenantOwnershipGuard (branding) — X-Instance-Id vs trusted X-Tenant-Id")
class TenantOwnershipGuardTest {

    private static final UUID INSTANCE_A = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID INSTANCE_B = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "user-1", null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    @Test
    @DisplayName("OWNER with X-Instance-Id == X-Tenant-Id → allowed (String + UUID overloads)")
    void owner_match_allowed() {
        authenticateAs("OWNER");
        assertThatCode(() ->
                TenantOwnershipGuard.requireInstanceOwnership(INSTANCE_A.toString(), INSTANCE_A.toString()))
                .doesNotThrowAnyException();
        assertThatCode(() ->
                TenantOwnershipGuard.requireInstanceOwnership(INSTANCE_A, INSTANCE_A.toString()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("OWNER sending another tenant's X-Instance-Id → 403")
    void owner_crossTenant_denied() {
        authenticateAs("OWNER");
        assertThatThrownBy(() ->
                TenantOwnershipGuard.requireInstanceOwnership(INSTANCE_B.toString(), INSTANCE_A.toString()))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() ->
                TenantOwnershipGuard.requireInstanceOwnership(INSTANCE_B, INSTANCE_A.toString()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("OWNER with missing X-Instance-Id (internal-call bypass closed for non-admin) → 403")
    void owner_missingInstance_denied() {
        authenticateAs("OWNER");
        assertThatThrownBy(() ->
                TenantOwnershipGuard.requireInstanceOwnership((String) null, INSTANCE_A.toString()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("OWNER with missing X-Tenant-Id → 403")
    void owner_missingTenant_denied() {
        authenticateAs("OWNER");
        assertThatThrownBy(() ->
                TenantOwnershipGuard.requireInstanceOwnership(INSTANCE_A.toString(), null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("IfPresent: OWNER omitting X-Instance-Id → allowed (optional-instance AI path)")
    void owner_ifPresent_missingInstance_allowed() {
        authenticateAs("OWNER");
        assertThatCode(() ->
                TenantOwnershipGuard.requireInstanceOwnershipIfPresent(null, INSTANCE_A.toString()))
                .doesNotThrowAnyException();
        assertThatCode(() ->
                TenantOwnershipGuard.requireInstanceOwnershipIfPresent("  ", null))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("IfPresent: OWNER sending another tenant's X-Instance-Id → still 403")
    void owner_ifPresent_crossTenant_denied() {
        authenticateAs("OWNER");
        assertThatThrownBy(() ->
                TenantOwnershipGuard.requireInstanceOwnershipIfPresent(INSTANCE_B.toString(), INSTANCE_A.toString()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("PLATFORM_ADMIN bypasses (may target any instance, even with no tenant header)")
    void admin_bypass() {
        authenticateAs("PLATFORM_ADMIN");
        assertThatCode(() ->
                TenantOwnershipGuard.requireInstanceOwnership(INSTANCE_B.toString(), null))
                .doesNotThrowAnyException();
        assertThatCode(() ->
                TenantOwnershipGuard.requireInstanceOwnership((String) null, null))
                .doesNotThrowAnyException();
    }
}
