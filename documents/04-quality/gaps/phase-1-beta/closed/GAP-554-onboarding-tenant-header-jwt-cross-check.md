# GAP-554: Onboarding tenant header trust without JWT claim cross-check

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend (Auth/Tenant Isolation)
**Found:** 2026-05-14 (Wave 78 post-wave Security /100 audit — P1-3)
**Affects:** `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/onboarding/controller/OnboardingProgressController.java:60-83`
**Phase:** Phase 1 BETA pre-launch
**Standards:** OWASP A01 (Broken Access Control) + defense-in-depth principle

## Problem

`OnboardingProgressController` trust `X-Tenant-Id` header trực tiếp từ request:

```java
@GetMapping
public ResponseEntity<OnboardingProgressResponse> getProgress(
        @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader) {
    UUID tenantId = resolveTenant(tenantHeader);
    return ResponseEntity.ok(service.getProgress(tenantId));
}

@PutMapping
public ResponseEntity<OnboardingProgressResponse> updateStep(
        @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
        @Valid @RequestBody OnboardingProgressUpdateCommand command) {
    UUID tenantId = resolveTenant(tenantHeader);
    return ResponseEntity.ok(service.updateStep(tenantId, command));
}
```

Header có nguồn từ gateway forward, nhưng:
1. Gateway trust model: gateway extract tenantId từ JWT → set `X-Tenant-Id` header → forward downstream
2. KHÔNG có cross-check trong subscription service: JWT claim `tenantId` (nếu có) vs header `X-Tenant-Id` — mismatch không bị reject
3. Authenticated qua `XUserRolesHeaderFilter` (`X-User-Id` + `X-User-Roles`) — tenantId TÁCH BIỆT với auth identity

## Root Cause

- Pattern "gateway is trusted boundary" được áp dụng — gateway responsible cho extract + forward tenant context.
- Internal services giả định trust gateway → không double-check.
- Acceptable Phase 1 BETA per gateway trust model docs, NHƯNG single point of failure: nếu gateway misconfig HOẶC attacker bypass gateway (direct call internal port via VPC misconfig, port exposed, lateral movement) → có thể spoof tenantId tự do.

## Impact

**P1 (defense-in-depth gap, không phải immediate exploit):**
- Nếu gateway bị bypass → attacker với valid JWT của tenant A có thể set `X-Tenant-Id: <tenant-B>` → đọc/sửa onboarding progress của tenant B
- Wave 78 onboarding progress là low-sensitivity data (checklist steps), nhưng PATTERN này tồn tại → áp dụng cho mọi controller tương tự (Feedback, BetaStatus, future tenant-scoped endpoints) sẽ leak nghiêm trọng hơn
- Tenant isolation = pillar của multi-tenant SaaS; relying solely trên gateway = single point of failure

## Proposed Fix

### Option A (preferred — JWT-driven tenant context)

Thay X-Tenant-Id header bằng derive tenantId từ JWT claim:

1. Extend `XUserRolesHeaderFilter` extract `X-Tenant-Id` (gateway forward từ JWT claim) → set vào `Authentication` principal hoặc `SecurityContext` custom attribute
2. Add util method `currentTenantId()` đọc từ SecurityContext (không phải request header directly)
3. Controller method receive `Principal` thay vì `@RequestHeader`

```java
public ResponseEntity<OnboardingProgressResponse> getProgress(Authentication auth) {
    UUID tenantId = TenantContext.requireTenantId(auth);
    return ResponseEntity.ok(service.getProgress(tenantId));
}
```

### Option B (incremental — cross-check guard)

Thêm filter sau auth filter: compare `X-Tenant-Id` header với JWT claim `tenantId` (nếu có); mismatch → 403:

```java
@Component
@Order(2)  // after XUserRolesHeaderFilter
public class TenantHeaderJwtCrossCheckFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(...) {
        String headerTenant = request.getHeader("X-Tenant-Id");
        String jwtTenant = extractJwtTenantClaim(request);  // null if no JWT or no claim

        if (headerTenant != null && jwtTenant != null && !headerTenant.equals(jwtTenant)) {
            response.setStatus(403);
            response.getWriter().write("{\"error\":\"TENANT_HEADER_JWT_MISMATCH\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
```

Option B lighter scope; recommended cho Phase 1 BETA. Option A là long-term target.

## Acceptance Criteria

- [ ] Either Option A (JWT-driven tenant) OR Option B (cross-check filter) shipped
- [ ] Integration test: mismatch JWT tenant claim ≠ X-Tenant-Id header → 403
- [ ] Existing onboarding integration tests pass (anonymous/legitimate requests work as before)
- [ ] Pattern documented in `documents/02-architecture/` cho future controllers reuse
- [ ] Apply pattern audit: list other controllers trust `X-Tenant-Id` header (FeedbackController GAP-547 sister scope, BrandingController nếu có, v.v.) → file follow-up gaps nếu cần

## Related

- Parent audit: `documents/04-quality/audits/security/2026-05-14-post-wave-78.md` §P1-3
- Rule: `pre-launch-owasp-rest-hardening-checklist.md` §2.1 (A01 per-resource authz — tenant isolation slice)
- Wave 78 GAP-538 (Bucket B onboarding): this gap is security follow-up
- Sister gaps GAP-547 (Feedback tenantId wiring) — overlap on "tenant context plumbing"

## Log

- **2026-05-14:** DONE — Wave 79 Bucket C closure. OnboardingProgressController X-Tenant-Id × JWT claim cross-check via TenantHeaderJwtMismatchException — gateway-bypass scenario blocked (OWASP A01) (PR #1367).

- **2026-05-14**: Filed from Wave 78 post-wave Security audit (89/100 B+).
