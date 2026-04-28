# GAP-253: Eval-fixtures pilot for `business-logic-audit` + `security-audit` skills

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (Meta — Anthropic 2026 best-practice; quality-floor for highest-stakes audits)
**Domain:** Governance / Skills / Quality
**Detected:** 2026-04-28 (ecosystem audit + external best-practice review)
**Affects:** `quality/business-logic-audit/` + `quality/security-audit/`; pattern propagates to 13 audit skills total

## Problem

**Anthropic 2026 best-practice update (post-Skills release Dec 2025):** "build evaluations BEFORE writing skill body; minimum 3 scenarios per skill". Currently only `gap-done-discipline.md` ships with 3-fixture self-test (synthetic GAP-997/998/999 scenarios under `data/eval-fixtures/`). 13+ quality audit skills — including the two highest-stakes ones (`business-logic-audit`, `security-audit`) — ship without eval fixtures.

Without fixtures:
- No regression check when audit skill body is edited
- No way to demonstrate the skill catches its target failure mode
- No reference for skill authors when extending checks

## Root Cause

Skills authored before the 2026 eval-first guidance landed; pre-existing audit skills relied on manual sampling, not synthetic fixtures. No convention requires fixtures, so they're omitted by default.

## Proposed Fix

Pilot 2 highest-stakes skills. Each gets 3 fixtures under `data/eval-fixtures/`:
- `good.md` (passes all audit checks)
- `bad-{specific-violation}.md` (fails on a specific check; filename names the check)
- `edge-{ambiguous-case}.md` (regression case from prior incident or known edge)

### Files (6 fixtures total)

**`quality/business-logic-audit/data/eval-fixtures/`:**
- `good.md` — domain rules.md + matching @Mapping + value validated end-to-end
- `bad-rule-not-implemented.md` — rules.md says X, controller does Y; audit MUST flag mismatch
- `edge-config-key-renamed.md` — rules.md uses old key, code uses new key, both mappings exist (regression case from a prior business-logic drift)

**`quality/security-audit/data/eval-fixtures/`:**
- `good.md` — pinned deps + no secret leak + auth on all endpoints
- `bad-secret-in-config.md` — `application.yml` has hardcoded API key; audit MUST flag
- `edge-transitive-cve.md` — direct dep clean but transitive (e.g. log4shell-style) flagged via SBOM (echoes `feedback_dependabot_pnpm_transitive.md`)

Each fixture file embeds:
- Header: `**Expected: PASS|FAIL**` + `**Check name:** <which audit check fires>`
- Body: minimal repro scaffold (~20-40 lines)
- Footer: `**Expected output snippet:**` + 2-3 line excerpt of audit report

### Convention update

Document the pattern in each piloted skill's SKILL.md (under `## Eval Fixtures` section):
- Where fixtures live: `data/eval-fixtures/`
- Naming: `good.md` / `bad-<check>.md` / `edge-<case>.md`
- How to run: invocation snippet for the audit script
- How to add a new fixture when extending the skill

### Optional CI WARN (advisory, not blocking)

`scripts/check-skill-conventions.sh` (GAP-251) optionally checks: any audit skill claiming /100 score should have `data/eval-fixtures/` folder with ≥3 files. Advisory WARN — not block — this PR.

## Acceptance Criteria

- [ ] 6 fixture files committed (3 per skill × 2 skills)
- [ ] Each fixture has expected-PASS/FAIL header + check name + expected output
- [ ] `business-logic-audit/SKILL.md` + `security-audit/SKILL.md` each have `## Eval Fixtures` section linking to fixture dir
- [ ] Pattern documented once in shared style (could be in `skill-conventions.md` follow-up)
- [ ] Optional WARN in `check-skill-conventions.sh` flagging audit skills missing fixtures (advisory only)

## Out-of-scope

- Retro-fitting fixtures to the other 11 audit skills (separate follow-up gap; this is the pilot)
- Automated runner that exercises every fixture as a smoke test in CI (deferred — fixtures are docs+scaffold this round)
- Updating `skill-conventions.md` to mandate fixtures globally (lift after pilot proves value)
- Real-world fixtures from prior incidents that need redaction (use synthetic scaffolds instead)

## Related

- GAP-251 (sister — adds optional WARN check)
- GAP-254 (sister — severity rubric, complementary 2026 best-practice)
- `gap-done-discipline.md` §6 (existing 3-fixture pattern is the prototype)
- `skill-conventions.md` §6 (file-system context engineering — where fixtures live)
- Anthropic 2026 Skills release notes (Dec 2025)

## Log

- **2026-04-28** Filed during ecosystem audit. Pilot scope (2 of 13 audit skills) keeps this gap shippable in 1 PR. Mirrors `gap-done-discipline.md` 3-fixture pattern.
