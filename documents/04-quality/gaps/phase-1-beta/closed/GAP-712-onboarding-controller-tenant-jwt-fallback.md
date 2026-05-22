---
id: GAP-712
title: OnboardingProgressController.resolveTenant() — fallback to JWT tenantId claim when X-Tenant-Id missing
status: OPEN
priority: P1
phase: phase-1-beta
found: 2026-05-22
audience: dev
related: [GAP-704, GAP-711, GAP-531, GAP-710]
---

# GAP-712 — OnboardingProgressController tenant JWT fallback

## Problem

Wave 104 Bucket A enriches Owner JWT với `tenantId` claim correctly. NHƯNG `OnboardingProgressController.resolveTenant(...)` vẫn require `X-Tenant-Id` header (throws `TenantContextMissingException` nếu null/blank). Code đã có helper `extractJwtTenantClaim(...)` để extract JWT claim — nhưng helper CHỈ dùng cho **cross-check** (defense-in-depth GAP-554), KHÔNG dùng làm primary resolution path.

Wave 104 Bucket A AC line 122: "Live verify: `curl GET /api/v1/onboarding-progress` Bearer Owner JWT → 200 OK (no X-Tenant-Id header needed)" — yêu cầu controller fallback JWT khi header missing. Hiện tại không có fallback.

## Root Cause

```java
// kitehub-subscription/.../OnboardingProgressController.java:96-115
private UUID resolveTenant(String tenantHeader, String authorizationHeader) {
    if (tenantHeader == null || tenantHeader.isBlank()) {
        throw new TenantContextMissingException("X-Tenant-Id header missing");
    }
    // ... cross-check JWT vs header
}
```

Method assumes header presence is REQUIRED. JWT helper `extractJwtTenantClaim` exists but only used to detect mismatch (cross-check), not as fallback when header absent.

## Proposed Fix

Refactor `resolveTenant` two-path resolution:

```java
private UUID resolveTenant(String tenantHeader, String authorizationHeader) {
    String jwtTenant = extractJwtTenantClaim(authorizationHeader);

    if (tenantHeader == null || tenantHeader.isBlank()) {
        // Fallback: derive from JWT (Wave 104 Bucket A enrichment)
        if (jwtTenant == null || jwtTenant.isBlank()) {
            throw new TenantContextMissingException("X-Tenant-Id header missing and JWT tenantId claim absent");
        }
        try {
            return UUID.fromString(jwtTenant);
        } catch (IllegalArgumentException ex) {
            throw new TenantContextMissingException("JWT tenantId claim malformed");
        }
    }

    // Header present: existing cross-check path
    final UUID tenantId;
    try {
        tenantId = UUID.fromString(tenantHeader);
    } catch (IllegalArgumentException ex) {
        throw new TenantContextMissingException("X-Tenant-Id header malformed");
    }
    if (jwtTenant != null && !jwtTenant.isBlank() && !jwtTenant.equals(tenantHeader)) {
        throw new TenantHeaderJwtMismatchException(...);
    }
    return tenantId;
}
```

## Acceptance Criteria

- [ ] `resolveTenant` derives tenant from JWT `tenantId` claim when X-Tenant-Id header missing
- [ ] Cross-check semantics preserved when both present (mismatch → 403)
- [ ] Live verify: `curl GET /api/v1/onboarding-progress` Bearer Owner JWT (no X-Tenant-Id) → HTTP 200
- [ ] IT test: 3 cases — both present matching (200), header missing JWT present (200), neither (400)
- [ ] No security regression: JWT signature still validated by upstream filter; helper is best-effort claim read only
- [ ] Other tenant-scoped controllers consuming same pattern reviewed (search for `TenantContextMissingException` consumers)

## Related

- Triggered by: Wave 104 Bucket E live verify finding (`2026-05-22-wave-104-bucket-e-partial-verify.md` §4.3 Bug 2)
- Sister: GAP-711 (gateway-side fallback)
- Wave 105 candidate scope per Wave 104 Bucket E AC "if surfaced → file Wave 105"
- Code ref: `kitehub-subscription/src/main/java/com/kitehub/subscription/onboarding/controller/OnboardingProgressController.java:96-115`
