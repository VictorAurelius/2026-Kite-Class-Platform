package com.kitehub.subscription.auth.twofactor;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Bridges short-lived HS256 challenge tokens (issued by {@link ChallengeTokenService}
 * during the 2FA dance) into a Spring {@link UsernamePasswordAuthenticationToken}
 * with the reserved authority {@code ROLE_CHALLENGE}. Active ONLY on the explicit
 * 2FA path allowlist; all other paths bypass this filter so regular gateway-issued
 * {@code X-User-Id} / {@code X-User-Roles} headers continue to drive authentication
 * via {@code XUserRolesHeaderFilter}.
 *
 * <p>Closes GAP-706 — without this filter, {@code POST /api/v1/auth/2fa/enroll-init}
 * with a Bearer challenge token (HS256) reaches Spring Security with an empty
 * {@code SecurityContext} → {@code 401 Unauthorized}.</p>
 *
 * <p>Defense-in-depth pairing with {@link com.kitehub.gateway} side (GAP-705):
 * the gateway enforces the same path scope before propagating
 * {@code X-User-Roles=CHALLENGE}; this filter is the local fallback when
 * subscription is reached directly (dev environment) OR when the gateway
 * propagates the {@code X-User-Id=...}/{@code X-User-Roles=CHALLENGE} pair AND
 * the Bearer token survives as the source of truth.</p>
 *
 * <p>Filter chain ordering: registered BEFORE {@code XUserRolesHeaderFilter}
 * (see {@link com.kitehub.subscription.config.SecurityConfig}). If this filter
 * sets an Authentication, the header-based filter sees a populated context and
 * does not overwrite (per its no-op-when-set semantics).</p>
 *
 * @since 1.0.0 (Wave 104 Bucket C / GAP-706)
 */
public class ChallengeTokenAuthenticationFilter extends OncePerRequestFilter {

    /** Reserved authority granted to verified challenge tokens. */
    public static final String ROLE_CHALLENGE = "ROLE_CHALLENGE";

    /** Bearer prefix on the {@code Authorization} header. */
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * Path allowlist — mirrors the gateway-side {@code isChallenge2faPath}
     * matchers. Tokens never authenticate outside this scope (defense-in-depth
     * vs access-token / challenge-token confusion attacks).
     */
    private static final String[] CHALLENGE_PATHS = {
            "/api/v1/auth/2fa/enroll-init",
            "/api/v1/auth/2fa/enroll-confirm",
            "/api/v1/auth/2fa/verify",
            "/api/v1/auth/2fa/setup",
            "/api/auth/2fa/enroll-init",
            "/api/auth/2fa/enroll-confirm",
            "/api/auth/2fa/verify",
            "/api/auth/2fa/setup",
    };

    private final ChallengeTokenService challengeTokenService;

    public ChallengeTokenAuthenticationFilter(ChallengeTokenService challengeTokenService) {
        this.challengeTokenService = challengeTokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!isChallengePath(path)) {
            chain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            // No Bearer → no auth set; downstream Spring Security will 401.
            chain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        try {
            ChallengeTokenService.Verified verified = challengeTokenService.verify(token);
            List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(ROLE_CHALLENGE));
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    verified.getUserId().toString(), null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (ChallengeTokenService.ChallengeTokenException ex) {
            // Invalid / expired challenge token → leave context empty; Spring
            // Security entry-point returns 401. Do NOT short-circuit here so
            // the controller-level error response stays consistent.
        } catch (Exception ex) {
            // Defensive: any unexpected verifier failure also yields 401 via
            // empty context; never propagate.
        }

        chain.doFilter(request, response);
    }

    private static boolean isChallengePath(String path) {
        if (path == null) {
            return false;
        }
        for (String p : CHALLENGE_PATHS) {
            if (path.equals(p) || path.startsWith(p + "/")) {
                return true;
            }
        }
        return false;
    }
}
