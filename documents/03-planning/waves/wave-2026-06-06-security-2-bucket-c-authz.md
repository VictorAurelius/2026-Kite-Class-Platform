---
title: Wave security-2 Bucket C — missing @PreAuthorize authz (GAP-1025 + GAP-1035)
status: complete
created: 2026-06-06
updated: 2026-06-06
waves: [security-2]
wave: 2
tag_primary: security
tags_secondary: [authz, owasp-a01, instance-controller, branding]
counter: 2
date_launch: 2026-06-06
gaps: [GAP-1025, GAP-1035]
---

# Wave security-2 Bucket C — missing @PreAuthorize authz

**Mục tiêu:** Bucket C của Wave security-2 (P0 cluster từ Flow Verification Campaign). Fix 2 missing-@PreAuthorize gaps: GAP-1025 (P0 InstanceController) + GAP-1035 (P1 BrandingController). User chọn C (authz, clearest) trước B (IDOR).

## 1. Brainstorm

GAP-1025 (P0) + GAP-1035 (P1) cùng class "controller thiếu @PreAuthorize → broken access control (OWASP A01)". Đơn giản nhất trong P0 cluster — chỉ thêm annotation, không cần redesign. InstanceController (kitehub-subscription) mix public/owner/admin ops → per-method authz (gate chỉ admin/destructive ops, không blanket). BrandingController (kiteclass-core) → clear ADMIN/OWNER (sister BrandingVersionController đã có pattern). Cả 2 service có @EnableMethodSecurity.

## 2. Task Breakdown

1. State-check: confirm 2 controller 0 @PreAuthorize (still live).
2. GAP-1035: @PreAuthorize('ADMIN','OWNER') trên BrandingController PUT/logo/favicon.
3. GAP-1025: @PreAuthorize('PLATFORM_ADMIN','ADMIN') trên InstanceController list/listCursor/delete/purge/extendTrial.
4. Sweep tests (api-contract-change-caller-sweep §3.2): @PreAuthorize đổi contract → IT/MVC tests cần @WithMockUser; run tests not just compile.
5. Rebuild 2 service + re-walk (STAFF/owner→403, admin→200).
6. Close gaps + PR.

## 3. Scope

- `kiteclass-core .../module/settings/controller/BrandingController.java` — @PreAuthorize on 3 mutation methods + import (GAP-1035).
- `kitehub-subscription .../controller/InstanceController.java` — @PreAuthorize on 5 admin methods + import (GAP-1025).
- `kitehub-subscription .../contract/InstanceApiContractTest.java` — class-level @WithMockUser(PLATFORM_ADMIN) (caller sweep).
- `kitehub-subscription .../controller/InstanceControllerIntegrationTest.java` — method @WithMockUser on shouldDelete (caller sweep).

## 4. State-Check Evidence

| Symbol | Verify | Verdict |
|--------|--------|---------|
| InstanceController 0 @PreAuthorize | `grep -c @PreAuthorize` | ✅ 0 (hole live) |
| BrandingController 0 @PreAuthorize | grep | ✅ 0 (hole live) |
| Both services @EnableMethodSecurity | grep SecurityConfig | ✅ active (subscription :61, kiteclass :28) |
| sister BrandingVersionController @PreAuthorize literal | grep | ✅ `hasAnyRole('ADMIN','OWNER')` (reuse) |
| AdminEmailController role-literal (subscription) | grep | ✅ `hasRole('PLATFORM_ADMIN')` (reuse PLATFORM_ADMIN/ADMIN) |

Gaps filed 2026-06-06 (<1 day, runtime-verified same session per `audit-to-gap-pipeline.md` §2.8).

## 5. Verification Gates

### Re-walk (live gateway :9000 post-rebuild, per `pre-handoff-self-test-completeness.md` §3)

| Check | Before | After |
|-------|--------|-------|
| GAP-1025 owner GET /api/platform/instances (enumerate all) | 200 | **403** ✅ |
| GAP-1025 owner DELETE purge any instance | reachable | **403** ✅ |
| GAP-1025 PLATFORM_ADMIN GET instances | 200 | **200** ✅ |
| GAP-1035 STAFF PUT branding (A01) | 200 | **403** ✅ |
| GAP-1035 OWNER PUT branding | 200 | **200** ✅ |
| GAP-1035 GET branding (ungated read) | 200 | **200** ✅ |

### Tests (per `api-contract-change-caller-sweep.md` §3.3 — run not just compile)

- InstanceApiContractTest (5 prior failures 403-vs-expected) → @WithMockUser(PLATFORM_ADMIN) class-level → re-run PASS (mvnw exit 0).
- InstanceControllerIntegrationTest.shouldDeleteInstanceSuccessfully → method @WithMockUser → PASS.
- BrandingControllerTest → 7/7 PASS unchanged (TestSecurityConfig permit-all; 403 gating verified via live re-walk).

## 6. Agent Spawn Pattern

Solo coordinator (NO agent spawn). Security-sensitive authz + cross-service → fix + test-sweep + re-walk tự làm. Caller-sweep per `api-contract-change-caller-sweep.md` (annotation = contract change → test fixup).

## 7. Closure Protocol

### Scope-Completeness Reconciliation (per `wave-closure-scope-completeness.md` §3)

| # | Bucket C item | Verdict | Follow-up |
|---|---|---|---|
| 1 | GAP-1025 InstanceController @PreAuthorize + re-walk + test sweep | ✅ DONE | — |
| 2 | GAP-1035 BrandingController @PreAuthorize + re-walk | ✅ DONE | unit-403 test (TestSecurityConfig harness) = P3 follow-up |

Bucket C complete. Wave security-2 still in-progress overall (Bucket B IDOR queued).

### Sync targets
- gap-status.csv: GAP-1025/1035 → DONE + moved closed/ ✅
- wave-history.jsonl: security-2 Bucket C entry ✅

### Outcome
Bucket C **DONE** — 2 authz gaps closed (1 P0 + 1 P1), verified live (owner/STAFF→403, admin/owner-legit→200, zero regression), tests swept + PASS. 5/7 P0 cluster now closed (Bucket A 3 + Bucket C 1 = GAP-1031/1034/1041/1025). Remaining P0: GAP-1015/1019/1023 (Bucket B IDOR). Code PR.

## 8. Log

- **2026-06-06:** Bucket C shipped. BrandingController + InstanceController @PreAuthorize. Caller-sweep: @PreAuthorize broke 5 InstanceController tests (403-vs-expected) → added @WithMockUser(PLATFORM_ADMIN) per `api-contract-change-caller-sweep.md` → re-run PASS. Rebuild subscription + kiteclass-core + re-walk all PASS. GAP-1025 + GAP-1035 DONE. Bucket B (IDOR) next.
