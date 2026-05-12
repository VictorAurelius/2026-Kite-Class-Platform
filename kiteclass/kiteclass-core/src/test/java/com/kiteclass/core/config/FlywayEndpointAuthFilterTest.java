package com.kiteclass.core.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link FlywayEndpointAuthFilter} — GAP-476 admin gate behavior.
 */
class FlywayEndpointAuthFilterTest {

    private final FlywayEndpointAuthFilter filter = new FlywayEndpointAuthFilter();

    @Test
    void rejectsUnauthenticatedFlywayRequestWith401() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/actuator/flyway");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(401);
        assertThat(res.getContentAsString()).contains("FLYWAY_ENDPOINT_UNAUTHORIZED");
        verify(chain, never()).doFilter(req, res);
    }

    @Test
    void rejectsRequestWithNonAdminRole() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/actuator/flyway");
        req.addHeader(FlywayEndpointAuthFilter.ROLES_HEADER, "TEACHER,PARENT");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(401);
        verify(chain, never()).doFilter(req, res);
    }

    @Test
    void allowsAdminRoleThroughFilter() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/actuator/flyway");
        req.addHeader(FlywayEndpointAuthFilter.ROLES_HEADER, "USER,ADMIN");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        // Filter passes — downstream chain handles the actual response.
        assertThat(res.getStatus()).isEqualTo(200);
        verify(chain, times(1)).doFilter(req, res);
    }

    @Test
    void allowsLowercaseAdminRole() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/actuator/flyway");
        req.addHeader(FlywayEndpointAuthFilter.ROLES_HEADER, "admin");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        verify(chain, times(1)).doFilter(req, res);
    }

    @Test
    void passesThroughNonFlywayPaths() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        // No header, but path is outside scope — filter does not intervene.
        verify(chain, times(1)).doFilter(req, res);
        assertThat(res.getStatus()).isEqualTo(200);
    }

    @Test
    void hasAdminRoleHandlesNullAndBlank() {
        assertThat(FlywayEndpointAuthFilter.hasAdminRole(null)).isFalse();
        assertThat(FlywayEndpointAuthFilter.hasAdminRole("")).isFalse();
        assertThat(FlywayEndpointAuthFilter.hasAdminRole("   ")).isFalse();
        assertThat(FlywayEndpointAuthFilter.hasAdminRole("ADMIN")).isTrue();
        assertThat(FlywayEndpointAuthFilter.hasAdminRole("admin, user")).isTrue();
        assertThat(FlywayEndpointAuthFilter.hasAdminRole("USER PARENT")).isFalse();
    }
}
