# GAP-356: Extend `audit-to-gap-pipeline.md` Step 2.5 state-check from gap-filing to wave-plan pre-flight

**Status:** 🟢 DONE 2026-05-05 — rule v1.2.0 + Rule 16 detector + 3-fixture self-test + `_TEMPLATE.md` + memory all shipped this PR per `rule-change-process.md` §6.5 Enforcement Parity Mandate. Self-test verifies detector flags Wave 18b3 incident symbols (`Incident.visibilityScope` + `BR-CHILD-PROTECT-005` + `Notification.audienceScope`). Wave 19 plan PR (separate, follow-on) is the first downstream PR that exercises detector against new wave-plan input.
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

- [x] `audit-to-gap-pipeline.md` v1.2.0 ships with §2.6 Wave-Plan Pre-Flight Protocol added
- [x] Detection mechanism wired in same PR as rule edit per `rule-change-process.md` §6.5 — Option A (`session-docs-check` Rule 16) chosen, doc-rules-matrix.md updated, `check-docs.sh` extended
- [x] Self-test on Wave 18b3 plan FAILS for the known-absent symbols — bad-symbol-no-evidence fixture mirrors incident with `Incident.visibilityScope` + `BR-CHILD-PROTECT-005` + `Notification.audienceScope`; runner reports FAIL as expected (6/6 fixtures pass)
- [x] Memory `feedback_wave_plan_state_check.md` saved + indexed in `MEMORY.md`
- [x] Cross-links updated — `feedback_wave_plan_through_pr.md` extended with state-check step; rule §2.6 references `_TEMPLATE.md` + `session-docs-check` Rule 16
- [x] Wave-plan template (`_TEMPLATE.md`) includes `## 4. State-Check Evidence` section with verdict legend
- [x] Recurrence list in `audit-to-gap-pipeline.md` updated to mark 5th entry + GAP-356 escalation outcome
- [x] ROADMAP.md `## 🎯 Current Status Snapshot` retro entry added (2026-05-05 marker)

## Out-of-scope (track separately if surfaces)

- Detection of *outdated* references (symbol exists today but will be deleted by another in-flight PR) — out-of-scope; current rule covers presence-only.
- Cross-PR coordination (wave plan A and wave plan B both reference symbol X being added by PR Y) — out-of-scope; assume single-wave-at-a-time per `feedback_parallel_agent_strategy.md` rule #9.
- Bare-class-name references (`` `Notification` `` without `.field`) — current detector requires `Class.field` or `Class.METHOD` shape to avoid false positives on prose. Notification entity in Wave 18b3 plan was caught via the paired `Notification.audienceScope` reference; pure bare-class detection would need NLP heuristics.
- Wave 19 K-12 LEGAL Phase 1C plan PR (separate, follow-on) is the first downstream consumer of new template + detector — verification that the template is usable in real-world flow, not part of GAP-356 AC.

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

## Log

- **2026-05-05** (DONE) Closed in stacked PR 2 (this PR). Rule v1.2.0 §2.6 + `session-docs-check` Rule 16 detector (`scripts/check-docs.sh`) + 3-fixture self-test + `_TEMPLATE.md` + memory `feedback_wave_plan_state_check.md` all shipped same PR per `rule-change-process.md` §6.5 Enforcement Parity Mandate. **Verification artifact:** runner output `bash .claude/skills/workflow/session-docs-check/test/run-rules.sh` → 6/6 fixtures pass; bad-symbol-no-evidence fixture (mirroring Wave 18b3 incident with `Incident.visibilityScope` + `BR-CHILD-PROTECT-005` + `Notification.audienceScope` — 5th-recurrence symbols) correctly reports `[FAIL] Rule 16 — wave-2026-99-02-fixture-bad: 3 symbol(s) in §Scope; missing State-Check Evidence row for: BR-CHILD-PROTECT-005, Incident.visibilityScope, Notification.audienceScope`. Detector fires as designed on the original incident.
- **2026-05-05** (filing) Filed by gap-triage session per self-mandate of `audit-to-gap-pipeline.md` v1.1.0 5th-recurrence escalation clause. Filed in PR 1 (#787); fix shipped stacked PR 2. Sprint assignment: Meta-P0 ahead of Wave 19 K-12 LEGAL Phase 1C per `meta-gap-priority.md` §3 (Meta tier > Feature-LEGAL tier at same P-level).
