---
title: Master Plan — All 86 Open Gaps (9 Waves, ~2-3 Months)
status: draft
created: 2026-04-20
updated: 2026-04-20
supersedes: none
---

# Master Plan — Fix All 86 Open Gaps

**Snapshot at time of drafting (2026-04-20):**
- Total gaps: 148 (52 closed + 86 open + ~10 retract/misc)
- Open breakdown: P0=13, P1=33, P2=40
- Current quality-audit: 77/100 C+ (refreshable)

**Goal:** close 86 open gaps across 11 waves (2 in-flight + 9 proposed new) in ~2-3 months wall-clock with heavy parallel-agent execution. Estimated 4-5 months if serial.

---

## 1. Wave Overview Table

| # | Wave | Gaps | Priority Mix | Effort (wall-clock) | Status |
|:-:|------|:----:|--------------|---------------------|:------:|
| 1 | **Wave 5** — Document Generation (GAP-047) | 1 (+ sub-PRs) | P0 | 1-2 wk | 🟢 Plan merged PR #361, execution ready |
| 2 | **Part C** — Score Recovery | 14 | 5 P0, 6 P1, 3 P2 | 8-11 days | 🟢 Plan merged PR #381, execution ready |
| 3 | **Wave 6** — UI Polish | 8 | 1 P0, 2 P1, 5 P2 | 1-2 wk | 🔵 New (this master plan) |
| 4 | **Wave 7** — Ops Maturation | 12 | 3 P0, 5 P1, 6 P2 | 2-3 wk | 🔵 New |
| 5 | **Wave 8** — Business Governance | 6 | 2 P0, 1 P1, 3 P2 | 1-2 wk | 🔵 New (meta-heavy) |
| 6 | **Wave 9** — GA Blockers Meta | 4 | 3 P0, 1 P0 in-progress | 1-2 wk | 🔵 New (Part A residual) |
| 7 | **Wave 10** — AI Branding Completeness | 15 | 7 P1, 8 P2 | 3-4 wk | 🔵 New (largest) |
| 8 | **Wave 11** — K-12 Features | 10 | 5 P1, 5 P2 | 2-3 wk | 🔵 New |
| 9 | **Wave 12** — Developer Platform | 5 | 3 P1, 2 P2 | 1-2 wk | 🔵 New |
| 10 | **Wave 13** — KiteHub Admin Console | 6 | 2 P1, 4 P2 | 2-3 wk | 🔵 New |
| 11 | **Wave 14** — P2 Final Cleanup | 5-10 | 0 P0, 0 P1, remaining P2 | 1 wk | 🔵 New (batch, optional deferral) |

**Total gaps addressed: ~86.** Coverage 100% of current open backlog (+ follow-up gaps tolerated per wave).

---

## 2. Waves 3-11 Detailed (New Waves)

Waves 1 + 2 (Wave 5 + Part C) already have dedicated plan docs. Below are the 9 new waves.

### Wave 6 — UI Polish (~1-2 wk)

**Goal:** KH 59→85, KC 81→95 out of 128; close post-audit FE UI gaps.

**Gaps (8):**
- P0: GAP-137 (bulk import UI — Wave 1 backend inaccessible)
- P1: GAP-138 (KC landing hero duplicated text), GAP-139 (parent dashboard MVP placeholder)
- P2: GAP-080 (KH loading/error UX), GAP-140 (form-select English placeholder), GAP-141 (date input locale), GAP-142 (parent-invite Shadcn select)
- Residual partials: GAP-076 (KH mock auth), GAP-079 (KC i18n)

**Parallel strategy:** 4 agents (KC refactor, KH refactor, shared components, residual partials). Blocked on Part C Sprint 2 completion (GAP-127 code-splitting conflict). See Part C §4b.2.

**Gate:** ui-review refresh per-screen, expect KH ≥80, KC ≥90.

---

### Wave 7 — Ops Maturation (~2-3 wk)

**Goal:** ops-readiness 49→85; close remaining monitoring/logging/DR gaps post Part C Sprint 3 foundation.

**Gaps (12):**
- P0: GAP-114 (structured JSON logging), GAP-117 (backup restore drill automation), GAP-130 (docker resource limits)
- P1: GAP-112 (distributed tracing), GAP-115 (log aggregation ELK/Loki), GAP-118 (MinIO backup), GAP-121 (per-alert runbooks), GAP-122 (missing platform alerts)
- P2: GAP-113 (FE error tracking — Sentry), GAP-116 (PII scrubbing), GAP-123 (HPA), GAP-124 (PDB/NetworkPolicy), GAP-125 (canary deploy), GAP-145 (Loki tracing stack)

Note: GAP-119 (DR runbook) done in Part C Sprint 3. GAP-143 (Grafana), GAP-144 (Alertmanager receivers) done in Part C Sprint 3.

**Parallel strategy:** 4-5 agents split by domain (logging, tracing, alerting, scaling, DR).

**Gate:** ops-readiness audit refresh ≥85.

---

### Wave 8 — Business Governance (~1-2 wk)

**Goal:** Close output-review-mandate violations + business correctness gaps.

**Gaps (6):**
- P0: GAP-049 (business logic CORRECTNESS review — BRD/legal/compliance), GAP-050 (persona-based business review — role-play 10 tenant types)
- P1: GAP-046 (design patterns systematic application — ref `ai-branding-design-patterns.md`)
- P2: GAP-001 (kiteclass-gateway keep/delete decision — ADR), GAP-102 (05-guides completion + ADR kickoff), GAP-110 (Ollama model inter-service alignment)

**Parallel strategy:** 3 agents. GAP-049 + GAP-050 are persona/process skills — could combine into 1 skill deliverable.

**Dependencies:** Depends on Part C Sprint 4 completion (biz debt cleanup — GAP-108/109/110).

**Gate:** business-logic audit refresh ≥85 + new persona-review skill shippable.

---

### Wave 9 — GA Blockers Meta (~1-2 wk)

**Goal:** Close 4 remaining Part A GA blockers (meta priority per `meta-gap-priority.md`).

**Gaps (4):**
- P0 meta: GAP-011 (template library curation — 30 templates), GAP-014 (wave mock plan include AI branding), GAP-016 (living docs impact scope — 3-layer sweep)
- P0 in-progress: GAP-005 (AI queue fair scheduling Phase 2 — continuation of Phase 1 closed Part B)

**Parallel strategy:** 4 agents, each gap 1 agent. GAP-011 heaviest (requires template creation + review pipeline).

**Gate:** quality-audit refresh targeting 90/100 A−.

---

### Wave 10 — AI Branding Completeness (~3-4 wk, LARGEST)

**Goal:** Complete AI branding feature set (Epic 2 + Epic 3 residuals).

**Gaps (15):**
- P1 (7): GAP-017 (AI usage → billing), GAP-019 (AI observability + cost monitoring), GAP-020 (wizard state persistence + error recovery), GAP-023 (admin moderation tools), GAP-026 (trial/freemium AI mechanics), GAP-036 (tier upgrade UX reveal/teaser/unlock), GAP-039 (webhook reliability — retry/idempotency/versioning)
- P2 (8): GAP-003 (multi-tier image gen strategy), GAP-004 (template-based image composition Canva-like), GAP-022 (template analytics), GAP-024 (asset lifecycle + storage cleanup), GAP-025 (mobile-first wizard UX), GAP-027 (multi-brand per tenant — franchise), GAP-028 (AI model versioning + migration), GAP-029 (quality gate calibration), GAP-030 (AI branding DR)

**Sub-waves (3 parallel tracks):**
- 10a: Billing/observability track (017, 019, 026, 036)
- 10b: UX/wizard track (020, 022, 023, 025, 029)
- 10c: Infrastructure track (003, 004, 024, 027, 028, 030, 039)

**Parallel strategy:** 3 sub-waves in parallel, each with 2-3 agents. Heavy coordination on domain API contracts.

**Gate:** Persona-based review + quality-audit refresh; AI branding feature shipped to all tiers.

---

### Wave 11 — K-12 Features (~2-3 wk)

**Goal:** Complete K-12 education-specific features (Epic retention).

**Gaps (10):**
- P1 (5): GAP-055 (official report card VN format), GAP-056 (homeroom teacher GVCN concept), GAP-057 (teacher payroll + commission), GAP-063 (SMS + Zalo notifications), GAP-066 (KH unified reports/analytics)
- P2 (5): GAP-059 (conduct/behavior hạnh kiểm), GAP-060 (period-based attendance), GAP-061 (promotion/retention logic), GAP-062 (payroll bank batch), GAP-064 (SCORM/xAPI)

**Parallel strategy:** 3-4 agents by subdomain (grading, attendance, payroll, notifications).

**Gate:** Persona-based review with real K-12 school scenario (500 students bulk import → end-of-semester report).

---

### Wave 12 — Developer Platform (~1-2 wk)

**Goal:** External developer enablement + accessibility.

**Gaps (5):**
- P1 (3): GAP-006 (upgrade Gemma 4), GAP-038 (developer API docs + SDK), GAP-040 (support impersonation + troubleshooting)
- P2 (2): GAP-074 (AI-generated alt-text a11y), GAP-075 (developer sandbox tenant)

**Parallel strategy:** 2-3 agents. GAP-006 (Gemma 4) is a model swap + retest — may ripple into kitehub-branding.

---

### Wave 13 — KiteHub Admin Console (~2-3 wk)

**Goal:** Build KiteHub admin UI (control plane) + community features.

**Gaps (6):**
- P1 (2): GAP-067 (KH instance control plane — AWS/Vercel-style), GAP-068 (KH admin AI-branding console)
- P2 (4): GAP-035 (wizard team collab multi-user edit), GAP-045 (template marketplace community), GAP-071 (branding migration on tier change), GAP-072 (scheduled rebrand academic-year-tied)

**Parallel strategy:** 3 agents (control plane, branding console, community/marketplace).

**Gate:** ui-review /128 new KH admin screens.

---

### Wave 14 — P2 Final Cleanup (~1 wk, optional deferral)

**Goal:** Close tail-end P2 gaps not yet covered.

**Gaps (~5-10 remaining):**
- GAP-044 (synthetic monitoring + feature flags)
- GAP-099 (structured class schedule)
- Any other P2 surface area not picked up by prior waves

**Parallel strategy:** 2-3 agents, batch close.

**Gate:** 0 P0 gaps open, ≤5 P1 gaps open, quality-audit ≥90.

**Note:** This wave is optional — if user prefers to ship features (Wave 15+ new capabilities) over closing P2 tail, skip.

---

## 3. Timeline & Parallelization

### Serial baseline (worst case): 4-5 months
Each wave runs sequentially with no parallel between waves. ~11 waves × 1.5-3 weeks average.

### Optimal parallel: 2-3 months

**Month 1 (Weeks 1-4):**
- Week 1: Part C Sprint 0+1 parallel + Wave 5 execution starts
- Week 2: Part C Sprint 2 + Wave 5 sub-PRs continue
- Week 3: Part C Sprint 3 + Sprint 4 parallel + Wave 5 finishing
- Week 4: Wave 6 UI kicks off (post Part C Sprint 2 merge) + Wave 9 GA blockers meta in parallel

**Month 2 (Weeks 5-8):**
- Week 5-6: Wave 7 Ops Maturation + Wave 8 Business Governance parallel
- Week 7-8: Wave 10 AI Branding Completeness starts (3 sub-waves in parallel)

**Month 3 (Weeks 9-12):**
- Week 9-10: Wave 10 finishes + Wave 11 K-12 Features starts
- Week 11: Wave 12 Developer Platform + Wave 13 KiteHub Admin Console parallel
- Week 12: Wave 14 P2 cleanup + final quality-audit refresh

**Monthly deliverable milestones:**
- End Month 1: quality 77→85 B+, closed ~20 gaps
- End Month 2: quality 85→90 A−, closed ~50 gaps
- End Month 3: quality ≥90 A, closed ~86 (all open)

---

## 4. Parallelization Principles (reused from Part A/B learnings)

Per `feedback_parallel_agent_strategy.md` hard rules:

1. **Pre-assigned file scopes** per agent — disjoint enough for zero-merge-conflict
2. **Lead owns shared files** — ROADMAP, output-review-mandate, MEMORY; consolidation PR per wave
3. **Pre-assigned GAP number ranges** for follow-up gaps per wave (assign sequential blocks)
4. **Fail-loud guards test profile** — for new config/validator introductions
5. **Sequence merges within wave, parallelize development** — no simultaneous merge of conflicting PRs
6. **Worktree cleanup manual** per wave completion

**Agent cap per wave:** 5 max concurrent (based on observed context/resource pressure). Split bigger waves into sub-waves.

---

## 5. Dependencies & Sequencing Rules

### Hard dependencies (must wait):
- Wave 6 UI depends on Part C Sprint 2 (GAP-127 code-splitting merge)
- Wave 7 Ops depends on Part C Sprint 3 (alertmanager + Grafana foundation)
- Wave 8 Business Governance depends on Part C Sprint 4 (biz debt merged)
- Wave 10 AI Branding depends on Wave 5 (document generation for AI brand export pack GAP-034)
- Wave 13 KiteHub Admin depends on Wave 6 UI (shared component library post-polish)

### Soft dependencies (nice to have but not blocking):
- Wave 9 GA Blockers Meta should precede Wave 10+ for stable template library (GAP-011) + wave mock (GAP-014)
- Wave 11 K-12 features should have Wave 8 persona-review process in place (GAP-050) to prevent regression

### Can parallel (no dependency):
- Wave 9 + Wave 10 (AI branding) — meta governance while AI work proceeds
- Wave 7 (ops) + Wave 8 (biz governance) — disjoint file sets
- Wave 12 (dev platform) + Wave 13 (KH admin) — different surface areas
- Wave 5 + any Part C sprint — zero overlap

---

## 6. Risks & Mitigations

| Risk | Probability | Impact | Mitigation |
|------|:-----------:|:------:|------------|
| Gap scope creep mid-wave | Medium | High | Strict per-wave scope doc; follow-up gaps go to next wave |
| Parallel agent context exhaustion | Medium | Medium | Max 5 agents/wave; serial fallback ready |
| Re-audit drift (scores not matching expected) | Medium | Medium | Quarterly quality-audit; delta tracking per wave |
| Team burnout from 11-wave marathon | High | High | 1-day breather between waves; max 2 weeks sprint without checkpoint |
| External dependency (Gemma 4 release, Ollama updates) | Low | Medium | Pin versions; Wave 12 flexibly timed |
| Production deploy triggered mid-wave (hotfix) | Medium | Low | Hotfix inserts as cross-wave PR; audit override documented |
| Gaps "fixed" but surface problem unaddressed | Medium | High | Persona-based review (GAP-050 in Wave 8) catches functional gaps in fixes |

---

## 7. Success Criteria

**Overall:**
- [ ] 86 gaps closed (allowing ≤5 P1/P2 deferred to next project phase)
- [ ] quality-audit refresh score ≥90/100 A
- [ ] business-logic audit ≥85/100 B+
- [ ] performance audit ≥85/100 B+
- [ ] ops-readiness audit ≥85/100 B+
- [ ] ui-review KH ≥100/128, KC ≥110/128
- [ ] All 5 Part A GA blockers closed (GAP-047, 046, 016, 011, 014, 005)
- [ ] Persona-based review skill operational (GAP-050)
- [ ] Zero CRITICAL violations in `output-review-mandate.md` §4

---

## 8. What's OUT of Master Plan (deferred / future waves)

- **New features beyond current backlog** — e.g., video lessons, live streaming, parent-teacher conference UI — not in any gap file
- **Performance at scale testing** — load testing, 10K tenant simulation (future wave after go-live)
- **Regional expansion** — additional VN-specific or Southeast Asia education standards
- **Mobile apps** — native iOS/Android; currently only responsive web
- **Compliance certifications** — SOC2, ISO 27001 (post-GA)
- **Partnerships / integrations** — LMS gateways, payment aggregators beyond current set

---

## 9. Execution Checkpoints

Every 2 weeks (mid-month):
- [ ] Wave status review (on-track / slipping / blocked)
- [ ] Gap closure velocity vs target
- [ ] Re-audit if merged ≥10 gaps since last audit
- [ ] Adjust wave ordering if dependencies surface

Every 4 weeks (end-month):
- [ ] Full quality-audit refresh
- [ ] ROADMAP snapshot update
- [ ] Master plan revision if scope/timeline shifts

End of master plan (target end Month 3):
- [ ] Final quality-audit = ≥90/100 A
- [ ] Close out remaining P2s OR document acceptance
- [ ] ROADMAP: mark "Phase 1 GA-readiness COMPLETE"
- [ ] Begin Phase 2 (features / scale / partnerships)

---

## 10. Next Actions (in order)

1. **Approve this master plan** (this PR)
2. Start Part C Sprint 0 (audit skill meta fix) — 3h
3. Start Part C Sprint 1 (perf/biz quick wins) — 1-2d
4. Re-audit perf post Sprint 1
5. Continue Part C Sprint 2-4 sequentially
6. Kick Wave 6 UI once Part C Sprint 2 merges
7. Proceed per §3 timeline

**Rollback:** this master plan is LIVING — revise at each 2-week checkpoint. Waves can re-prioritize based on new info (new audit findings, external deadlines, team feedback).

---

## 11. Log

- **2026-04-20:** Master plan drafted after user request for full-backlog coverage. Builds on Part A baselines + Part B fixes + Part C sprint plan + Wave 5 document generation plan. Supersedes prior implicit sequencing.
