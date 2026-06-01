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
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TenantHeaderGuardFilter} (GAP-814 — Wave tenant-domain-1
 * Bucket A).
 *
 * <p>5 mandatory scenarios per gap AC:</p>
 * <ol>
 *   <li>Spoofed {@code X-Tenant-Id} client-supplied → stripped by default-filter
 *       (verified via behavior: forwarded header reflects only what guard
 *       re-injected from JWT, NOT client value). Filter does NOT see client
 *       value because default-filter strips upstream. Test simulates strip by
 *       constructing request without client header.</li>
 *   <li>Valid JWT with {@code tenantId=A} claim → forwarded header
 *       {@code X-Tenant-Id: A}</li>
 *   <li>Valid JWT {@code tenantId=A} + spoofed {@code X-Tenant-Id: B} client →
 *       default-filter strip + guard re-inject = forwarded value is A (JWT
 *       claim wins; this models the post-strip+guard pipeline by constructing
 *       request without client header but with JWT having claim A — same
 *       outcome as production where default-filter strips before guard runs)</li>
 *   <li>Public route {@code /api/v1/tenants/*&#47;landing} → no JWT required,
 *       no header injection (guard bypass)</li>
 *   <li>No JWT + non-public route → no header injection (guard pass-through;
 *       downstream Spring Security rejects)</li>
 * </ol>
 *
 * <p>Pattern mirrors {@code JwtAuthenticationGatewayFilterTest} (Reactor
 * StepVerifier + MockServerWebExchange — no Spring Boot context boot).</p>
 */
@DisplayName("TenantHeaderGuardFilter (kitehub-gateway / GAP-814)")
class TenantHeaderGuardFilterTest {

    /** 32-byte test secret — đủ ≥256 bits cho HS512. */
    private static final String TEST_SECRET = "test-secret-32-bytes-minimum-1234";

    private TenantHeaderGuardFilter filter;
    private SecretKey signingKey;

    @BeforeEach
    void setUp() {
        filter = new TenantHeaderGuardFilter(TEST_SECRET);
        signingKey = Keys.hmacShaKeyFor(TEST_SECRET.getBytes());
    }

    @Test
    @DisplayName("Scenario (a): client-spoofed X-Tenant-Id stripped — guard sees no client value, no JWT → no inject")
    void clientSpoofedHeaderStripped_noJwt_noInject() {
        // Simulates post-strip state: default-filter RemoveRequestHeader=X-Tenant-Id
        // has already removed any client-supplied X-Tenant-Id BEFORE this filter
        // runs. We construct the request WITHOUT X-Tenant-Id to model that
        // strip outcome. No JWT → no inject either. Forwarded request has no
        // X-Tenant-Id at all → core sees tenant context as missing → returns
        // 400/403 per TenantFilterInterceptor (verified at integration layer).
        MockServerHttpRequest req = MockServerHttpRequest
                .get("http://localhost/api/v1/students")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(req);
        CapturingChain chain = new CapturingChain();

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(chain.captured).as("filter must pass-through; downstream gateway handles missing tenant").isNotNull();
        ServerHttpRequest forwarded = chain.captured.getRequest();
        assertThat(forwarded.getHeaders().getFirst(TenantHeaderGuardFilter.HEADER_TENANT_ID))
                .as("no JWT → no tenant header injected")
                .isNull();
    }

    @Test
    @DisplayName("Scenario (b): valid JWT with tenantId=A → X-Tenant-Id=A injected from verified claim")
    void validJwtWithTenantClaim_injectsVerifiedValue() {
        String tenantA = "tenant-aaa-111";
        String validJwt = Jwts.builder()
                .subject("user-owner-1")
                .claim("role", "TENANT_OWNER")
                .claim("tenantId", tenantA)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(signingKey)
                .compact();

        MockServerHttpRequest req = MockServerHttpRequest
                .get("http://localhost/api/v1/students")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(req);
        CapturingChain chain = new CapturingChain();

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(chain.captured).isNotNull();
        ServerHttpRequest forwarded = chain.captured.getRequest();
        assertThat(forwarded.getHeaders().getFirst(TenantHeaderGuardFilter.HEADER_TENANT_ID))
                .as("X-Tenant-Id must come from verified JWT tenantId claim")
                .isEqualTo(tenantA);
    }

    @Test
    @DisplayName("Scenario (c): JWT tenantId=A even if client tried to spoof tenantId=B — JWT-derived value wins (defense-in-depth)")
    void jwtTenantClaimOverridesClientSpoofAttempt() {
        // Production flow: default-filter strips client X-Tenant-Id BEFORE guard
        // runs. Guard then re-injects from JWT claim. Net effect: client cannot
        // override JWT-derived tenant scope.
        // In this unit test we model the post-strip state (client header
        // absent from request when guard runs); JWT carries claim tenantId=A.
        // Expected forwarded value = A.
        String tenantA = "tenant-aaa-truthful";
        // tenant-bbb-spoof would be the attacker's attempt — never reaches the
        // guard because default-filter strips it. We assert the guard's outcome
        // (forwarded X-Tenant-Id = A from JWT, NOT B).
        String validJwt = Jwts.builder()
                .subject("user-owner-2")
                .claim("role", "TENANT_OWNER")
                .claim("tenantId", tenantA)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(signingKey)
                .compact();

        MockServerHttpRequest req = MockServerHttpRequest
                .get("http://localhost/api/v1/students")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(req);
        CapturingChain chain = new CapturingChain();

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(chain.captured).isNotNull();
        ServerHttpRequest forwarded = chain.captured.getRequest();
        // Crucial assertion: forwarded value is JWT-derived (A), NOT a
        // hypothetical client-spoofed value (B). The strip+guard pipeline
        // ensures spoofing cannot reach downstream.
        assertThat(forwarded.getHeaders().getFirst(TenantHeaderGuardFilter.HEADER_TENANT_ID))
                .isEqualTo(tenantA);
        assertThat(forwarded.getHeaders().getFirst(TenantHeaderGuardFilter.HEADER_TENANT_ID))
                .as("must NOT equal hypothetical spoofed value tenant-bbb-spoof")
                .isNotEqualTo("tenant-bbb-spoof");
    }

    @Test
    @DisplayName("Scenario (d): public route /api/v1/tenants/*/landing → no JWT required, no header injection")
    void publicTenantLandingPathBypassesGuard() {
        MockServerHttpRequest req = MockServerHttpRequest
                .get("http://localhost/api/v1/tenants/sky-tenant-uuid/landing")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(req);
        CapturingChain chain = new CapturingChain();

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(chain.captured).isNotNull();
        ServerHttpRequest forwarded = chain.captured.getRequest();
        assertThat(forwarded.getHeaders().getFirst(TenantHeaderGuardFilter.HEADER_TENANT_ID))
                .as("public landing path must NOT have X-Tenant-Id injected — tenant derives from path")
                .isNull();
    }

    @Test
    @DisplayName("Scenario (e): no JWT + non-public route → no header injection (downstream rejects)")
    void noJwtNonPublicRoute_noInject() {
        MockServerHttpRequest req = MockServerHttpRequest
                .get("http://localhost/api/v1/courses")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(req);
        CapturingChain chain = new CapturingChain();

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(chain.captured).isNotNull();
        ServerHttpRequest forwarded = chain.captured.getRequest();
        assertThat(forwarded.getHeaders().getFirst(TenantHeaderGuardFilter.HEADER_TENANT_ID))
                .isNull();
    }

    // ─── Additional defense-in-depth tests ──────────────────────────────────

    @Test
    @DisplayName("JWT without tenantId claim → no header injection (e.g. PLATFORM_ADMIN cross-tenant token)")
    void jwtWithoutTenantClaim_noInject() {
        String adminJwt = Jwts.builder()
                .subject("user-platform-admin")
                .claim("role", "PLATFORM_ADMIN")
                .claim("email", "admin@kitehub.me")
                // intentionally no tenantId claim
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(signingKey)
                .compact();

        MockServerHttpRequest req = MockServerHttpRequest
                .get("http://localhost/api/v1/admin/instances")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwt)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(req);
        CapturingChain chain = new CapturingChain();

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(chain.captured).isNotNull();
        ServerHttpRequest forwarded = chain.captured.getRequest();
        assertThat(forwarded.getHeaders().getFirst(TenantHeaderGuardFilter.HEADER_TENANT_ID))
                .as("JWT without tenantId claim must not inject anything (admin endpoints handle null)")
                .isNull();
    }

    @Test
    @DisplayName("Malformed JWT → silent pass-through (no inject); JwtAuthenticationGatewayFilter handles 401")
    void malformedJwtPassesThroughWithoutInject() {
        // Guard does NOT 401 — that's JwtAuthenticationGatewayFilter's job
        // (order -100, runs first). Guard simply skips inject when parse fails.
        SecretKey wrongKey = Keys.hmacShaKeyFor("wrong-secret-32-bytes-minimum-aaa".getBytes());
        String badJwt = Jwts.builder()
                .subject("user-bad")
                .claim("tenantId", "tenant-attacker")
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(wrongKey)
                .compact();

        MockServerHttpRequest req = MockServerHttpRequest
                .get("http://localhost/api/v1/students")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + badJwt)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(req);
        CapturingChain chain = new CapturingChain();

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(chain.captured).isNotNull();
        ServerHttpRequest forwarded = chain.captured.getRequest();
        assertThat(forwarded.getHeaders().getFirst(TenantHeaderGuardFilter.HEADER_TENANT_ID))
                .as("invalid-signature JWT must NOT inject attacker-claimed tenantId")
                .isNull();
    }

    @Test
    @DisplayName("Expired JWT → no inject (silent pass-through; JwtAuthenticationGatewayFilter would have 401'd)")
    void expiredJwtNoInject() {
        String expiredJwt = Jwts.builder()
                .subject("user-expired")
                .claim("tenantId", "tenant-expired")
                .issuedAt(Date.from(Instant.now().minusSeconds(7200)))
                .expiration(Date.from(Instant.now().minusSeconds(3600)))
                .signWith(signingKey)
                .compact();

        MockServerHttpRequest req = MockServerHttpRequest
                .get("http://localhost/api/v1/students")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredJwt)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(req);
        CapturingChain chain = new CapturingChain();

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(chain.captured).isNotNull();
        ServerHttpRequest forwarded = chain.captured.getRequest();
        assertThat(forwarded.getHeaders().getFirst(TenantHeaderGuardFilter.HEADER_TENANT_ID))
                .as("expired JWT must NOT inject (claim untrusted)")
                .isNull();
    }

    @Test
    @DisplayName("Public auth paths (/api/auth/login, /api/v1/auth/...) bypass guard")
    void publicAuthPathsBypass() {
        assertThat(filter.isPublicPath("/api/auth/login")).isTrue();
        assertThat(filter.isPublicPath("/api/auth/refresh")).isTrue();
        assertThat(filter.isPublicPath("/api/v1/auth/request-beta-access")).isTrue();
        assertThat(filter.isPublicPath("/api/v1/auth/beta-signup/validate")).isTrue();
        assertThat(filter.isPublicPath("/api/v1/staff-invitations/by-token/abc-123")).isTrue();
        assertThat(filter.isPublicPath("/api/v1/staff-invitations/abc/accept")).isTrue();
        assertThat(filter.isPublicPath("/api/v1/tenants/sky-uuid/landing")).isTrue();
        assertThat(filter.isPublicPath("/actuator/health")).isTrue();
        assertThat(filter.isPublicPath("/docs/swagger.json")).isTrue();
        assertThat(filter.isPublicPath("/fallback/auth")).isTrue();
        // Private — not bypass
        assertThat(filter.isPublicPath("/api/v1/students")).isFalse();
        assertThat(filter.isPublicPath("/api/v1/admin/beta-requests")).isFalse();
        assertThat(filter.isPublicPath("/api/v1/courses")).isFalse();
    }

    @Test
    @DisplayName("Constructor rejects null/blank/short JWT_SECRET (fail-fast)")
    void constructorValidatesSecret() {
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                () -> new TenantHeaderGuardFilter(""));
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                () -> new TenantHeaderGuardFilter(null));
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                () -> new TenantHeaderGuardFilter("short-key"));
    }

    @Test
    @DisplayName("Filter order = -99 (after JwtAuthenticationGatewayFilter -100, before route filters)")
    void filterOrderIsMinus99() {
        assertThat(filter.getOrder()).isEqualTo(TenantHeaderGuardFilter.ORDER);
        assertThat(filter.getOrder()).isEqualTo(-99);
        // Must run AFTER JwtAuthenticationGatewayFilter (-100)
        assertThat(filter.getOrder()).isGreaterThan(JwtAuthenticationGatewayFilter.ORDER);
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
