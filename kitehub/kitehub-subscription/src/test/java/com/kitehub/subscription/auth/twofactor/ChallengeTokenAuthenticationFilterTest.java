package com.kitehub.subscription.auth.twofactor;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ChallengeTokenAuthenticationFilter} (GAP-706).
 *
 * <p>Coverage of all four required cases:</p>
 * <ol>
 *   <li>Valid challenge token on 2FA path → Authentication set with ROLE_CHALLENGE</li>
 *   <li>Invalid challenge token on 2FA path → 401-equivalent (empty context, chain continues)</li>
 *   <li>Expired challenge token on 2FA path → empty context</li>
 *   <li>Valid token but on non-2FA path → filter bypass, no Authentication set</li>
 * </ol>
 */
@DisplayName("ChallengeTokenAuthenticationFilter (GAP-706)")
class ChallengeTokenAuthenticationFilterTest {

    private ChallengeTokenService challengeTokenService;
    private ChallengeTokenAuthenticationFilter filter;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        challengeTokenService = mock(ChallengeTokenService.class);
        filter = new ChallengeTokenAuthenticationFilter(challengeTokenService);
        chain = mock(FilterChain.class);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Valid challenge token on 2FA path → Authentication with ROLE_CHALLENGE")
    void validChallengeOn2faPath_setsAuthentication() throws ServletException, IOException {
        UUID userId = UUID.randomUUID();
        ChallengeTokenService.Verified verified = mock(ChallengeTokenService.Verified.class);
        when(verified.getUserId()).thenReturn(userId);
        when(challengeTokenService.verify("good-token")).thenReturn(verified);

        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/auth/2fa/verify");
        req.addHeader(HttpHeaders.AUTHORIZATION, "Bearer good-token");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        filter.doFilter(req, resp, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).as("ROLE_CHALLENGE Authentication MUST be set").isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo(userId.toString());
        assertThat(auth.getAuthorities()).extracting("authority")
                .containsExactly(ChallengeTokenAuthenticationFilter.ROLE_CHALLENGE);
        verify(chain, times(1)).doFilter(req, resp);
    }

    @Test
    @DisplayName("Invalid challenge token on 2FA path → empty context (Spring 401)")
    void invalidChallengeOn2faPath_leavesContextEmpty() throws ServletException, IOException {
        when(challengeTokenService.verify("bad-token"))
                .thenThrow(new ChallengeTokenService.ChallengeTokenException(
                        ChallengeTokenService.FailureReason.INVALID, "bad"));

        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/auth/2fa/enroll-init");
        req.addHeader(HttpHeaders.AUTHORIZATION, "Bearer bad-token");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        filter.doFilter(req, resp, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .as("invalid token MUST NOT populate Authentication").isNull();
        // Filter does not short-circuit; downstream Spring Security entry-point returns 401.
        verify(chain, times(1)).doFilter(req, resp);
    }

    @Test
    @DisplayName("Expired challenge token on 2FA path → empty context")
    void expiredChallengeOn2faPath_leavesContextEmpty() throws ServletException, IOException {
        when(challengeTokenService.verify("expired-token"))
                .thenThrow(new ChallengeTokenService.ChallengeTokenException(
                        ChallengeTokenService.FailureReason.EXPIRED, "expired"));

        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/auth/2fa/enroll-confirm");
        req.addHeader(HttpHeaders.AUTHORIZATION, "Bearer expired-token");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        filter.doFilter(req, resp, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain, times(1)).doFilter(req, resp);
    }

    @Test
    @DisplayName("Valid token but non-2FA path → filter bypasses verify, no Authentication")
    void validTokenOnNon2faPath_doesNotInvokeService() throws ServletException, IOException {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/admin/beta-requests");
        req.addHeader(HttpHeaders.AUTHORIZATION, "Bearer some-token");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        filter.doFilter(req, resp, chain);

        // verify(...) MUST NOT be called — filter skipped on non-2FA path
        Mockito.verifyNoInteractions(challengeTokenService);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain, times(1)).doFilter(req, resp);
    }

    @Test
    @DisplayName("Missing Authorization header on 2FA path → bypass verify, chain continues")
    void missingAuthHeaderOn2faPath_bypasses() throws ServletException, IOException {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/auth/2fa/verify");
        // no Authorization header
        MockHttpServletResponse resp = new MockHttpServletResponse();

        filter.doFilter(req, resp, chain);

        Mockito.verifyNoInteractions(challengeTokenService);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain, times(1)).doFilter(req, resp);
    }

    @Test
    @DisplayName("/api/auth/2fa/** legacy path also recognised")
    void legacy2faPath_recognised() throws ServletException, IOException {
        UUID userId = UUID.randomUUID();
        ChallengeTokenService.Verified verified = mock(ChallengeTokenService.Verified.class);
        when(verified.getUserId()).thenReturn(userId);
        when(challengeTokenService.verify(any())).thenReturn(verified);

        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/2fa/verify");
        req.addHeader(HttpHeaders.AUTHORIZATION, "Bearer good-token");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        filter.doFilter(req, resp, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .as("legacy /api/auth/2fa/** path MUST also bridge challenge token").isNotNull();
    }
}
