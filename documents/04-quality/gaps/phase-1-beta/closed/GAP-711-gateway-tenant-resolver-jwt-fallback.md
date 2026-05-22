---
id: GAP-711
title: Gateway TenantResolverFilter — fallback to JWT tenantId claim when Host-based resolution fails
status: OPEN
priority: P1
phase: phase-1-beta
found: 2026-05-22
audience: dev
related: [GAP-704, GAP-531, GAP-710]
---

# GAP-711 — Gateway TenantResolverFilter JWT claim fallback

## Problem

Wave 104 Bucket A enriches Owner JWT với `tenantId` claim correctly, NHƯNG gateway `TenantResolverGatewayFilterFactory` chỉ resolve tenant qua Host header (subdomain pattern). Local stack với `localhost` host không có tenant subdomain → resolver returns null → downstream forwarding strips tenant context → backend returns 400/401 dù Owner JWT chứa đầy đủ tenantId.

**Evidence (verify 2026-05-22 Bucket E):**

```
2026-05-22 10:44:25.468 DEBUG c.k.g.f.TenantResolverGatewayFilterFactory - TenantResolverFilter: Host = localhost
2026-05-22 10:44:25.983 WARN  c.k.g.f.TenantResolverGatewayFilterFactory - Could not resolve tenant from request
```

Request: `GET /api/v1/onboarding-progress` với Bearer Owner JWT containing `tenantId: 96cc496c-...`, no X-Tenant-Id header.
Gateway response: HTTP 400 (content-length 0).
Direct subscription :8081 với same JWT + X-Tenant-Id + spoofed X-User-Roles: HTTP 200 + full payload.

## Root Cause

`TenantResolverGatewayFilterFactory.java` only inspects `Host` header (subdomain extraction) for tenant resolution. Does not parse JWT Bearer token to extract `tenantId` claim as fallback. Wave 104 Bucket A code change touched JWT issuance (`AuthService.buildAccessToken`) but NOT gateway tenant resolution path.

## Proposed Fix

Extend `TenantResolverGatewayFilterFactory.resolve(...)`:
1. Try Host-based resolution (existing behavior — preserve for production subdomain routing)
2. If null AND request has Bearer Authorization → parse JWT (best-effort; failure = null), extract `tenantId` claim
3. If claim present → use as tenant context + forward via downstream header

Reference existing helper logic: `OnboardingProgressController.extractJwtTenantClaim(...)` (best-effort JWT parse pattern).

## Acceptance Criteria

- [ ] `TenantResolverGatewayFilterFactory` falls back to JWT `tenantId` claim when Host-based resolution returns null
- [ ] Live verify: `curl GET /api/v1/onboarding-progress` Bearer Owner JWT via gateway:9000 → HTTP 200 (no X-Tenant-Id header)
- [ ] Existing production subdomain routing path unchanged (Host header still primary)
- [ ] IT test: `WebTestClient` mock with JWT-only request → 200
- [ ] No security regression: JWT signature validation enforced before claim trust

## Related

- Triggered by: Wave 104 Bucket E live verify finding (`2026-05-22-wave-104-bucket-e-partial-verify.md` §4.3 Bug 1)
- Sister: GAP-712 (controller-side fallback)
- Wave 105 candidate scope per Wave 104 Bucket E AC "if surfaced → file Wave 105"
- Wave 104 Bucket A: `kitehub-subscription/src/main/java/.../auth/AuthService.java` (JWT issuance side)
- Gateway code: `kitehub-gateway/src/main/java/com/kitehub/gateway/filter/TenantResolverGatewayFilterFactory.java`
