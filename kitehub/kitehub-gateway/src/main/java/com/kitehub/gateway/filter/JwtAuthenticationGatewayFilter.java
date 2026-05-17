package com.kitehub.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Gateway-edge JWT filter — parses the {@code Authorization: Bearer <token>}
 * header, validates signature via the shared {@code JWT_SECRET}, and propagates
 * the resolved identity to downstream services through {@code X-User-Id},
 * {@code X-User-Roles}, và {@code X-User-Email} request headers.
 *
 * <p>Downstream services (kitehub-subscription, kitehub-admin) tin tưởng các
 * header này thông qua {@code XUserRolesHeaderFilter} (xem
 * {@code com.kitehub.subscription.config.SecurityConfig.XUserRolesHeaderFilter}).
 * Trước Wave 89, gateway KHÔNG set các header này → Spring Security tại downstream
 * thấy SecurityContext rỗng → mọi endpoint {@code @PreAuthorize} reject với 401
 * dù JWT hợp lệ. Closes GAP-604.</p>
 *
 * <p>Public paths bypass filter (login, signup, verify-email, refresh, actuator
 * health, docs) — listed in {@link #isPublicPath(String)}. Invalid JWT → 401
 * short-circuit. Missing Authorization header → pass-through (downstream sẽ
 * reject nếu endpoint cần auth).</p>
 *
 * <p>Order {@code -100} — chạy SỚM, trước CircuitBreaker + RequestRateLimiter
 * filters configured trong {@code application.yml} routes (those have default
 * order ~10000+). Sau filter này, downstream nhận JWT-derived headers thay vì
 * raw Authorization header.</p>
 *
 * @since 1.3.0 (GAP-604 — Wave 89 Bucket A)
 */
@Component
public class JwtAuthenticationGatewayFilter implements GlobalFilter, Ordered {

    /** Filter order — chạy trước CircuitBreaker + RateLimiter route filters. */
    static final int ORDER = -100;

    static final String HEADER_USER_ID = "X-User-Id";
    static final String HEADER_USER_ROLES = "X-User-Roles";
    static final String HEADER_USER_EMAIL = "X-User-Email";
    static final String BEARER_PREFIX = "Bearer ";

    private final SecretKey signingKey;

    public JwtAuthenticationGatewayFilter(@Value("${jwt.secret:${JWT_SECRET:}}") String jwtSecret) {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET (or jwt.secret) is required for kitehub-gateway. "
                            + "Must match the JWT_SECRET configured in kitehub-subscription so issued tokens can be validated.");
        }
        if (jwtSecret.getBytes().length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET must be ≥32 bytes (256 bits) for HS256. Current length: "
                            + jwtSecret.getBytes().length + " bytes.");
        }
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Public paths bypass — downstream tự handle auth (e.g., /api/auth/login issues JWT).
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        // No Authorization header → pass-through. Downstream Spring Security sẽ reject
        // nếu endpoint cần auth; cho phép pass-through để các endpoint optionally-authed
        // vẫn hoạt động.
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return chain.filter(exchange);
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String userId = claims.getSubject();
            String role = claims.get("role", String.class);
            String email = claims.get("email", String.class);

            ServerHttpRequest.Builder mutated = request.mutate();
            if (userId != null) {
                mutated.header(HEADER_USER_ID, userId);
            }
            if (role != null) {
                // Convention: comma-separated nếu future multi-role; hiện tại single role.
                mutated.header(HEADER_USER_ROLES, role);
            }
            if (email != null) {
                mutated.header(HEADER_USER_EMAIL, email);
            }

            return chain.filter(exchange.mutate().request(mutated.build()).build());
        } catch (JwtException | IllegalArgumentException ex) {
            // Invalid/expired/malformed JWT → 401 short-circuit. KHÔNG pass-through
            // vì client sent a Bearer token (intent to authenticate); silent failure
            // tại downstream sẽ confusing.
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    /**
     * Public paths bypass JWT validation. Includes:
     * <ul>
     *   <li>{@code /api/auth/**} — login / refresh / verify-email / password-reset</li>
     *   <li>{@code /api/v1/auth/**} — beta signup + 2FA endpoints (GAP-509/547)</li>
     *   <li>{@code /actuator/health} — Spring Boot health probe</li>
     *   <li>{@code /docs/**} — OpenAPI/Swagger docs nếu serve</li>
     *   <li>{@code /fallback/**} — CircuitBreaker fallback routes</li>
     * </ul>
     */
    boolean isPublicPath(String path) {
        return path.startsWith("/api/auth/")
                || path.startsWith("/api/v1/auth/")
                || path.equals("/actuator/health")
                || path.startsWith("/actuator/health/")
                || path.startsWith("/docs/")
                || path.startsWith("/fallback/");
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
