---
title: Wave security-2 — P0 security cluster Bucket A (gateway routing/exposure)
status: in-progress
created: 2026-06-06
updated: 2026-06-06
waves: [security-2]
wave: 2
tag_primary: security
tags_secondary: [gateway, idor, authz, routing, p0-cluster]
counter: 2
date_launch: 2026-06-06
gaps: [GAP-1031, GAP-1034, GAP-1041, GAP-1042]
---

# Wave security-2 — P0 security cluster (Bucket A: gateway routing/exposure)

**Mục tiêu:** Fix 7 P0 security gaps surfaced bởi Flow Verification Campaign G1 walks (KH-5..10 + KC-10..12). PR này ship **Bucket A (gateway routing/exposure)** — 3 P0 + 1 META. Bucket B (IDOR) + C (authz) queued. (counter=2: security counter 1 = wave-2026-06-05 tenant-isolation GAP-983.)

## 1. Brainstorm

7 P0 nhóm theo root cause thành 3 bucket disjoint:
- **A Gateway routing/exposure** (GAP-1031/1034/1041 + GAP-1042 META) — gateway route predicate quá rộng → shadow/expose nhầm service. Config-scoped (application.yml), testable per-route, highest leverage (unblock 3 flows). Blast-radius cao (gateway = mọi flow đi qua) → fix cẩn thận + re-walk exhaustive, KHÔNG spawn blind agent.
- **B Cross-tenant IDOR** (GAP-1015/1019/1023) — service trust client-controlled id không verify ownership; shared root (gateway tenant-propagation + service ownership check). Architecturally deep → queued.
- **C Missing @PreAuthorize** (GAP-1025 + P1 GAP-1035) — controller thiếu authz annotation. Clearest fix → queued (user chọn C trước B).

Bucket A ship trước vì lowest-design-risk + highest-flow-unblock + force-multiplier (GAP-1042 META).

## 2. Task Breakdown

1. State-check 4 routes (email/payroll/branding) còn collision không (gaps <1 day, runtime-verified same session).
2. Edit gateway application.yml: remove platform-email route; add kiteclass-payroll + 3 kiteclass-branding routes before respective catch-alls.
3. Extend audit-gateway-routes.sh với INTERNAL_ONLY_PATTERNS exemption (email).
4. Rebuild gateway + re-walk 3 fixes + regression sweep.
5. Close GAP-1031/1034/1041 DONE + GAP-1042 PARTIAL; ship PR.

## 3. Scope

- `kitehub/kitehub-gateway/src/main/resources/application.yml` — 3 route blocks added (payroll + branding public/versions/package → kiteclass-core), 1 removed (platform-email).
- `scripts/audit-gateway-routes.sh` — `INTERNAL_ONLY_PATTERNS` exemption for intentionally-internal email controller.

## 4. State-Check Evidence

| Symbol | Verify | Verdict |
|--------|--------|---------|
| precedent `kitehub-admin-beta-requests-v1` (:525) + `kitehub-admin-impersonate` (:539) | grep application.yml | ✅ exists — "route subpath before catch-all" pattern |
| `instance-apis` catch-all TenantResolver | grep application.yml :746 | ✅ exists — auth+tenant route pattern |
| `public-tenant-landing` no-TenantResolver | grep application.yml :689 | ✅ exists — public-path pattern |
| kiteclass-core URI `${KITECLASS_CORE_URL:http://kiteclass-core:8080}` | grep | ✅ exists |
| GAP-1031/1034/1041 symptoms still live | runtime curl same session (anon-email 200, payroll 404, branding-public 401) | ✅ confirmed (gaps <1 day, no drift per `audit-to-gap-pipeline.md` §2.8) |

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

## 6. Agent Spawn Pattern

Solo coordinator (NO agent spawn). Gateway blast-radius cao → fix + re-walk exhaustive tự làm, không spawn blind agent. Investigation-first per `release-fix-retry-budget` §3.5 (comment Path= literal confused audit parser → reworded; comma-predicate → single-predicate split).

## 7. Closure Protocol

### Scope-Completeness Reconciliation (per `wave-closure-scope-completeness.md` §3)

| # | Bucket A item | Verdict | Follow-up |
|---|---|---|---|
| 1 | GAP-1031 email route removal + re-walk | ✅ DONE | — |
| 2 | GAP-1034 branding 3-route fix + re-walk | ✅ DONE | — |
| 3 | GAP-1041 payroll route + re-walk | ✅ DONE | — |
| 4 | GAP-1042 concrete-3 fix | 🟡 PARTIAL | systematic audit + CI-gate → follow-up wave |
| 5 | Bucket B IDOR (GAP-1015/1019/1023) | ❌ NOT-IMPLEMENTED | queued security-2 Bucket B (next session) |
| 6 | Bucket C authz (GAP-1025 + GAP-1035) | ❌ NOT-IMPLEMENTED | queued security-2 Bucket C (next session) |

Wave status = `in-progress` (Bucket A shipped; B/C queued). NOT flipped complete.

### Sync targets
- gap-status.csv: GAP-1031/1034/1041 → DONE + moved closed/; GAP-1042 → PARTIAL ✅
- wave-history.jsonl: security-2 counter 2 entry ✅
- ROADMAP: security wave entry (deferred — curated)

### Outcome
Bucket A **DONE** — 3 P0 routing/exposure gaps closed + verified live (email arbitrary-send hole closed, branding + payroll routing fixed, zero regression). GAP-1042 META PARTIAL. Code PR (gateway config + audit script). Bucket C (authz) next per user, then B (IDOR).

## 8. Log

- **2026-06-06:** Bucket A shipped. Gateway application.yml: removed platform-email route (GAP-1031), added kiteclass-payroll (GAP-1041) + kiteclass-branding-public/versions/package (GAP-1034) before respective catch-alls. audit-gateway-routes.sh INTERNAL_ONLY_PATTERNS exemption. Investigation: comment `Path=` literal confused audit parser (false-positive 6 findings) → reworded; comma-predicate split to single-predicate routes. Rebuild + re-walk all PASS, audit back to baseline 4. 3 P0 DONE + GAP-1042 PARTIAL. Renamed security-1 → security-2 (counter collision: security counter 1 = wave-2026-06-05 tenant-isolation GAP-983). Buckets C then B queued.
