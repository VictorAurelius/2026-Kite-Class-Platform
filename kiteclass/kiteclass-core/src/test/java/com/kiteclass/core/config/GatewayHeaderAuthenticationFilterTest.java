package com.kiteclass.core.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link GatewayHeaderAuthenticationFilter} — KC-7 G1 walk fix
 * (X-User-Roles → Spring authority bridge so hasRole/hasAnyRole guards work).
 */
class GatewayHeaderAuthenticationFilterTest {

    private final GatewayHeaderAuthenticationFilter filter = new GatewayHeaderAuthenticationFilter();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private List<String> authorities() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? List.of() : auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }

    @Test
    void singleRole_mapsToRolePrefixedAuthority_andContinuesChain() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/invoices/28/record-payment");
        req.addHeader(GatewayHeaderAuthenticationFilter.ROLES_HEADER, "OWNER");
        req.addHeader(GatewayHeaderAuthenticationFilter.USER_ID_HEADER, "11111111-1111-1111-1111-111111111111");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(authorities()).containsExactly("ROLE_OWNER");
        assertThat(SecurityContextHolder.getContext().getAuthentication().isAuthenticated()).isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isEqualTo("11111111-1111-1111-1111-111111111111");
        verify(chain, times(1)).doFilter(req, res);
    }

    @Test
    void commaSeparatedRoles_mapEach() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/invoices/28/payment-records");
        req.addHeader(GatewayHeaderAuthenticationFilter.ROLES_HEADER, "OWNER,ADMIN");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(authorities()).containsExactlyInAnyOrder("ROLE_OWNER", "ROLE_ADMIN");
    }

    @Test
    void alreadyPrefixedRole_isNotDoublePrefixed() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/anything");
        req.addHeader(GatewayHeaderAuthenticationFilter.ROLES_HEADER, "ROLE_TEACHER");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(authorities()).containsExactly("ROLE_TEACHER");
    }

    @Test
    void lowercaseRole_isUppercased() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/anything");
        req.addHeader(GatewayHeaderAuthenticationFilter.ROLES_HEADER, "owner");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(authorities()).containsExactly("ROLE_OWNER");
    }

    @Test
    void noRolesHeader_leavesContextUnauthenticated_andContinuesChain() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/anything");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain, times(1)).doFilter(req, res);
    }

    @Test
    void blankRolesHeader_leavesContextUnauthenticated() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/anything");
        req.addHeader(GatewayHeaderAuthenticationFilter.ROLES_HEADER, "   ");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void missingUserId_fallsBackToPlaceholderPrincipal() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/anything");
        req.addHeader(GatewayHeaderAuthenticationFilter.ROLES_HEADER, "TEACHER");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isEqualTo("gateway-user");
        assertThat(authorities()).containsExactly("ROLE_TEACHER");
    }
}
