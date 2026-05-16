package com.kitehub.subscription.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link MagicLinkCacheControlInterceptor} verifying that origin
 * Cache-Control + Pragma + Expires headers are set on every intercepted request.
 *
 * <p>Closes GAP-584 AC#2 (Wave 86) — origin defense-in-depth pairing the edge
 * layer Cloudflare Page Rule cache-bypass.</p>
 */
@DisplayName("MagicLinkCacheControlInterceptor — Wave 86 GAP-584 AC#2")
class MagicLinkCacheControlInterceptorTest {

    private final MagicLinkCacheControlInterceptor interceptor = new MagicLinkCacheControlInterceptor();

    @Test
    @DisplayName("preHandle sets Cache-Control: no-store header (closes GAP-584 AC#2)")
    void preHandleSetsCacheControlNoStore() {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse resp = Mockito.mock(HttpServletResponse.class);

        boolean proceed = interceptor.preHandle(req, resp, new Object());

        assertThat(proceed).isTrue();
        verify(resp).setHeader("Cache-Control",
                "no-store, no-cache, max-age=0, must-revalidate");
    }

    @Test
    @DisplayName("preHandle sets Pragma: no-cache (legacy proxy compat)")
    void preHandleSetsPragmaNoCache() {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse resp = Mockito.mock(HttpServletResponse.class);

        interceptor.preHandle(req, resp, new Object());

        verify(resp).setHeader("Pragma", "no-cache");
    }

    @Test
    @DisplayName("preHandle sets Expires: 0 (HTTP/1.0 compat)")
    void preHandleSetsExpiresZero() {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse resp = Mockito.mock(HttpServletResponse.class);

        interceptor.preHandle(req, resp, new Object());

        verify(resp).setHeader("Expires", "0");
    }
}
