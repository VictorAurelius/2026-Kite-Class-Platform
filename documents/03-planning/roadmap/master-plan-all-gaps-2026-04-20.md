---
title: Master Plan — All Open Gaps (12+ Waves, ~2-3 Months)
status: draft
created: 2026-04-20
updated: 2026-04-20
supersedes: none
---

# Master Plan — Fix All Open Gaps

**Snapshot at time of drafting (2026-04-20, post Phase 1+1.5+Phase 2 prep):**
- Total gaps: 178 (59 closed + 119 open)
- Open breakdown: ~16 P0, ~45 P1, ~58 P2 (updated post GAP-190..201 filing)
- Current quality-audit: 77/100 C+ (next refresh due **2026-04-26** per `post-wave-audit-mandate.md` §2.3 weekly cadence)

**Goal:** close ~119 open gaps across 14 waves (2 in-flight + 12 proposed new) in ~2-3 months wall-clock with heavy parallel-agent execution. Estimated 4-5 months if serial.

**2026-04-20 updates:**
- Phase 1 (PR #395): 12 gap skeletons filed GAP-190..201 from action-1 + simulation.
- Phase 1.5 (PR #396): GAP-196 dropped (user decision); GAP-190/197 revised to 🟡 PARTIAL post state-check.
- Phase 1.5 meta (PR #397/398): state-check rule + template + 5 workflow skills hardened.
- Phase 2 (this PR): Master plan rebalanced per `meta-gap-priority.md` §3 Business-Logic tier added 2026-04-20. **New Wave 9 "Audit-Followup Cluster"** absorbs 11 previously-unscheduled OPEN gaps + new BL-P0 (GAP-192) + BL-P1 overflow (GAP-190/191/200). **Existing Wave 9 (GA Blockers Meta — GAP-011/014/016/005) renamed to Wave 9b** to preserve its scope while honoring the new tier ordering (BL-P0 before Feature-P0). **Wave 8b expanded** +6 meta gaps. **Waves 10/11 expanded** with feature tail (GAP-033/052/197).

---

## 1. Wave Overview Table

| # | Wave | Gaps | Priority Mix | Effort (wall-clock) | Status |
|:-:|------|:----:|--------------|---------------------|:------:|
| 1 | **Wave 5** — Document Generation (GAP-047) | 1 (+ sub-PRs) | P0 | 1-2 wk | 🟢 Plan merged PR #361, execution ready |
| 2 | **Part C** — Score Recovery | 14 | 5 P0, 6 P1, 3 P2 | 8-11 days | 🟢 Plan merged PR #381, execution ready |
| 3 | **Wave 6** — UI Polish + UI/UX Pro Max Integration | 9 | 1 P0, 3 P1, 5 P2 | 1-2 wk | 🔵 New (incl. GAP-176) |
| 4 | **Wave 7** — Ops Maturation | 12 | 3 P0, 5 P1, 6 P2 | 2-3 wk | 🔵 New |
| 5 | **Wave 8** — Business Governance | 18 | 13 P0, 2 P1, 3 P2 | 2-3 wk | 🔵 Expanded 2026-04-20 (+ GAP-150..154 + 180..186) |
| 5b | **Wave 8b** — Output Review + Process Governance | 12 | 3 P0 meta + 3 P1 meta + 3 P1 meta (new) + 3 P2 meta (new) | 1-2 wk | 🔵 **Expanded 2026-04-20** (GAP-170–175 + GAP-193/194/195/198/199/201) |
| 6 | **Wave 9** — **Audit-Followup Cluster + BL-P0** | 13 | 1 P0 biz + 3 P1 biz + 6 mixed (ops/perf/biz-cleanup) + 1 hotfix + 2 residual perf | 2-3 wk | 🔵 **NEW 2026-04-20** (absorbs 11 unscheduled + new BL-P0/P1) |
| 6b | **Wave 9b** — GA Blockers Meta (was Wave 9) | 4 | 3 P0 feature, 1 P0 in-progress | 1-2 wk | 🔵 Renamed 2026-04-20 (BL-P0 now ordered ahead per tier rule) |
| 7 | **Wave 10** — AI Branding Completeness | 16 | +1 feature (GAP-052) | 3-4 wk | 🔵 +GAP-052 parent portal completion |
| 8 | **Wave 11** — K-12 Features + Feature Tail | 12 | +2 feature (GAP-033 + GAP-197) | 2-3 wk | 🔵 +GAP-033 version history, +GAP-197 attendance calendar variants |
| 9 | **Wave 12** — Developer Platform | 5 | 3 P1, 2 P2 | 1-2 wk | 🔵 New |
| 10 | **Wave 13** — KiteHub Admin Console | 6 | 2 P1, 4 P2 | 2-3 wk | 🔵 New |
| 11 | **Wave 14** — P2 Final Cleanup | 5-10 | 0 P0, 0 P1, remaining P2 | 1 wk | 🔵 New (batch, optional deferral) |

**Total gaps addressed: ~119.** Coverage 100% of current open backlog (+ follow-up gaps tolerated per wave).

**Tier-driven ordering** (per `.claude/rules/meta-gap-priority.md` §3 Business-Logic tier added 2026-04-20):
Meta-P0 → **Business-Logic-P0 (Wave 9 — GAP-192)** → Feature-P0 (Wave 9b residuals) → Meta-P1 (Wave 8b) → Business-Logic-P1 (Wave 9) → Feature-P1 → ...
The key structural change: **Wave 9 (BL-P0) precedes Wave 9b (Feature-P0)**, closing the tier violation that existed when GA Blockers Meta was the only Wave 9.

---

## 2. Waves 3-11 Detailed (New Waves)

Waves 1 + 2 (Wave 5 + Part C) already have dedicated plan docs. Below are the 9 new waves.

### Wave 6 — UI Polish + UI/UX Pro Max Integration (~1-2 wk)

**Goal:** KH 59→85, KC 81→95 out of 128; close post-audit FE UI gaps + adopt design-system advisor from ui-ux-pro-max-skill.

**Gaps (9 = 8 original + GAP-176):**
- P0: GAP-137 (bulk import UI — Wave 1 backend inaccessible)
- P1: GAP-138 (KC landing hero duplicated text), GAP-139 (parent dashboard MVP placeholder), **GAP-176 (ui-ux-pro-max skill integration)**
- P2: GAP-080 (KH loading/error UX), GAP-140 (form-select English placeholder), GAP-141 (date input locale), GAP-142 (parent-invite Shadcn select)
- Residual partials: GAP-076 (KH mock auth), GAP-079 (KC i18n)

**Sub-sprints:**
- 6a: Bulk import UI + residuals (GAP-137 + GAP-076 + GAP-079)
- 6b: Polish fixes (GAP-138 + GAP-139 + GAP-080 + GAP-140-142)
- 6c: **UI/UX Pro Max integration (GAP-176)** — 3-PR execution per `plan-ui-ux-design-system-integration.md`

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

### Wave 8 — Business Governance (~2-3 wk, expanded 2026-04-20)

**Goal:** Close output-review-mandate violations + business correctness gaps + **BRD Phase 1 P0 docs** (7 new sub-gaps from GAP-154 simulation).

**Gaps (18 — expanded from 6 after GAP-150/151/152/153/154 + GAP-180..186 added):**

*Process + framework (6 original):*
- P0: GAP-049 (business logic CORRECTNESS review — PROCESS scope after split), GAP-050 (persona-based business review — FRAMEWORK scope after split)
- P1: GAP-046 (design patterns systematic application — ref `ai-branding-design-patterns.md`)
- P2: GAP-001 (kiteclass-gateway keep/delete decision — ADR), GAP-102 (05-guides completion + ADR kickoff), GAP-110 (Ollama model inter-service alignment)

*Persona + BRD structural (5 new 2026-04-20):*
- P0: GAP-151 (persona AC template + 4 Tier 1 AC docs), GAP-152 (execute persona review round 1), GAP-153 (secondary persona AC — Student/Parent/Teacher/Admin × tenant contexts), GAP-154 (BRD scope expansion umbrella)
- P1: GAP-150 (BRD 5 strategic skeletons: business-objectives, compliance-scope, pricing-model, nfr-catalog, go-to-market)

*BRD Phase 1 P0 legal docs (7 new 2026-04-20 — GAP-154 Phase 1):*
- P0: GAP-180 (Terms of Service), GAP-181 (Acceptable Use Policy), GAP-182 (Privacy Policy — VN PDPL mandatory), GAP-183 (Refund + Dispute Resolution — VN Consumer Protection Law mandatory), GAP-184 (Data Retention + Deletion — VN PDPL Art 6 mandatory), GAP-185 (Billing Terms + VAT/TCT — Circular 78/2021 mandatory), GAP-186 (Child Protection Policy — K-12 blocker, Law on Children)

**Parallel strategy:** Expanded to **5-6 agents** given 18 gaps:
- Agent A: GAP-049 + GAP-050 (process framework)
- Agent B: GAP-151 + GAP-153 (persona AC — sequential dependency)
- Agent C: GAP-152 (persona review execution) — blocks on B
- Agent D: GAP-150 + GAP-180/182 (BRD strategic + TOS + Privacy — shared legal review)
- Agent E: GAP-181 + GAP-183 + GAP-184 (AUP + Refund + Retention — policy cluster)
- Agent F: GAP-185 + GAP-186 (Billing + Child Protection — finance + K-12 cluster)

*P2 gaps (001, 046, 102, 110) can piggyback any agent with capacity.*

**Dependencies:** Depends on Part C Sprint 4 completion (biz debt cleanup — GAP-108/109/110). **Phase 2 content fill for GAP-180..186 requires legal counsel engagement** — can proceed with skeletons first.

**Gate:** business-logic audit refresh ≥85 + persona-review skill shippable + 7 BRD legal docs skeletons exist + Phase 2 content roadmap documented.

**Note on sizing:** Wave 8 scope expanded ~3x from original (6 → 18 gaps). If effort becomes unmanageable, split into Wave 8a (process + persona, 11 gaps) and Wave 8c (BRD legal docs, 7 gaps). Initial execution plan: single wave, 2-3 weeks with 5-6 parallel agents.

---

### Wave 8b — Output Review + Process Governance (~1-2 wk, expanded 2026-04-20)

**Goal:** Close 6 output-review-mandate §4 VIOLATIONS as dedicated meta wave + absorb 6 new Meta-P1/P2 gaps from action-1 reorganization. Force-multiplier for all future output quality.

**Gaps (12, all meta — 6 original + 6 new from GAP-190..201 filing):**

*Original (output-review-mandate violations):*
- P0: GAP-170 (gap reports review template), GAP-171 (rules docs ADR-like), GAP-172 (architecture ADR process)
- P1: GAP-173 (email template review), GAP-174 (marketing + legal review)
- P2: GAP-175 (logs format standard)

*New 2026-04-20 (action-1 §15 + simulation Part C):*
- P1: GAP-193 (session orchestration + /start-session skill + multi-session lock), GAP-194 (shellcheck + ruff CI enforcement), GAP-199 (rework audit for context-degraded PRs), GAP-201 (tenant off-boarding runbook — consumes GAP-073 deferred items)
- P2: GAP-195 (starter-kit bulk retro-sync), GAP-198 (FE↔BE decoupled consumer-side contract tests — producer side DONE via GAP-090)

**Parallel strategy:** 5-6 agents, disjoint file sets:
- Agent 8b-A: GAP-170 + GAP-171 (meta-governance skills — gap review + rule ADR)
- Agent 8b-B: GAP-172 (architecture ADR folder + 5 retrospective ADRs)
- Agent 8b-C: GAP-173 + GAP-174 (email + marketing/legal review skills)
- Agent 8b-D: GAP-175 + GAP-194 (logging standard + shellcheck CI — both standards)
- Agent 8b-E: GAP-193 + GAP-199 (session skill + rework audit — sibling session concerns)
- Agent 8b-F: GAP-201 + GAP-195 + GAP-198 (runbook + starter-kit sync + contract tests — remaining meta)

**Dependencies:**
- Coordinates with Wave 7 Ops (GAP-175 standards drive GAP-114/115/116 implementation)
- Precedes Wave 10 AI branding (email template review needed for branding emails)
- GAP-201 consumes GAP-073 DONE's deferred items (MinIO streaming export, @Scheduled expiry, pseudonymization)
- GAP-199 consumes GAP-193 session-orchestration detection heuristic

**Gate:** output-review-mandate §4 VIOLATIONS count: 6 → 0 + 6 new meta gaps DONE.

---

### Wave 9 — Audit-Followup Cluster + BL-P0 (NEW 2026-04-20, ~2-3 wk)

**Goal:** Close Business-Logic-P0 (ahead of Feature-P0 per new tier rule) + absorb 11 previously-unscheduled OPEN gaps from audit catch-up (Parts A/B/re-audit) + add BL-P1 overflow that lacked prior wave home.

**Gaps (13):**

*BL-P0 (tier leader — per `.claude/rules/meta-gap-priority.md` §3):*
- **GAP-192** — Trial → Paid zero-downtime migration (state machine + outbox + rollback; layers below GAP-026 AI-budget concern)

*BL-P1 (new 2026-04-20 from action-1 — no prior wave home):*
- GAP-190 — KiteHub SEO completion (pricing SSR, canonical schemas, GA4, content plan, Lighthouse CI; sitemap/robots/OG/JsonLd/blog MDX already shipped)
- GAP-191 — Domain registration + DNS strategy (kitehub.vn + per-instance subdomain + custom CNAME)
- GAP-200 — School MIS/SMS integration (VNEDU + SMAS + Base.vn; K-12 P5 onboarding blocker)

*Business-logic cleanup (Part A re-audit residuals — `audit-to-gap-pipeline.md` Step 6):*
- GAP-106 — Branding routing config keys missing
- GAP-109 — Bulk import rules.md documentation
- GAP-148 — BR-QUEUE-015/018 dead CB config (circuit breaker annotations missing)

*Performance (Part A baseline residuals):*
- GAP-043 — Cache stampede protection
- GAP-132 — `@EnableCaching` missing kitehub services
- GAP-134 — JOIN FETCH / `@EntityGraph` near-absent
- GAP-135 — API p95 latency SLOs undocumented

*Resilience + hotfix (Part A / Part B residuals):*
- GAP-146 — External HTTP timeouts remainder (3/9 unresolved from GAP-131)
- GAP-147 — Kitehub-admin bean conflict (pre-existing, discovered in Part B)

**Parallel strategy:** 5-6 agents, disjoint file sets:
- Agent 9-A: **GAP-192** (trial→paid — highest priority, BL-P0; 3-layer docs + state machine + outbox design)
- Agent 9-B: GAP-190 + GAP-191 (SEO completion + DNS strategy — shared kitehub-frontend + infra concerns)
- Agent 9-C: GAP-200 (school MIS — integration catalog + VNEDU pilot adapter)
- Agent 9-D: GAP-106 + GAP-109 + GAP-148 (business-logic cleanup batch — shared rules.md edits)
- Agent 9-E: GAP-043 + GAP-132 + GAP-134 + GAP-135 (performance cluster — shared Hibernate/Spring config)
- Agent 9-F: GAP-146 + GAP-147 (resilience + hotfix — disjoint services)

**Dependencies:**
- GAP-192 depends on GAP-108 (trial config hardcoded — OPEN; should be closed in same PR batch)
- GAP-190/191 block GTM (GAP-150 Phase 2 content fill)
- GAP-135 API SLO doc needed before Wave 7 ops alerts are tuned

**Gate:** business-logic audit refresh ≥ 80, performance audit refresh ≥ 75 (both up from current 72/64); quality-audit refresh targeting 85/100 B+.

**Stop condition (this PR):** per original Phase 3 prompt — after GAP-192 3-layer docs drafted, pause for user review before Agents B-F launch.

---

### Wave 9b — GA Blockers Meta (was Wave 9, ~1-2 wk)

**Goal:** Close 4 remaining Part A GA blockers (meta priority per `meta-gap-priority.md`).

Renamed from Wave 9 to Wave 9b on 2026-04-20 to make room for the new Wave 9 (Audit-Followup + BL-P0) per tier-ordering rule: BL-P0 precedes Feature-P0.

**Gaps (4):**
- P0 meta: GAP-011 (template library curation — 30 templates), GAP-014 (wave mock plan include AI branding), GAP-016 (living docs impact scope — 3-layer sweep)
- P0 in-progress: GAP-005 (AI queue fair scheduling Phase 2 — continuation of Phase 1 closed Part B)

**Parallel strategy:** 4 agents, each gap 1 agent. GAP-011 heaviest (requires template creation + review pipeline).

**Gate:** quality-audit refresh targeting 90/100 A−.

---

### Wave 10 — AI Branding Completeness (~3-4 wk, LARGEST)

**Goal:** Complete AI branding feature set (Epic 2 + Epic 3 residuals) + absorb parent portal completion.

**Gaps (16 — 15 original + GAP-052 added 2026-04-20):**
- P1 (7): GAP-017 (AI usage → billing), GAP-019 (AI observability + cost monitoring), GAP-020 (wizard state persistence + error recovery), GAP-023 (admin moderation tools), GAP-026 (trial/freemium AI mechanics — align with Wave 9 GAP-192), GAP-036 (tier upgrade UX reveal/teaser/unlock), GAP-039 (webhook reliability — retry/idempotency/versioning)
- P1 feature (added 2026-04-20): **GAP-052** (parent portal completion — IN_PROGRESS; enables GAP-197 parent-variant calendar in Wave 11)
- P2 (8): GAP-003 (multi-tier image gen strategy), GAP-004 (template-based image composition Canva-like), GAP-022 (template analytics), GAP-024 (asset lifecycle + storage cleanup), GAP-025 (mobile-first wizard UX), GAP-027 (multi-brand per tenant — franchise), GAP-028 (AI model versioning + migration), GAP-029 (quality gate calibration), GAP-030 (AI branding DR)

**Sub-waves (3 parallel tracks):**
- 10a: Billing/observability track (017, 019, 026, 036)
- 10b: UX/wizard track (020, 022, 023, 025, 029)
- 10c: Infrastructure track (003, 004, 024, 027, 028, 030, 039)

**Parallel strategy:** 3 sub-waves in parallel, each with 2-3 agents. Heavy coordination on domain API contracts.

**Gate:** Persona-based review + quality-audit refresh; AI branding feature shipped to all tiers.

---

### Wave 11 — K-12 Features + Feature Tail (~2-3 wk, expanded 2026-04-20)

**Goal:** Complete K-12 education-specific features (Epic retention) + close feature tail from action-1 reorganization.

**Gaps (12 — 10 original + GAP-033 + GAP-197 added 2026-04-20):**
- P1 (5): GAP-055 (official report card VN format), GAP-056 (homeroom teacher GVCN concept), GAP-057 (teacher payroll + commission), GAP-063 (SMS + Zalo notifications), GAP-066 (KH unified reports/analytics)
- P2 (5): GAP-059 (conduct/behavior hạnh kiểm), GAP-060 (period-based attendance), GAP-061 (promotion/retention logic), GAP-062 (payroll bank batch), GAP-064 (SCORM/xAPI)
- P2 feature tail (added 2026-04-20):
  - **GAP-033** — Branding version history + rollback (IN_PROGRESS Wave 4 partial — manual rollback done; auto + A/B deferred to this wave)
  - **GAP-197** — Attendance calendar variants (enhanced-attendance-calendar shipped PR 3.8.1; narrowed to parent/student variants + a11y + week view + E2E; parent variant depends on GAP-052 Wave 10)

**Parallel strategy:** 3-4 agents by subdomain (grading, attendance + GAP-197, payroll, notifications, branding history GAP-033).

**Gate:** Persona-based review with real K-12 school scenario (500 students bulk import → end-of-semester report) + UI review /128 on GAP-197 updates.

**Dependencies:**
- GAP-197 parent-variant blocked by GAP-052 (Wave 10)
- GAP-033 consumes branding export/package APIs (all DONE in earlier waves)

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
- **Wave 9 (BL-P0 + Audit-Followup) depends on GAP-108 (trial config hardcoded) closing first — blocks GAP-192 clean config**
- **Wave 9 precedes Wave 9b (GA Blockers Meta)** — per `meta-gap-priority.md` §3 tier rule BL-P0 before Feature-P0
- Wave 10 AI Branding depends on Wave 5 (document generation for AI brand export pack GAP-034)
- **Wave 10 GAP-052 (parent portal) blocks Wave 11 GAP-197 parent variant**
- Wave 13 KiteHub Admin depends on Wave 6 UI (shared component library post-polish)

### Soft dependencies (nice to have but not blocking):
- Wave 9b GA Blockers Meta should precede Wave 10+ for stable template library (GAP-011) + wave mock (GAP-014)
- Wave 11 K-12 features should have Wave 8 persona-review process in place (GAP-050) to prevent regression
- Wave 8b GAP-193 session-orchestration skill should precede Wave 9+ for better session handoff

### Can parallel (no dependency):
- **Wave 9 Agent 9-A (GAP-192) serial with Wave 9 Agents B-F** — prompt stop condition: pause after GAP-192 design for user review before other agents launch
- Wave 9b + Wave 10 (AI branding) — meta GA while AI work proceeds
- Wave 7 (ops) + Wave 8 (biz governance) — disjoint file sets
- Wave 8b + Wave 9 — meta governance while BL-P0 in flight (disjoint: 8b = skills/rules/runbooks, 9 = code + docs)
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
- [ ] ~119 gaps closed (allowing ≤5 P1/P2 deferred to next project phase)
- [ ] quality-audit refresh score ≥90/100 A (next refresh due 2026-04-26 weekly cadence)
- [ ] business-logic audit ≥85/100 B+
- [ ] performance audit ≥85/100 B+
- [ ] ops-readiness audit ≥85/100 B+
- [ ] ui-review KH ≥100/128, KC ≥110/128
- [ ] All 5 Part A GA blockers closed (GAP-047, 046, 016, 011, 014, 005) — via Wave 9b
- [ ] **GAP-192 trial→paid BL-P0 shipped** — via Wave 9
- [ ] Persona-based review skill operational (GAP-050)
- [ ] Zero CRITICAL violations in `output-review-mandate.md` §4
- [ ] State-check rule (`audit-to-gap-pipeline.md` Step 2.5) followed for all Wave 9+ gap work (no rewrite debt like GAP-190/197)

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

## 15. Operational Tasks Parallel to Waves

Non-gap operational tasks that run ALONGSIDE waves, not as sub-waves themselves.

### 15.1 Starter-Kit Upstream Sync (3 scheduled syncs)

Project is SOURCE of learnings for `github.com/VictorAurelius/claude-starter-kit` (canonical remote, currently v2.2.0 from 2026-04-04). Remote is 16+ days stale vs project's 40+ PRs. Phased sync per `reference-repos-and-starter-kit-coverage-2026-04-20.md`:

| Sync | Timing | Scope | Version bump | Effort |
|:----:|--------|-------|:------------:|:------:|
| **Sync 1** | End of Month 1 (after Part C + Wave 5 complete) | 6 new rules (post-wave-audit-mandate, meta-gap-priority, audit-to-gap-pipeline, mcp-first-with-fallback, output-review-mandate, docs-folder-structure) + 4-5 core audit skills (business-logic, performance, ops-readiness, pr-health) + audit-gate.py hook | 2.2.0 → **2.3.0** (MINOR) | 2-3 giờ |
| **Sync 2** | End of Month 2 (after Wave 7 ops + Wave 8/8b governance complete) | Ops-readiness-audit refined + output-review skills (gap review, rule ADR, email template, marketing/legal, logs standard) | 2.3.0 → **2.4.0** (MINOR) | 2-3 giờ |
| **Sync 3** | End of Month 3 (after master plan complete) | Comprehensive upgrade — parallel-agent pattern doc + all remaining skills + doc structure reorganization learnings | 2.4.0 → **3.0.0** (MAJOR — doc structure may break) | 2-3 giờ |

**Sync process per `.claude/rules/skill-conventions.md` §Remote Repo Sync.**

**NOT in scope for sync:** memories (project-specific), CLAUDE.md (project-specific), ROADMAP.md, gap files, PR logs.

**Trigger:** reminder at end of each month checkpoint (§9).

### 15.2 Quarterly review of deferred decisions

Per ADR-015 (AWS Agent Plugins — DEFER to Q3 2026), schedule reminder to revisit at Q3 2026 when 3 conditions may trigger:
- Pilot tenant committed to AWS production
- Wave 10 AI Branding observability shipped (Prometheus/Grafana baseline)
- AWS Agent Plugins pricing model public

Track in ADR-015 §Revisit Schedule.

### 15.3 Monthly quality-audit refresh

- End Month 1: quality-audit /100 refresh post Part C + Wave 5
- End Month 2: quality-audit /100 refresh post Wave 7/8/8b
- End Month 3: quality-audit /100 refresh final — target ≥90 A

Audit runs per `post-wave-audit-mandate.md` freshness window. Results feed into §9 checkpoints.

---

## 11. Log

- **2026-04-20 (Phase 2 — this PR):** Rebalanced master plan per `meta-gap-priority.md` §3 (Business-Logic tier added 2026-04-20) + absorbed 12 new gaps from action-1 reorganization.
  - **NEW Wave 9** "Audit-Followup Cluster + BL-P0" (13 gaps): GAP-192 BL-P0 + GAP-190/191/200 BL-P1 + 9 previously-unscheduled Part A/B residuals (GAP-106/109/148 biz-cleanup, GAP-043/132/134/135 performance, GAP-146 resilience, GAP-147 hotfix).
  - **Renamed existing Wave 9 → Wave 9b** "GA Blockers Meta" (content unchanged: GAP-011/014/016/005). Structural change: BL-P0 now precedes Feature-P0 per tier rule.
  - **Expanded Wave 8b** 6 → 12 gaps: +GAP-193/194/199/201 (P1 meta) + GAP-195/198 (P2 meta). GAP-196 was dropped by user (see PR #396).
  - **Expanded Wave 10** 15 → 16: +GAP-052 parent portal completion (blocks Wave 11 GAP-197 parent variant).
  - **Expanded Wave 11** 10 → 12: +GAP-033 branding version history, +GAP-197 attendance calendar variants.
  - Snapshot: total 148→178 gaps (12 new skeletons, 1 dropped GAP-196, 5 closed today), 59 DONE.
  - Quality-audit next refresh due **2026-04-26** per `post-wave-audit-mandate.md` §2.3 weekly cadence.
  - Phase 3 stop condition: pause after GAP-192 3-layer docs drafted for user review.
- **2026-04-20 (later):** Added GAP-176 (ui-ux-pro-max integration) to Wave 6 scope as sub-sprint 6c. Added §15 "Operational Tasks Parallel to Waves" covering starter-kit sync schedule (3 phased releases: 2.3.0 M1, 2.4.0 M2, 3.0.0 M3), quarterly deferred-decisions review, monthly quality-audit refresh cadence.
- **2026-04-20:** Master plan drafted after user request for full-backlog coverage. Builds on Part A baselines + Part B fixes + Part C sprint plan + Wave 5 document generation plan. Supersedes prior implicit sequencing.
