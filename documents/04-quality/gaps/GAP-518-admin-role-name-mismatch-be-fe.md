# GAP-518: BE seed role PLATFORM_ADMIN vs FE role-guard 'ADMIN' mismatch

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 (Plan 1 Bước 4 launch blocker — admin UI completely unusable)
**Domain:** Backend ↔ Frontend contract
**Found:** 2026-05-13 (Wave 71c per `pre-handoff-self-test-completeness.md` §2.4 retroactive check)
**Affects:** admin@kitehub.me PLATFORM_ADMIN user — cannot access ANY /admin/* route

## Problem

Backend `scripts/seed-direct-sql.sh:21` seeds admin with role `PLATFORM_ADMIN`.
Frontend `kitehub-frontend/src/app/(auth)/login/page.tsx:38` redirects `user.role === 'ADMIN' ? '/admin' : '/dashboard'`.
Frontend `kitehub-frontend/src/components/layout/AdminLayout.tsx:20,33` blocks `user?.role !== 'ADMIN'`.

Result: admin@kitehub.me logs in successfully (BE accepts), JWT contains role=PLATFORM_ADMIN, FE redirects to `/dashboard` (not `/admin`), and `/admin/*` routes hard-block. **Admin UI 100% unusable in production.**

Missed because Wave 71b "verify live" was curl-level only (`POST /api/v1/auth/request-beta-access → 201`); UI flow not walked.

## Proposed Fix

Choose ONE option per Wave 71c plan (likely Option B for least churn):

**Option A — BE seed role = `ADMIN`** (simpler, but loses platform-vs-tenant distinction)
**Option B — FE accepts both `ADMIN` and `PLATFORM_ADMIN`** ✅ recommended
- Update `(auth)/login/page.tsx:38` redirect condition: `['ADMIN','PLATFORM_ADMIN'].includes(user.role) ? '/admin' : '/dashboard'`
- Update `AdminLayout.tsx:20,33` guard: `!['ADMIN','PLATFORM_ADMIN'].includes(user?.role)`
- Update `auth-store.ts:8` Role type: `'OWNER' | 'ADMIN' | 'PLATFORM_ADMIN'`
- Test: login as admin@kitehub.me → expect redirect `/admin` → expect /admin/beta-requests visible

**Option C — Add `PLATFORM_ADMIN` everywhere consistently** (largest scope, cleanest long-term)

## Acceptance Criteria

- [ ] Login admin@kitehub.me → redirects to `/admin`
- [ ] `/admin/beta-requests` renders without 403/redirect
- [ ] Approve/reject buttons fire correct endpoint
- [ ] Unit test added for role-guard accepting both values

## Related

- Rule: `pre-handoff-self-test-completeness.md` §2.4 (originating)
- Wave 71c candidate
