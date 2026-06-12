package com.kitehub.gateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link JwtAuthenticationGatewayFilter} (GAP-604 — Wave 89 Bucket A).
 *
 * <p>5 cases bắt buộc per Wave 89 Bucket A spec:</p>
 * <ol>
 *   <li>Valid JWT → headers X-User-Id + X-User-Roles + X-User-Email được set,
 *       request pass-through downstream</li>
 *   <li>Expired JWT → 401 short-circuit, không pass downstream</li>
 *   <li>Missing Authorization header (private path) → pass-through (downstream
 *       Spring Security reject)</li>
 *   <li>Malformed JWT (invalid signature / không đủ 3 segments) → 401 short-circuit</li>
 *   <li>Public path bypass — không cần JWT, không set headers, pass-through</li>
 * </ol>
 *
 * <p>Pattern mirrors {@code SecurityHeadersFilterTest} (Reactor StepVerifier +
 * MockServerWebExchange — no Spring Boot context boot for unit-test speed).</p>
 */
@DisplayName("JwtAuthenticationGatewayFilter (kitehub-gateway / GAP-604)")
class JwtAuthenticationGatewayFilterTest {

    /** 66-byte test secret — ≥512 bits, exercises real HS512 like prod (GAP-1012). */
    private static final String TEST_SECRET = "test-secret-64-bytes-minimum-for-hs512-access-token-key-abcdefghij";

    /** Separate 32-byte challenge secret — HS256, distinct from access-token key. */
    private static final String TEST_CHALLENGE_SECRET = "challenge-secret-32-bytes-min-pad";

    private JwtAuthenticationGatewayFilter filter;
    private SecretKey signingKey;
    private SecretKey challengeKey;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationGatewayFilter(TEST_SECRET, TEST_CHALLENGE_SECRET);
        signingKey = Keys.hmacShaKeyFor(TEST_SECRET.getBytes());
        challengeKey = Keys.hmacShaKeyFor(TEST_CHALLENGE_SECRET.getBytes());
    }

    @Test
    @DisplayName("Case 1: valid JWT → propagate X-User-Id + X-User-Roles + X-User-Email headers")
    void validJwtSetsDownstreamHeaders() {
        String validJwt = Jwts.builder()
                .subject("user-abc-123")
                .claim("role", "PLATFORM_ADMIN")
                .claim("email", "admin@kitehub.me")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(signingKey)
                .compact();

        MockServerHttpRequest req = MockServerHttpRequest
                .get("http://localhost/api/v1/admin/beta-requests")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(req);
        CapturingChain chain = new CapturingChain();

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        // Chain phải được invoke (request không bị short-circuit)
        assertThat(chain.captured).isNotNull();
        ServerHttpRequest mutated = chain.captured.getRequest();
        // Reference exchange.getResponse() instead of casting captured (mutate chain returns decorator)
        // OK to inspect chain.captured.getRequest() — that's the mutated request from exchange.mutate().request(...)
        assertThat(mutated.getHeaders().getFirst(JwtAuthenticationGatewayFilter.HEADER_USER_ID))
                .isEqualTo("user-abc-123");
        assertThat(mutated.getHeaders().getFirst(JwtAuthenticationGatewayFilter.HEADER_USER_ROLES))
                .isEqualTo("PLATFORM_ADMIN");
        assertThat(mutated.getHeaders().getFirst(JwtAuthenticationGatewayFilter.HEADER_USER_EMAIL))
                .isEqualTo("admin@kitehub.me");
        // Response status không bị set 401 (filter pass-through)
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    @DisplayName("GAP-1020: valid JWT with tier claim → inject trusted X-Subscription-Tier header")
    void validJwtInjectsTrustedSubscriptionTier() {
        String validJwt = Jwts.builder()
                .subject("owner-xyz-789")
                .claim("role", "OWNER")
                .claim("email", "owner@skyedu.vn")
                .claim("tier", "PREMIUM")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(signingKey)
                .compact();

        // Client tries to spoof a higher tier — gateway must overwrite with the verified claim.
        MockServerHttpRequest req = MockServerHttpRequest
                .post("http://localhost/api/platform/branding/ai/generate-theme")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt)
                .header(JwtAuthenticationGatewayFilter.HEADER_SUBSCRIPTION_TIER, "ENTERPRISE")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(req);
        CapturingChain chain = new CapturingChain();

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(chain.captured).isNotNull();
        ServerHttpRequest mutated = chain.captured.getRequest();
        assertThat(mutated.getHeaders().getFirst(JwtAuthenticationGatewayFilter.HEADER_SUBSCRIPTION_TIER))
                .as("Gateway MUST inject the verified tier claim, overriding any client-supplied value")
                .isEqualTo("PREMIUM");
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    @DisplayName("GAP-1020: valid JWT WITHOUT tier claim (legacy token) → default X-Subscription-Tier=FREE")
    void legacyJwtWithoutTierClaimDefaultsToFree() {
        // Backward-compat: token issued before the tier claim landed (e.g. test-8 pre-V68).
        String legacyJwt = Jwts.builder()
                .subject("owner-legacy-456")
                .claim("role", "OWNER")
                .claim("email", "legacy@skyedu.vn")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(signingKey)
                .compact();

        MockServerHttpRequest req = MockServerHttpRequest
                .post("http://localhost/api/platform/branding/ai/generate-theme")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + legacyJwt)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(req);
        CapturingChain chain = new CapturingChain();

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(chain.captured).isNotNull();
        ServerHttpRequest mutated = chain.captured.getRequest();
        assertThat(mutated.getHeaders().getFirst(JwtAuthenticationGatewayFilter.HEADER_SUBSCRIPTION_TIER))
                .as("Token with no tier claim → least-privilege FREE default")
                .isEqualTo(JwtAuthenticationGatewayFilter.DEFAULT_TIER);
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    @DisplayName("Case 2: expired JWT → 401 short-circuit, không pass downstream")
    void expiredJwtReturns401() {
        String expiredJwt = Jwts.builder()
                .subject("user-expired")
                .claim("role", "TENANT_OWNER")
                .issuedAt(Date.from(Instant.now().minusSeconds(7200)))
                .expiration(Date.from(Instant.now().minusSeconds(3600))) // expired 1h ago
                .signWith(signingKey)
                .compact();

        MockServerHttpRequest req = MockServerHttpRequest
                .get("http://localhost/api/v1/admin/instances")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredJwt)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(req);
        CapturingChain chain = new CapturingChain();

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(chain.captured).as("expired JWT should NOT reach downstream chain").isNull();
    }

    @Test
    @DisplayName("Case 3: missing Authorization header (private path) → pass-through, no headers set")
    void missingAuthHeaderPassesThrough() {
        MockServerHttpRequest req = MockServerHttpRequest
                .get("http://localhost/api/v1/admin/payments")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(req);
        CapturingChain chain = new CapturingChain();

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        // Pass-through — downstream Spring Security sẽ reject (filter không short-circuit)
        assertThat(chain.captured).isNotNull();
        ServerHttpRequest forwarded = chain.captured.getRequest();
        assertThat(forwarded.getHeaders().getFirst(JwtAuthenticationGatewayFilter.HEADER_USER_ID))
                .isNull();
        assertThat(forwarded.getHeaders().getFirst(JwtAuthenticationGatewayFilter.HEADER_USER_ROLES))
                .isNull();
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    @DisplayName("Case 4: malformed JWT (invalid signature) → 401 short-circuit")
    void malformedJwtReturns401() {
        // Token signed bằng key KHÁC → signature verify fail
        SecretKey wrongKey = Keys.hmacShaKeyFor("wrong-secret-32-bytes-minimum-aaaa".getBytes());
        String badJwt = Jwts.builder()
                .subject("user-bad")
                .claim("role", "PLATFORM_ADMIN")
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(wrongKey)
                .compact();

        MockServerHttpRequest req = MockServerHttpRequest
                .get("http://localhost/api/v1/admin/revenue")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + badJwt)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(req);
        CapturingChain chain = new CapturingChain();

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(chain.captured).as("invalid-signature JWT should NOT reach downstream").isNull();
    }

    @Test
    @DisplayName("Case 5: public path bypass — /api/auth/login không cần JWT, không set headers")
    void publicPathBypassesFilter() {
        MockServerHttpRequest req = MockServerHttpRequest
                .post("http://localhost/api/auth/login")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(req);
        CapturingChain chain = new CapturingChain();

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(chain.captured).isNotNull();
        ServerHttpRequest forwarded = chain.captured.getRequest();
        assertThat(forwarded.getHeaders().getFirst(JwtAuthenticationGatewayFilter.HEADER_USER_ID))
                .isNull();
        assertThat(exchange.getResponse().getStatusCode()).isNull();

        // Cross-check additional public paths
        assertThat(filter.isPublicPath("/api/auth/login")).isTrue();
        assertThat(filter.isPublicPath("/api/auth/refresh")).isTrue();
        assertThat(filter.isPublicPath("/api/v1/auth/request-beta-access")).isTrue();
        // /api/v1/auth/2fa/** is technically in the isPublicPath() prefix set, but
        // filter() carves it out via isChallenge2faPath so the challenge bridge still
        // runs. See GAP-705 cases below.
        assertThat(filter.isPublicPath("/api/v1/auth/2fa/verify")).isTrue();
        assertThat(filter.isChallenge2faPath("/api/v1/auth/2fa/verify")).isTrue();
        assertThat(filter.isChallenge2faPath("/api/auth/2fa/enroll-init")).isTrue();
        assertThat(filter.isChallenge2faPath("/api/v1/admin/beta-requests")).isFalse();
        // GAP-1021 (branding-100): SSE deploy-stream — EventSource không set được
        // Authorization header; gateway pass-through, auth enforced service-side bằng
        // SseQueryTokenAuthFilter (HMAC ?access_token= bound jobId, TTL 120s).
        assertThat(filter.isPublicPath("/api/v1/branding/jobs/42/deploy-stream")).isTrue();
        // Mint endpoint + các path branding khác VẪN cần JWT (default-deny giữ).
        assertThat(filter.isPublicPath("/api/v1/branding/jobs/42/sse-token")).isFalse();
        assertThat(filter.isPublicPath("/api/v1/branding/jobs/42")).isFalse();
        assertThat(filter.isPublicPath("/api/v1/branding/jobs/42/deploy-stream/extra")).isFalse();
        assertThat(filter.isPublicPath("/actuator/health")).isTrue();
        assertThat(filter.isPublicPath("/docs/swagger.json")).isTrue();
        assertThat(filter.isPublicPath("/fallback/auth")).isTrue();
        // GAP-610 / GAP-611 (Wave 91 Bucket D) — lock down beta signup public paths.
        // GAP-610: GET /api/v1/auth/beta-signup/validate?token=... — invitee pre-fill.
        // GAP-611: POST /api/v1/auth/beta-signup — invitee redeems token + provisions tenant.
        // Both must be public (no JWT) so the gateway filter does NOT short-circuit them.
        assertThat(filter.isPublicPath("/api/v1/auth/beta-signup")).isTrue();
        assertThat(filter.isPublicPath("/api/v1/auth/beta-signup/validate")).isTrue();
        assertThat(filter.isPublicPath("/api/v1/auth/beta-signup/exchange-claim-code")).isTrue();
        // Private paths
        assertThat(filter.isPublicPath("/api/v1/admin/beta-requests")).isFalse();
        assertThat(filter.isPublicPath("/api/v1/instances")).isFalse();
    }

    /**
     * GAP-611 (Wave 91 Bucket D) — explicit live-request bypass test for
     * {@code POST /api/v1/auth/beta-signup}. Prevents future filter regression
     * where {@link JwtAuthenticationGatewayFilter#isPublicPath} could be
     * tightened (e.g., switched to exact-match prefix matrix) without noticing
     * that the beta signup completion path stops bypassing.
     *
     * <p>The endpoint is unauthenticated by design: invitees do not have a JWT
     * yet — they are redeeming a single-use {@code invite_token} UUID to
     * create their first tenant + owner account.</p>
     */
    @Test
    @DisplayName("GAP-611: POST /api/v1/auth/beta-signup bypasses JWT filter (no Authorization header)")
    void postBetaSignupBypassesFilter() {
        MockServerHttpRequest req = MockServerHttpRequest
                .post("http://localhost/api/v1/auth/beta-signup")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(req);
        CapturingChain chain = new CapturingChain();

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(chain.captured).isNotNull();
        ServerHttpRequest forwarded = chain.captured.getRequest();
        // Public path bypass MUST NOT inject identity headers.
        assertThat(forwarded.getHeaders().getFirst(JwtAuthenticationGatewayFilter.HEADER_USER_ID))
                .isNull();
        assertThat(forwarded.getHeaders().getFirst(JwtAuthenticationGatewayFilter.HEADER_USER_ROLES))
                .isNull();
        // Filter MUST NOT short-circuit with 401/403.
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    /**
     * GAP-610 (Wave 91 Bucket D) — explicit live-request bypass test for
     * {@code GET /api/v1/auth/beta-signup/validate?token=...}. Same rationale
     * as the GAP-611 case above; this is the deep-link entry point invitees
     * land on from the email magic link.
     */
    @Test
    @DisplayName("GAP-610: GET /api/v1/auth/beta-signup/validate bypasses JWT filter")
    void getBetaSignupValidateBypassesFilter() {
        MockServerHttpRequest req = MockServerHttpRequest
                .get("http://localhost/api/v1/auth/beta-signup/validate"
                        + "?token=98446443-e5cc-43e9-9498-6799d460d2db")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(req);
        CapturingChain chain = new CapturingChain();

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(chain.captured).isNotNull();
        ServerHttpRequest forwarded = chain.captured.getRequest();
        assertThat(forwarded.getHeaders().getFirst(JwtAuthenticationGatewayFilter.HEADER_USER_ID))
                .isNull();
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    @DisplayName("Constructor rejects null/blank/short JWT_SECRET (fail-fast)")
    void constructorValidatesSecret() {
        // Blank → IllegalStateException
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                () -> new JwtAuthenticationGatewayFilter("", TEST_CHALLENGE_SECRET));
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                () -> new JwtAuthenticationGatewayFilter(null, TEST_CHALLENGE_SECRET));
        // Too short (<32 bytes) → IllegalStateException (HS512 yêu cầu ≥256 bits)
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                () -> new JwtAuthenticationGatewayFilter("short-key", TEST_CHALLENGE_SECRET));
        // Challenge secret null/blank is tolerated (challenge bridge becomes a no-op);
        // verified via constructor not throwing here.
        new JwtAuthenticationGatewayFilter(TEST_SECRET, null);
        new JwtAuthenticationGatewayFilter(TEST_SECRET, "");
    }

    /**
     * GAP-705 Case A: HS256 challenge token on a 2FA path → filter accepts and
     * propagates {@code X-User-Id} + {@code X-User-Roles=CHALLENGE} downstream.
     * Closes the gateway side of "2FA enrollment via gateway 401" (GAP-705).
     */
    @Test
    @DisplayName("GAP-705-A: HS256 challenge token on 2FA path → propagate X-User-Id + ROLE=CHALLENGE")
    void shouldAcceptChallengeTokenOn2faPath() {
        String challengeJwt = Jwts.builder()
                .subject("user-challenge-123")
                .claim("type", "challenge")
                .claim("purpose", "TWO_FACTOR_VERIFY")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(challengeKey)
                .compact();

        MockServerHttpRequest req = MockServerHttpRequest
                .post("http://localhost/api/v1/auth/2fa/verify")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + challengeJwt)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(req);
        CapturingChain chain = new CapturingChain();

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(chain.captured).as("challenge token on 2FA path MUST reach downstream").isNotNull();
        ServerHttpRequest mutated = chain.captured.getRequest();
        assertThat(mutated.getHeaders().getFirst(JwtAuthenticationGatewayFilter.HEADER_USER_ID))
                .isEqualTo("user-challenge-123");
        assertThat(mutated.getHeaders().getFirst(JwtAuthenticationGatewayFilter.HEADER_USER_ROLES))
                .isEqualTo(JwtAuthenticationGatewayFilter.CHALLENGE_ROLE);
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    /**
     * GAP-705 Case B (defense-in-depth): HS256 challenge token on a NON-2FA path
     * MUST be rejected. Separate key namespace must not be reusable to bypass
     * access-token role-guard on admin/instance endpoints.
     */
    @Test
    @DisplayName("GAP-705-B: HS256 challenge token on non-2FA path → 401 (defense-in-depth)")
    void shouldRejectChallengeTokenOnNon2faPath() {
        String challengeJwt = Jwts.builder()
                .subject("user-evil-456")
                .claim("type", "challenge")
                .claim("purpose", "TWO_FACTOR_VERIFY")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(challengeKey)
                .compact();

        MockServerHttpRequest req = MockServerHttpRequest
                .get("http://localhost/api/v1/admin/beta-requests")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + challengeJwt)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(req);
        CapturingChain chain = new CapturingChain();

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(chain.captured).as("challenge token on non-2FA path MUST NOT reach downstream").isNull();
    }

    /**
     * GAP-705 Case C: HS512 access token on a 2FA path still works (a fully-authed
     * user with a regular access token can also hit /2fa/setup, e.g. for re-enrol).
     */
    @Test
    @DisplayName("GAP-705-C: HS512 access token on 2FA path → standard propagation")
    void shouldAcceptAccessTokenOn2faPath() {
        String accessJwt = Jwts.builder()
                .subject("user-access-789")
                .claim("role", "PLATFORM_ADMIN")
                .claim("email", "admin@kitehub.me")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(signingKey)
                .compact();

        MockServerHttpRequest req = MockServerHttpRequest
                .post("http://localhost/api/v1/auth/2fa/setup")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessJwt)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(req);
        CapturingChain chain = new CapturingChain();

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(chain.captured).isNotNull();
        ServerHttpRequest mutated = chain.captured.getRequest();
        assertThat(mutated.getHeaders().getFirst(JwtAuthenticationGatewayFilter.HEADER_USER_ID))
                .isEqualTo("user-access-789");
        assertThat(mutated.getHeaders().getFirst(JwtAuthenticationGatewayFilter.HEADER_USER_ROLES))
                .isEqualTo("PLATFORM_ADMIN");
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    @DisplayName("Filter order = LOWEST_PRECEDENCE-2 (chạy sau default-filter strip + trước NettyRoutingFilter, per GAP-916)")
    void filterOrderIsLowestPrecedenceMinus2() {
        assertThat(filter.getOrder()).isEqualTo(JwtAuthenticationGatewayFilter.ORDER);
        // GAP-916 fix: bump từ -100 → LOWEST_PRECEDENCE-2 để header inject chạy
        // sau default-filter RemoveRequestHeader (Order ~0) + trước NettyRoutingFilter
        // (LOWEST_PRECEDENCE). Pre-fix Order=-100 chạy SỚM → default-filter strip cancel inject.
        assertThat(filter.getOrder()).isEqualTo(org.springframework.core.Ordered.LOWEST_PRECEDENCE - 2);
    }

    /** Test helper — captures the exchange when chain.filter() được invoke. */
    private static class CapturingChain implements GatewayFilterChain {
        ServerWebExchange captured;

        @Override
        public Mono<Void> filter(ServerWebExchange exchange) {
            this.captured = exchange;
            return Mono.empty();
        }
    }
}
