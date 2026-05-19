# GAP-684: GAP-518 live admin-login walk gated GAP-612 AWS restore

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 (Plan 1 launch verification blocker — code shipped nhưng chưa live verify)
**Domain:** DevOps (AWS infra restore blocker)
**Found:** 2026-05-19 (Wave 101 Bucket A retry — GAP-518 PARTIAL 95→97% exit ramp)
**Affects:** GAP-518 final closure (admin role-guard live verification path)

## Problem

GAP-518 (BE seed `PLATFORM_ADMIN` vs FE guard mismatch) code-side complete tại Wave 101 Bucket A:
- BE `RoleGuardMatrixIT` 8/8 PASS local (`cd kitehub && ./mvnw -pl kitehub-subscription verify -Dtest=RoleGuardMatrixIT`)
- FE 27/27 PASS local (`pnpm test --run auth-helpers RoleGuard AdminLayout Sidebar`)
- `auth-helpers.ts` line 18 accepts both `PLATFORM_ADMIN` (canonical) và legacy `ADMIN`
- AdminLayout + login redirect consume helper đúng

**Live browser walkthrough chưa thực hiện được** vì AWS production stack (account 906286017800) suspended từ 2026-05-17 16:50 UTC per GAP-612 (status OPEN, completion_pct=0). Cannot test:
- POST /api/auth/login với seeded PLATFORM_ADMIN credential → expect HTTP 200 + JWT
- Browser → `/admin` dashboard render
- Browser → `/admin/beta-requests` no 403/redirect

Per `pre-handoff-self-test-completeness.md` §2.4 admin-flow checklist (b)(c)(d): all "Pending live verify" cho đến khi AWS restore.

## Root Cause

GAP-612 chain dependency: AWS Activate Founder application denied (per GAP-459 history) + account suspension → production stack down → no live verify endpoint available → GAP-518 cannot reach 100% per `gap-done-discipline.md` §2 (every AC verified) until GAP-612 closes.

## Proposed Fix

1. Monitor GAP-612 AWS restore status (user action required check email reply AWS support)
2. Sau khi AWS restore (GAP-612 → DONE):
   - Retrieve admin@kitehub.me credential từ AWS Secrets Manager
   - Execute live walk per `pre-handoff-self-test-completeness.md` §2.4 admin-flow checklist:
     - (a) Credential available ✓
     - (b) Login API works (curl POST /api/auth/login → HTTP 200 + JWT)
     - (c) Login UI works (browser → submit → redirects to `/admin`)
     - (d) Role-guard accepts (post-login user sees admin dashboard, NOT 403/redirect)
     - (e) Navigation: nav button hoặc URL `/admin/beta-requests` works
     - (f) Page renders (data loads, NOT spinner/crash)
3. Update GAP-518 status PARTIAL 97% → DONE 100% với live verify evidence
4. Update GAP-518 CSV row last_verified date
5. git mv GAP-518 file sang `phase-1-beta/closed/` per `gap-folder-organization.md` §3.3

## Acceptance Criteria

- [ ] GAP-612 status DONE (AWS account 906286017800 restored)
- [ ] Admin credential retrieved từ AWS Secrets Manager (`aws secretsmanager get-secret-value ...`)
- [ ] curl POST /api/auth/login → HTTP 200 + JWT trong response body
- [ ] Browser walkthrough complete: login → `/admin` dashboard render
- [ ] Browser walkthrough complete: `/admin/beta-requests` page render
- [ ] GAP-518 flipped DONE 100% + git mv sang `closed/`
- [ ] GAP-518 CSV row updated với live verify evidence trong notes

## Related

- Parent: GAP-518 (PARTIAL 97% — code-side complete Wave 101 Bucket A)
- Blocker: GAP-612 (AWS account 906286017800 suspension recovery — OPEN P0)
- Rule: `pre-handoff-self-test-completeness.md` §2.4 + §5.4 (PRE_HANDOFF_PARTIAL: AWS-blocked trailer)
- Rule: `gap-done-discipline.md` §3 (PARTIAL exit ramp — code shipped + follow-up gap filed)

## Log

- **2026-05-19 (Wave 101 Bucket A)** — Gap được tạo do GAP-518 đã ship 100% code-side (RoleGuardMatrixIT 8/8 + FE auth-helpers/RoleGuard/AdminLayout/Sidebar 27/27 PASS local), nhưng live browser walk gated bởi GAP-612 AWS suspension. Per `gap-done-discipline.md` §3 PARTIAL exit ramp: GAP-518 stays PARTIAL 97% với follow-up gap (this file) tracking live verify path khi AWS restore. PRE_HANDOFF_PARTIAL: AWS-blocked trailer cited trong commit body.
