---
title: Wave onboarding-polish-1 — state-check audit (6-gap baseline + AWS-free advance candidates)
status: complete
created: 2026-06-01
phase: wave-onboarding-polish-1
wave: onboarding-polish-1
gaps: [GAP-534, GAP-535, GAP-536, GAP-538, GAP-599, GAP-610]
---

# Wave onboarding-polish-1 — State-Check Audit Report

**Per `audit-to-gap-pipeline.md` §2.8 fix-time state-check** prep before any Bucket execution. Goal: empirical reality of 6 multi-tenant onboarding cluster gaps + identify AWS-free advance candidates + classify AWS-blocked vs code-task remaining work.

## Scope

Track B Phase 1 BETA gate onboarding cluster close per `path-to-thesis-goal.md` §4 (sister wave email-finalize-1 — same Track B parallel).

6 gap target: GAP-534/535/536/538/599/610 — code-side mostly shipped Wave 77/78/92/98/101 era; live verify universally AWS-gated.

## Commands run (Tier 1 read-only)

```bash
# Canonical-status lookup per gap-architecture-v2.md §3
python3 -c "..."  # query CSV rows for 6 gap IDs

# Per-gap AC + Status scan
for g in GAP-534 GAP-535 GAP-536 GAP-538 GAP-599 GAP-610; do
  f=$(find documents/04-quality/gaps -name "${g}-*.md" -type f | head -1)
  grep -E "^\*\*Status:\*\*|^- \[" "$f"
done

# AWS stack state per session-start collector
# Result: 0 running / 3 stopped EC2 + RDS stopped (EOD save)
```

## Findings — per-gap state

### GAP-534 — Invite token single-use enforcement

- **CSV state:** PARTIAL 90% (last_verified 2026-05-26)
- **AC:** 7/8 ✅ — 1 deferred (live verify post-deploy)
- **Pending shipping:** ZERO code work — only deploy + smoke curl
- **AWS-needed share:** 100% — depends on Flyway V39 applied production + reuse-attempt curl

### GAP-535 — Tenant slug normalize VN diacritics

- **CSV state:** PARTIAL 70% (last_verified 2026-05-26)
- **AC:** 6/8 ✅ — 2 deferred (wiring `InstanceService.createInstance` + live verify)
- **Pending shipping:**
  - **Code task — AWS-FREE:** wire `TenantSlugNormalizer` vào `InstanceService.createInstance` collision-recovery loop (10-retry → 409)
  - **AWS task:** live verify POST với VN diacritic name → 201 + DB row slug normalized
- **AWS-needed share:** 50%

### GAP-536 — POST /tenants idempotency key

- **CSV state:** PARTIAL 65% (last_verified 2026-05-26)
- **AC:** 5/8 ✅ — 3 deferred (HandlerInterceptor wire + FE submit-button debounce + live verify)
- **Pending shipping:**
  - **Code task — AWS-FREE:** ship `IdempotencyHandlerInterceptor` wire vào POST `/api/platform/instances`
  - **Code task — FE:** UUID v4 idempotency-key generation + submit-button debounce
  - **AWS task:** live verify 2 sequential POSTs same key → 1 row + replay; hash mismatch → 422
- **AWS-needed share:** 33%

### GAP-538 — Day-1 onboarding checklist + sample data seed

- **CSV state:** PARTIAL 96% (last_verified 2026-05-26)
- **AC:** 6/8 ✅ — 2 deferred (live walkthrough + VN sample seed worker)
- **Pending shipping:**
  - **Code task — AWS-FREE:** Vietnamese sample seed data (student names "Nguyễn Văn An" + course names tiếng Việt) trong seed worker
  - **AWS task:** live walkthrough auth-gated flow per `pre-handoff-self-test-completeness.md` §2.1
- **AWS-needed share:** 50%

### GAP-599 — JWT 2-tab storage isolation

- **CSV state:** PARTIAL 90% (last_verified 2026-05-26)
- **AC:** 0/6 ticked — Wave 92 Bucket B code + 17 unit + 3 sim tests PASS nhưng AC checkbox state stale (CSV pct 90 reflects empirical reality per Wave 92 closure)
- **Pending shipping:**
  - **AC tick refresh:** unit + sim tests PASS evidence cần update gap file (Wave 92 closure note documented but checkbox state never updated — same pattern as Wave email-finalize-1 GAP-543 retroactive tick)
  - **AWS task:** live multi-tab browser UX verify (DevTools Network tab inspect)
- **AWS-needed share:** ~50% (4 of 6 AC need AWS live verify; 2 docs AC could ship without AWS)

### GAP-610 — Beta-signup validate returns TOKEN_NOT_FOUND for valid token

- **CSV state:** PARTIAL 75% (last_verified 2026-05-26)
- **AC:** 0/6 ticked — Status mentions "defensive hardening shipped + Testcontainers IT verified"
- **Pending shipping:**
  - **Code task — AWS-FREE:** root cause identification (3 hypotheses listed — verify which fired)
  - **AWS task:** curl validate + FE page load + deploy verify
- **AWS-needed share:** ~70%

## Aggregate

| Gap | Pre-pct | AWS-free advance possible | Realistic post-AWS-up pct |
|---|---|---|---|
| GAP-534 | 90% | No (deploy-only) | 100% |
| GAP-535 | 70% | YES (wire normalizer) | 85% AWS-free / 100% post-AWS |
| GAP-536 | 65% | YES (HandlerInterceptor + FE) | 80% AWS-free / 100% post-AWS |
| GAP-538 | 96% | YES (VN seed) | 98% AWS-free / 100% post-AWS |
| GAP-599 | 90% | YES (AC tick refresh + 2 docs AC) | 93% AWS-free / 100% post-AWS |
| GAP-610 | 75% | Possible (root cause debug) | 85% AWS-free / 100% post-AWS |
| **Aggregate** | **486/600 = 81%** | **+9pp** to ~90% AWS-free | **+19pp** to ~100% post-AWS |

## Surface findings

| # | Finding | Severity | Follow-up |
|---|---|---|---|
| 1 | GAP-599 AC checkbox state universally stale — Wave 92 Bucket B closure documented work but never ticked AC (recurrence #N of CSV ↔ AC drift pattern surfaced by Wave meta-8 Bucket D detector 226 baseline) | n/a — expected | GAP-822 detector now catches; AC tick refresh = standard close-out maintenance |
| 2 | GAP-534 already may flip DONE via PR #2007 (Wave meta-8 Bucket A catalog had GAP-534 in 19 SHIPPED list) — verify post-merge | n/a | Wave meta-8 PR #2007 merge confirms |
| 3 | 4 of 6 gap có "Wiring/Interceptor/seed defer follow-up" notes — these are real code work, not bookkeeping | P1 | Estimate ~3-5h coordinator-inline + ~2-3h FE work; suitable cho Wave onboarding-polish-1 next session |
| 4 | AWS stack stopped → live verify universally blocked. All 6 gap close-to-DONE requires `bash scripts/aws/start-stack.sh` next session | n/a — expected | Standard pattern Wave email-finalize-1 sister |

## Verdict + decision

Per `audit-to-gap-pipeline.md` §2.8 decision matrix:

| Gap | State-check result | Action |
|---|---|---|
| GAP-534 | Symptom verified present (code shipped, live verify pending) | Proceed with fix (deploy + curl smoke) |
| GAP-535 | Symptom partially present (normalizer shipped, wiring pending) | Scope-revise fix to wiring + live verify |
| GAP-536 | Symptom partially present (infra shipped, interceptor pending) | Scope-revise fix to wiring + live verify |
| GAP-538 | Symptom near-DONE (96%) | Ship VN seed + live verify |
| GAP-599 | Symptom verified resolved code-side, AC checkbox stale | AC tick refresh + docs AC + live verify |
| GAP-610 | Symptom present (root cause not identified yet) | Investigation step + fix + live verify |

## Recommended Wave onboarding-polish-1 scope

### AWS-free buckets (this/next coordinator-inline session ~3-5h)

- **Bucket A:** GAP-599 AC tick refresh + 2 docs AC (auth-storage.md + concurrent-browser-session mitigation note) — ~30min
- **Bucket B:** GAP-535 wire `TenantSlugNormalizer` vào `InstanceService.createInstance` + 10-retry loop test — ~1h
- **Bucket C:** GAP-536 `IdempotencyHandlerInterceptor` + wire vào POST `/api/platform/instances` + integration test — ~1.5h
- **Bucket D:** GAP-538 VN sample seed data (student/course names) + worker test — ~30min
- **Bucket E:** GAP-610 root cause investigation (hypothesis 1/2/3 verify via Testcontainers reproduce) — ~1h

### AWS-required buckets (after AWS start)

- **Bucket F:** All 6 gap live verify (deploy + curl smoke + multi-tab DevTools inspect) — ~2h
- **Bucket G:** FE submit-button debounce + UUID v4 idempotency-key generation (GAP-536 FE half) — ~1h

## Prior actions verified (per `audit-to-gap-pipeline.md` §2.8 — avoid duplicate work)

| Action | When | Where verified |
|---|---|---|
| Wave 77 Bucket D — GAP-534/535/536 code+tests shipped | 2026-05-XX | Per gap Status field + AC ✅ list |
| Wave 78 Bucket B — GAP-538 FE/BE shipped (~95%) | 2026-05-14 | Status field |
| Wave 92 Bucket B PR #1515 — GAP-599 sessionStorage migration | 2026-05-18 | Status field |
| Wave 98 B2 PR #1553 — GAP-538 AC7 VN seed worker foundation | 2026-05-18 | Status field |
| Wave 101 Bucket D — GAP-538 Playwright E2E spec | 2026-05-XX | Status field |
| Wave aws-restore-1 — GAP-612 ✅ DONE 2026-05-26 (unblocks all 6 live verify) | 2026-05-26 | gap-status.csv |

## Pending (this audit + future scope)

| Action | Owner | Notes |
|---|---|---|
| Wave onboarding-polish-1 plan draft (this PR) | coordinator | docs-only |
| AWS-free Bucket A-E execution | next session OR continue this session (context-budget allowing) | ~3-5h coordinator-inline |
| Bucket F+G AWS execution | next session AWS up | ~3h |

## Recommendations

1. ✅ This PR: state-check audit + Wave onboarding-polish-1 plan draft (no execution) — docs-only
2. ⏸ Next session (if context permits OR fresh start): Bucket A-E AWS-free execution
3. ⏸ AWS-up session: Bucket F+G (live verify all 6 gap close-to-DONE)
4. 📋 Confirm GAP-534 DONE post-Wave-meta-8 PR #2007 merge (catalog flip)

## References

- Wave plan stub: `documents/03-planning/waves/wave-2026-06-01-onboarding-polish-1.md` (to be created same PR)
- Path roadmap: `documents/03-planning/roadmap/path-to-thesis-goal.md` §4 Track B
- Gap files: 6 phase-1-beta gaps GAP-534/535/536/538/599/610
- Sister wave: Wave email-finalize-1 (just shipped — same Track B parallel pattern)
- Rules applied: `audit-to-gap-pipeline.md` §2.8 (fix-time state-check), `gap-architecture-v2.md` §3 (CSV canonical), Wave meta-8 GAP-822 detector context (CSV ↔ AC drift baseline)
