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

    /** 32-byte test secret — đủ ≥256 bits cho HS256. */
    private static final String TEST_SECRET = "test-secret-32-bytes-minimum-1234";

    private JwtAuthenticationGatewayFilter filter;
    private SecretKey signingKey;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationGatewayFilter(TEST_SECRET);
        signingKey = Keys.hmacShaKeyFor(TEST_SECRET.getBytes());
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
        assertThat(filter.isPublicPath("/api/v1/auth/2fa/verify")).isTrue();
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
                () -> new JwtAuthenticationGatewayFilter(""));
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                () -> new JwtAuthenticationGatewayFilter(null));
        // Too short (<32 bytes) → IllegalStateException (HS256 yêu cầu ≥256 bits)
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                () -> new JwtAuthenticationGatewayFilter("short-key"));
    }

    @Test
    @DisplayName("Filter order = -100 (chạy sớm, trước CircuitBreaker route filters)")
    void filterOrderIsMinusHundred() {
        assertThat(filter.getOrder()).isEqualTo(JwtAuthenticationGatewayFilter.ORDER);
        assertThat(filter.getOrder()).isEqualTo(-100);
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
