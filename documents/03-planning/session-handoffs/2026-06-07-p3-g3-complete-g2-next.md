# Session handoff — 2026-06-07 (P3 G3 production-parity COMPLETE; G2 human walks next)

**Scope:** Completed the full P3 G3 production-parity phase of the Flow Verification Campaign (G1→G3→G2 reorder). All non-deferred flows now G3 ✅. NEXT = P4 hand to G2 (human walks) — that is the dev's work, not Claude's.

## Shipped this session

| PR | Scope | Status |
|----|-------|--------|
| #2232 | Fix 2 stale CI checks on prior audit-reports PR (audits-index + gap-folder) | ✅ merged |
| #2233 | Gateway carve-out runtime walk C1-C5 → GAP-1042/1049 DONE + GAP-1051/1052 filed | ✅ merged |
| #2234 | KC-11 P1 fixes: GAP-1039 reports cross-tenant leak + GAP-1040 SSRF (2 parallel Opus agents) | ✅ merged |
| #2235 | Batch 2 campaign flips: KC-2/3/4/5/6 + KH-9 + KC-12 → G3 PASS | ✅ merged (or merging) |

## P3 G3 status — ALL non-deferred flows verified

**Method:** production-parity walk via gateway :9000 with minted HS512 JWT (recipe → memory `project_g3_walk_recipe.md` + helper `.claude/g3-walk-scratch/mint.py`). Cross-tenant IDOR = owner-B token (tenantId=B) → tenant-A resource → 403/404.

- **Carve-outs C1-C5** (GAP-1042/1049): all 5 reach correct service ✅
- **Batch 1 security** (KH-5/6/7/8/10, KC-10): all IDOR/authz/email isolation hold ✅
- **KC-11** (GAP-1039 reports scoped 2M not 3.5M + fail-closed; GAP-1040 SSRF logoUrl stripped) ✅
- **Batch 2** (KC-3/4/5/6 academic, KH-9 admin, KC-2 staff, KC-12 payroll): isolation + happy ✅
- **Acked**: KC-7/KC-8 (G3 earlier), KH-3 (KH-5 IDOR), KH-2a (auth chain), KC-1 (saga LIVE walk#3)
- **KC-9**: DEFERRED Phase 2 (student portal — auth path gated)

## Gaps closed this session
GAP-1042, GAP-1049 (gateway routing) · GAP-1039, GAP-1040 (KC-11 P1) — all DONE → closed/.
Filed: GAP-1051 (P3 webhook instance_id 500, Phase 1.5) · GAP-1052 (P2 HARD CI-gate for audit-gateway-routes.sh).

## NEXT SESSION — P4 hand to G2 (human)
1. The stack is G3-verified. Dev does **G2 human walks** per the per-flow G2 recipes in `documents/05-guides/operations/2026-06-0X-g2-recipe-*.md`. Each flow ✅ THÔNG only when G2 PASS.
2. Per `g2-handoff-md-mandate.md`, G2 recipes already exist for most flows; verify coverage for batch 1/2 flows (some may need a recipe written before handoff).
3. Flip campaign §4 → ✅ THÔNG per flow as G2 lands.

## Residual follow-ups (non-blocking)
- GAP-1051 (P3) webhook instance_id 500 — Phase 1.5 payment scope
- GAP-1052 (P2) wire audit-gateway-routes.sh HARD CI gate (detector already extended #2228)
- GAP-1043 (P2) KC-12 reschedule past-date validation
- ~15 other-module aggregate repos with latent missing-instance_id predicate (DEFER, separate IDOR triage wave per GAP-1039 cross-flow sweep) — mostly gateway-authenticated so filter active
- GAP-1048 candidate: EntityPersistenceListener System.err debug spam ("FAILED: NOT setting instanceId" noise in CI logs)
- GAP-721 Zalo stub (Wave 106 reconcile)

## Stack state
Local prod-parity stack UP + all backend services fresh from main (gateway/subscription/kiteclass-core/branding rebuilt this session). AWS STOPPED (cost-save). Test-data note: course id=1 (tenant aaaabbbb) fixture status fixed `active`→`PUBLISHED` mid-walk.
