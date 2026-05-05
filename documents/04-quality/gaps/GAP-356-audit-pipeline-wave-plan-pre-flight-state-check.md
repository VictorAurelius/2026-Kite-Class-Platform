# GAP-356: Extend `audit-to-gap-pipeline.md` Step 2.5 state-check from gap-filing to wave-plan pre-flight

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 Meta (rule itself failing despite existing documentation — `meta-gap-priority.md` §3 force-multiplier)
**Domain:** Governance / Meta-rule
**Found:** 2026-05-04 (Wave 18b3 Bucket C — 5th GAP-190/197 head-truncation recurrence)
**Affects:** Every wave plan PR + every gap-filing — both currently relying on Step 2.5 protocol that only covers gap-filing
**Self-mandated by:** `audit-to-gap-pipeline.md` Step 2.5 protocol last paragraph: *"If 5th recurrence detected, escalate to file gap on `audit-to-gap-pipeline.md` itself — rule is failing despite documentation."*

## Problem

`audit-to-gap-pipeline.md` Step 2.5 (state-check against current codebase) was hardened 2026-05-04 (v1.1.0) after the 4th GAP-190/197 head-truncation recurrence. The hardened protocol bans `| head` truncation and mandates multi-grep cross-checks before filing a GAP.

**The 5th recurrence (Wave 18b3, 2026-05-04)** revealed the protocol's blind spot: Step 2.5 covers **gap-filing**, but NOT **wave-plan drafting**. Wave plans are treated as ground-truth by spawned agents — agents read the plan §3 Scope, take the referenced entities/rules/fields/migrations as given, and execute. When the plan itself references absent symbols, agents discover the mismatch only at execution time.

### Concrete 5th-recurrence detail (Wave 18b3 Bucket C)

Wave plan `wave-2026-05-04-18b3-k12-legal-phase-1b-remainder.md` §3 Bucket C scope referenced 3 symbols that did NOT exist in the codebase:

| Symbol referenced in plan | Code state at plan-merge time | Recovery cost |
|---|---|---|
| `Incident.visibilityScope` enum | 0 matches in `kiteclass-core/.../childprotection/` | Bucket C agent state-checked at execution → flagged → scope split to GAP-321b-1-conduct |
| `BR-CHILD-PROTECT-005` business rule | 0 matches in `documents/01-business/kiteclass/child-protection/rules.md` | Same agent escalation |
| `Notification` entity for parent-audience scope | 0 matches anywhere; engine itself is GAP-063b (open) | Hard-blocked → filed GAP-321b-1-notifications + flagged GAP-063b dependency |

Cost compounded because the wave plan was MERGED before agents spawned (per `feedback_wave_plan_through_pr.md`), so the plan was authoritative documentation at agent-spawn time. Agents had to do reverse-state-checking during execution.

## Root Cause

Step 2.5 v1.1.0 protocol applies the state-check at gap-filing time ONLY:

> "Step 2.5: State-Check Against Current Codebase (BẮT BUỘC trước tạo gap)"

It does NOT apply at wave-plan drafting time. Wave plans are themselves a higher-leverage artifact:
- 1 gap = 1 unit of work, ~1-3 days
- 1 wave plan = 3-5 buckets × ~1-3 days each = 3-15× gap-leverage

A wave plan referencing absent symbols cascades: bucket A may depend on bucket B's contract, bucket B's contract may depend on plan-§3 symbol, plan-§3 symbol absent → bucket A blocked, bucket B blocked, bucket C blocked. The wave-pack 5x speedup observed in Waves 18a-18b3 evaporates if plan accuracy is lower than gap accuracy.

## Current State (verified 2026-05-05)

| Artifact | State |
|---|---|
| `audit-to-gap-pipeline.md` v1.1.0 Step 2.5 | ✅ exists, covers gap-filing |
| Same file Step 2.5 wave-plan-pre-flight clause | ❌ missing |
| Recurrence-tracking inline list (4 entries 2026-04-20 → 2026-05-04) | ✅ tracked |
| 5th recurrence escalation (this gap) | 🔵 this file = the escalation |
| `session-docs-check` Rule for wave-plan symbol verification | ❌ missing (Rule 13 covers gap closure; Rule 14 was deferred per `incident-to-rule-pipeline.md`) |
| `scripts/check-docs.sh` branch covering wave-plan diff | ❌ missing |
| Wave-plan template includes "state-check evidence" section | ❌ missing |

## Proposed Fix (separate PR — per `audit-to-gap-pipeline.md` "no fix in audit-discovery PR" rule)

Per `incident-to-rule-pipeline.md` 5-stage + `rule-change-process.md` §6.5 Enforcement Parity Mandate, the fix PR ships **rule + detection + self-test + memory** in same PR:

### 1. Rule extension (`audit-to-gap-pipeline.md` v1.2.0 — MINOR per `rule-change-process.md` §4)

Extend Step 2.5 to cover two trigger points:
- (existing) **Pre-gap-filing state-check** — unchanged
- (NEW) **Pre-wave-plan-merge state-check** — every entity/rule/field/migration referenced in §3 Scope of a wave plan PR must be grep-verified before merge

Add `## Wave-Plan Pre-Flight Protocol` subsection mirroring the gap-filing protocol:
- Required commands per symbol type (entity / rule / field / migration / DTO / API endpoint)
- Required output: inline `## State-Check Evidence` section in the wave plan with grep counts
- Banned shortcuts: `| head` truncation (already banned for gap-filing), unverified "future-proof" references

### 2. Detection mechanism (per §6.5 Enforcement Parity)

Option A (RECOMMENDED): extend `session-docs-check` skill with a new rule that fires when a PR diff includes a new file under `documents/03-planning/waves/wave-*.md` and verifies every code-symbol-shaped reference (`ClassName.field`, `BR-XXX-NNN`, `V\d+__`, `entity_name` table) in the diff has ≥1 grep match in the repo. WARN by default, BLOCK in `--strict` mode.

Option B: standalone `scripts/check-wave-plan-state.sh` invoked from PR template checkbox.

Choice A preferred for consistency with `session-docs-check` Rules 13/15 already governing related artifacts.

### 3. Self-test fixture (per `incident-to-rule-pipeline.md` Stage 4)

Run new detector against `wave-2026-05-04-18b3-k12-legal-phase-1b-remainder.md` retroactively → expected output: 3 FAIL lines for `Incident.visibilityScope`, `BR-CHILD-PROTECT-005`, `Notification`. PR description quotes the FAIL output as proof rule fires on the original 5th-recurrence incident.

### 4. Wave-plan template (`documents/03-planning/waves/_TEMPLATE.md` if exists, else create)

Add `## State-Check Evidence` section with table:
| Referenced symbol | Grep command | Match count | Verdict |

### 5. Cross-link updates

- `audit-to-gap-pipeline.md` §9 Related — add wave-plan rule cross-link
- `feedback_parallel_agent_strategy.md` (memory) — add rule "wave plans must include state-check evidence section"
- `feedback_wave_plan_through_pr.md` (memory) — extend with pre-merge state-check requirement

### 6. Memory entry

`feedback_wave_plan_state_check.md` — describe the 5th-recurrence miss, why prior protocol missed it, which rule extension now prevents it.

## Acceptance Criteria

- [ ] `audit-to-gap-pipeline.md` v1.2.0 ships with Step 2.5 extended to wave-plan pre-flight
- [ ] Detection mechanism wired (Option A or B) in same PR as rule edit per `rule-change-process.md` §6.5
- [ ] Self-test on Wave 18b3 plan FAILS for the 3 known-absent symbols (output quoted in PR description)
- [ ] Memory `feedback_wave_plan_state_check.md` saved + indexed in `MEMORY.md`
- [ ] Cross-links updated in 3 related rules / memory entries
- [ ] Wave-plan template (`_TEMPLATE.md`) includes State-Check Evidence section
- [ ] Recurrence list in `audit-to-gap-pipeline.md` updated to mark 5th + this gap as the escalation outcome
- [ ] ROADMAP.md `## 🎯 Current Status Snapshot` retro entry per `incident-to-rule-pipeline.md` Stage 5
- [ ] Future Wave 19 (K-12 LEGAL Phase 1C) plan PR uses the new template + passes the new detector

## Related

- `audit-to-gap-pipeline.md` v1.1.0 — the rule being extended (self-mandated escalation)
- `rule-change-process.md` §6.5 — Enforcement Parity Mandate (rule + detection same PR)
- `incident-to-rule-pipeline.md` — 5-stage process governing this gap
- `meta-gap-priority.md` §3 — Meta-P0 (rule failure trumps feature work)
- `feedback_wave_plan_through_pr.md` — wave plan PR-first (this gap extends with state-check)
- `feedback_parallel_agent_strategy.md` — wave-pack methodology depends on plan accuracy
- `gap-done-discipline.md` — sibling rule with its own Rule 13 detector (precedent for Option A)
- Wave 18b3 plan: `documents/03-planning/waves/wave-2026-05-04-18b3-k12-legal-phase-1b-remainder.md` (the fixture)
- 5 prior recurrences inline-tracked in `audit-to-gap-pipeline.md` §6 Log

## Out-of-scope (track separately if surfaces)

- Detection of *outdated* references (symbol exists today but will be deleted by another in-flight PR) — out-of-scope; current gap covers presence-only.
- Cross-PR coordination (wave plan A and wave plan B both reference symbol X being added by PR Y) — out-of-scope; assume single-wave-at-a-time per `feedback_parallel_agent_strategy.md` rule #9.

## Log

- **2026-05-05** Filed by gap-triage session per self-mandate of `audit-to-gap-pipeline.md` v1.1.0 5th-recurrence escalation clause. Filed standalone (PR 1); fix PR (PR 2 — rule v1.2.0 + detector + self-test + memory) tracked separately per Step 6 "no fix in discovery PR" rule. Sprint assignment: Meta-P0 ahead of Wave 19 K-12 LEGAL Phase 1C per `meta-gap-priority.md` §3 (Meta tier > Feature-LEGAL tier at same P-level).
