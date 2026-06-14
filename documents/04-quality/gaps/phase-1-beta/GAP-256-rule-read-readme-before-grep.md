# GAP-256: No rule mandates AI to read README before grep/code-search (navigation discipline)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (Meta — context efficiency; conditional on GAP-255 landing first)
**Domain:** Governance / AI workflow / Skills
**Detected:** 2026-04-28 (user-flagged: "có rules là không greps mà đọc readme trước chưa?")
**Affects:** Every AI-driven task that searches the repo (which is most tasks)

## Problem

The repo has 43 READMEs structured per `docs-folder-structure.md` (every top-level folder gets one with directory map + file placement rules). They exist as a navigation index — read first, search precisely after.

But there is **no rule instructing AI to read README first** before `Grep` / `Glob` blind searches. State-check 2026-04-28 across:
- `.claude/rules/*.md` (14 files) — `docs-folder-structure.md` mandates README *structure*, not *AI behavior*
- `CLAUDE.md` (project + global) — no read-first directive
- `.claude/skills/**/SKILL.md` (27 files) — none reference README-first navigation
- Memory entries — none

Result: agents default to `Grep` / `Glob` against the whole repo when a 50-line README would have answered the question in one read. Token waste compounds across every task.

## Why Conditional on GAP-255

A "read README first" rule **assumes READMEs are accurate**. With 34/43 READMEs stale (2026-04-28 audit) and 3 critically wrong (60-day stale, version drift), enforcing read-first today would actively mislead the AI in those cases.

**Order of operations:**
1. Foundation PR rewrites 3 critical READMEs (this wave)
2. GAP-255 ships freshness CI (this wave or next)
3. GAP-256 (this gap) ships only after GAP-255 enforcement is active and READMEs hit a steady-state ≤30d staleness across the board.

Skipping the conditionality risks the rule becoming a liability — exactly the failure mode `incident-to-rule-pipeline.md` §3 warns against (premature rule = future incident).

## Proposed Fix

### Layer 1: New rule `.claude/rules/readme-first-navigation.md`

Specifies:
- When AI is asked to find / understand / locate something in a folder, **first read** that folder's `README.md` (if exists)
- Then `Grep` / `Glob` for precision lookup using terms learned from README
- Skip-rule: if folder has no README OR README is stale-flagged (per GAP-255 CI), proceed direct to search
- Override-rule: if user explicitly says "grep" or "search," respect their intent

### Layer 2: Skill update `.claude/skills/workflow/start-session/SKILL.md`

Already loads CLAUDE.md + ROADMAP. Extend session-init to also surface a **README inventory snapshot** (count fresh / stale / outdated, top 3 stale flagged for caution).

### Layer 3: PR template checkbox

Reviewer-facing line: "If this PR added/moved a folder, did you update the parent README's directory map?" Mirror to `output-review-mandate.md` §3 row.

### Self-test (per `incident-to-rule-pipeline.md` §2 Stage 4)

Synthetic scenario: "Find the AI Branding state machine code." Two paths:
- WITHOUT rule (baseline): grep for "state machine" → 80 hits across repo, 4 minutes filtering
- WITH rule (proposed): read `kitehub/kitehub-branding/README.md` first (when it exists) → 1 path identified, 30 seconds

PR description must quote the comparison. (For initial pilot, baseline can be qualitative.)

## Acceptance Criteria

- [ ] GAP-255 (freshness CI) merged FIRST and active for ≥7 days — verify by checking `audit-gate.py` reports / repo activity
- [ ] After GAP-255 active, file `.claude/rules/readme-first-navigation.md` per `rule-change-process.md` workflow (Version 1.0.0, Last-Reviewed, Reviewer-Approver)
- [ ] Rule includes §Enforcement section (links to PR template + skill update + freshness CI)
- [ ] `start-session` skill extended with README inventory snapshot
- [ ] `output-review-mandate.md` §3 gets new row "README navigation discipline" linked here
- [ ] PR template checkbox added (re. directory-map sync)
- [ ] Self-test demonstration in PR description

## Out-of-scope

- Per-skill instrumentation that measures README-read frequency (telemetry — separate gap)
- Localized READMEs (Vietnamese versions) — defer
- Forcing `cat README.md` invocation — rule expresses intent, doesn't micromanage tool sequence
- Retroactively annotating skills with "consult README X first" — incremental adoption

## Related

- GAP-255 (paired — must land first; freshness CI is precondition)
- `docs-folder-structure.md` — provides READMEs as the navigable index this rule consumes
- `incident-to-rule-pipeline.md` — this gap is itself a Stage 1→3 candidate (user flagged miss → classify → rule + enforcement parity)
- `output-review-mandate.md` §3 — eventually gets a "README navigation discipline" row
- `start-session` skill — natural surface for surfacing README inventory health

## Log


- 2026-06-14: phase re-triage — n/a→phase-1-beta (meta rule — AI read README before grep; process).
- **2026-04-28** Filed during ecosystem audit (Wave Meta-Gov 1, Phase 1E follow-up). User asked: "có rules là không greps mà đọc readme trước chưa?" — answer was no. Conditional on GAP-255 to avoid premature-rule incident pattern.
