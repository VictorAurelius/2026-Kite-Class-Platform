# GAP-1420: Branding X-Instance-Id cross-tenant IDOR (GAP-1019 sweep miss)

**Status:** 🟢 DONE
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-15 (header-injection class sweep — user-requested cross-flow sweep)
**Affects:** `kitehub-branding` `BrandingWizardController.regenerate` + `DeployStreamController.mintSseToken`

## Problem

Header-injection class sweep found the gateway strips + re-injects all identity headers from the verified JWT (X-Tenant-Id GAP-814, X-User-Id, X-User-Reference-Id, X-User-Roles GAP-1308, X-User-Email GAP-1310, X-Subscription-Tier GAP-1020) — anti-spoof CLOSED. X-Instance-Id is NOT stripped; instead `BrandingJobController` binds it to the gateway-trusted X-Tenant-Id via `TenantOwnershipGuard.requireInstanceOwnership` (GAP-1019).

But GAP-1019 fixed only `BrandingJobController` — it did NOT sweep its sibling controllers (cross-flow-bug-class-sweep miss). Two residual sites trusted client-supplied scope without binding:

1. **`BrandingWizardController.regenerate`** (`POST /api/v1/branding/jobs/{jobId}/regenerate`) — read client `X-Instance-Id`, used it directly for `tierResolver.resolveEffectiveTier` + `quotaService.regenerate`, **no `TenantOwnershipGuard`**. An OWNER of tenant A could pass tenant B's instanceId → regenerate against B's quota/job.
2. **`DeployStreamController.mintSseToken`** (`POST .../{jobId}/sse-token`) — looked up the job by `jobId` alone, minted a (jobId-scoped) SSE token with **no tenant check**. Any OWNER could mint a token for another tenant's job and stream its deploy progress. (`deploy-stream` GET is transitively protected — `SseTokenService.verify` binds the token to the path jobId.)

OWASP A01 (Broken Access Control), same class as GAP-1019/1015/1023/1025.

## Fix (this PR)

- `regenerate`: added `X-Tenant-Id` param + `TenantOwnershipGuard.requireInstanceOwnership(instanceId, tenantHeader)` after the instance-id null check (mirrors `BrandingJobController`).
- `mintSseToken`: added `X-Tenant-Id` param + `TenantOwnershipGuard.requireInstanceOwnership(job.getInstanceId(), tenantHeader)` before minting.

## Acceptance Criteria

- [x] regenerate cross-tenant → 403, quota service never invoked.
- [x] mintSseToken cross-tenant → 403, token never minted.
- [x] own-tenant happy paths still pass (regenerate 200 / mint 200).
- [x] Tests: `WizardDeployTenantOwnershipTest` (4/4) + existing `BrandingWizardControllerTest` (8/8) + `DeployStreamControllerTest` (11/11) green.

## Related

- Found in: header-injection class sweep 2026-06-15 (sister of GAP-1419 contract-drift sweep)
- Parent fix (swept-from): GAP-1019 (BrandingJobController X-Instance-Id binding)
- Rule: `cross-flow-bug-class-sweep.md` — GAP-1019 fix should have swept sibling controllers
- Recurrence class: GAP-1015/1023/1025 (cross-tenant IDOR)
