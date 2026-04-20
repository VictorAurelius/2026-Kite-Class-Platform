---
title: Wave — Audit Part C Score Recovery (77 → ~88-90 A−)
status: draft
created: 2026-04-20
updated: 2026-04-20
waves: [C]
gaps: [GAP-132, GAP-134, GAP-146, GAP-148, GAP-127, GAP-126, GAP-043, GAP-144, GAP-143, GAP-119, GAP-108, GAP-109, GAP-110, GAP-149-meta]
---

# Wave Audit Part C — Score Recovery Sprint

**Owner:** TBD (currently drafted by parent session 2026-04-20)
**Timeline:** ~8-11 days wall-clock (4 sub-sprints, parallel agents per sprint)
**Target:** quality-audit 77/100 C+ → **~88-90 A−**
**Approach:** leverage-first ordering, meta-boost per `.claude/rules/meta-gap-priority.md`

---

## 1. Context

| Phase | Status | Output |
|-------|:------:|--------|
| Audit catch-up Part A | 🟢 COMPLETE (2026-04-19) | 5 baselines captured (biz 65, ops 49, perf 58, ui 81/59, quality 77) |
| Audit fix Part B | 🟢 COMPLETE (2026-04-20) | 9 gaps closed (GAP-104/105/111/120/128/129/131/133/136) + 5 follow-ups |
| Re-audit validation | 🟢 COMPLETE (2026-04-20) | biz 65→72 (+7), perf 58→64 (+6); GAP-107 retracted FP; GAP-148 new |
| **Part C Score Recovery** | 🔵 **THIS PLAN** | Target 77 → ~88-90 via 4 targeted sprints |

## 2. Why score is still at 77 (C+)

Per re-audit breakdown, weakest categories:

| Category | Score | Weak drivers |
|----------|:-----:|--------------|
| Performance FE Bundle | 8/20 | **GAP-127** FE code-splitting (~5-8 pts alone) |
| DevOps/Infra | 5/10 | GAP-111/120 foundation shipped; dashboards + receivers + DR still missing |
| UI/UX | 6/10 | KH 59/128 (3 screens @ 33/128) — deferred to Wave 6 UI sprint |
| Caching | 13/20 | GAP-132 EnableCaching missing, GAP-043 stampede unprotected |
| Business config accuracy | 13/20 | GAP-108 payment 12 keys hardcoded 27d, GAP-106 branding keys |

**Conclusion:** Score low because **gaps đã document chưa fix**, không phải audit miss. Re-audit only added 1 net new gap (GAP-148 − GAP-107 FP retract) — audits are thorough. Fix pipeline = score recovery path.

## 3. Sprint Plan (leverage-first ordering)

### Sprint 0 (meta, optional) — Audit skill refinement

**Duration:** 2-3 hours
**Type:** Meta (skill improvement)

**Work:**
- New gap: **GAP-149** (P2, meta) — business-logic-audit skill grep scope too narrow
- Lesson from GAP-107 FP: baseline grep searched `kitehub/` + `kiteclass/` top-level, missed `kiteclass/kiteclass-core/`
- Fix: update skill to search `**/*.java` (excluding test/target) OR explicitly list all core/submodule paths

**Why 0 not 1:** Run BEFORE Sprint 4 (biz debt) so future re-audits use corrected skill. Not blocking; can parallelize with Sprint 1.

**Expected impact:** prevents recurrence of GAP-107-style false positives in future audit cycles.

**Effort:** 1 agent, 1 PR.

---

### Sprint 1 (P-1) — Quick Perf/Biz Wins — 1-2 days

**Gap pool (4):**

| Gap | Domain | Scope | Effort |
|-----|--------|-------|:------:|
| **GAP-132** | Perf/Cache | Add `@EnableCaching` to kitehub-subscription, kitehub-admin, kitehub-platform | S |
| **GAP-148** | Biz/Wiring | Wire `@CircuitBreaker` on `AIQueueDispatcher` + `AIJobConsumer` (currently config exists, annotations missing) | S |
| **GAP-146** | Perf/Resilience | 3 remaining HTTP clients (payment, email, captcha) — timeouts + Resilience4j where applicable | M |
| **GAP-134** | Perf/DB | Adopt `@EntityGraph` or `JOIN FETCH` on 5-10 top list endpoints to eliminate N+1 | M |

**Parallel strategy:** 4 agents, disjoint file sets:
- Agent P1-A: GAP-132 (3 `@EnableCaching` additions + config tests)
- Agent P1-B: GAP-148 (CB annotations + integration test)
- Agent P1-C: GAP-146 (3 HTTP clients + WireMock tests)
- Agent P1-D: GAP-134 (query audits + @EntityGraph additions + N+1 regression tests)

**Expected delta:** Perf 64→70 (+6), Biz 72→76 (+4). Quality-audit impact: +2-3 overall.

**Gates:**
- Each PR docs-only hook exception false; CI must pass per gap
- Re-audit perf only after Sprint 1 merge (not biz — biz deltas come from Sprint 4)

---

### Sprint 2 (P-2) — High-Leverage Design — 2-3 days

**Gap pool (3):**

| Gap | Domain | Scope | Effort |
|-----|--------|-------|:------:|
| **GAP-127** | Perf/FE | Frontend code-splitting: convert 64 pages to dynamic imports; lazy-load framer-motion + recharts; bundle analyzer added to CI | L |
| **GAP-126** | Perf/Cache | Admin dashboard unbounded findAll — add pagination + `@Cacheable` with TTL + short-circuit | M |
| **GAP-043** | Perf/Cache | Cache stampede protection via `Lettuce`/`Caffeine` lock OR `refresh-ahead` pattern | M |

**Parallel strategy:** 3 agents — but GAP-127 is the biggest (~5-8 score pts alone). Consider:
- Split Agent P2-A into 2 sub-agents: one kiteclass-frontend (40 pages), one kitehub-frontend (24 pages) if scope bursts; OR keep single agent if E2E complexity needs coordination
- Agent P2-B: GAP-126 (3-5 controller methods + cache config)
- Agent P2-C: GAP-043 (cache library + stampede tests with concurrent load)

**Expected delta:** Perf 70→80 (+10), overall 77→~82 B. GAP-127 alone is the biggest single lever in this wave.

**Gates:**
- Bundle size check: First Load JS must drop from ~400-550KB → <200KB per page (audit threshold)
- E2E tests must still pass post-code-splitting
- Cache stampede load test: 100 concurrent requests on same key → only 1 backend call

---

### Sprint 3 (P-3) — Ops Readiness — 3-4 days

**Gap pool (3):**

| Gap | Domain | Scope | Effort |
|-----|--------|-------|:------:|
| **GAP-144** | Ops (P0) | Alertmanager production receivers: Slack webhook + PagerDuty integration + SMTP email + routing rules by severity | M |
| **GAP-143** | Ops (P1) | Grafana dashboards Helm chart: 5 service dashboards + JVM + RabbitMQ + database pool | L |
| **GAP-119** | Ops (P1) | Platform DR runbook: backup verification drill + restore procedure + SLA targets | M |

**Parallel strategy:** 3 agents. GAP-144 is prod deploy blocker — prioritize.
- Agent P3-A: GAP-144 (receiver configs + secrets template + routing)
- Agent P3-B: GAP-143 (dashboard JSON + Helm wiring)
- Agent P3-C: GAP-119 (runbook doc + restore test script)

**Expected delta:** Ops 49→65 (+16). Quality-audit impact: +3.

**Gates:**
- GAP-144: at least 1 receiver (Slack OR email) must be wired + tested via `amtool alert add`
- GAP-143: `helm lint` + `helm template` with dashboards populated
- GAP-119: restore drill successful on staging DB copy

**Risk:** Requires staging environment access + SMTP/Slack credentials. If not available, mark GAP-144 PARTIAL (config + stubs only, defer production wiring).

---

### Sprint 4 (P-4) — Business Debt Cleanup — 2 days

**Gap pool (3):**

| Gap | Domain | Scope | Effort |
|-----|--------|-------|:------:|
| **GAP-108** | Biz/Config | Payment-invoice: externalize 12 hardcoded config keys (LATE_FEE_RATE, grace periods, etc.) — 27d aged | M |
| **GAP-109** | Biz/Docs | Bulk import Wave 1 BR-IMPORT rules documentation | S |
| **GAP-110** | Biz/Consistency | Ollama model ID inter-service alignment (kitehub-branding vs kiteclass-core) | S |

**Parallel strategy:** 3 agents, all docs + config (no heavy code):
- Agent P4-A: GAP-108 (config extraction + BR-INVOICE rules + test)
- Agent P4-B: GAP-109 (BR-IMPORT-001..N rules in existing domain)
- Agent P4-C: GAP-110 (config key sync + docs alignment)

**Expected delta:** Biz 76→82 (+6). Quality-audit impact: +2.

**Gates:**
- Re-audit biz ONLY after Sprint 4 merge
- GAP-108: no regression in payment flow (existing tests still pass)

---

## 4. Cumulative Score Projection

| Checkpoint | Quality /100 | Business /100 | Performance /100 | Ops /100 |
|------------|:------------:|:-------------:|:----------------:|:--------:|
| Baseline 2026-04-19 | 77 C+ | 65 D | 58 F | 49 F |
| Post-Part B (2026-04-20) | (not refreshed) | 72 C | 64 D | 49 F (unchanged) |
| Post Sprint 1 | ~79 | 76 | 70 | 49 |
| Post Sprint 2 | ~82 B | 76 | 80 | 49 |
| Post Sprint 3 | ~85 B+ | 76 | 80 | 65 |
| Post Sprint 4 | **~88 A−** | **82** | **80** | **65** |

## 5. Parallel Agent Strategy Recap

Applying learnings from Part A + Part B + Re-audit:

- Pre-assigned file scopes per agent (hard rule 3 from `feedback_parallel_agent_strategy.md`)
- Pre-assigned GAP number ranges for any follow-up gaps discovered per sprint:
  - Sprint 0 meta: GAP-149 only
  - Sprint 1: GAP-150 → GAP-154 (5 slots)
  - Sprint 2: GAP-155 → GAP-159 (5 slots)
  - Sprint 3: GAP-160 → GAP-164 (5 slots)
  - Sprint 4: GAP-165 → GAP-169 (5 slots)
- Parent owns shared files: ROADMAP, output-review-mandate, MEMORY — consolidation PR per sprint
- Parent sequences merges within sprint (not across — each sprint is atomic)
- Re-audit after Sprint 1, 2, 4 (ops tracked separately post-Sprint 3)

## 6. Success Criteria

- [ ] Quality-audit refresh ≥85/100 B+ after all 4 sprints merged
- [ ] All 13 target gaps CLOSED or PARTIAL-with-follow-up
- [ ] Zero regression on existing fresh audits
- [ ] CI green on all PRs
- [ ] No new P0 gaps created (P1/P2 follow-ups acceptable)

## 7. Risk & Mitigation

| Risk | Mitigation |
|------|------------|
| GAP-127 FE code-splitting breaks E2E | Run full E2E suite pre-merge; allow 1 extra day for E2E adjustments |
| Sprint 3 blocked by missing SMTP/Slack credentials | Ship GAP-144 as PARTIAL (config stubs only) + track in follow-up gap |
| Re-audit timing: parallel agents ≥5 concurrent may exhaust parent context | Cap 3-4 agents/sprint; use serial fallback if context pressure |
| GAP-108 payment externalize breaks prod | Mandatory 1 integration test run + 1 manual smoke before merge |
| `mcp__ide__*` disconnect recurrence (saw during re-audit) | Respawn procedure documented; not blocking |

## 8. Rollback Plan

- Each sprint = independent PR batch → can rollback individual sprints via revert
- No destructive migrations in this wave (all backward-compatible)
- Config changes use environment-override pattern (defaults preserve current behavior)

## 9. What's NOT in this wave

Explicitly deferred (tracked separately):

- UI/UX improvements (KiteHub 59/128, KC 81/128) — Wave 6 UI sprint
- Wave 5 GAP-047 document generation — separate wave in progress (PR #361)
- GAP-147 kitehub-admin OpenAPI bean conflict (P2) — cleanup PR any time
- Wave 1-4 K-12 critical features not yet in flight
- 6 remaining Part A GA Blockers from snapshot (GAP-047/046/016/011/014/005) — their own waves

## 10. Next Steps (after user approval of this plan)

1. Create follow-up gap **GAP-149** (meta, audit skill scope)
2. Kick off Sprint 0 (meta) + Sprint 1 (perf/biz quick wins) in parallel
3. After Sprint 1 merge: re-audit performance, measure delta
4. Proceed Sprint 2 if on-track; else replan
5. Follow sprint cascade with re-audit gates

## 11. Log

- **2026-04-20:** Plan drafted after re-audit PRs #378 + #379 + consolidation #380 merged. Based on category breakdowns of 2026-04-20 refresh scores.
