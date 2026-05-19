# GAP-518: BE seed role PLATFORM_ADMIN vs FE role-guard 'ADMIN' mismatch

**Status:** 🟡 PARTIAL 97% — Code-side complete (BE RoleGuardMatrixIT 8/8 + FE 27/27 PASS local Wave 101 A); live browser walk gated GAP-684 (AWS GAP-612 restore)
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

- [ ] Login admin@kitehub.me → redirects to `/admin` (live verify pending — code shipped + runbook §4 documents flow)
- [ ] `/admin/beta-requests` renders without 403/redirect (live verify pending — code shipped + runbook §4 documents flow)
- [ ] Approve/reject buttons fire correct endpoint (separate scope — not in FE role-guard PR)
- [x] Unit test added for role-guard accepting both values
- [x] Dedicated `auth-helpers.test.ts` regression-safe suite (10 cases: canonical + alias + rejections) — Wave 78 Bucket D

## Related

- Rule: `pre-handoff-self-test-completeness.md` §2.4 (originating)
- Wave 71c candidate
- Wave 72a Bucket C — FE Option B implementation (this PR)

## Log

- **2026-05-19 (Wave 101 Bucket A)** — PARTIAL 95 → 97% bump. Code-side re-verified local sau hardening: BE `RoleGuardMatrixIT` 8/8 PASS (`cd kitehub && ./mvnw -pl kitehub-subscription verify -Dtest=RoleGuardMatrixIT` — Tests run 8, Failures 0, Errors 0); FE 27/27 PASS (`cd kitehub/kitehub-frontend && pnpm test --run auth-helpers RoleGuard AdminLayout Sidebar` — 4 test files, 27 tests passed). State-check confirmed: BE `User.java:101` PLATFORM_ADMIN seed comment; FE `auth-helpers.ts:18` accepts both `PLATFORM_ADMIN` (canonical) || `ADMIN` (legacy alias); AdminLayout + login redirect consume helper đúng. Live browser walkthrough VẪN gated GAP-612 AWS account 906286017800 suspension (status OPEN per CSV 2026-05-19). Per `gap-done-discipline.md` §3 PARTIAL exit ramp: follow-up gap GAP-684 filed tracking live walk execution path khi AWS restore. CSV row updated last_verified=2026-05-19, completion_pct=97. `PRE_HANDOFF_PARTIAL: AWS-blocked` trailer cited per `pre-handoff-self-test-completeness.md` §5.4.
- **2026-05-18 (Wave 98 Bucket B7)** — P3 Center Manager role-guard verify extension shipped. Fix-time state-check (per `audit-to-gap-pipeline.md` §2.8): role naming in F-NEW-7 framing (`CENTER_OWNER` / `CENTER_MANAGER` / `PLATFORM_ADMIN`) does NOT match shipped canonical (Wave 79 GAP-562 migration — `OWNER` / `STAFF` + legacy aliases `PLATFORM_ADMIN` / `ADMIN` → resolve to `OWNER`). Mapping documented in new test class `RoleGuardMatrixIT.java` javadoc (BE) + `playwright/role-guard.spec.ts` header (FE). NO silent rename across BE+FE — `PlatformRole` enum + `auth-helpers.ts` legacy-alias resolver remain shipped. Files added: (1) `kitehub-subscription/src/test/java/.../security/RoleGuardMatrixIT.java` — 4-role × 2-operation MockMvc matrix (OWNER 201 / PLATFORM_ADMIN legacy 201 / STAFF 403 mutation + 200 read / ADMIN legacy 200 / anonymous 401), exercises real Spring Security filter chain via `SecurityConfig`; (2) `kitehub-frontend/playwright/role-guard.spec.ts` — 6 specs covering OWNER/STAFF/PLATFORM_ADMIN/anonymous against `/admin`, `/dashboard`, `/settings` (RoleGuard `(customer)/settings` layout). Existing per-controller security tests (`SubscriptionControllerSecurityTest`, `PaymentControllerSecurityTest`, `BetaAccessControllerTest`) confirmed comprehensive — synthesis test surfaces single point of failure for future role-mapping drift. CSV completion_pct 90 → 95 (PARTIAL — live browser walkthrough on prod still gated GAP-612 AWS account suspension per `pre-handoff-self-test-completeness.md` §5.4). `PRE_HANDOFF_PARTIAL: AWS-blocked` trailer cited.
- **2026-05-17 (Wave 87 Bucket D)** — Verify-at-spawn state-check per `audit-to-gap-pipeline.md` §2.8: BE seed = `PLATFORM_ADMIN` (verified `grep -rn "PLATFORM_ADMIN" kitehub/kitehub-frontend/src/lib/`); FE `auth-helpers.ts` line 18 accepts both `'PLATFORM_ADMIN' || 'ADMIN'`; AdminLayout + login redirect consume helper; `auth-helpers.test.ts` 10 cases shipped Wave 78 Bucket D. **Code-side fix complete; remaining live browser walk on prod is `pre-handoff-self-test-completeness.md` §2.4 verification step, not code work.** Wave 87 Bucket D NO-OP for this gap — Status PARTIAL 90% unchanged pending live walkthrough gated on prod admin session.
- **2026-05-14 (Wave 78 Bucket D)** — Regression-safe test suite `lib/__tests__/auth-helpers.test.ts` added (10 cases covering canonical PLATFORM_ADMIN, legacy ADMIN alias, OWNER rejection, empty/null/undefined, case sensitivity, partial-match rejections like `SCHOOL_ADMIN` / `PLATFORM`). CSV row `completion_pct` bumped 80 → 90. Live browser walkthrough on prod still pending (gated per `pre-handoff-self-test-completeness.md` §2.4 (b)(c) — documented step-by-step in `documents/05-guides/operations/beta-invite-flow.md` §4.2 + §4.3 runbook).
- **2026-05-14 (Wave 72a Bucket C)** — FE-only Option B shipped. `auth-store.ts` union widened to `'OWNER' | 'ADMIN' | 'PLATFORM_ADMIN'`; `lib/auth-helpers.ts` adds `isPlatformAdmin()` helper accepting both legacy ADMIN and canonical PLATFORM_ADMIN. `AdminLayout.tsx:20,33` + `login/page.tsx:38` consume helper. `AccountTab.tsx:26` widened for consistency. `(school-admin)/layout.tsx` untouched — that route group uses tenant-scoped ADMIN role (different semantics) and authenticates by login alone (no role gate). Tests: extended `auth-store.test.ts` with PLATFORM_ADMIN case; new `AdminLayout.test.tsx` covers (a) PLATFORM_ADMIN accepted, (b) legacy ADMIN accepted, (c) OWNER rejected → redirect /login, (d) unauthenticated rejected. Local verify GREEN: 664/664 tests + lint (warnings only, pre-existing) + production build. Status PARTIAL pending user live walk: login as admin@kitehub.me → expect `/admin` → expect /admin/beta-requests page (sidebar nav added by GAP-519 same PR).

- **2026-05-18 (PR #1556 merged)** — Wave 98 Bucket B7 — Status PARTIAL 90 → 95%. `RoleGuardMatrixIT` 8 MockMvc tests + Playwright `role-guard.spec.ts` 6 specs. **Key finding documented:** Wave 79 GAP-562 already migrated BE to canonical OWNER/STAFF + legacy aliases (PLATFORM_ADMIN/ADMIN resolve to OWNER until 2026-06-14). Audit F-NEW-7 framing (CENTER_OWNER/CENTER_MANAGER/PLATFORM_ADMIN) doesn't match shipped reality — documented in test javadocs not silently renamed. Live browser walkthrough still PARTIAL — gated GAP-612 AWS suspension per `pre-handoff-self-test-completeness.md` §5.4 `PRE_HANDOFF_PARTIAL: AWS-blocked` trailer. Sync per `post-merge-sync-completeness.md` §4.
