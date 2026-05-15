---
id: GAP-562b
title: FE role-guard component + @PreAuthorize coverage extension billing/branding
status: PARTIAL
priority: P0
layer: Mixed
phase: phase-1-beta
percent_complete: 85
created: 2026-05-14
updated: 2026-05-15
parent: GAP-562
wave_target: 80
---

# GAP-562b — FE role-guard + @PreAuthorize billing/branding coverage

**Type:** Sister/follow-up gap (split from GAP-562 PARTIAL 50% Wave 79 Bucket B)
**Priority:** 🔴 P0 — RBAC enforcement incomplete = STAFF role sees OWNER-only screens
**Wave target:** 80 DEPLOY+SMOKE (bundle with GAP-561b — sister invite gap)
**Estimate:** ~3-4h single bucket

---

## Problem

Wave 79 Bucket B (PR #1366) shipped V46 migration + PlatformRole enum (OWNER/STAFF) + `@PreAuthorize("hasAuthority('OWNER')")` on staff endpoints only. Coverage gap:

- ❌ FE has NO `RoleGuard` component — every Owner-only page (billing/branding/staff settings) accessible if user knows the URL
- ❌ Billing controllers (`/api/v1/billing/*`) missing @PreAuthorize — STAFF can read invoices + update payment method
- ❌ Branding controllers (`/api/v1/branding/*`) missing @PreAuthorize — STAFF can change tenant logo + colors
- ❌ Staff settings page (`/admin/staff/*`) reachable without role check (sister to GAP-561b which adds the routes)

Result: Manager (STAFF role) invited via beta has Owner-level access to billing + branding — privacy + privilege escalation risk.

---

## Scope (this follow-up)

### FE RoleGuard component

- [ ] `kitehub-frontend/src/components/RoleGuard.tsx` — wraps children, redirects to `/dashboard` if role mismatch
- [ ] `useRole()` hook reads JWT claim `role` (OWNER | STAFF)
- [ ] HOC alternative: `withRoleGuard(Component, allowedRoles[])`
- [ ] Apply to routes:
  - `/admin/billing/**` — OWNER only
  - `/admin/branding/**` — OWNER only
  - `/admin/staff/**` — OWNER only (sister GAP-561b adds these)
  - `/admin/settings/dangerzone` — OWNER only

### BE @PreAuthorize extension

- [ ] `BillingController` — all mutation endpoints `@PreAuthorize("hasAuthority('OWNER')")`
- [ ] `InvoiceController` — read endpoints OWNER+STAFF, mutation OWNER only
- [ ] `BrandingController` — all endpoints OWNER only (no STAFF coverage at all per business rule)
- [ ] `TenantSettingsController` — segment by sub-resource (general OWNER+STAFF read, dangerzone OWNER only)
- [ ] Method security default `@EnableMethodSecurity(prePostEnabled = true)` verified in SecurityConfig

### Sidebar nav role-aware

- [ ] `Sidebar.tsx` `customerNav` filters items by role — STAFF doesn't see Billing/Branding/Staff sections
- [ ] Visual indicator on owner-only items (lock icon + tooltip "Owner only")

### Tests

- [ ] `BillingControllerSecurityTest` — STAFF role hitting Owner endpoints → 403
- [ ] `BrandingControllerSecurityTest` — STAFF role → 403 across all endpoints
- [ ] FE Playwright E2E: login as STAFF → navigate to /admin/billing → expect redirect to /dashboard
- [ ] FE Vitest unit: `RoleGuard` renders children when role matches, redirects when mismatch

## Acceptance Criteria

- [ ] STAFF role authenticated user → all /admin/billing/* requests return 403 from BE
- [ ] STAFF role logged in → /admin/branding/* URL bar entry → FE redirects to /dashboard within 100ms
- [ ] Sidebar nav hides Owner-only sections when STAFF logged in
- [ ] OWNER role unchanged behavior — full access maintained
- [ ] Audit log entry on 403 attempts (security event)
- [ ] Wave 80 post-wave Security audit v2 format (per GAP-564) verifies RBAC + provides per-control evidence

## Dependencies

- **Sister:** GAP-561b (invite-staff email + FE routes + InvitationController real impl) — bundle in same Wave 80 bucket
- **Upstream:** GAP-562 PARTIAL 50% (V46 + PlatformRole enum + staff endpoints @PreAuthorize) — Wave 79 Bucket B PR #1366 SHIPPED
- **Cross-link:** GAP-564 audit format v2 — must verify this RBAC coverage with per-control evidence (Command run + Output + Verdict + Evidence artifact ID)

## Refs

- `documents/01-business/roles/{rules,use-cases,api-contract}.md` (Wave 79 Bucket 0 Foundation)
- `documents/03-planning/session-handoffs/2026-05-15-post-wave-79-handoff.md` §2.2
- `.claude/rules/pre-launch-auth-hardening-checklist.md` — RBAC enforcement section

## Log

- **2026-05-15 (Wave 80 Bucket C):** PARTIAL 85% shipped via this PR. Delivered:
  - **FE** `useRole` hook + `RoleGuard` component (canonical OWNER/STAFF + legacy PLATFORM_ADMIN/ADMIN aliases resolve to OWNER)
  - **FE layouts** wrap `/billing/*`, `/branding/*`, `/settings/*` with `<RoleGuard allowedRoles={['OWNER']}>` (STAFF → `/dashboard` within 100ms)
  - **FE Sidebar** `customerNav` items gated by `requiresRole`; OWNER sees 5 items, STAFF sees 2 (Tổng quan + Bắt đầu); hidden during hydration to prevent flash
  - **FE auth-store** `User.role` union extended with `STAFF`
  - **BE `PaymentController`** all 5 endpoints `@PreAuthorize`: POST OWNER-only; GET endpoints OWNER+STAFF (`OWNER_AUTHZ` / `OWNER_OR_STAFF_AUTHZ` constants mirroring `StaffInvitationController` pattern)
  - **BE `SubscriptionController`** all 9 endpoints `@PreAuthorize`: mutations (POST/PATCH/DELETE) OWNER-only, GET endpoints OWNER+STAFF
  - **BE `RbacAccessDeniedHandler`** writes `admin_audit_log` row on every 403 (action=`RBAC_DENIED:<METHOD>:<URI>`, success=false, payload includes denied roles + reason); wired into SecurityConfig via `ObjectProvider<AccessDeniedHandler>` so test profile remains permissive
  - **BE tests** `PaymentControllerSecurityTest` (7 tests) + `SubscriptionControllerSecurityTest` (8 tests) — 15/15 pass; full `kitehub-subscription` suite 636/636 pass
  - **FE tests** `RoleGuard.test.tsx` (5 tests) + updated `Sidebar.test.tsx` (8 tests, role-aware) — 13/13 pass; full FE suite 745/745 pass
  - **Playwright** `e2e/role-guard.spec.ts` (8 scenarios) — STAFF→OWNER routes redirect, OWNER full access, Sidebar visibility per role; `test-helpers.setupMockAuth` extended with STAFF support
  - **TS typecheck + ESLint** clean across new files

  **Deferred (PARTIAL 15%):**
  - **`kitehub-branding` module** does NOT have `spring-boot-starter-security` on classpath → `@PreAuthorize` not enforceable here. Defense in Wave 80 = (a) FE `RoleGuard` at `/branding/*` route layout, (b) gateway path-level auth filter. Documented at class javadoc of `BrandingJobController`. Wave 81 follow-up: add spring-security dep + `@EnableMethodSecurity` to branding module + replicate `@PreAuthorize` annotations + security tests. Tracked as in-place defer per `gap-done-discipline.md` §3 exit ramp.
  - **`TenantSettingsController` dangerzone sub-resource segmentation** deferred — current `/settings/*` is FE-only RoleGuard OWNER-only; no separate BE controller exists at `/api/v1/tenants/me/settings` yet. When DangerZone BE lands, mutation endpoints get OWNER-only, general GET OWNER+STAFF.
  - **OWNER+STAFF read split for tenant settings** (general read could be OWNER+STAFF, DangerZone OWNER-only) — Wave 81+ work once the BE controller exists.

  Status PARTIAL 85% — main attack surface closed (billing + branding URL bar + Sidebar leakage); deferred portion = post-Wave-80 wiring once branding security + tenant settings BE land.

- **2026-05-14:** Filed as Wave 79 Bucket B PARTIAL exit-ramp per `gap-done-discipline.md` §3. Sister to GAP-561b. P0 BLOCKING v0.9.0-beta — STAFF role privilege escalation risk if not enforced before tenant invite.
