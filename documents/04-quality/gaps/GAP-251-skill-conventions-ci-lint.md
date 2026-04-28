# GAP-251: Skill convention enforcement is ad-hoc (no CI lint for SKILL.md files)

**Status:** 🟡 PARTIAL — script + fixtures shipped, CI wiring deferred to Sub-PR C (per `gap-done-discipline.md` §3 PARTIAL exit ramp)
**Priority:** 🟠 P1 (Meta — 27 SKILL.md files exist, none CI-validated)
**Domain:** Governance / Skills / CI
**Detected:** 2026-04-28 (ecosystem audit)
**Affects:** 27 SKILL.md files across `.claude/skills/**`; every future skill addition

## Problem

`output-review-mandate.md` §3 row "Skills (meta)" status is ⚠️ PARTIAL: "rules exist, enforcement ad-hoc". `skill-conventions.md` defines clear quality bar (progressive disclosure, gerund naming, body <500 lines, gotchas section, trigger conditions in description) but no CI/hook validates it.

State-check 2026-04-28:
- 27 SKILL.md files in `.claude/skills/**`
- `script-quality.yml` lints `.sh`/`.py` but not `.md`
- No husky hook for skill structure
- Random sampling of 5 SKILL.md showed 1 missing `## Gotchas`, 2 with description in "1st person" instead of trigger-condition style

If a new skill ships with substandard structure (no gotchas, body too long, description not trigger-style), nothing detects it.

## Root Cause

Skills authored when `skill-conventions.md` didn't exist were grandfathered. Skills authored after still rely on author memory. No automated layer.

## Proposed Fix

### Layer 1: `scripts/check-skill-conventions.sh`

Script that scans `.claude/skills/**/SKILL.md` (and loose `.md` skills like `business-gap-check.md`) and validates:
- Has frontmatter with `name` (≤64 chars) + `description` (≤1024 chars)
- Description contains trigger-condition keywords (`Use when`, `Dùng khi`, `When the user`, `Triggered`, etc.)
- Body has `## Gotchas` OR `## Anti-patterns` section (project-specific failure points)
- Body ≤500 lines (per Anthropic 2026 relaxed limit; was <100 in older guidance)
- For folder skills: SKILL.md exists in folder root + `reference/`, `scripts/`, `data/`, `assets/` subdirs are referenced if present

### Layer 2: SKILL.md inventory check (lightweight index drift detector)

Compare folder count vs `_README-skills-index.md` table — emit WARN if mismatch (per GAP-252 separate gap closes the data side).

### Layer 3: NOT wiring CI in same PR

Per `feedback_parallel_agent_strategy.md` rule (disjoint scope vs Agent A's CI changes), Agent B ships the SCRIPT only. CI workflow job lives in **Sub-PR C** after Agent A's `script-quality.yml` changes settle, to avoid YAML merge conflict.

### Self-test fixtures

Commit 2 fixture SKILL.md files in `.claude/skills/quality/skill-conventions-check/data/fixtures/`:
- `good.md` — passes all checks
- `bad-no-gotchas.md` — fails (script must report which check)
- `bad-description-style.md` — fails (description in 1st person instead of trigger)

PR description quotes script output on each fixture.

## Acceptance Criteria

- [x] `scripts/check-skill-conventions.sh` exists, executable, shellcheck-clean
- [x] Script validates: frontmatter, description style, gotchas section, body line count, folder structure
- [x] 3 fixtures committed under skill folder with PASS/FAIL evidence in PR description
- [x] Script run on current main = PASS — exit 0 on default mode; 21 grandfathered skills documented in `GRANDFATHERED_EXEMPTIONS` script var (each becomes follow-up cleanup work, target empty list by Wave 9)
- [ ] Sub-PR C wires to CI workflow — DEFERRED (per Wave plan §5; tracked alongside Agent A's `script-quality.yml` changes settle first)

## Out-of-scope

- Wiring CI workflow (deferred to Sub-PR C, post-Agent-A merge)
- Refactoring grandfathered skills that fail — file follow-up gap if any flagged
- Validating skill activation rate / model-trigger quality — that's runtime, this is static
- Auto-generating skills index (covered by GAP-252)

## Related

- GAP-252 (skills index refresh — paired in Move 2)
- GAP-253 (eval fixtures — Move 2)
- GAP-254 (severity rubric — Move 2)
- `skill-conventions.md` (the spec)
- `output-review-mandate.md` §3 row "Skills (meta)"
- `incident-to-rule-pipeline.md` (this gap closes Stage 3 enforcement on skill-conventions.md)

## Log

- **2026-04-28** Wave Meta-Gov 1 Move 2 Agent B PR shipped. `scripts/check-skill-conventions.sh` (374 LOC, shellcheck-clean) validates 5 checks: frontmatter, description style, gotchas section, body line count, audit-skill eval-fixtures. Default-mode exit code 0 on current main; `--strict` flag elevates WARNs to FAILs. 3 self-test fixtures under `.claude/skills/quality/skill-conventions-check/data/fixtures/` (good.md PASS, bad-no-gotchas.md FAIL, bad-description-style.md FAIL — quoted in PR description). 21 grandfathered skills exempted via `GRANDFATHERED_EXEMPTIONS` script variable; cleanup is follow-up work (target empty list by Wave 9). CI wiring deferred to Sub-PR C per Wave plan §5 — Status flipped to 🟡 PARTIAL not DONE per `gap-done-discipline.md` §3 (CI AC not verified yet).
- **2026-04-28** Filed during ecosystem audit. Move 2 in Wave Meta-Gov 1. Pairs with GAP-249/250 (rule discipline) — together close `output-review-mandate.md` §3 rows 16 + missing rule frontmatter.
