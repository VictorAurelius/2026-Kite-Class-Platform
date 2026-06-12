package com.kitehub.branding.wizard.sse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Establishes Spring authentication for browser {@code EventSource} (SSE) connections from a
 * short-lived {@code ?access_token=} query param (GAP-1021 part 2). Runs only on the SSE
 * endpoints ({@code .../jobs/{jobId}/deploy-stream} + {@code .../jobs/{jobId}/preview}) where
 * a browser cannot send the gateway {@code X-User-*} headers.
 *
 * <p>A valid token (minted by {@link SseTokenService} from an already-authenticated fetch) is
 * verified + bound to the path jobId, then translated into the same {@code ROLE_<role>}
 * authorities the {@code XUserRolesHeaderFilter} would have produced — so the downstream
 * {@code authenticated()} authorization passes for that one request. Invalid/missing tokens
 * are a no-op (default-deny then rejects with 401).</p>
 *
 * <p>{@code shouldNotFilterAsyncDispatch=false} + {@code shouldNotFilterErrorDispatch=false}
 * because SSE responses trigger a servlet ASYNC dispatch; without re-running, the token-derived
 * auth would be lost on re-dispatch → 401 (same pattern as {@code XUserRolesHeaderFilter}).</p>
 *
 * @since GAP-1021 (Wave branding-100 Bucket C)
 */
public class SseQueryTokenAuthFilter extends OncePerRequestFilter {

    private static final Pattern SSE_PATH = Pattern.compile(
            "/api/v1/branding/jobs/([0-9a-fA-F-]{36})/(deploy-stream|preview)$");

    private final SseTokenService tokenService;

    public SseQueryTokenAuthFilter(SseTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        // Only act when no auth is already set (gateway header path takes precedence).
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = request.getParameter("access_token");
            if (token != null && !token.isBlank()) {
                Matcher m = SSE_PATH.matcher(request.getRequestURI());
                if (m.find()) {
                    UUID jobId = parseUuid(m.group(1));
                    if (jobId != null) {
                        tokenService.verify(token, jobId).ifPresent(auth -> {
                            List<GrantedAuthority> authorities = toAuthorities(auth.roles());
                            UsernamePasswordAuthenticationToken springAuth =
                                    new UsernamePasswordAuthenticationToken(
                                            auth.userId() == null ? "anonymous" : auth.userId(),
                                            null, authorities);
                            SecurityContextHolder.getContext().setAuthentication(springAuth);
                        });
                    }
                }
            }
        }
        chain.doFilter(request, response);
    }

    private static List<GrantedAuthority> toAuthorities(String rolesCsv) {
        if (rolesCsv == null || rolesCsv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(rolesCsv.split(","))
                .map(String::trim)
                .filter(r -> !r.isEmpty())
                .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    private static UUID parseUuid(String s) {
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }
}
