---
title: Wave security-1 — P0 security cluster from Flow Verification Campaign (Bucket A: gateway routing/exposure)
status: in-progress
created: 2026-06-06
updated: 2026-06-06
waves: [security-1]
wave: wave-2026-06-06-security-1
tag_primary: security-1
tags_secondary: [gateway, idor, authz, routing, p0-cluster]
counter: 1
date: 2026-06-06
gaps: [GAP-1031, GAP-1034, GAP-1041, GAP-1042]
---

# Wave security-1 — P0 security cluster (Bucket A: gateway routing/exposure)

**Mục tiêu:** Fix 7 P0 security gaps surfaced bởi Flow Verification Campaign G1 walks (KH-5..10 + KC-10..12). PR này ship **Bucket A (gateway routing/exposure)** — 3 P0 + 1 META. Bucket B (IDOR) + C (authz) queued.

## 1. P0 cluster + bucket structure

7 P0 nhóm theo root cause:

| Bucket | Gaps | Root cause | Status |
|--------|------|-----------|--------|
| **A — Gateway routing/exposure** | GAP-1031 (email expose), GAP-1034 (branding shadow), GAP-1041 (payroll shadow), GAP-1042 META | Gateway route predicate quá rộng → shadow/expose nhầm service | ✅ **THIS PR** (3 DONE + 1042 PARTIAL) |
| **B — Cross-tenant IDOR** | GAP-1015 (subscription), GAP-1019 (branding X-Instance-Id), GAP-1023 (domain) | Service trust client-controlled instance/tenant id không verify ownership; cần gateway tenant-propagation + service ownership check | ⏳ QUEUED (architecturally deep — shared root) |
| **C — Missing @PreAuthorize** | GAP-1025 (InstanceController any-user-purge) | Controller thiếu authz annotation | ⏳ QUEUED (+ P1 GAP-1035 BrandingController same class) |

P1 follow-ups (queued, separate buckets): GAP-1039 (reports cross-tenant + payroll repos filter-only), GAP-1040 (document SSRF), GAP-1016/1017/1020/1021/1024/1026/1028/1029/1035/1036.

## 2. Bucket A — what shipped (this PR)

### 2.1 GAP-1031 — email arbitrary-send hole CLOSED (DONE)
Removed `platform-email` gateway route. `/api/platform/emails/**` không còn routable qua gateway (internal callers dùng direct docker `kitehub-email:8080`). Audit extended với `INTERNAL_ONLY_PATTERNS` exemption.
- Re-walk: anon → **404** (was 200 SENT); internal direct → **SENT** (intact).

### 2.2 GAP-1034 — branding routing collision FIXED (DONE)
Added 3 explicit KiteClass branding routes (public/versions/package → kiteclass-core) BEFORE `kitehub-branding-v1`. Single-predicate each.
- Re-walk: public → **200**, versions → **200** (was 401); kitehub-branding AI wizard intact (regenerate-quota/slug 200).

### 2.3 GAP-1041 — payroll routing collision FIXED (DONE)
Added `kiteclass-payroll` route (`/api/v1/admin/payroll/**` → kiteclass-core, TenantResolver) BEFORE `kitehub-admin-v1` (mirrors beta-requests/impersonate precedent).
- Re-walk: ADMIN-role payroll → **200** (was 404); kitehub-admin KH-9 intact (PLATFORM_ADMIN instances 200).

### 2.4 GAP-1042 META — gateway route-predicate audit (PARTIAL)
3 concrete collisions fixed + predicate-discipline applied (specific routes before catch-all, single-predicate). Audit back to 4-finding main baseline + email exemption. REMAINING: full systematic audit of all catch-all routes + wire `audit-gateway-routes.sh` HARD CI gate + route-ordering invariant doc → dedicated follow-up.

## 3. Scope

- `kitehub/kitehub-gateway/src/main/resources/application.yml` — 3 route blocks added (payroll + branding public/versions/package), 1 removed (platform-email).
- `scripts/audit-gateway-routes.sh` — `INTERNAL_ONLY_PATTERNS` exemption for intentionally-internal email controller.

## 4. State-Check Evidence

Symbols verified present pre-fix: precedent routes `kitehub-admin-beta-requests-v1` (:525) + `kitehub-admin-impersonate` (:539) = "route specific subpath before catch-all" pattern; `instance-apis` catch-all TenantResolver pattern; `public-tenant-landing` (public-path no-TenantResolver pattern); kiteclass-core URI `${KITECLASS_CORE_URL:http://kiteclass-core:8080}`. Gaps filed 2026-06-06 (<1 day, no drift; runtime-verified same session per `audit-to-gap-pipeline.md` §2.8).

## 5. Verification Gates

### Fix re-walk (live gateway :9000 post-rebuild, per `pre-handoff-self-test-completeness.md` §3)

| Check | Before | After |
|-------|--------|-------|
| GAP-1031 anon email send | 200 SENT | **404** ✅ |
| GAP-1031 internal email (direct :8080) | SENT | **SENT** ✅ |
| GAP-1034 branding public | 401 | **200** ✅ |
| GAP-1034 branding versions (OWNER) | 401 | **200** ✅ |
| GAP-1041 payroll (ADMIN role) | 404 | **200** ✅ |
| Regression: login | 200 | **200** ✅ |
| Regression: kitehub-branding regenerate-quota/slug (AI wizard) | 200 | **200** ✅ |
| Regression: admin instances (PLATFORM_ADMIN) | 200 | **200** ✅ |
| Regression: kiteclass settings-branding | 200 | **200** ✅ |
| Regression: beta-status v1 | 200 | **200** ✅ |
| gateway-route audit findings | 4 (baseline) | **4** (no new) ✅ |

Gateway rebuilt via `kitehub/scripts/rebuild.sh gateway`. YAML validated. Zero regression.

## 6. Closure Protocol

### Scope-Completeness Reconciliation (per `wave-closure-scope-completeness.md` §3)

| # | Bucket A item | Verdict | Follow-up |
|---|---|---|---|
| 1 | GAP-1031 email route removal + re-walk | ✅ DONE | — |
| 2 | GAP-1034 branding 3-route fix + re-walk | ✅ DONE | — |
| 3 | GAP-1041 payroll route + re-walk | ✅ DONE | — |
| 4 | GAP-1042 concrete-3 fix | 🟡 PARTIAL | systematic audit + CI-gate → follow-up wave |
| 5 | Bucket B IDOR (GAP-1015/1019/1023) | ❌ NOT-IMPLEMENTED | queued security-1 Bucket B (next session) |
| 6 | Bucket C authz (GAP-1025 + GAP-1035) | ❌ NOT-IMPLEMENTED | queued security-1 Bucket C (next session) |

Wave status = `in-progress` (Bucket A shipped; B/C queued). NOT flipped complete.

### Sync targets
- gap-status.csv: GAP-1031/1034/1041 → DONE + moved closed/; GAP-1042 → PARTIAL ✅
- wave-history.jsonl: security-1 counter 1 entry ✅
- ROADMAP: security wave entry ✅

### Outcome
Bucket A **DONE** — 3 P0 routing/exposure gaps closed + verified live (email arbitrary-send hole closed, branding + payroll routing fixed, zero regression). GAP-1042 META PARTIAL. Code PR (gateway config + audit script). Bucket B (IDOR) + C (authz) next.

## 7. Log

- **2026-06-06:** Bucket A shipped. Gateway application.yml: removed platform-email route (GAP-1031), added kiteclass-payroll (GAP-1041) + kiteclass-branding-public/versions/package (GAP-1034) before respective catch-alls. audit-gateway-routes.sh INTERNAL_ONLY_PATTERNS exemption. Investigation: comment `Path=` literal confused audit parser (false-positive 6 findings) → reworded; comma-predicate split to single-predicate routes. Rebuild + re-walk all PASS, audit back to baseline 4. 3 P0 DONE + GAP-1042 PARTIAL. Buckets B/C queued.
