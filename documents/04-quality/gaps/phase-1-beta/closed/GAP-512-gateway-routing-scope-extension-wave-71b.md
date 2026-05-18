# GAP-512: Wave 71b — gateway routing scope extension (22 wrong-service findings + 1 orphan)

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 (Plan 1 BETA blocker for non-auth flows: consent, DSAR, branding lifecycle, notification preferences, admin emails/instances)
**Domain:** Backend / DevOps
**Found:** 2026-05-13 (Bucket E `scripts/audit-gateway-routes.sh` post-merge on Wave 71 main)
**Affects:** kitehub-subscription consent/DSAR/notification flows, kitehub-branding lifecycle, kitehub-admin platform endpoints

## Problem

Wave 71 Bucket A added 2 specific routes (`/api/v1/auth/**` + `/api/v1/admin/**`) but `audit-gateway-routes.sh` on post-merge main still reports 22 wrong-service routings + 1 orphan:

### Category 1 — `/api/v1/**` catch-all still misroutes kitehub controllers to kiteclass-core (10 findings)

- `/api/v1/consent/{record,/{visitorId},/{visitorId}/revoke}` (kitehub-subscription ConsentController)
- `/api/v1/dsar/{request,/{ticketId}}` (kitehub-subscription DsarController)
- `/api/v1/notification-preferences/{notificationType}` (kitehub-subscription NotificationPreferenceController)
- `/api/v1/branding/{slug-availability,regenerate-quota,jobs/{jobId},jobs/{jobId}/regenerate,jobs/{jobId}/quality-score,instances/{instanceId}/lifecycle/events}` (kitehub-branding)

### Category 2 — Bucket A `/api/v1/admin/**` route mis-targets kitehub-admin, but controller lives in kitehub-subscription (3 findings)

- `/api/v1/admin/beta-requests` (BetaAccessController.list)
- `/api/v1/admin/beta-requests/{id}/approve`
- `/api/v1/admin/beta-requests/{id}/reject`

Bucket A added `kitehub-admin-v1` → `kitehub-admin:8080`, but `BetaAccessController.adminListBetaRequests/approveBetaRequest/rejectBetaRequest` are exposed by kitehub-subscription. Admin approve/reject flows in Plan 1 Bước 4 will hit wrong service.

### Category 3 — `/api/platform/admin/**` catch-all forwards to kitehub-admin but subscription owns 7 endpoints (7 findings)

- `/api/platform/admin/emails/{history,stats,config,trigger}` (kitehub-subscription)
- `/api/platform/admin/instances/{id}/force-convert,/rollback-migration` (kitehub-subscription)

### Category 4 — Orphan backend controller (1 finding)

- `/api/instances/{id}/domain/verify` (kitehub-subscription) — no gateway route covers this path

## Root Cause

Wave 71 Bucket A scope was minimal (2 routes) to unblock Plan 1 Bước 2 only. Comprehensive route↔controller mapping deferred to follow-up. Audit infrastructure landed in Bucket E surfaced the full set.

## Proposed Fix (Wave 71b)

1. Replace `/api/v1/**` catch-all with explicit specific routes per controller-owning service
2. Split `kitehub-admin-v1` into two routes:
   - `/api/v1/admin/beta-requests/**` → `kitehub-subscription:8080`
   - `/api/v1/admin/**` (remainder) → `kitehub-admin:8080`
3. Split `/api/platform/admin/**` similarly per controller ownership
4. Add route for orphan `/api/instances/{id}/domain/verify`
5. Self-test: `bash scripts/audit-gateway-routes.sh` exits 0

## Acceptance Criteria

- [ ] `bash scripts/audit-gateway-routes.sh` exits 0 (no wrong-service routings, no orphans)
- [ ] Live verify: POST `/api/v1/consent/record` from kitehub.me → reaches kitehub-subscription
- [ ] Live verify: GET `/api/v1/admin/beta-requests` → reaches kitehub-subscription (not kitehub-admin)
- [ ] Live verify: POST `/api/v1/dsar/request` → reaches kitehub-subscription
- [ ] CircuitBreaker fallback URIs wired for new routes

## Related

- Parent: Wave 71 (GAP-509 closed routing for auth only)
- Sibling: GAP-507/508/510/511 (Wave 71 sibling fixes)
- Detector: `scripts/audit-gateway-routes.sh` (Bucket E)
- Wave 71b candidate

## Log

- **2026-05-13:** Filed at Wave 71 closure. Audit run on post-merge main reported FAIL with 23 findings. Plan 1 Bước 2 (auth) confirmed PASS; remaining Plan 1 steps depend on this fix.
