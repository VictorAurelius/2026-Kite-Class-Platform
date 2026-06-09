package com.kitehub.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
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
 * {@code X-User-Roles}, {@code X-User-Email}, và {@code X-Subscription-Tier}
 * (GAP-1020 — trusted tier for AI branding entitlement) request headers.
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
 * <p>Order {@code LOWEST_PRECEDENCE-2} — chạy SAU default-filter
 * {@code RemoveRequestHeader=X-User-Id} (Order ~0) để header inject không bị strip
 * downstream. NettyRoutingFilter ở LOWEST_PRECEDENCE chạy SAU filter này, forward
 * request đã mutate xong tới subscription. TenantHeaderGuardFilter ở
 * LOWEST_PRECEDENCE-1 chạy ngay sau, inject X-Tenant-Id cùng nguyên lý.</p>
 *
 * @since 1.3.0 (GAP-604 — Wave 89 Bucket A)
 * @since 1.4.0 (GAP-916 — Wave flow-kh2 Bucket B: Order LOWEST_PRECEDENCE-2 fix để
 *               header inject không bị default-filter RemoveRequestHeader strip)
 */
@Slf4j
@Component
public class JwtAuthenticationGatewayFilter implements GlobalFilter, Ordered {

    /**
     * Filter order — chạy SAU default-filter {@code RemoveRequestHeader=X-User-Id}
     * (Order ~0) để header inject KHÔNG bị strip downstream.
     *
     * <p>GAP-916 fix (Wave flow-kh2 walk 2026-06-03): trước đây Order=-100 chạy SỚM
     * → default-filter sau strip X-User-Id vừa inject → subscription nhận empty headers
     * → 401. Bump Order=Ordered.LOWEST_PRECEDENCE-2 để chạy ngay trước NettyRoutingFilter
     * (LOWEST_PRECEDENCE) sau khi mọi strip + route filter xong. TenantHeaderGuardFilter
     * cũng cần bump (Order=LOWEST_PRECEDENCE-1).</p>
     */
    static final int ORDER = Ordered.LOWEST_PRECEDENCE - 2;

    static final String HEADER_USER_ID = "X-User-Id";
    static final String HEADER_USER_ROLES = "X-User-Roles";
    static final String HEADER_USER_EMAIL = "X-User-Email";
    /**
     * Numeric domain reference id (parents.id / teachers.id / students.id) for
     * kiteclass-core reference-id authz (GAP-798). Present only on KC-native tokens
     * (Wave auth-1). Client-supplied value is stripped by {@code default-filters}
     * {@code RemoveRequestHeader=X-User-Reference-Id} before this re-injects the
     * verified claim — same anti-spoof pattern as X-User-Id.
     */
    static final String HEADER_USER_REFERENCE_ID = "X-User-Reference-Id";
    /**
     * Trusted subscription tier (GAP-1020). Re-injected from the verified {@code tier}
     * JWT claim issued by kitehub-subscription so tier-gated features (AI branding) cannot
     * be spoofed via a client-supplied header. Client-supplied value is stripped by
     * {@code default-filters} {@code RemoveRequestHeader=X-Subscription-Tier} before this
     * re-injects the verified claim — same anti-spoof pattern as X-User-Id / X-Tenant-Id.
     * Defaults to {@code FREE} (least privilege) for backward-compat tokens issued before
     * the tier claim landed, and for challenge tokens (2FA paths do not read tier).
     */
    static final String HEADER_SUBSCRIPTION_TIER = "X-Subscription-Tier";
    /** Least-privilege tier fallback when the verified token carries no {@code tier} claim. */
    static final String DEFAULT_TIER = "FREE";
    static final String BEARER_PREFIX = "Bearer ";

    /** Reserved role propagated for verified HS256 challenge tokens on 2FA paths. */
    static final String CHALLENGE_ROLE = "CHALLENGE";

    private final SecretKey accessSigningKey;
    private final SecretKey challengeSigningKey;

    public JwtAuthenticationGatewayFilter(
            @Value("${jwt.secret:${JWT_SECRET:}}") String jwtSecret,
            @Value("${jwt.challenge-secret:${JWT_CHALLENGE_SECRET:}}") String challengeSecret) {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET (or jwt.secret) is required for kitehub-gateway. "
                            + "Must match the JWT_SECRET configured in kitehub-subscription so issued tokens can be validated.");
        }
        if (jwtSecret.getBytes().length < 64) {
            throw new IllegalStateException(
                    "JWT_SECRET must be ≥64 bytes (512 bits) for HS512. Current length: "
                            + jwtSecret.getBytes().length + " bytes.");
        }
        this.accessSigningKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());

        // Challenge secret is optional in dev; if absent, gateway cannot route challenge
        // tokens — 2FA paths will fail to authenticate. Production MUST set
        // JWT_CHALLENGE_SECRET to match kitehub-subscription's jwt.challenge-secret.
        if (challengeSecret == null || challengeSecret.isBlank()) {
            this.challengeSigningKey = null;
        } else {
            byte[] raw = challengeSecret.getBytes();
            if (raw.length < 32) {
                byte[] padded = new byte[32];
                System.arraycopy(raw, 0, padded, 0, raw.length);
                raw = padded;
            }
            this.challengeSigningKey = Keys.hmacShaKeyFor(raw);
        }
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        boolean challenge2faPath = isChallenge2faPath(path);

        // Public paths bypass — downstream tự handle auth (e.g., /api/auth/login issues JWT).
        // 2FA challenge paths are CARVED OUT here (they live under /api/v1/auth/* but
        // need filter scrutiny to bridge challenge tokens to X-User-Id headers).
        if (!challenge2faPath && isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        // Resolve the bearer token from EITHER the Authorization header (default for
        // XHR/fetch) OR a `?token=` query param. GAP-1021: browser EventSource cannot
        // set request headers, so SSE endpoints (e.g. AI Branding deploy-stream)
        // authenticate via short-lived token-in-query. Header takes precedence; the
        // query token is a fallback only when no Bearer header is present.
        String token = null;
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            token = authHeader.substring(BEARER_PREFIX.length()).trim();
        } else {
            String queryToken = request.getQueryParams().getFirst("token");
            if (queryToken != null && !queryToken.isBlank()) {
                token = queryToken.trim();
            }
        }

        // No usable token → pass-through. Downstream Spring Security sẽ reject nếu
        // endpoint cần auth; cho phép pass-through để các endpoint optionally-authed
        // vẫn hoạt động.
        if (token == null) {
            return chain.filter(exchange);
        }

        // GAP-705: dual-secret parse. Try the HS512 access-token key first (covers >99%
        // of traffic). If verification fails AND we're on a 2FA challenge path, retry
        // with the HS256 challenge-secret key. Challenge tokens MUST never authenticate
        // on non-2FA paths — that is the defense-in-depth guard against access-token /
        // challenge-token confusion attacks (separate secret namespace is the whole
        // point of having two keys).
        Claims claims;
        boolean isChallenge;
        try {
            claims = Jwts.parser()
                    .verifyWith(accessSigningKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            isChallenge = false;
        } catch (Exception accessEx) {
            log.debug("Access JWT parse failed path={} exClass={} msg={}",
                    path, accessEx.getClass().getSimpleName(), accessEx.getMessage());
            if (!challenge2faPath || challengeSigningKey == null) {
                // Access-key parse failed on a NON-2FA path (or challenge key not
                // configured) → 401. KHÔNG pass-through vì client sent a Bearer token
                // (intent to authenticate); silent failure tại downstream sẽ confusing.
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
            try {
                claims = Jwts.parser()
                        .verifyWith(challengeSigningKey)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();
            } catch (Exception challengeEx) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
            // Sanity: challenge token must self-declare type=challenge.
            String typeClaim = claims.get("type", String.class);
            if (!"challenge".equals(typeClaim)) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
            isChallenge = true;
        }

        String userId = claims.getSubject();
        String role = claims.get("role", String.class);
        String email = claims.get("email", String.class);

        ServerHttpRequest.Builder mutated = request.mutate();
        if (userId != null) {
            mutated.header(HEADER_USER_ID, userId);
        }
        if (isChallenge) {
            // Challenge tokens convey only "I am user X mid-2FA"; propagate the
            // reserved CHALLENGE role for subscription role-guard to recognise.
            mutated.header(HEADER_USER_ROLES, CHALLENGE_ROLE);
        } else if (role != null) {
            // Convention: comma-separated nếu future multi-role; hiện tại single role.
            mutated.header(HEADER_USER_ROLES, role);
        }
        if (email != null) {
            mutated.header(HEADER_USER_EMAIL, email);
        }
        // GAP-1020 — inject trusted X-Subscription-Tier from the verified `tier` claim.
        // Always set (even for challenge / claim-absent tokens) so the stripped client value
        // is unconditionally replaced by a trusted one on every authed request; downstream
        // tier-gated features (AI branding) read this header. Default FREE = least privilege.
        String tier = claims.get("tier", String.class);
        mutated.header(HEADER_SUBSCRIPTION_TIER, tier != null ? tier : DEFAULT_TIER);
        if (!isChallenge) {
            // KC-native tokens (Wave auth-1) carry referenceId = parents/teachers/students.id
            // for kiteclass-core reference-id authz (GAP-798). Absent on OWNER/STAFF tokens.
            Object referenceId = claims.get("referenceId");
            if (referenceId != null) {
                mutated.header(HEADER_USER_REFERENCE_ID, String.valueOf(referenceId));
            }
        }

        log.debug("Injected X-User-Id={} X-User-Roles={} X-Subscription-Tier={} isChallenge={} path={}",
                userId, isChallenge ? CHALLENGE_ROLE : role, tier != null ? tier : DEFAULT_TIER, isChallenge, path);
        return chain.filter(exchange.mutate().request(mutated.build()).build());
    }

    /**
     * Paths that legitimately accept HS256 challenge tokens (issued by
     * {@code ChallengeTokenService} after password-but-pre-2FA login). Non-2FA paths
     * never honor challenge tokens — see {@link #filter} for the defense-in-depth
     * scope guard.
     *
     * <p>Closes GAP-705. The set mirrors the subscription-side
     * {@code ChallengeTokenAuthenticationFilter} matchers so both layers agree on
     * where challenge tokens are valid.</p>
     */
    boolean isChallenge2faPath(String path) {
        return path.startsWith("/api/v1/auth/2fa/")
                || path.startsWith("/api/auth/2fa/");
    }

    /**
     * Public paths bypass JWT validation. Includes:
     * <ul>
     *   <li>{@code /api/auth/**} — login / refresh / verify-email / password-reset</li>
     *   <li>{@code /api/v1/auth/**} — beta signup + 2FA endpoints (GAP-509/547)</li>
     *   <li>{@code /api/v1/staff-invitations/by-token/**} — recipient preview invite (no JWT yet)</li>
     *   <li>{@code /api/v1/staff-invitations/*&#47;accept} — recipient accepts + sets password (no JWT yet)</li>
     *   <li>{@code /api/platform/webhooks/**} — vendor payment webhooks (SePay) authenticate
     *       via {@code Authorization: Apikey <key>}, NOT a JWT. Without whitelisting, this
     *       filter would 401 the Apikey-only request before it reaches the subscription
     *       service. Apikey verification happens server-side in PaymentWebhookController
     *       (Wave flow-kh3-2/3 — GAP-976). See api-contract.md UC-SUB-08.</li>
     *   <li>{@code /actuator/health} — Spring Boot health probe</li>
     *   <li>{@code /docs/**} — OpenAPI/Swagger docs nếu serve</li>
     *   <li>{@code /fallback/**} — CircuitBreaker fallback routes</li>
     * </ul>
     *
     * <p>Bug #18 (Wave A Bucket B walk 2026-05-28): staff invitation accept paths
     * are designed public per controller javadoc ("Recipient accepts invitation
     * + sets password (public)") but were not whitelisted here. Surfaced during
     * RST walk per {@code feature-ship-runtime-walk-mandate.md} §3 — recipient
     * has no JWT yet, would receive 401 from this filter before reaching
     * backend. Same incident-class as Wave meta-6 Bug #16 (gateway public-path
     * gap).</p>
     */
    boolean isPublicPath(String path) {
        return path.startsWith("/api/auth/")
                || path.startsWith("/api/v1/auth/")
                || path.startsWith("/api/v1/staff-invitations/by-token/")
                || (path.startsWith("/api/v1/staff-invitations/") && path.endsWith("/accept"))
                || path.startsWith("/api/platform/webhooks/")
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
