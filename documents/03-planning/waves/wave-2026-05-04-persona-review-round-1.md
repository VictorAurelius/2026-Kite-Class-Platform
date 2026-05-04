---
title: Wave Persona Review Round 1 — Execute GAP-152 (4 Tier-1 personas)
status: planned
created: 2026-05-04
waves: [17]
gaps: [GAP-152, GAP-050]
ac_input: [P1, P2, P3, P5 + 8 secondary]
expected_outputs: 4 review reports + ~25 candidate new gaps
---

# Wave 17 — Persona Review Round 1 Execution (GAP-152)

**Wave kickoff readiness:** 🟢 ALL preconditions met (GAP-151 + GAP-153 shipped 2026-04-30; 12 AC docs available; `persona-based-business-review.md` skill exists).

**Wall-clock estimate:** ~60-90 min for 4 parallel agents in Phase 2 (next session). This document = Phase 1 plan + foundation, ~15 min.

---

## §1 Brainstorm (5-min sprint)

**Q1 — Persona alignment.** Does this wave touch all Tier-1 personas the catalog claims coverage for? **Yes** — P1 (solo teacher), P2 (small center), P3 (medium center), P5 (K-12 school). Tier-1 = highest-priority targets for GA. Tier-2/3 deferred to GAP-155+ per GAP-152 §Out-of-scope.

**Q2 — Trade-offs.** Sequential vs parallel:
- Sequential (original GAP-152 design 2026-04-20): first review calibrates methodology, AC patterns refine subsequent reviews. Cost: 4× wall-clock.
- Parallel (ROADMAP signpost 2026-04-30 update): 4 disjoint personas, no shared state, agents read-only on AC docs. Cost: methodology calibration risk if Agent A's pattern doesn't propagate.

**Decision: parallel** per `feedback_wave_plan_before_serial_prs.md` (4 disjoint sub-tasks = wave). Calibration risk mitigated by explicit report template (GAP-152 §Report template) — every agent uses same scoring schema.

**Q3 — Risks.**
- Risk 1: agents file overlapping gaps (e.g., "no bulk import UI" surfaced by both P3 and P5). Mitigation: **dedupe + cross-link in closure PR**, not in agent PRs (prevents agent-side coordination overhead).
- Risk 2: P5 review is largest (49.7K AC + 4 secondaries = ~165K input). May exhaust agent context. Mitigation: agent processes section-by-section, scores stored incrementally; if context heat → ship partial report, file follow-up gap to finish.
- Risk 3: Pre-allocated GAP ranges over/under-shoot. Mitigation: ranges generous (60 reserved, ~25 expected); unused numbers fine.

---

## §2 Task Breakdown

| # | Task | Phase | Wall-clock | Owner |
|---|------|:---:|:---:|---|
| 1 | This plan + foundation README | 1 | 15 min | Claude (parent) |
| 2 | Open + merge foundation PR | 1 | 5 min | User approve |
| 3 | Spawn 4 background agents (worktree-isolated) | 2 | <5 min | Claude (parent, fresh session) |
| 4a | Agent A — P1 review | 2 | 15-20 min | Background agent |
| 4b | Agent B — P2 review | 2 | 15-20 min | Background agent |
| 4c | Agent C — P3 review | 2 | 25-30 min | Background agent |
| 4d | Agent D — P5 review | 2 | 35-45 min | Background agent (largest) |
| 5 | 4 individual review PRs reviewed + merged | 2 | 10-15 min | User approve, Claude merge |
| 6 | Closure PR — dedupe gaps, ROADMAP sync, catalog scores update, GAP-152 → DONE | 2 | 15 min | Claude (parent) |

**Phase 1 total:** ~20 min. **Phase 2 total:** ~75-100 min.

---

## §3 Scope per Bucket

Each agent ships ONE PR with their persona's review report + any new gap files filed.

### Bucket A — P1 Solo Teacher

| Item | Value |
|------|-------|
| Branch (in worktree) | `wave/persona-review-p1` |
| AC docs consumed | `documents/00-brd/persona-criteria/P1-solo-teacher.md` (no secondaries — solo persona) |
| Scale | 1 teacher, 15 students, 3 courses |
| Output report | `documents/00-brd/persona-reviews/P1-solo-teacher-round-1-2026-05-04.md` |
| GAP range reserved | **GAP-286..295** (10 reserved) |

### Bucket B — P2 Small Center

| Item | Value |
|------|-------|
| Branch | `wave/persona-review-p2` |
| AC docs consumed | `P2-small-center.md` + `secondary/student-in-P2.md` |
| Scale | 2 teachers + 1 owner, 60 students, 8 classes |
| Output report | `documents/00-brd/persona-reviews/P2-small-center-round-1-2026-05-04.md` |
| GAP range reserved | **GAP-296..305** (10 reserved) |

### Bucket C — P3 Medium Center

| Item | Value |
|------|-------|
| Branch | `wave/persona-review-p3` |
| AC docs consumed | `P3-medium-center.md` + `secondary/student-in-P3.md` + `secondary/admin-in-P3.md` + `secondary/teacher-employee-in-P3.md` |
| Scale | 12 teachers, 300 students, 30 classes, 2 admins |
| Output report | `documents/00-brd/persona-reviews/P3-medium-center-round-1-2026-05-04.md` |
| GAP range reserved | **GAP-306..320** (15 reserved) |

### Bucket D — P5 K-12 School (largest, user's priority)

| Item | Value |
|------|-------|
| Branch | `wave/persona-review-p5` |
| AC docs consumed | `P5-k12-school.md` + `secondary/student-in-P5.md` + `secondary/parent-in-P5.md` + `secondary/teacher-employee-in-P5.md` + `secondary/admin-in-P5.md` |
| Scale | 45 teachers, 1200 students, 40 classes, principal + 2 VPs + 3 dept heads + 5 staff + 1200 parents |
| Output report | `documents/00-brd/persona-reviews/P5-k12-school-round-1-2026-05-04.md` |
| GAP range reserved | **GAP-321..345** (25 reserved — biggest persona, expect most findings esp. legal/compliance per parent-in-P5 84 legal citations) |

---

## §4 Design Layer Coverage (per `design-layer-coverage.md` §2.3)

This is a **review wave** — consumes existing artifacts, doesn't ship new design. Coverage:

| Layer | Status | Notes |
|---|:---:|---|
| 要件定義 (Requirements) | ✅ explicit | All 4 personas have full AC docs (288 ACs total) — input to this wave |
| 基本設計 (External / UI) | ⚠️ implicit | Reviewed via screenshots/UI walk-throughs as evidence; missing screens become FAIL evidence (not new design) |
| 詳細設計 (Internal) | N/A | Review wave — no new state machines / ADRs / sequences |
| コンポーネント設計 (Component) | N/A | Review wave — no new components |

N/A reasoning per §2.3 override: review consumes layer 1 + measures against layer 2 (existing). Layers 3+4 not produced. Future fix waves for FAIL findings will require all 4 layers per `design-layer-coverage.md` standard.

---

## §5 Pre-allocated GAP Number Ranges

Reserved before agents spawn to prevent collision. Agents file new gaps **starting from their range bottom**, skipping gaps already filed elsewhere.

| Range | Bucket | Reserved | Expected use |
|---|---|:---:|:---:|
| GAP-286..295 | A (P1) | 10 | ~5-7 |
| GAP-296..305 | B (P2) | 10 | ~5-7 |
| GAP-306..320 | C (P3) | 15 | ~7-10 |
| GAP-321..345 | D (P5) | 25 | ~10-15 |

**Total reserved:** GAP-286..345 (60 slots). **Expected actual:** ~25 new gaps. Unused numbers fine — gap numbering is monotonic, gaps fine.

If agent overflows their range → ship report PARTIAL with note, parent allocates extension range in closure PR.

---

## §6 Agent Prompt Template (per bucket)

Each agent gets this prompt with bucket-specific values filled in. **Spawn in Phase 2 with `isolation: worktree` + `run_in_background: true`** per `agent-background-spawn-default.md`:

```
You are executing Wave 17 Bucket {X} — Persona Review Round 1 for {persona_id} {persona_name}.

CONTEXT:
- Wave plan: documents/03-planning/waves/wave-2026-05-04-persona-review-round-1.md (your bucket = §3 Bucket {X})
- GAP being closed (parent): documents/04-quality/gaps/GAP-152-execute-persona-review-round-1.md
- Reusable skill: .claude/skills/quality/persona-based-business-review.md (METHODOLOGY)
- Audit-to-gap pipeline: .claude/rules/audit-to-gap-pipeline.md (NEW gap filing protocol)
- 4-layer design coverage: .claude/rules/design-layer-coverage.md (verify §2.1 matrix when filing FAIL gaps)
- Output review mandate: .claude/rules/output-review-mandate.md §3 (review report standards)

INPUTS (read fully):
- Tenant AC: documents/00-brd/persona-criteria/{P_FILE}.md
- Secondary ACs (if applicable): {SECONDARY_PATHS}

TASK:
1. Role-play scenario at scale: {SCALE_DESCRIPTION}
2. Walk persona's journey end-to-end: discovery → signup → provisioning → daily ops → financial → communication → edge case → termination
3. For EACH AC item, score PASS / PARTIAL / FAIL with concrete evidence:
   - PASS: cite working endpoint / UI screen path / config key
   - PARTIAL: cite partial implementation + what's missing
   - FAIL: cite non-existence (no endpoint / no UI / no config) — this drives a NEW gap
4. File NEW gap files for FAIL cases NOT already tracked:
   - Search existing gaps first (Step 2 of audit-to-gap-pipeline)
   - Reserved range for this bucket: **GAP-{RANGE_LO}..{RANGE_HI}**
   - Use template per audit-to-gap-pipeline.md §3
5. Write review report to: documents/00-brd/persona-reviews/{REPORT_FILE}
   - Use template per GAP-152 §Report template
   - Status: draft (closure PR flips to approved)
   - Report MUST include: total ACs scored, PASS/PARTIAL/FAIL counts, coverage score /100, list of NEW gap IDs filed, top 3-5 critical findings, priority-reordering recommendation
6. Commit + push + open PR with title `docs: Wave 17 Bucket {X} — {persona_id} review (round 1)`

CONSTRAINTS:
- Stay in your bucket — DO NOT touch other personas' AC docs or modify existing files outside scope
- Reports go to documents/00-brd/persona-reviews/ ONLY — directory + README already created in foundation PR
- New gap files go to documents/04-quality/gaps/GAP-XXX-*.md — use reserved range above
- Use RELATIVE paths (per feedback_worktree_absolute_path_contamination.md)
- Use Vietnamese for narrative communication; English OK for code/citations
- Report MUST include verification artifact pointers (file path / line number / API path) for every PASS/PARTIAL/FAIL — no unsupported claims
- If you exhaust your bucket's reserved GAP range, STOP, ship report PARTIAL with note — parent allocates extension

DELIVERABLES (this PR):
- 1× review report at documents/00-brd/persona-reviews/{REPORT_FILE}
- N× new gap files (where FAIL findings warrant new gaps)
- Branch pushed, PR opened against main

DO NOT:
- Update ROADMAP — closure PR does that
- Update personas-catalog.md "Coverage Review Status" — closure PR does that
- Flip GAP-152 → DONE — closure PR does that
- Spawn nested agents
```

---

## §7 Acceptance Criteria (this wave)

- [ ] Phase 1 foundation PR merged (wave plan + persona-reviews/README.md + directory)
- [ ] 4 review report PRs (one per Tier-1 persona) merged
- [ ] At least 1 NEW finding per persona NOT already tracked in GAP-051..064 (proves review adds value beyond quick scan)
- [ ] Closure PR merged: ROADMAP §Status Snapshot updated, personas-catalog.md "Coverage Review Status" replaced estimates with measured scores, GAP-152 → 🟢 DONE per `gap-done-discipline.md`
- [ ] All new gaps filed land in their reserved ranges + cross-referenced in ROADMAP
- [ ] No banned phrases in GAP-152 closure Log entry; verification artifact pointer cites this wave's PRs

---

## §8 Sequence (execution order)

```
Phase 1 (THIS PR — current session, ~20 min)
  └─ Foundation PR
     ├─ wave plan (this file)
     ├─ documents/00-brd/persona-reviews/README.md
     └─ Reserved GAP ranges documented
  └─ User approves + merge

Phase 2 (NEXT session — fresh /clear, ~75-100 min)
  └─ /start-session
  └─ Claude (parent) spawns 4 background agents in single message:
     ├─ Agent A (P1) — worktree, ~15-20 min
     ├─ Agent B (P2) — worktree, ~15-20 min
     ├─ Agent C (P3) — worktree, ~25-30 min
     └─ Agent D (P5) — worktree, ~35-45 min
  └─ Claude (parent) coordinates as agents complete:
     ├─ Verify each agent's PR matches §3 bucket scope
     ├─ Approve + merge in completion order (no specific sequence required)
     └─ Track GAP numbers actually used vs reserved
  └─ Closure PR
     ├─ Dedupe new gaps across personas (cross-link if same finding)
     ├─ Update ROADMAP §Status Snapshot
     ├─ Update personas-catalog.md "Coverage Review Status" with measured scores
     ├─ Flip GAP-152 → 🟢 DONE per `gap-done-discipline.md`
     └─ Update Log + memory entry if methodology lessons surface
```

---

## §9 Anti-patterns to avoid

| ❌ Don't | ✅ Do |
|---------|------|
| Spawn agents foreground | `run_in_background: true` per `agent-background-spawn-default.md` |
| Agents push directly to main | Agents open PRs; user approves; Claude merges |
| Agents update ROADMAP / catalog | Closure PR does cross-cutting updates |
| Agents file gaps outside their reserved range | Stop + report PARTIAL if range exhausted |
| Skip state-check before filing | Each new gap follows `audit-to-gap-pipeline.md` Step 2 (dedupe) + Step 2.5 (state-check against current code) |
| Flip GAP-152 → DONE in any agent's PR | Closure PR ONLY (per `gap-done-discipline.md`) |

---

## §10 Log

- **2026-05-04** — Phase 1 wave plan created (this file). Followed `feedback_wave_plan_through_pr.md` (PR before agents). Pre-allocated GAP ranges GAP-286..345. Agent spawn deferred to fresh /clear session per `/start-session` skill degradation rule (current session ~3-4h includes hotfix + SSH setup; wave execution requires fresh context).
