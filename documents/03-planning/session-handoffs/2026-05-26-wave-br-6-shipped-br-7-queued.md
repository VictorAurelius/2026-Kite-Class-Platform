---
title: Session handoff 2026-05-26 — Wave beta-readiness-6 SHIPPED + Wave br-7 queued (HELD per user)
status: complete
created: 2026-05-26
session_date: 2026-05-26
session_duration_approx: ~3h
session_lock: session-20260526-003918-kite-dev.lock
context_end_pct: ~55% (Opus 4.7 1M)
audience: dev
---

# Session handoff — 2026-05-26 Wave beta-readiness-6 SHIPPED

## What shipped session này (6 PRs merged + closure PR pending)

| # | PR | Wave | Description | Impact |
|---|---|---|---|---|
| 1 | #1831 | meta-5 | cert-expiry alarm false-positive fix (TreatMissingData breaching→notBreaching) | Tier 3 alarm config + audit artifact + alb-architecture.md §8.1 |
| 2 | #1836 | br-6 plan patch | 3 scope corrections (kitehub-subscription→kiteclass-core paths + Opus mandate) | Pre-spawn state-check eliminated ~30-60 min round-trips |
| 3 | #1837 | br-7 plan patch | 5 scope corrections (GAP-215 already-shipped scope reduce + Bucket C+E helm path + Opus + P0 sync) | Wave size 6-7h → 4-5h |
| 4 | #1838 | br-6 Bucket A | payment-invoice api-contract drift sync (GAP-231) | 189→830 lines, 31 endpoints, drift 0/4→4/4 |
| 5 | #1839 | br-6 Bucket B | attendance api-contract drift sync (GAP-232) | 113→599 lines, 18 endpoints across 4 ctrl |
| 6 | #1840 | br-6 Bucket C | student-enrollment api-contract drift sync (GAP-233) | 91→803 lines, 25 endpoints across 5 ctrl |
| 7 | (this PR) | br-6 closure | 3 GAP DONE + git mv → closed/ + wave-history + ROADMAP + handoff | Wave br-6 SHIPPED status: complete |

## Gap status flips

| GAP | Status flip | Path move | Wave |
|---|---|---|---|
| GAP-231 | OPEN P0 → 🟢 DONE P0 100% | `phase-1-beta/` → `phase-1-beta/closed/` | br-6 Bucket A |
| GAP-232 | OPEN P0 → 🟢 DONE P0 100% | `phase-1-beta/` → `phase-1-beta/closed/` | br-6 Bucket B |
| GAP-233 | OPEN P0 → 🟢 DONE P0 100% | `phase-1-beta/` → `phase-1-beta/closed/` | br-6 Bucket C |

## What's queued next session — Wave beta-readiness-7 spawn

User direction "chưa spawn vội, đang 85% session rồid" (actual 50% per `session-end-context-check.md` verify; intent honored) → spawn HELD.

### Pre-conditions verified pre-handoff

- ✅ Wave br-7 plan patch merged (PR #1837) — paths + Opus + P0 + Bucket E in §3 all corrected
- ✅ GAP-215 state-check verified: `BrandingServiceImpl.getBranding()` line 67 already has `@Cacheable("branding-by-tenant", sync=true, key=tenant)` + 3 `@CacheEvict` annotations on update/uploadLogo/uploadFavicon. CacheConfig (Redis) exists.
- ✅ All 5 gap files exist OPEN P0 (215/216/217/218) + GAP-742 OPEN P1
- ✅ Verified paths: `kiteclass/kiteclass-core/.../module/settings/service/BrandingServiceImpl.java` + `infrastructure/helm/kitehub/templates/prometheusrule.yaml` + `alertmanager-config.yaml`

### Spawn plan next session

1. **Coordinator inline ~30 min: Bucket A verify GAP-215**
   - Verify integration test exists verifying cache hit/evict (`@SpyBean` repository, assert `findByInstanceIdAndDeletedFalse` invoked once per cache window; `updateBranding` evicts)
   - If IT missing → add inline OR file follow-up gap
   - Run `cd kiteclass/kiteclass-core && ./mvnw verify -Dtest='BrandingResourceTest,DocumentBrandingIntegrationTest'` PASS
   - Flip GAP-215 DONE per `gap-done-discipline.md` §2 + git mv → `phase-1-beta/closed/`

2. **Spawn 4 Opus 1M bg-agents** (parallel batch 1, isolated worktrees)
   - **Bucket B** GAP-216: JMH micro-benchmark + Prometheus histogram. Files: `kitehub/kitehub-branding/src/test/java/.../benchmark/DocumentRenderBenchmark.java` (new) + Micrometer Timer instrumentation. ~3-4h.
   - **Bucket C** GAP-217: Alert rules + escalation. Files: `infrastructure/helm/kitehub/templates/prometheusrule.yaml` (extend) + `alertmanager-config.yaml`. ~1.5-2h.
   - **Bucket D** GAP-218: Font runbook + Dockerfile assertion. Files: `kitehub/kitehub-branding/Dockerfile` + `documents/05-guides/operations/pdf-font-missing-runbook.md` (new). ~1-1.5h.
   - **Bucket E** GAP-742: Outbox DLQ alert wiring. Files: same `prometheusrule.yaml` + `alertmanager-config.yaml`. ~2h. ⚠️ Shares files với Bucket C → coordinator merge SEQUENTIAL C→E.

3. **After all 4 verify** → coordinator sequential merge C→E first (shared files), then B+D parallel-safe. Flip GAP-216/217/218/742 DONE. 5-target sync + closure PR.

### Agent prompt template

Each agent prompt must include per `release-fix-retry-budget.md` §3.5 Investigation phase:
- Read GAP-XXX full file
- Empirically verify current code state before designing fix
- Don't trust gap problem statement filed 2026-04-25 verbatim (module may have drifted)
- `## Investigation finding` section mandatory in PR body
- Branch: `wave/br-7-bucket-<X>-<topic>`
- Commit: `feat(wave-br-7-bucket-<X>): <summary>`
- Vietnamese narrative + English identifiers per `dev-readable-doc-language.md`
- DON'T merge PR (coordinator owns sequential merge)
- DON'T flip GAP DONE (coordinator handles in closure)

## Worktrees outstanding (cleanup queue)

3 br-6 agent worktrees still attached (CSV updated + 3 branches merged on GitHub but local branches couldn't delete during merge due to active worktrees):

```bash
# Run next session after wave closure merges:
bash scripts/prune-merged-worktrees.sh --yes
```

Expected: prunes 3 worktrees (agent-a265.../a37.../a72...) + deletes 3 local branches (wave/br-6-bucket-a/b/c).

## Tasks state (TaskList)

| Task | Status |
|---|---|
| #5 Patch wave br-6 plan PR | ✅ completed (PR #1836) |
| #6 3 br-6 Opus bg-agents SHIPPED | ✅ completed (PRs #1838/1839/1840) |
| #7 br-7 plan patch MERGED | ✅ completed (PR #1837) |
| #8 Coordinator inline: verify GAP-215 already done | ⏳ pending → next session |
| #9 Spawn 4 Opus bg-agents (br-7 B/C/D/E) | ⏳ pending → next session |
| #10 br-6 3 bucket PRs merged | ✅ completed |
| #11 br-6 closure PR + handoff | ⏳ in_progress (this PR) |

## Quality audit follow-ups

Per `post-wave-audit-mandate.md` §2.2 3-day window post-wave merge:
- **Wave br-6 audit suite due ≤2026-05-29** — API contract audit refresh expected ≥82/100 B PASS (76/100 C FAIL Wave 98 → +6 delta from 3 domain drift→0)
- **Wave br-7 audit suite due ≤(spawn date + 3)** — Performance + Ops audit refresh post Bucket B benchmark + Bucket C alerts

## Out-of-scope items surfaced (NOT filed as new gaps per agent spec)

3 items annotated inline in bucket PRs:
1. **Bucket A:** kitehub `/api/platform/payments` + `/api/v1/admin/payments` controllers flagged by drift detector — out of GAP-231 scope (kitehub platform module, not kiteclass payment-invoice domain)
2. **Bucket B:** Enum `AttendanceStatus.EXCUSED` vs use-cases.md + rules.md BR-ATT-005 reference `EXCUSED_ABSENCE` — rename needed
3. **Bucket B:** use-cases.md missing UC entries for attendance period (§4) + parent-facet (§5) endpoints

Next session decision: file these as 3 new P2 follow-up gaps OR consume into Wave audit-2 scope (already queued for post-Wave br-5+6 audit refresh).

## Cross-link

- Wave plan: [`documents/03-planning/waves/wave-2026-05-25-beta-readiness-6-api-contract-drift-trio.md`](../waves/wave-2026-05-25-beta-readiness-6-api-contract-drift-trio.md)
- Wave br-7 plan: [`documents/03-planning/waves/wave-2026-05-25-beta-readiness-7-document-performance-cluster.md`](../waves/wave-2026-05-25-beta-readiness-7-document-performance-cluster.md)
- ROADMAP §🎯 Current Status: see updated entry 2026-05-26 Wave br-6 SHIPPED
- wave-history.jsonl: appended `beta-readiness-6` entry with full outcome narrative
