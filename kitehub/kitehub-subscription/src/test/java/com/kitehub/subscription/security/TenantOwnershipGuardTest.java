package com.kitehub.subscription.security;

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
 * Unit tests for {@link TenantOwnershipGuard} — GAP-1015 / GAP-1023 cross-tenant binding.
 */
@DisplayName("TenantOwnershipGuard (subscription) — cross-tenant ownership binding")
class TenantOwnershipGuardTest {

    private static final UUID INSTANCE_A = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID INSTANCE_B = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");
    private static final UUID USER_A = UUID.fromString("cccccccc-0000-0000-0000-000000000001");
    private static final UUID USER_B = UUID.fromString("dddddddd-0000-0000-0000-000000000002");

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
    @DisplayName("OWNER acting on own instance → allowed")
    void owner_ownInstance_allowed() {
        authenticateAs("OWNER");
        assertThatCode(() -> TenantOwnershipGuard.requireOwnership(INSTANCE_A, INSTANCE_A.toString()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("OWNER acting on another tenant's instance → 403")
    void owner_crossTenant_denied() {
        authenticateAs("OWNER");
        assertThatThrownBy(() -> TenantOwnershipGuard.requireOwnership(INSTANCE_B, INSTANCE_A.toString()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("OWNER with missing X-Tenant-Id → 403")
    void owner_missingTenant_denied() {
        authenticateAs("OWNER");
        assertThatThrownBy(() -> TenantOwnershipGuard.requireOwnership(INSTANCE_A, null))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> TenantOwnershipGuard.requireOwnership(INSTANCE_A, "  "))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("OWNER with malformed X-Tenant-Id → 403")
    void owner_malformedTenant_denied() {
        authenticateAs("OWNER");
        assertThatThrownBy(() -> TenantOwnershipGuard.requireOwnership(INSTANCE_A, "not-a-uuid"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("PLATFORM_ADMIN bypasses cross-tenant check (even with no X-Tenant-Id)")
    void platformAdmin_bypass() {
        authenticateAs("PLATFORM_ADMIN");
        assertThatCode(() -> TenantOwnershipGuard.requireOwnership(INSTANCE_B, null))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("ADMIN legacy alias also bypasses")
    void admin_bypass() {
        authenticateAs("ADMIN");
        assertThatCode(() -> TenantOwnershipGuard.requireOwnership(INSTANCE_B, INSTANCE_A.toString()))
                .doesNotThrowAnyException();
    }

    // ── GAP-1050: requireSelfOrAdmin (owner-enumeration, X-User-Id axis) ──

    @Test
    @DisplayName("OWNER enumerating own ownerId → allowed")
    void owner_selfEnumeration_allowed() {
        authenticateAs("OWNER");
        assertThatCode(() -> TenantOwnershipGuard.requireSelfOrAdmin(USER_A, USER_A.toString()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("OWNER enumerating another user's ownerId → 403")
    void owner_crossUserEnumeration_denied() {
        authenticateAs("OWNER");
        assertThatThrownBy(() -> TenantOwnershipGuard.requireSelfOrAdmin(USER_B, USER_A.toString()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("OWNER with missing/malformed X-User-Id → 403")
    void owner_missingOrMalformedUser_denied() {
        authenticateAs("OWNER");
        assertThatThrownBy(() -> TenantOwnershipGuard.requireSelfOrAdmin(USER_A, null))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> TenantOwnershipGuard.requireSelfOrAdmin(USER_A, "  "))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> TenantOwnershipGuard.requireSelfOrAdmin(USER_A, "not-a-uuid"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("PLATFORM_ADMIN bypasses cross-user enumeration check")
    void platformAdmin_bypassEnumeration() {
        authenticateAs("PLATFORM_ADMIN");
        assertThatCode(() -> TenantOwnershipGuard.requireSelfOrAdmin(USER_B, null))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("isPlatformAdmin reflects authorities")
    void isPlatformAdmin_flag() {
        authenticateAs("OWNER");
        org.assertj.core.api.Assertions.assertThat(TenantOwnershipGuard.isPlatformAdmin()).isFalse();
        SecurityContextHolder.clearContext();
        authenticateAs("PLATFORM_ADMIN");
        org.assertj.core.api.Assertions.assertThat(TenantOwnershipGuard.isPlatformAdmin()).isTrue();
    }
}
