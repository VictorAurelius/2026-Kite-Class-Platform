# GAP-562: RBAC role separation Customer vs Staff — staff layout + permission guard missing

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Mixed
**Detected:** 2026-05-14
**Related Audits:** `documents/04-quality/audits/persona-review/2026-05-14-pre-wave-79-outside-in.md` (Persona 2 N2-P0)
**Related Gaps:** GAP-561 (invite-staff template + endpoint — sister gap; both block P3 Manager flow); GAP-554 (onboarding JWT cross-check — partial overlap)

## Current State (verified 2026-05-14)

| Piece | File / Path | Status |
|-------|-------------|--------|
| Sidebar variant `admin` (PLATFORM_ADMIN) | `kitehub/kitehub-frontend/src/components/layout/Sidebar.tsx` line 25-30 (adminNav 4 entries) | ✅ shipped |
| Sidebar variant `customer` (P2 Owner) | line 16-21 (customerNav 4 entries) | ✅ shipped |
| Sidebar variant `staff` (P3 Manager/Teacher/Accountant) | n/a | ❌ missing |
| AdminLayout role guard | `AdminLayout.tsx` line 21-25: `isPlatformAdmin(user?.role)` | ✅ partial — only checks PLATFORM_ADMIN, not staff sub-roles |
| Customer-route layout role guard | `(customer)/layout.tsx` — need verify | ⚠️ likely accepts ANY authenticated user, không phân biệt Owner vs Staff |
| Permission matrix doc | `documents/01-business/kitehub/auth/rules.md` | ⚠️ likely missing per-role permission table |
| Backend `@PreAuthorize` on sensitive endpoints | `kitehub-subscription/.../billing/BillingController` + `branding/BrandingController` | ⚠️ verify required |

**Grep commands run:**
```bash
grep -rn "MANAGER\|TEACHER\|ACCOUNTANT" kitehub/kitehub-frontend/src --include="*.tsx" --include="*.ts" 2>/dev/null
# Result: TBD — likely 0 hits, no role-specific UI gating
grep -rn "@PreAuthorize" kitehub/kitehub-subscription/src/main/java 2>/dev/null
# Result: TBD — verify role-based authorization
find kitehub/kitehub-frontend/src/lib -iname "*role*" -o -iname "*permission*" 2>/dev/null
# Result: lib/auth-helpers.ts known (isPlatformAdmin); per-role helpers TBD
```

## Problem

Current architecture có 2 user layout: `customer` (P2 Owner) + `admin` (PLATFORM_ADMIN). KHÔNG có variant cho staff sub-roles (MANAGER/TEACHER/ACCOUNTANT). Hậu quả khi P3 Manager (invited bởi GAP-561 flow) login:

- AdminLayout không match (not PLATFORM_ADMIN) → redirect /login
- Customer layout accepts any authenticated user → Manager thấy đầy đủ Sidebar customerNav: **Tổng quan / Thanh toán / AI Branding / Cài đặt**
- Manager click "Thanh toán" → thấy thẻ tín dụng + invoice history của Owner → **sensitive data leak**
- Manager click "AI Branding" → regenerate banner → **incur cost cho Owner mà không có permission**
- Manager click "Cài đặt" → modify tenant config → **operational risk**

→ **Trust crisis nghiêm trọng**: nếu beta tenant phát hiện invited staff thấy data nhạy cảm → churn Owner toàn bộ tenant + có thể trigger PDPL data-protection ticket nếu staff không nên thấy.

Inside-out queue Wave 79 KHÔNG có gap cover RBAC. GAP-554 chỉ cover X-Tenant-Id JWT cross-check (cross-tenant isolation), không cover **within-tenant** role permission boundary.

## Context

Outside-in audit Persona 2 Anh Tâm walkthrough Bước 2 surface: Manager login → customer layout full access → permission boundary KHÔNG enforce.

Sister audit 2026-05-14 PR-THUY-14 đã flag RBAC P0 nhưng chưa filed → file gap riêng đây.

Tham chiếu: industry standard SaaS multi-tenant (Notion, Linear, Stripe) — role-based access là baseline expectation cho B2B SaaS. Vắng RBAC = unprofessional + risky.

## Evidence

- `Sidebar.tsx` chỉ có 2 variant `customer` | `admin` — không phân biệt role granularity
- `AdminLayout.tsx` line 22 `isPlatformAdmin(user?.role)` — single-role check
- `auth-helpers.ts` known to have `isPlatformAdmin` — likely no `isOwner` / `isManager` / `isTeacher` helpers
- `BetaRequestForm.tsx` line 21 enum `BetaPersona = 'P1_SOLO_TEACHER' | 'P2_CENTER_OWNER'` — only persona, không phải role within tenant

## Proposed Fix

### Backend (kitehub-subscription)

1. **DB schema:** tenant_users.role enum extends (P0 only enumerate Phase 1 BETA roles):
   ```
   OWNER (default cho P2/P3 tenant creator)
   MANAGER (operational manager)
   TEACHER (read+attendance for assigned classes only)
   ACCOUNTANT (read+payment write, no student PII)
   ```
2. **Spring Security @PreAuthorize** trên sensitive endpoints:
   - `BillingController` → `@PreAuthorize("hasRole('OWNER')")`
   - `BrandingController` (regenerate) → `@PreAuthorize("hasRole('OWNER')")`
   - `TenantSettingsController` → `@PreAuthorize("hasRole('OWNER')")`
   - `AttendanceController` write → `@PreAuthorize("hasAnyRole('OWNER','MANAGER','TEACHER')")`
   - `PaymentController` write → `@PreAuthorize("hasAnyRole('OWNER','MANAGER','ACCOUNTANT')")`
   - `StudentController` read PII → `@PreAuthorize("hasAnyRole('OWNER','MANAGER','TEACHER')")` (no ACCOUNTANT)
3. **JWT claims** include `role` per-tenant (đã có user.role; verify scope tenant-specific)
4. **Business doc** `documents/01-business/kitehub/auth/permission-matrix.md` (or extend rules.md) — table role × resource × action

### Frontend (kitehub-frontend)

5. **Add Sidebar variant** `staff` với conditional nav based on role:
   ```tsx
   const navByRole: Record<UserRole, NavItem[]> = {
     OWNER: [Tổng quan, Thanh toán, AI Branding, Thành viên, Lớp học, Học viên, Cài đặt],
     MANAGER: [Tổng quan, Lớp học, Học viên, Chấm công, Báo cáo],
     TEACHER: [Tổng quan, Lớp học (assigned), Chấm công],
     ACCOUNTANT: [Tổng quan, Thanh toán (no PII), Báo cáo tài chính],
   };
   ```
6. **Update `Sidebar.tsx`** variant prop từ `'customer' | 'admin'` → `'admin' | UserRole`
7. **Update `(customer)/layout.tsx`** add role-based redirect:
   - OWNER → render customer layout
   - MANAGER/TEACHER/ACCOUNTANT → render staff layout (different nav)
   - PLATFORM_ADMIN → redirect /admin
8. **Add `lib/auth-helpers.ts`** helpers:
   ```ts
   export function isOwner(role?: string): boolean { return role === 'OWNER'; }
   export function isStaff(role?: string): boolean { return ['MANAGER', 'TEACHER', 'ACCOUNTANT'].includes(role ?? ''); }
   export function canAccessBilling(role?: string): boolean { return role === 'OWNER'; }
   // ... per-resource gates
   ```
9. **Update FE pages** với conditional render:
   - `/billing/page.tsx` → check `canAccessBilling` → else redirect /dashboard với toast "Bạn không có quyền truy cập"
   - `/branding/page.tsx` → tương tự
   - `/settings/page.tsx` → tương tự

### Testing

10. **E2E tests** `kitehub-frontend/src/e2e/rbac.spec.ts`:
    - Login as MANAGER → navigate `/billing` → expect redirect + toast
    - Login as MANAGER → API call `POST /api/v1/branding/regenerate` → expect 403
    - Login as TEACHER → navigate `/payments` → expect redirect
11. **BE integration tests** `BillingControllerSecurityTest`, `BrandingControllerSecurityTest` — verify @PreAuthorize enforces

## Acceptance Criteria

- [ ] DB: `tenant_users.role` enum extended với MANAGER/TEACHER/ACCOUNTANT
- [ ] BE: 6+ sensitive controller endpoints có `@PreAuthorize` matching permission matrix
- [ ] Business doc `documents/01-business/kitehub/auth/permission-matrix.md` ship với role × resource × action table
- [ ] FE: `Sidebar` variant prop accepts UserRole + staff nav rendered correctly
- [ ] FE: role-based redirect + permission helpers in `auth-helpers.ts`
- [ ] FE: 3+ sensitive pages check permission before render (`/billing` / `/branding` / `/settings`)
- [ ] E2E test `rbac.spec.ts` cover 6+ permission scenarios
- [ ] BE Security tests cover @PreAuthorize on 6+ endpoints
- [ ] Persona test: P2 Owner invites MANAGER → MANAGER login → MANAGER cannot see billing/branding → MANAGER can do attendance/class

## Related

- GAP-561 (invite-staff template + endpoint — sister; must ship together)
- GAP-554 (X-Tenant-Id JWT cross-check — partial overlap, cross-tenant isolation; this gap covers within-tenant)
- GAP-552 (SecurityConfig default-allow fallback — sister auth hardening)
- Rule: `.claude/rules/pre-launch-auth-hardening-checklist.md` §2.7 admin audit log + §2.4 2FA admin
- Audit: `documents/04-quality/audits/persona-review/2026-05-14-pre-wave-79-outside-in.md`
- Sister audit 2026-05-14 phase-1-beta-walkthrough PR-THUY-14

## Log

- **2026-05-15 (Wave 80 Bucket C):** Upgrade to **PARTIAL 90%** via GAP-562b PR. FE RoleGuard + 3 layouts + role-aware Sidebar + auth-store STAFF type + BE @PreAuthorize on PaymentController + SubscriptionController + RbacAccessDeniedHandler audit log. 15 BE security tests + 13 FE component tests + 8 Playwright e2e all pass. Remaining 10% = `kitehub-branding` `@PreAuthorize` (module needs spring-security dep first, Wave 81 follow-up) + `TenantSettingsController` dangerzone split (controller doesn't exist yet). Main attack surface (URL bar + Sidebar leak) closed.

- **2026-05-14:** PARTIAL 50% — Wave 79 Bucket B closure. V46__create_rbac_roles.sql + PlatformRole enum + @PreAuthorize on staff endpoints shipped via PR #1366. FE role-guard + full @PreAuthorize coverage (billing/branding) deferred to GAP-562b (Wave 80+). Sister gap to GAP-561.

- 2026-05-14 — Filed via Wave 79 outside-in audit (Persona 2 N2-P0). State-check confirmed no staff sub-role enum / no @PreAuthorize / no FE role gating. Priority P0 vì multi-user beta tenant trust + PDPL data isolation. Sister gap GAP-561 phải ship cùng để Manager invite + login an toàn.
