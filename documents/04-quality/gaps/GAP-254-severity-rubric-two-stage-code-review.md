# GAP-254: Add 5-tier severity rubric (Blocker → Praise) to `two-stage-code-review`

**Status:** 🟢 DONE 2026-04-28 — `## Severity Rubric` section + `## Log` shipped in `core/two-stage-code-review.md`
**Priority:** 🟡 P2 (Meta — review-quality lift; reduces deficit-bias per 2026 community standard)
**Domain:** Governance / Skills / Code Review
**Detected:** 2026-04-28 (ecosystem audit + external research)
**Affects:** `core/two-stage-code-review.md`; cascades optionally to 13 audit skills emitting /100

## Problem

External research (top community skill repos) found a **converged severity rubric** in:
- `awesome-skills/code-review-skill`
- `sennaBruno/claude-skills/quality-gate`

Both ship a **5-tier severity label set** instead of (or alongside) numeric /100:

| Tier | Meaning |
|------|---------|
| **Blocker** | Must fix before merge — correctness / security / data loss risk |
| **Critical** | Must address — quality regression, contract breach, severe smell |
| **Warning** | Should address — anti-pattern, missing test, perf concern |
| **Info** | Nice to know — style, minor refactor opportunity |
| **Praise** | Explicit recognition of good work — novel pattern, useful test, well-documented decision |

The **Praise** tier is novel. Empirical claim from 2026 reviewer-fatigue research: explicit positive labels reduce the deficit-focused bias in code review (where reviewers cumulatively wear down on long PRs). Costs nothing and balances tone.

Currently `core/two-stage-code-review.md` is deficit-focused: it scans for issues without a positive-recognition step. Consequence — long PRs accumulate criticism with no balancing signal.

## Root Cause

`two-stage-code-review.md` was authored before the 5-tier rubric crystallized in the community. No prior PR proposed adding it because the gap surfaces only in cross-repo comparison.

## Proposed Fix

### Add `## Severity Rubric` section to `core/two-stage-code-review.md`

~80 lines added max. Content:

1. The 5-tier table (Blocker / Critical / Warning / Info / Praise) with definitions
2. Examples for each tier in this codebase (1-2 lines each):
   - Blocker example: "Outbox bypassed without §3.5.1 exception A/B/C/D marker"
   - Critical example: "@Cacheable on @Service without invalidation path"
   - Warning example: "Missing unit test for new public method"
   - Info example: "Variable name could be clearer"
   - Praise example: "Novel use of XState machine for complex wizard flow"
3. When to use each tier (rule of thumb: if you can articulate a Praise tier and don't, you're being deficit-biased)
4. Cross-link references: `awesome-skills/code-review-skill`, `sennaBruno/claude-skills/quality-gate`

### Cascading change: **NOT this PR**

Audit skills emitting /100 (business-logic, security, performance, ops-readiness, etc.) MAY OPTIONALLY map findings to severity tiers in their reports. **Not mandated this PR** — a 13-skill cascade is too big. Flag for follow-up gap (e.g. GAP-255+) if user wants to lift across skills.

### Versioning

`two-stage-code-review.md` is a skill, not a rule, so `rule-change-process.md` semver doesn't directly apply. Use a simple PATCH-style bump in the skill body — add date+summary to a `## Log` section at the bottom (create if missing).

## Acceptance Criteria

- [x] `core/two-stage-code-review.md` has `## Severity Rubric` section with 5 tiers (🛑 Blocker / 🔴 Critical / 🟡 Warning / ℹ️ Info / 🌟 Praise) + per-tier examples + when-to-use guidance
- [x] ≤80 lines added — actual: 31 lines added (98 → 129 LOC)
- [x] Cross-links to `awesome-skills/code-review-skill` and `sennaBruno/claude-skills/quality-gate` present
- [x] `## Log` section appended at file bottom with 2026-04-28 entry citing GAP-254
- [x] No constraint on existing review process — rubric explicitly described as "additive, not replacement" in the new section

## Out-of-scope

- Cascading rubric to 13 audit skills (file as GAP-255 or similar follow-up if needed)
- Changing PR template severity labels (separate gap; PR template lives in `.github/`)
- Replacing /100 numeric scores with tiers (keep both; tiers describe individual findings, /100 describes overall)
- Auto-applying rubric labels via tooling (manual reviewer action this round)

## Related

- GAP-253 (sister — eval fixtures, complementary 2026 best-practice)
- `core/two-stage-code-review.md` (the target)
- External: `awesome-skills/code-review-skill`, `sennaBruno/claude-skills/quality-gate`
- `rule-change-process.md` §4 (semver intent — skill bump style is informal but parallel)
- `output-review-mandate.md` §3 row "Code"

## Log

- **2026-04-28** Wave Meta-Gov 1 Move 2 Agent B PR shipped. `## Severity Rubric` section appended to `core/two-stage-code-review.md` (31 LOC added, well under 80-line cap). 5 tiers — 🛑 Blocker / 🔴 Critical / 🟡 Warning / ℹ️ Info / 🌟 Praise — each with project-specific example. Praise tier is the novel addition (e.g., "Novel use of XState machine for complex wizard flow"). Section explicitly notes additive-not-replacement to keep existing 🔴/🟠/🟡 GRADED buckets primary. Cascade to 13 audit skills emitting /100 explicitly listed under gap §Out-of-scope (separate follow-up gap if user wants global lift).
- **2026-04-28** Filed during ecosystem audit + external research. Praise tier is the novel addition; rest are codification of community standard.
