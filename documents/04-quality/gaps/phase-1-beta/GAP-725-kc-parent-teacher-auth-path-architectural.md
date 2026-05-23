---
id: GAP-725
title: KC Parent/Teacher persona auth path — architectural gap
status: 🟡 OPEN
priority: P1
phase: phase-1-beta
type: feature
created: 2026-05-23
discovered_via: Wave 105 RST UI walk 2026-05-23 — KC Parent/Teacher persona walks
related: [GAP-724, Wave 105]
---

## Problem

Wave 105 RST UI walk attempted Parent + Teacher persona browser walks on kc-frontend
(routes `/parent`, `/teacher`). Both redirected to `/login` because:

1. KH `PlatformRole` enum exposes only **OWNER / STAFF / PLATFORM_ADMIN** (per
   `kitehub-subscription/.../auth/role/PlatformRole.java`). PARENT and TEACHER are
   not valid platform roles.
2. KC frontend `types/auth.ts UserType` declares ADMIN/STAFF/TEACHER/PARENT/STUDENT
   (intended as KC-internal multi-tenant ops roles), but the FE login flow
   currently calls `POST /api/auth/login` which lands on **KH subscription**
   (not KC). KH issues OWNER/STAFF JWT only.
3. Net effect: there is no production login path that produces a JWT with
   `role: PARENT` or `role: TEACHER` for the KC route-guard to accept.

## Impact

- KC Parent portal (`/parent/*` route family) cannot be reached in production
  even though backend endpoints exist (per V61/V64 migrations + Bucket D
  Wave 105 Parent persona walk).
- KC Teacher dashboard (`/teacher/*`) likewise unreachable.
- Wave 105 RST UI walk goal "RST full UI" partial — Owner walk PASS only.

## Proposed Fix (3 options)

### Option A — Extend KH PlatformRole (architectural change)

Add PARENT + TEACHER to `PlatformRole` enum + DB CHECK constraint + JWT claim.
KH issues unified JWT for all roles. KC route-guard already accepts.

Pros: single auth path, simpler mental model
Cons: changes KH from "tenant management" to "multi-role identity provider"

### Option B — KC-native login endpoint

KC core exposes `/api/v1/auth/login` for PARENT/TEACHER/STUDENT. Each tenant
has its own auth scope. KC issues its own JWT signed with same secret.

Pros: keeps KH narrow scope; matches multi-tenant SaaS pattern (tenant-local users)
Cons: 2 login paths to maintain; user model split across KH (owner) + KC (parents)

### Option C — Federated identity (Phase 2+)

KH stays OWNER-only. Parents/teachers sign in via tenant's invite link + OTP
(no password). KC issues short-lived session token per device.

Pros: best UX for non-tech parents; aligns with `dev-readable-doc-language.md`
VN edu market (`vn-localization-audit-checklist.md` §4 phone OTP culture)
Cons: largest scope; Phase 2 work

## Decision deferred

Wave 105 RST goal "RST full UI" closed PARTIAL — Owner walk PASS via PR #1737;
Parent/Teacher walks tracked here. Phase 1 BETA may continue with Owner-only
self-test scope; Parent/Teacher persona on roadmap before public launch.

## References

- PR #1737 — GAP-724 Owner login chain fix
- `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/auth/role/PlatformRole.java`
- `kiteclass/kiteclass-frontend/src/types/auth.ts` (UserType enum)
- Wave 105 wave plan §Bucket D Parent persona walk
- `vn-localization-audit-checklist.md` §4 phone OTP culture (informs Option C)
