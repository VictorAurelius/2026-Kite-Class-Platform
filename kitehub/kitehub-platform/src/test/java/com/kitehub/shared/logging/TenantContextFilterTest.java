package com.kitehub.shared.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TenantContextFilterTest {

    private TenantContextFilter filter;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new TenantContextFilter();
        response = new MockHttpServletResponse();
        MDC.clear();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void populatesMdcFromHeaders() throws ServletException, IOException {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader(TenantContextFilter.HDR_TENANT_ID, "tenant-abc");
        req.addHeader(TenantContextFilter.HDR_USER_ID, "user-123");
        req.addHeader(TenantContextFilter.HDR_TRACE_ID, "trace-xyz");
        req.addHeader(TenantContextFilter.HDR_REQUEST_ID, "req-1");

        AtomicReference<String> tenantInside = new AtomicReference<>();
        AtomicReference<String> userInside = new AtomicReference<>();
        FilterChain chain = (rq, rs) -> {
            tenantInside.set(MDC.get(TenantContextFilter.MDC_TENANT_ID));
            userInside.set(MDC.get(TenantContextFilter.MDC_USER_ID));
        };

        filter.doFilter(req, response, chain);

        assertThat(tenantInside.get()).isEqualTo("tenant-abc");
        assertThat(userInside.get()).isEqualTo("user-123");
        assertThat(MDC.get(TenantContextFilter.MDC_TENANT_ID)).isNull();
        assertThat(MDC.get(TenantContextFilter.MDC_USER_ID)).isNull();
        assertThat(MDC.get(TenantContextFilter.MDC_TRACE_ID)).isNull();
        assertThat(response.getHeader(TenantContextFilter.HDR_TRACE_ID)).isEqualTo("trace-xyz");
    }

    @Test
    void synthesizesTraceIdWhenAbsent() throws ServletException, IOException {
        MockHttpServletRequest req = new MockHttpServletRequest();

        AtomicReference<String> traceInside = new AtomicReference<>();
        AtomicReference<String> requestInside = new AtomicReference<>();
        FilterChain chain = (rq, rs) -> {
            traceInside.set(MDC.get(TenantContextFilter.MDC_TRACE_ID));
            requestInside.set(MDC.get(TenantContextFilter.MDC_REQUEST_ID));
        };

        filter.doFilter(req, response, chain);

        assertThat(traceInside.get()).isNotBlank().doesNotContain("-");
        assertThat(requestInside.get()).startsWith("req-");
        assertThat(response.getHeader(TenantContextFilter.HDR_TRACE_ID)).isNotBlank();
    }

    @Test
    void parsesTraceparentHeader() throws ServletException, IOException {
        MockHttpServletRequest req = new MockHttpServletRequest();
        // version-traceId-spanId-flags
        req.addHeader(TenantContextFilter.HDR_TRACEPARENT,
            "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01");

        AtomicReference<String> traceInside = new AtomicReference<>();
        FilterChain chain = (rq, rs) -> traceInside.set(MDC.get(TenantContextFilter.MDC_TRACE_ID));
        filter.doFilter(req, response, chain);
        assertThat(traceInside.get()).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
    }

    @Test
    void fallsBackToSecurityContextUsername() throws ServletException, IOException {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader(TenantContextFilter.HDR_TENANT_ID, "tenant-x");
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("alice", null, java.util.List.of())
        );

        AtomicReference<String> userInside = new AtomicReference<>();
        FilterChain chain = (rq, rs) -> userInside.set(MDC.get(TenantContextFilter.MDC_USER_ID));
        filter.doFilter(req, response, chain);
        assertThat(userInside.get()).isEqualTo("alice");
    }

    @Test
    void clearsMdcEvenIfChainThrows() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader(TenantContextFilter.HDR_TENANT_ID, "tenant-throw");
        FilterChain chain = (rq, rs) -> {
            throw new IOException("boom");
        };
        try {
            filter.doFilter(req, response, chain);
        } catch (Exception ignored) {
            // expected
        }
        assertThat(MDC.get(TenantContextFilter.MDC_TENANT_ID)).isNull();
    }
}
