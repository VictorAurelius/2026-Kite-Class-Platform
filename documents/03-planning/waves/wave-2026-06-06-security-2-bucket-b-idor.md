---
title: Wave security-2 Bucket B — cross-tenant IDOR ownership binding (GAP-1015 + GAP-1019 + GAP-1023)
status: complete
created: 2026-06-06
updated: 2026-06-06
waves: [security-2]
wave: 2
tag_primary: security
tags_secondary: [idor, owasp-a01, subscription, branding, domain, tenant-binding]
counter: 2
date_launch: 2026-06-06
gaps: [GAP-1015, GAP-1019, GAP-1023]
---

# Wave security-2 Bucket B — cross-tenant IDOR ownership binding

**Mục tiêu:** Bucket B của Wave security-2 (P0 cluster cuối từ Flow Verification Campaign). Fix 3 cross-tenant IDOR gaps (OWASP A01): GAP-1015 (P0 subscription lifecycle) + GAP-1019 (P0 branding X-Instance-Id) + GAP-1023 (P0 domain). 3/7 P0 cuối của security cluster.

## 1. Brainstorm

3 gaps cùng class "controller role-gated nhưng không bind resource instance → caller tenant → Owner A thao tác resource của Owner B (cross-tenant IDOR)". Gaps' Root Cause cũ giả định "gateway không forward tenantId" → cần gateway change.

**Fix-time state-check (per `audit-to-gap-pipeline.md` §2.8)** — Explore agent map gateway: `TenantHeaderGuardFilter` (GlobalFilter, mọi non-public route) ĐÃ inject trusted `X-Tenant-Id` từ verified JWT `tenantId` claim + global `RemoveRequestHeader=X-Tenant-Id` strip client-sent value. → Root Cause cũ outdated. **KHÔNG cần gateway change.** Fix = service-layer ownership binding dùng trusted header sẵn có, theo precedent `StaffInvitationController` (`existing.getTenantId().equals(tenantId)`).

Scope-reduction: 3 gaps coupled qua MỘT ownership-binding pattern → 1 PR mạch lạc + shared guard helper > parallel agents (parallel mỗi agent reinvent pattern → divergence).

## 2. Task Breakdown

1. State-check gateway tenant propagation (Explore agent) → confirm trusted X-Tenant-Id available, no gateway change.
2. `TenantOwnershipGuard` helper (subscription + branding) — admin bypass via SecurityContext authorities, throw AccessDeniedException → 403.
3. GAP-1015: bind subscription get/cancel/downgrade/upgrade/renew + create-body instanceId + instance-path getters; restrict global `/expiring` → admin-only (sweep — list-all leak).
4. GAP-1023: bind domain path `{id}` (4 endpoints).
5. GAP-1019: bind branding client `X-Instance-Id == X-Tenant-Id` (BrandingJobController 5 + AIBrandingController 4).
6. Caller-sweep (per `api-contract-change-caller-sweep.md`): @RequestHeader thêm vào controller → migrate existing tests gửi X-Tenant-Id; run tests not compile.
7. TDD: guard unit tests + cross-tenant 403 regression tests (`*Test.java` để CI run).
8. Close 3 gaps + PR.

## 3. Scope

- `kitehub-subscription .../security/TenantOwnershipGuard.java` (NEW) — shared ownership guard.
- `kitehub-subscription .../controller/SubscriptionController.java` — X-Tenant-Id bind 8 endpoints + `/expiring` admin-only (GAP-1015).
- `kitehub-subscription .../controller/DomainController.java` — X-Tenant-Id bind 4 endpoints (GAP-1023).
- `kitehub-branding .../security/TenantOwnershipGuard.java` (NEW) — branding ownership guard (String + UUID overloads).
- `kitehub-branding .../controller/BrandingJobController.java` — bind X-Instance-Id 5 endpoints (GAP-1019).
- `kitehub-branding .../controller/AIBrandingController.java` — bind X-Instance-Id 4 endpoints (GAP-1019).
- Tests (NEW): `TenantOwnershipGuardTest` (sub + branding unit), `SubscriptionTenantOwnershipTest` (sub+domain cross-tenant @WebMvcTest), `BrandingTenantOwnershipTest` (@WebMvcTest).
- Tests (migrated caller-sweep): `SubscriptionControllerSecurityTest` + `RoleGuardMatrixIT` (add X-Tenant-Id; `/expiring` STAFF→403 + admin→200).

## 4. State-Check Evidence

| Symbol | Verify | Verdict |
|--------|--------|---------|
| Gateway `TenantHeaderGuardFilter` injects X-Tenant-Id from JWT tenantId | Explore read filter:129-133 | ✅ exists (no gateway change needed) |
| Global `RemoveRequestHeader=X-Tenant-Id` strip client value | application.yml:826 | ✅ exists |
| SubscriptionController 0 ownership bind (role gate only) | grep @PreAuthorize, no tenant compare | ✅ hole live (KH-5 walk 200/204 cross-tenant) |
| DomainController OWNER_AUTHZ added but no tenant bind | DomainController:43-48 comment cites GAP-1023 | ✅ hole live |
| BrandingController X-Instance-Id client-controlled, gateway no strip | grep gateway X-Instance-Id → 0 hits | ✅ hole live |
| Subscription entity has instanceId | Subscription.java:28 | ✅ (ownership compare feasible) |
| AccessDeniedException → 403 | subscription GlobalExceptionHandler:181 | ✅ mapped |
| Both services @EnableMethodSecurity + ROLE_ authorities | SecurityConfig (sub:192, branding:52) | ✅ |

Gaps filed 2026-06-06 (<1 day, runtime-verified per `audit-to-gap-pipeline.md` §2.8).

## 5. Verification Gates

### Local tests (per `api-contract-change-caller-sweep.md` §3.3 — run not just compile, strict-warnings)

- **Subscription:** 33 tests PASS (TenantOwnershipGuardTest 7 + SubscriptionTenantOwnershipTest 9 [5 GAP-1015 + 4 GAP-1023] + SubscriptionControllerSecurityTest 9 migrated + RoleGuardMatrixIT 8 migrated). BUILD SUCCESS.
- **Branding:** TenantOwnershipGuardTest + BrandingTenantOwnershipTest + BrandingRoleAuthorizationTest (existing, unbroken). See §8 Log for counts.

### Cross-tenant 403 regression (the IDOR guard)

| Check | Verdict |
|-------|---------|
| GAP-1015 OWNER GET/cancel/create subscription cross-tenant | **403** ✅ (mutation never runs — verify never()) |
| GAP-1015 OWNER own subscription | 200 ✅ |
| GAP-1015 PLATFORM_ADMIN any subscription (no X-Tenant-Id) | 200 bypass ✅ |
| GAP-1015 `/expiring` STAFF | **403** (now admin-only) ✅; PLATFORM_ADMIN 200 ✅ |
| GAP-1023 OWNER GET/DELETE domain cross-tenant | **403** ✅ (destructive blocked) |
| GAP-1023 ADMIN delete any domain | bypass ✅ |
| GAP-1019 OWNER create/get branding cross-tenant X-Instance-Id | **403** ✅ (job never created) |
| GAP-1019 OWNER own instance | 201 ✅; PLATFORM_ADMIN any → 201 bypass ✅ |

Note: live gateway re-walk deferred — AWS stack STOPPED; @WebMvcTest + unit tests cover the guard logic on production-equivalent Spring Security chain (`@EnableMethodSecurity` + real `SecurityConfig`). Per `pre-handoff-self-test-completeness.md` §3.5 override: `POST_FIX_REWALK_DEFER: AWS stack stopped; @WebMvcTest with real SecurityConfig covers 403 mapping`.

## 6. Agent Spawn Pattern

1 Explore agent (Opus) for gateway/controller mapping; solo coordinator for fix + test-sweep. Security-sensitive cross-service ownership + shared pattern consistency → single authorship beats parallel (avoid each agent reinventing guard). Caller-sweep per `api-contract-change-caller-sweep.md` (header add = contract change → test migration + run tests).

## 7. Closure Protocol

### Scope-Completeness Reconciliation (per `wave-closure-scope-completeness.md` §3)

| # | Bucket B item | Verdict | Follow-up |
|---|---|---|---|
| 1 | GAP-1015 subscription ownership binding + `/expiring` admin-only + tests | ✅ DONE | — |
| 2 | GAP-1019 branding X-Instance-Id binding (job + AI controllers) + tests | ✅ DONE | — |
| 3 | GAP-1023 domain ownership binding (4 endpoints) + tests | ✅ DONE | — |
| 4 | SubscriptionBillingIT SUB-20 staleness + @SpringBootTest context-load (discovered) | 🟡 DEFER | GAP-1043 filed (pre-existing, out of IDOR scope) |
| 5 | Live gateway re-walk | 🟡 DEFER | AWS stack STOPPED; @WebMvcTest covers guard; re-walk post-restore |

Bucket B complete → **7/7 P0 security cluster CLOSED**. Wave security-2 overall done for P0 cluster.

### Sync targets
- gap-status.csv: GAP-1015/1019/1023 → DONE + moved closed/ ✅
- wave-history.jsonl: security-2 Bucket B entry ✅
- GAP-1043 (discovery): SubscriptionBillingIT SUB-20 staleness filed ✅

### Outcome
Bucket B **DONE** — 3 cross-tenant IDOR P0 gaps closed via service-layer ownership binding (no gateway change needed — trusted X-Tenant-Id already injected). Shared `TenantOwnershipGuard` (admin bypass + 403). Caller-sweep migrated 2 existing test classes. 33 subscription tests + branding tests PASS strict-warnings. **7/7 P0 security cluster closed** (Bucket A 3: GAP-1031/1034/1041 + Bucket C 2: GAP-1025/1035 + Bucket B 3: GAP-1015/1019/1023; note GAP-1035 was P1). Code PR.

## 8. Log

- **2026-06-06:** Bucket B shipped. Fix-time state-check revealed gateway already injects trusted X-Tenant-Id (gaps' "gateway forward tenantId" root cause outdated) → no gateway change, pure service-layer binding. `TenantOwnershipGuard` (sub + branding) admin-bypass via SecurityContext authorities. SubscriptionController 8 endpoints + `/expiring`→admin-only; DomainController 4; BrandingJobController 5 + AIBrandingController 4. Caller-sweep: @RequestHeader add broke `SubscriptionControllerSecurityTest`/`RoleGuardMatrixIT` (owner happy paths → 403 without header; `/expiring` STAFF→403) → migrated + run tests PASS (subscription 33/33 strict-warnings). New regression guards as `*Test.java` (CI-run). Discovery: `SubscriptionBillingIT` (*IT, @SpringBootTest) can't load context locally + SUB-20-stale (create asserts ACTIVE but service returns PENDING) → reverted my edits + filed GAP-1043 (pre-existing, out of scope). Live gateway re-walk deferred (AWS STOPPED). GAP-1015/1019/1023 DONE → 7/7 P0 security cluster closed.
