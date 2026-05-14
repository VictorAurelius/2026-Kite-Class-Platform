# Agent Background-Spawn Default

**Priority:** 🟠 MANDATORY — agent invocation pattern
**Version:** 1.0.0
**Created:** 2026-04-29
**Last-Reviewed:** 2026-05-14
**Reviewer-Approver:** @nguyenvankiet (starter-kit upstream maintainer)
**Applies to:** Every `Agent` tool invocation (subagent spawn) by Claude in this project — Explore, general-purpose, Plan, statusline-setup, or any future subagent type

---

## 1. The Rule

> **Always spawn agents with `run_in_background: true`** unless one of the documented exceptions in §3 applies.

When using the `Agent` tool, the default invocation pattern is:

```
Agent(
  description: "...",
  subagent_type: "...",
  isolation: "worktree",  // when applicable
  run_in_background: true,  // ← MANDATORY default
  prompt: "..."
)
```

---

## 2. Why background-by-default

| Benefit | Foreground (blocking) | Background (non-blocking) |
|---------|----------------------|---------------------------|
| Parent context cost | Full agent transcript loaded into parent | Only summary on completion notification |
| Parent throughput | Blocked until agent done (15-25 min idle) | Free to coordinate, prep next steps, answer user |
| Parallel execution | Cannot truly parallelize even with multiple Agent calls in same message | True parallelism — N agents run concurrently |
| Cache pressure | Parent's prompt cache may invalidate during long agent run | Parent stays warm with smaller per-turn context |
| User experience | Long silence between user messages | User sees "agent launched, will notify" + parent stays responsive |

For wave-pack methodology (4-5 parallel agents per wave per `feedback_parallel_agent_strategy.md` rule #9), foreground spawning would mean serial execution masquerading as parallel — defeats the purpose.

---

## 3. Allowed exceptions (use foreground sparingly)

Use `run_in_background: false` (or omit, since false is the implicit default) ONLY when one of these applies:

| Case | Why exception | Example |
|------|--------------|---------|
| **Exploration → immediate next step** | Parent's next action depends DIRECTLY on agent's findings + cannot proceed without them | "Find which file imports X" → next tool call uses that file |
| **Single short query (<2 min wall)** | Cost of background coordination > foreground wait | "What's the version in package.json?" |
| **Plan agent producing immediate prompt** | Parent feeds plan output into next message verbatim | `subagent_type=Plan` with synchronous handoff |
| **Critical-path verification before next decision** | User explicitly waiting for green/red answer to proceed | "Verify the migration applies — I'll wait" |
| **statusline-setup or setup-only agents** | Agent makes one config change, parent has no other work to do | Status-line config |

If unsure: **default to background**. The cost of an unnecessary background spawn is one extra completion notification message; the cost of an unnecessary foreground spawn is 15+ minutes of blocked parent context.

---

## 4. Anti-patterns

| ❌ Don't | ✅ Do |
|---------|------|
| Spawn 4 wave-pack agents in foreground "to wait for them" | `run_in_background: true` for all 4; coordinate on completion notifications |
| Foreground because "it's just one Explore" | Even single Explore agents block ~5-10min — background unless next step depends on result |
| Mix foreground + background in same wave | Pick one mode per wave; consistency simplifies coordinator state |
| Use foreground because "user wants me to focus on this" | User does not benefit from blocked context; user benefits from parent staying responsive |
| Spawn background then immediately poll for status | Wait for completion notification — runtime delivers it. No `sleep` loops, no `tail -f` on transcripts |

---

## 5. Self-test

Worked example (Wave UI Kits Round 3, 2026-04-29):

- 4 agents spawned in single message with `run_in_background: true` (Agent A your-product-b-student / Agent B your-service-admin / Agent C components batch 1 / Agent D components batch 2)
- Wall-clock: ~21 min (longest agent) vs ~80 min serial estimated
- Parent stayed responsive throughout — answered user questions, prepped closure PR template, maintained todo list
- Each agent completion notification arrived independently; coordinator merged sequentially A→B→C→D after all 4 done
- 26th consecutive 0-clarification streak

If parent had spawned all 4 in foreground, parent would have been blocked ~21 min × 4 = ~84 min serial blocking, even with `Agent` calls in same message (each foreground call serializes by harness design).

---

## 6. Enforcement

### 6.1 Reviewer-checklist (manual)

When reviewing a PR that includes agent-spawn patterns (in skill files, agent prompt templates, wave-coordinator examples), check:

- [ ] All `Agent` tool invocations use `run_in_background: true`
- [ ] Foreground exception (if any) is documented with reason matching §3 list
- [ ] Wave-pack waves follow the rule for ALL bucket agents (no mixed mode)

### 6.2 Memory-as-enforcement (auto-loaded per session)

Memory entry `feedback_parallel_agent_strategy.md` rule #11 added same PR (this rule's enforcement parity per `rule-change-process.md` §6.5 Enforcement Parity Mandate). Memory is auto-loaded at session start, so Claude sees the rule every session without explicit reference.

### 6.3 Wave-pack-planner skill reference

`.claude/skills/quality/wave-pack-planner/reference/agent-spawning-template.md` — agent-spawn examples in this project's templates use `run_in_background: true` (verified post-merge of this rule).

### 6.4 CLAUDE.md no-touch decision

CLAUDE.md NOT updated this PR — this rule is granular workflow guidance, fits better in `.claude/rules/` index than CLAUDE.md tool-pattern section. CLAUDE.md auto-loads from project root; rules folder is browsed when relevant. Memory entry covers per-session reminder.

### 6.5 Override mechanism

If a genuine foreground exception is needed but doesn't fit §3 categories, document inline:

```
// AGENT_FOREGROUND_OVERRIDE: <reason — explain why §3 categories don't fit>
Agent(... run_in_background: false, ...)
```

Rare case; rate higher than ~5% of agent spawns triggers meta-review of §3 categories.

---

## 7. Anti-edge-cases (clarifications)

- **`Explore` agents** for keyword search: STILL background unless next step is unblocked-by-finding (per §3 row 1).
- **Multiple Agent calls in same message** with both background flags: harness handles concurrency correctly. No need to "stagger" or sequence.
- **Sub-agent calling sub-agent**: same rule applies recursively. Nested agents default to background.
- **`isolation: worktree` + `run_in_background: true`** combination: this is the standard wave-pack pattern. Both flags compose cleanly.

---

## 8. Relationship to other rules

- **`feedback_parallel_agent_strategy.md`** (memory) — rule #11 added same PR. This rule formalizes what was a working assumption.
- **`feedback_wave_plan_through_pr.md`** (memory) — wave plan PR-first; this rule operates AFTER plan merges (during agent spawn).
- **`feedback_parallel_agent_strategy.md`** rule #9 — max 5 concurrent agents per wave. This rule complements: 5 max × always background = max parallelism without parent blocking.
- **`feedback_worktree_absolute_path_contamination.md`** — RELATIVE paths in agent prompts. Orthogonal concern; both apply.
- **`gap-done-discipline.md`** — agent PRs that flip gaps to DONE follow §2 criteria. Orthogonal: this rule is about HOW to spawn, that rule is about HOW to close.

---

## 9. Log

- **2026-04-29 (v1.0.0):** Rule created at user request "thêm rules agent luôn được spawn ở dạng background task" immediately following Wave UI Kits Round 3 SHIPPED (which used 4 background agents — worked example). Per `incident-to-rule-pipeline.md` 5-stage: Detect ✓ (user flagged the missing default) → Classify ✓ (no existing rule covers; memory `feedback_parallel_agent_strategy.md` 10 rules don't mention foreground/background) → Rule+Enforce ✓ (this file + memory rule #11 paired same PR per `rule-change-process.md` §6.5) → Self-test ✓ (Wave UI Kits R3 4-agent worked example, ~21 min vs ~84 min foreground serial) → Retro Log ✓ (this entry). Solo-dev MINOR self-approve per `rule-change-process.md` §5 — new constraint, no constraint loosening.
