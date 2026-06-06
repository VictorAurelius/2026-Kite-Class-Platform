package com.kitehub.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Tenant header guard — re-inject {@code X-Tenant-Id} from VERIFIED JWT
 * {@code tenantId} claim after the global {@code RemoveRequestHeader=X-Tenant-Id}
 * default-filter strips any client-supplied value.
 *
 * <p>Closes GAP-814 (Wave tenant-domain-1 Bucket A) — cross-tenant IDOR.
 * Previously, gateway only stripped/re-set {@code X-Tenant-Id} inside
 * {@link TenantResolverGatewayFilterFactory} which applies per-route. Routes
 * WITHOUT TenantResolver (public by-token paths, /api/v1/tenants/(id)/landing,
 * staff-invitation accept, etc.) passed CLIENT-spoofed headers straight to
 * core → core's TenantFilterInterceptor trusted spoofed value → reads/writes
 * data of arbitrary tenant.</p>
 *
 * <p>Architecture (post-GAP-814):
 * <pre>
 * Request → default-filter `RemoveRequestHeader=X-Tenant-Id` (strip client value)
 *         → JwtAuthenticationGatewayFilter (order -100) verifies JWT signature
 *           + sets X-User-Id / X-User-Roles / X-User-Email
 *         → TenantHeaderGuardFilter (order -99, this class) re-parses same
 *           JWT (HS512 access key only — NOT HS256 challenge key) and sets
 *           X-Tenant-Id from verified claim
 *         → Route filters (TenantResolver on instance-api routes may further
 *           override X-Tenant-Id from Host subdomain — that wins for legitimate
 *           multi-host setup; defense-in-depth)
 *         → Downstream service trusts X-Tenant-Id as gateway-resolved
 * </pre>
 *
 * <p>Public paths (no JWT expected) get NO {@code X-Tenant-Id} header — public
 * controllers (e.g., {@code /api/v1/tenants/{id}/landing}) MUST derive tenant
 * from path or other server-side evidence, NOT from header. This is by design:
 * the filter cannot prove tenant identity on a public path without a token.</p>
 *
 * <p>Defense-in-depth: HS256 challenge tokens (per {@link JwtAuthenticationGatewayFilter}
 * GAP-705) are NOT honored here. Challenge tokens convey "user X mid-2FA" only;
 * tenant context is not part of their semantic. Using the HS256 key here would
 * also widen attack surface for cross-key confusion. Tenant inject is HS512-
 * access-token-only.</p>
 *
 * @since 1.4.0 (GAP-814 — Wave tenant-domain-1 Bucket A)
 */
@Component
public class TenantHeaderGuardFilter implements GlobalFilter, Ordered {

    /**
     * Filter order — runs AFTER {@link JwtAuthenticationGatewayFilter}
     * (Ordered.LOWEST_PRECEDENCE-2) which sets X-User-* headers, ngay trước
     * NettyRoutingFilter (LOWEST_PRECEDENCE).
     *
     * <p>GAP-916 fix: trước đây Order=-99 (sớm) → default-filter
     * {@code RemoveRequestHeader=X-Tenant-Id} (Order ~0) strip header vừa inject.
     * Bump Order=LOWEST_PRECEDENCE-1 để chạy sau strip + sau JWT filter (-2).</p>
     */
    static final int ORDER = Ordered.LOWEST_PRECEDENCE - 1;

    static final String HEADER_TENANT_ID = "X-Tenant-Id";
    static final String BEARER_PREFIX = "Bearer ";
    static final String TENANT_CLAIM = "tenantId";

    private final SecretKey accessSigningKey;

    public TenantHeaderGuardFilter(@Value("${jwt.secret:${JWT_SECRET:}}") String jwtSecret) {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET (or jwt.secret) is required for TenantHeaderGuardFilter. "
                            + "Must match the JWT_SECRET configured in kitehub-subscription so issued "
                            + "tokens can be validated for tenantId claim extraction.");
        }
        if (jwtSecret.getBytes().length < 64) {
            throw new IllegalStateException(
                    "JWT_SECRET must be ≥64 bytes (512 bits) for HS512. Current length: "
                            + jwtSecret.getBytes().length + " bytes.");
        }
        this.accessSigningKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Public paths skip tenant inject entirely — no JWT expected, no claim
        // to read. Public controllers derive tenant from path/other evidence.
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            // No JWT → no tenant claim to inject. Downstream Spring Security
            // will reject if endpoint requires auth; if endpoint is optional-auth
            // it will see no X-Tenant-Id and behave accordingly (typically
            // returns tenant-anonymous response or 400 TENANT_CONTEXT_MISSING).
            return chain.filter(exchange);
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();

        // HS512 access-token key only — DO NOT attempt HS256 challenge key.
        // Challenge tokens have no tenant scope; mixing keys here would widen
        // attack surface. If parse fails (invalid signature, expired,
        // malformed), simply pass-through without setting header — the prior
        // JwtAuthenticationGatewayFilter (order -100) has already short-
        // circuited bad tokens with 401 on non-2FA paths, so we'd only reach
        // here with: (a) a 2FA-path challenge token that the prior filter
        // accepted via HS256 — skip tenant inject (no claim), (b) valid
        // access token — proceed.
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(accessSigningKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String tenantId = claims.get(TENANT_CLAIM, String.class);
            if (tenantId != null && !tenantId.isBlank()) {
                ServerHttpRequest mutated = request.mutate()
                        .header(HEADER_TENANT_ID, tenantId)
                        .build();
                return chain.filter(exchange.mutate().request(mutated).build());
            }
        } catch (Exception ignored) {
            // Token not parseable with HS512 key (likely HS256 challenge token
            // already accepted by JwtAuthenticationGatewayFilter on 2FA path).
            // Pass-through without tenant header — challenge flow doesn't need it.
        }

        return chain.filter(exchange);
    }

    /**
     * Public paths that bypass tenant inject. Mirrors
     * {@link JwtAuthenticationGatewayFilter#isPublicPath} for the most part,
     * with one addition: {@code /api/v1/tenants/*&#47;landing} is public per
     * gateway route definition (PUBLIC tenant landing page, anonymous visitors).
     *
     * <p>Note: even if a request to a public path carries a valid JWT (e.g.,
     * authenticated user browses landing page of arbitrary tenant), we still
     * DO NOT inject the JWT's tenantId — public endpoints derive tenant from
     * path (e.g., {@code /api/v1/tenants/{id}/landing} → path UUID), not from
     * the requester's authenticated context.</p>
     */
    boolean isPublicPath(String path) {
        return path.startsWith("/api/auth/")
                || path.startsWith("/api/v1/auth/")
                || path.startsWith("/api/v1/staff-invitations/by-token/")
                || (path.startsWith("/api/v1/staff-invitations/") && path.endsWith("/accept"))
                || isPublicTenantLandingPath(path)
                || path.equals("/actuator/health")
                || path.startsWith("/actuator/health/")
                || path.startsWith("/docs/")
                || path.startsWith("/fallback/");
    }

    /**
     * Matches GAP-809 public tenant landing route:
     * {@code /api/v1/tenants/{id}/landing} (anonymous visitors, no JWT).
     */
    private boolean isPublicTenantLandingPath(String path) {
        if (!path.startsWith("/api/v1/tenants/")) {
            return false;
        }
        return path.endsWith("/landing")
                || path.contains("/landing/")
                || path.contains("/landing?");
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
