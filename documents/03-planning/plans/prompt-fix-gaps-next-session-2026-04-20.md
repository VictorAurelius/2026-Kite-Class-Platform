---
title: Next-Session Prompt — File 12 New Gaps + Fix Plan Coverage + Execute Wave 9
status: active
created: 2026-04-20
updated: 2026-04-20
purpose: Self-contained prompt for the next Claude session to (1) file gap candidates identified in action-1 reorganization + system simulation, (2) repair master plan coverage, (3) begin executing Wave 9.
use: Paste §Prompt section as the first message of the next session.
---

# Next-Session Prompt

**Copy §Prompt verbatim as your opening message in the next session.**
Companion files (already on disk, do NOT re-read unless needed):
- `documents/action-1.md` — thematic action log (14 sections + §15 new gap candidates + §0 decisions log)
- `documents/04-quality/audits/simulation-action-1-2026-04-20.md` — simulation report (Part A-D findings)
- `.claude/rules/meta-gap-priority.md` — priority tier order (Meta → Business-Logic → Feature at each P-level)
- `.claude/rules/audit-to-gap-pipeline.md` — gap filing process (dedupe → file → memory → roadmap → fix)
- `.claude/rules/post-wave-audit-mandate.md` — audit cadence enforcement

---

## Prompt

Session goal: fix all identified gaps (new candidates + plan-coverage bugs) and start Wave 9.

### Context (read if needed)
- `documents/action-1.md` §15 — 10 new gap candidates (GAP-190..199) derived from action-1 thematic reorganization.
- `documents/04-quality/audits/simulation-action-1-2026-04-20.md` — 12 total new candidates (adds GAP-200 MIS integration + GAP-201 tenant off-boarding) + 11 unscheduled OPEN gaps + 4 plan-alignment issues.
- `documents/03-planning/roadmap/master-plan-all-gaps-2026-04-20.md` — current master plan (needs Wave 9 update).
- Priority rule: **Meta → Business-Logic → Feature** at each P-level (`.claude/rules/meta-gap-priority.md` §3).

### Work plan (execute in order)

**Phase 1 — File gap candidates (1 PR)**
File 12 skeleton gap files using `.claude/rules/audit-to-gap-pipeline.md` §3 template. Order of filing:

1. **Business-Logic-P0** (1 gap): GAP-192 Trial→Paid zero-downtime migration
2. **Business-Logic-P1** (3 gaps): GAP-190 SEO + Marketing, GAP-191 Domain + DNS strategy, GAP-200 School MIS/SMS integration
3. **Meta-P1** (4 gaps): GAP-193 Session orchestration, GAP-194 Script compliance (shellcheck/ruff), GAP-199 Rework audit for context-degraded PRs, GAP-201 Tenant off-boarding runbook
4. **Meta-P2** (3 gaps): GAP-195 Starter-kit retro-sync, GAP-196 9router evaluation, GAP-198 FE↔BE mock contract tests
5. **Feature-P2** (1 gap): GAP-197 Attendance calendar-mode UI

For each gap: use template, include Problem / Root Cause / Proposed Fix / AC / Related sections. Keep skeletons concise — Phase 2/3 fills content.

**Phase 2 — Repair plan coverage (same PR or follow-up PR)**
Update `documents/03-planning/roadmap/master-plan-all-gaps-2026-04-20.md`:

1. Add **Wave 9 "Audit-Followup Cluster"** containing the 11 unscheduled OPEN gaps:
   - Business-logic cleanup: GAP-106, GAP-109, GAP-148
   - Performance: GAP-043, GAP-132, GAP-134, GAP-135
   - Resilience: GAP-146
   - Hotfix: GAP-147
   - Feature: GAP-033 (version history) → Wave 11 instead
   - Feature: GAP-052 (parent portal completion) → Wave 10 instead
2. Assign newly filed GAP-190..201 to waves per priority:
   - Wave 9: GAP-192 (BL-P0) + add to existing cluster
   - Wave 9 or 10: GAP-190, 191, 200 (BL-P1)
   - Wave 8b (meta): GAP-193, 194, 199, 201, 195, 196, 198
   - Wave 11 (features): GAP-197
3. Re-order wave sequence to front-load Business-Logic-P0 before Feature-P0 (applies per new tier rule 2026-04-20).
4. Add audit-refresh schedule: quality-audit /100 next refresh by 2026-04-26 (weekly cadence per `post-wave-audit-mandate.md` §2.3).

Also update `documents/04-quality/gaps/ROADMAP.md` to include new GAP-190..201 rows in Epic 14 or appropriate epic, and list the 11 previously-unscheduled gaps under Wave 9.

**Phase 3 — Begin Wave 9 execution**
Start with highest-priority gap per `meta-gap-priority.md`: **GAP-192** (Business-Logic-P0 — Trial→Paid zero-downtime migration design).

Deliverables for GAP-192:
- 3-layer docs for the migration in `documents/01-business/kitehub/trial-to-paid-migration/` (rules.md / use-cases.md / api-contract.md)
- State machine design: trial_active → trial_ending → paid_migrating → paid_active (or trial_ending → trial_grace → archived)
- Acceptance criteria: zero-downtime requirement, handoff rollback, event outbox
- BRD reference doc in `documents/00-brd/` if not covered by GAP-150 skeletons

Stop and ask for user input after GAP-192 design is reviewed — do NOT auto-proceed to next gap.

### Workflow rules (mandatory)
- Every change via PR (no direct main push) — `CLAUDE.md §Wave Branch Strategy`
- Follow Superpowers: brainstorm → task breakdown → TDD → implementation → self-review
- Update ROADMAP for every gap filed (`.claude/rules/audit-to-gap-pipeline.md` §Step 5)
- Audit gate hook enforces freshness — `post-wave-audit-mandate.md`
- MCP-first with CLI fallback for GitHub ops
- No Co-Authored-By trailer in commits
- Docs-only PRs exempt from code audits (hook grants exception)
- Parallel agents allowed for Phase 1 gap filing if using worktree isolation (`feedback_parallel_agent_strategy.md`) — keep each agent to 2-3 skeleton gaps max
- Sequential required for Phase 2 (master plan edit) and Phase 3 (Wave 9 execution)

### Stop conditions
- After Phase 1 merged → pause, report progress
- After Phase 2 merged → pause, report progress
- After GAP-192 3-layer docs drafted → pause for user review

### Success criteria
- [ ] 12 new gap skeletons filed (GAP-190..201)
- [ ] Master plan updated — 11 unscheduled gaps + 12 new gaps all assigned waves
- [ ] ROADMAP.md reflects new totals (e.g. "X/179 gaps closed")
- [ ] GAP-192 design doc drafted and ready for review
- [ ] Wave 9 ready for parallel-agent execution of audit-followup cluster

### Communication
- Respond in Vietnamese per CLAUDE.md §Communication Language
- Code comments + commit messages in English
- Length: terse updates during work, one-line summary at end of each phase
