# GAP-604: Gateway thiếu JWT-to-headers filter — admin endpoints 401

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Backend
**Found:** 2026-05-17 (Wave 88 Claude walkthrough Platform_Admin)
**Affects:** Tất cả admin endpoints requiring Spring Security `@PreAuthorize` — `kitehub-admin` + `kitehub-subscription` admin controllers

## Problem

Production admin operations (approve beta request, list instances, view payments, audit log) ĐỀU return **HTTP 401** dù JWT valid với `role: PLATFORM_ADMIN`.

Curl reproduction:
```bash
JWT=$(curl -s -X POST https://api.kitehub.me/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@kitehub.me","password":"<...>"}' | jq -r .accessToken)

# Returns 401 despite valid JWT with role=PLATFORM_ADMIN
curl -s "https://api.kitehub.me/api/v1/admin/beta-requests?status=PENDING" \
  -H "Authorization: Bearer $JWT" -o /dev/null -w "%{http_code}\n"
# → 401
```

JWT claims decoded:
```json
{"sub":"...","email":"admin@kitehub.me","role":"PLATFORM_ADMIN","type":"access","iat":...,"exp":...}
```

## Root Cause

Gateway `kitehub-gateway` THIẾU JWT validation filter. Existing filters:
- `RateLimitMetricsFilter`
- `SecurityHeadersFilter`
- `TenantResolverGatewayFilterFactory`

Không có filter convert `Authorization: Bearer <JWT>` thành downstream headers.

Downstream services (kitehub-subscription, kitehub-admin) dùng `XUserRolesHeaderFilter` (per `SecurityConfig.java:144`) đọc `X-User-Id` + `X-User-Roles` headers từ gateway pass-through:

```java
String userId = request.getHeader("X-User-Id");
String rolesHeader = request.getHeader("X-User-Roles");
if (userId != null && rolesHeader != null) {
    // Create UsernamePasswordAuthenticationToken with ROLE_X authorities
    SecurityContextHolder.getContext().setAuthentication(auth);
}
```

Gateway không set headers này → `SecurityContext` empty → `@PreAuthorize("hasRole('PLATFORM_ADMIN')")` reject với 401.

## Affected endpoints

**ALL endpoints requiring Spring Security on backend services routed through gateway:**

- `GET /api/v1/admin/beta-requests` (kitehub-subscription)
- `POST /api/v1/admin/beta-requests/{id}/approve` (kitehub-subscription)
- `POST /api/v1/admin/beta-requests/{id}/reject` (kitehub-subscription)
- `GET /api/v1/admin/instances` (kitehub-admin)
- `GET /api/v1/admin/payments` (kitehub-admin)
- `GET /api/v1/admin/revenue` (kitehub-admin)
- `GET /api/platform/admin/*` (any kitehub-admin endpoint)
- All `@PreAuthorize`-guarded endpoints

**NOT affected (different auth path):**
- `POST /api/auth/login` (public, no Security)
- `POST /api/auth/refresh` (reads Authorization header directly)
- `POST /api/v1/auth/request-beta-access` (public)

## Production impact

🔴 **Beta cohort onboarding BLOCKED** — admin không thể approve beta request → tenant không được create → invite email không gửi. Wave 88 walkthrough surfaced 9 ADM-BETA-APPROVE/REJECT/INST rows ALL blocked.

## Proposed Fix

Implement `JwtAuthenticationGatewayFilter` trong `kitehub-gateway/src/main/java/com/kitehub/gateway/filter/`:

```java
@Component
public class JwtAuthenticationGatewayFilter implements GlobalFilter, Ordered {

    private final String jwtSecret; // injected from JWT_SECRET env

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String auth = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        // Skip public paths
        String path = request.getURI().getPath();
        if (isPublicPath(path)) return chain.filter(exchange);

        if (auth == null || !auth.startsWith("Bearer ")) {
            return chain.filter(exchange); // let downstream reject
        }

        try {
            String token = auth.substring(7);
            Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
                .build().parseSignedClaims(token).getPayload();

            String userId = claims.getSubject();
            String role = claims.get("role", String.class);

            ServerHttpRequest mutated = request.mutate()
                .header("X-User-Id", userId)
                .header("X-User-Roles", role) // single role; comma-separated if multiple
                .header("X-User-Email", claims.get("email", String.class))
                .build();

            return chain.filter(exchange.mutate().request(mutated).build());
        } catch (JwtException ex) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    private boolean isPublicPath(String path) {
        return path.startsWith("/api/auth/")
            || path.startsWith("/api/v1/auth/")
            || path.equals("/actuator/health")
            || path.startsWith("/docs/");
    }

    @Override public int getOrder() { return -100; } // before circuit breaker filter
}
```

Tests:
- Unit test: JWT valid → headers set, JWT invalid → 401, public path → pass-through
- Integration test: end-to-end với real subscription service

## Acceptance Criteria

- [ ] `JwtAuthenticationGatewayFilter` implemented + unit tested
- [ ] Gateway rebuilt, deployed via `deploy-production.yml`
- [ ] `curl -H "Authorization: Bearer $JWT" /api/v1/admin/beta-requests` returns 200 với role=PLATFORM_ADMIN
- [ ] Claude walkthrough re-run admin persona — ADM-BETA-APPROVE-001..005 PASS
- [ ] Integration test cover: valid JWT, expired JWT, missing role, public path bypass
- [ ] Document trong `documents/02-architecture/adr/` ADR mới (gateway auth pattern)
- [ ] Security audit `pre-launch-auth-hardening-checklist.md` §X check

## Related

- Wave 88 closure: `documents/04-quality/audits/aws-verification/2026-05-17-wave-88-cutover-post-apply.md` §7.3
- GAP-518 admin role mismatch (companion — partial fix, this is true root cause for admin endpoints)
- `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/config/SecurityConfig.java:144` `XUserRolesHeaderFilter`
- `kitehub/kitehub-gateway/src/main/resources/application.yml` route definitions
- `pre-launch-auth-hardening-checklist.md`

## Log

- **2026-05-17:** Gap filed during Wave 88 closure. Production admin endpoints non-functional. **BLOCKER cho beta cohort onboarding** — Wave 89 P0 fix mandatory before invite send.
