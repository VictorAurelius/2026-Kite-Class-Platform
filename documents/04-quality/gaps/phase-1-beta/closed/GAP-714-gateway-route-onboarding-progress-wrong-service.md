---
id: GAP-714
title: Gateway routes `/api/v1/onboarding-progress` → kiteclass-core (wrong service) instead of kitehub-subscription
status: OPEN
priority: P1
phase: phase-1-beta
found: 2026-05-22
audience: dev
related: [GAP-711, GAP-712, GAP-710]
---

# GAP-714 — Gateway routing misconfiguration for `/api/v1/onboarding-progress`

## Problem

`OnboardingProgressController` lives in **kitehub-subscription** (port 8081), but kite-gateway routes `/api/v1/onboarding-progress` requests to **kiteclass-core** (port 8088). kiteclass-core has no handler for this path → returns `NoHandlerFoundException` → 500.

Even with GAP-711 (gateway tenant resolver JWT fallback) and GAP-712 (controller resolveTenant JWT fallback) fixes deployed, the end-to-end Owner walk via gateway:9000 still fails because the request reaches the WRONG backend service.

## Evidence

Live verify 2026-05-22 post-GAP-711/712 fix:

**Gateway log (routing decision):**
```
TenantResolverFilter: Host = localhost
Resolved tenant from JWT claim: 83143cd8-318f-4ee7-8f87-dcb899295dd6
Routing to instance: sky-w105e-fix-1779447684 (tenant ID: 83143cd8-...)
```

**kiteclass-core log (404 → 500 conversion):**
```
NoHandlerFoundException: No endpoint GET /api/v1/onboarding-progress.
Resolved [NoHandlerFoundException]
Completed 500 INTERNAL_SERVER_ERROR
```

**Direct kitehub-subscription :8081 (proves controller works):**
```
HTTP 200
{"tenantId":"83143cd8-...","completionPercent":0,"totalSteps":5, ...}
```

## Root Cause

Gateway route config (`kitehub-gateway/src/main/resources/application.yml`) likely has a blanket `/api/v1/**` rule forwarding to kiteclass-core (the catch-all backend in Phase 1 BETA). The `/api/v1/onboarding-progress` path was added to kitehub-subscription but the route predicate was not updated to match `/api/v1/onboarding-progress*` → subscription.

This is the exact bug class that `production-env-config-registry.md` §11 `scripts/audit-gateway-routes.sh` was designed to catch (Wave 71 added 3 audit scripts after similar Wave 71 P0 incidents).

## Proposed Fix

Update gateway route config:
- Add explicit predicate `/api/v1/onboarding-progress*` → kitehub-subscription:8080
- Place BEFORE the catch-all `/api/v1/**` → kiteclass-core route (route order matters in Spring Cloud Gateway)
- Verify other kitehub-subscription endpoints (e.g., `/api/v1/admin/beta-requests/**`) similarly explicit-routed

Run `bash scripts/audit-gateway-routes.sh` post-fix → expect 0 findings.

## Acceptance Criteria

- [ ] Gateway routes `/api/v1/onboarding-progress` → kitehub-subscription:8080
- [ ] Live verify via gateway:9000: `GET /api/v1/onboarding-progress` Bearer Owner JWT (no X-Tenant-Id header) → HTTP 200 + full payload
- [ ] `audit-gateway-routes.sh` PASS (exit 0)
- [ ] No regression: existing kiteclass-core paths still route correctly (e.g., `/api/v1/students/**`)
- [ ] IT test: WebTestClient against gateway routes `/api/v1/onboarding-progress` to subscription mock, NOT kiteclass-core mock

## Related

- Triggered by: 2026-05-22 Bucket E live verify post-GAP-711/712 fix (audit doc updated)
- Sister: GAP-711 (gateway tenant resolver — landed but blocked by this routing bug)
- Sister: GAP-712 (controller tenant fallback — landed, works direct :8081)
- Detector: `scripts/audit-gateway-routes.sh` per `production-env-config-registry.md` §11
- Wave 105 candidate scope
