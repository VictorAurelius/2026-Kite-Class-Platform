package com.kitehub.subscription.consent.immutable;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ConsentAuthorizationBean} — Wave beta-readiness-8 Bucket A (GAP-737).
 *
 * <p>Covers the IDOR decision matrix: null userId, missing auth, anonymous principal,
 * platform-admin escape hatch, principal-name string match, mismatch (the IDOR vector).</p>
 */
@DisplayName("ConsentAuthorizationBean — IDOR decision matrix")
class ConsentAuthorizationBeanTest {

    private final ConsentAuthorizationBean bean = new ConsentAuthorizationBean();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("null userId → deny")
    void nullUserIdReturnsFalse() {
        setAuth("42", "ROLE_TENANT_USER");
        assertThat(bean.canAccessUser(null)).isFalse();
    }

    @Test
    @DisplayName("no authentication on context → deny")
    void noAuthReturnsFalse() {
        SecurityContextHolder.clearContext();
        assertThat(bean.canAccessUser(42L)).isFalse();
    }

    @Test
    @DisplayName("anonymous principal → deny")
    void anonymousReturnsFalse() {
        Authentication anon = new AnonymousAuthenticationToken(
                "key", "anonymousUser",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        // AnonymousAuthenticationToken#getName() returns "anonymousUser" — not numeric;
        // ensures the equals-check denies even before role check picks anything up.
        SecurityContextHolder.getContext().setAuthentication(anon);
        assertThat(bean.canAccessUser(42L)).isFalse();
    }

    @Test
    @DisplayName("same userId principal → allow")
    void sameUserAllowed() {
        setAuth("42", "ROLE_TENANT_USER");
        assertThat(bean.canAccessUser(42L)).isTrue();
    }

    @Test
    @DisplayName("different userId principal → deny (IDOR vector)")
    void differentUserDenied() {
        setAuth("42", "ROLE_TENANT_USER");
        assertThat(bean.canAccessUser(99L)).isFalse();
    }

    @Test
    @DisplayName("platform admin → allow even on cross-user")
    void platformAdminAllowedCrossUser() {
        setAuth("admin-uuid-1", "ROLE_PLATFORM_ADMIN");
        assertThat(bean.canAccessUser(42L)).isTrue();
        assertThat(bean.canAccessUser(99L)).isTrue();
    }

    @Test
    @DisplayName("multiple roles including PLATFORM_ADMIN → allow")
    void multipleRolesIncludingAdminAllowed() {
        setAuth("admin-uuid-2", "ROLE_TENANT_USER", "ROLE_PLATFORM_ADMIN");
        assertThat(bean.canAccessUser(7L)).isTrue();
    }

    @Test
    @DisplayName("tenant owner without admin role + cross-user → deny")
    void tenantOwnerCrossUserDenied() {
        setAuth("100", "ROLE_TENANT_OWNER");
        assertThat(bean.canAccessUser(101L)).isFalse();
    }

    @Test
    @DisplayName("authenticated principal but null name → deny")
    void nullPrincipalNameDenied() {
        // Construct a token whose principal stringifies to null is not directly supported by
        // UsernamePasswordAuthenticationToken (it falls back to ""). Use a custom subclass to
        // simulate a logged-in but name-less authentication.
        Authentication weird = new UsernamePasswordAuthenticationToken(
                null, null,
                List.of(new SimpleGrantedAuthority("ROLE_TENANT_USER"))) {
            @Override
            public String getName() {
                return null;
            }
        };
        SecurityContextHolder.getContext().setAuthentication(weird);
        assertThat(bean.canAccessUser(42L)).isFalse();
    }

    private void setAuth(String principalName, String... roleAuthorities) {
        List<SimpleGrantedAuthority> authorities = java.util.Arrays.stream(roleAuthorities)
                .map(SimpleGrantedAuthority::new)
                .toList();
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principalName, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
