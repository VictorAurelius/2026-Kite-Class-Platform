---
title: Wave flow-kc12 — KC-12 Reschedule/payroll/gamification/analytics G1 walk
status: complete
created: 2026-06-06
updated: 2026-06-06
waves: [flow-kc12]
wave: wave-2026-06-06-flow-kc12
tag_primary: flow-kc12
tags_secondary: [payroll, reschedule, gamification, analytics, campaign-g1]
date: 2026-06-06
flow: KC-12 (Reschedule / payroll / gamification / analytics)
gaps: [GAP-1041, GAP-1042, GAP-1043]
---

# Wave flow-kc12 — KC-12 Reschedule/payroll/gamification/analytics G1 walk

**Mục tiêu:** G1 (agent runtime walk) cho flow KC-12 — payroll (admin read-only) + class reschedule + gamification (internal) + analytics. **Flow secondary thứ 9 — G1 CUỐI CÙNG → 22/22 G1 hoàn tất.**

## 1. Brainstorm

Thin flow như dự đoán. Walkable surface thực = 3 payroll GET (ADMIN read-only) + 1 reschedule POST. Gamification = internal `PointService` (no controller, award on attendance). Analytics = overlaps ReportController (walked KC-11). Risk: routing collision (recurrence?), payroll cross-tenant (GAP-1039 class), reschedule IDOR + state machine.

## 2. Task Breakdown

1. Pre-walk Opus persona-sim → artifact.
2. MUST-run (payroll routing/role + reschedule authz/IDOR).
3. Walk payroll + reschedule; verify gamification/analytics no-surface.
4. Catalog → file gaps → wave plan + sync.

## 3. Scope

- `kiteclass-core`: `PayrollController` (`/api/v1/admin/payroll/{configs,periods,periods/{id}}`, ADMIN GET); `ClassController.rescheduleClass` (`POST /api/v1/classes/{classId}/reschedule`, `@authz.hasAccessToClass`); `PointServiceImpl` (internal, no controller); analytics = ReportController (KC-11).
- `kitehub-gateway`: route `kitehub-admin-v1` (collision source).

## 4. State-Check Evidence

- Stack up healthy. Auth: minted ADMIN/TEACHER HS512 (payroll ADMIN-only; OWNER→403). Classes: id 4,5 (owner.test tenant aaaabbbb, IN_PROGRESS), 13 (ad0fa96e SCHEDULED), 8,10 (0edaee10 SCHEDULED).
- Payroll tables: `payroll_periods` (0 rows), `payroll_configs`. RescheduleReasonCategory enum: GV_OM_BAN_DOT_XUAT / PHONG_HOC_KHONG_KHA_DUNG / MAT_DIEN_INTERNET / LE_TET_NGHI_CHINH_THUC / HOC_SINH_XIN_NGHI_TAP_THE / LY_DO_KHAC.

## 5. Verification Gates

### Pre-walk

Opus persona-sim, 8 FM, artifact `documents/04-quality/audits/persona-review/2026-06-06-pre-walk-kc12-reschedule-payroll-gamification.md` (🟠2 🟡4 🟢2). Thin-flow confirmed (gamification/analytics no walkable surface). Highest: payroll cross-tenant leak = KC-11 GAP-1039 recurrence (repos filter-only, salary data).

### G1 walk — evidence (live)

**Payroll:**
- 🔴 **routing collision** (Bug #1): `GET :9000/api/v1/admin/payroll/{configs,periods}` → **404** "Endpoint not found" (routes to kitehub-admin via `/api/v1/admin/**`). Direct :8080 → **200** (backend OK, empty). OWNER→403, ADMIN-only gate đúng. → GAP-1041.

**Reschedule — fully working + secure:**
- Happy: ADMIN reschedule class 4 (set SCHEDULED) → **200**, DB dates → 2026-07-01/2026-09-30, **outbox `class.rescheduled` event published** ✅.
- Authz: `@authz.hasAccessToClass` — admin bypass (ROLE_ADMIN/PLATFORM_ADMIN→true) + non-admin khớp `classes.teacher_id`. OWNER (không teacher của class) → 403.
- **IDOR DEFENDED:** OWNER cross-tenant class 13 → 403; ADMIN cross-tenant class 13 → **404 CLASS_NOT_FOUND** (admin bypass authz nhưng lookup tenant-scoped → không leak).
- **State machine guard:** ADMIN reschedule IN_PROGRESS class → **400 CLASS_CANNOT_RESCHEDULE** (chỉ SCHEDULED).
- 🟡 **Validation gap** (Bug #3): `newStartDate=2020-01-01` (past) → **200** accepted (thiếu @FutureOrPresent). → GAP-1043.

**Gamification/analytics:** PointService no controller (internal, attendance-triggered) → no walkable surface ✅. Analytics = ReportController (walked KC-11) ✅.

**Bug surfaced (1 P0 + 1 P1-meta + 1 P2 — all filed, no inline fix):**
- 🔴 **GAP-1041 P0**: payroll routing collision (recurrence #3).
- 🟠 **GAP-1042 P1 META**: gateway route-predicate audit (systemic — 3 recurrences GAP-1031/1034/1041).
- 🟡 **GAP-1043 P2**: reschedule past-date validation gap.

**Latent (noted, not separate gaps):** payroll cross-tenant leak (repos filter-only, GAP-1039 sister — payroll_periods empty + unreachable via gateway → doubly-latent; fix cùng GAP-1039 security-1); FM-3 StudentPoint no @Filter (gamification cross-tenant point collision, internal no-controller, Wave 106); FM-4 reschedule outbox tenantId=null when context null (Wave 106 dispatcher).

**No inline fix** — GAP-1041 gateway edge + GAP-1042 systemic audit → dedicated gateway-route wave; GAP-1043 validation → clazz hardening. Per `release-fix-retry-budget` §3.5 investigation-first done.

## 6. Agent Spawn Pattern

1 Opus pre-walk persona-sim (background, model opus). Walk solo coordinator.

## 7. Closure Protocol

### Discoveries filed (per `discovery-to-gap-inline-filing.md` §3)

- GAP-1041 P0 — payroll routing collision (Backend/gateway)
- GAP-1042 P1 META — gateway route-predicate audit (Backend/systemic)
- GAP-1043 P2 — reschedule past-date validation (Backend/validation)

### Cross-flow sweep (per `cross-flow-bug-class-sweep.md`)

**Routing collision class — 3rd recurrence → ESCALATED to META GAP-1042.** GAP-1031 (email expose) + GAP-1034 (branding shadow) + GAP-1041 (payroll shadow) đều root "gateway predicate too broad". Systemic audit filed. **Cross-flow sweep cho gateway routes = GAP-1042 scope** (audit all catch-all `/**` routes + service-ownership).

**Payroll cross-tenant (GAP-1039 sister):** payroll repos filter-only như reports → GAP-1039 fix phải cover payroll repos. Documented in GAP-1041 Related.

### Sync targets

- gap-status.csv: 3 rows ✅
- campaign §4 table: KC-12 → 🔄 walk-pass-pending-human ✅ **(22/22 G1 complete)**
- wave-history.jsonl: flow-kc12 entry ✅
- audits-index.csv: pre-walk row ✅

### Outcome

KC-12 G1 **PASS** — reschedule fully working + secure (IDOR defended, state-machine guard, outbox); payroll backend OK (blocked by routing collision); gamification/analytics no walkable surface. 1 P0 routing + 1 P1 META gateway-audit + 1 P2 validation. **🎯 22/22 flow đạt G1 — hoàn tất giai đoạn G1-all-first.** Next = mở đợt G2 human-walk tập trung (8 flow secondary thiếu G2 recipe + KC-11/KC-12). Campaign KC-12 → `🔄 walk-pass-pending-human`. Docs-only PR.

## 8. Log

- **2026-06-06:** G1 walk (flow G1 cuối → 22/22). Pre-walk Opus 8 FM (thin-flow confirmed). Walk: payroll 404 via gateway / 200 direct (routing collision GAP-1041 recurrence #3); reschedule happy 200 + outbox + IDOR DEFENDED (cross-tenant 403/404) + state-machine guard (IN_PROGRESS→400) + past-date validation gap (GAP-1043); gamification/analytics no-surface. Escalated routing collision → META GAP-1042 (gateway route audit). Payroll cross-tenant = GAP-1039 sister latent. No inline fix. Campaign → walk-pass-pending-human. **G1-all-first DONE.**
