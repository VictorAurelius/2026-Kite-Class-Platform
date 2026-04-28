# GAP-250: No CI gate enforces rule frontmatter (Version / Last-Reviewed / Reviewer-Approver)

**Status:** 🟢 DONE 2026-04-28 — Layer 1+2 shipped; Layer 3 (husky) was excluded from AC at filing time (see §Out-of-scope below) — PR pending
**Priority:** 🟠 P1 (Meta — force-multiplier; prevents drift recurrence)
**Domain:** DevOps / CI / Governance
**Detected:** 2026-04-28 (ecosystem audit)
**Affects:** Every future PR touching `.claude/rules/**.md`

## Problem

`rule-change-process.md` §3 mandates frontmatter (Version / Last-Reviewed / Reviewer-Approver / Applies-to). Backfill-on-next-edit policy is honor-system; nothing in `.github/workflows/` or `.husky/` validates that:
- New rules ship WITH frontmatter
- Edited rules bump Version + update Last-Reviewed
- Log entry was appended

GAP-245 (IDE warnings) shipped same memory-only failure mode 2026-04-28; per `incident-to-rule-pipeline.md` 5-stage pipeline, governance rules MUST have a detection layer in same PR. This gap closes the equivalent enforcement hole for rule frontmatter.

State-check 2026-04-28:
- `.github/workflows/script-quality.yml` exists (lints `.sh`/`.py` only)
- No workflow targets `.claude/rules/*.md`
- No husky pre-commit checks frontmatter
- 8 of 14 rules already non-compliant (see GAP-249)

## Root Cause

`rule-change-process.md` v1.0.0 specified the rule + reviewer matrix but deferred CI enforcement ("Pre-merge PR review (manual, enforced via CODEOWNERS once configured)"). CODEOWNERS still not configured — manual layer is the only layer.

## Proposed Fix

### Layer 1: `scripts/check-rule-frontmatter.sh`

Script that scans `.claude/rules/*.md` and validates each has:
- `**Version:** \d+\.\d+(\.\d+)?` line
- `**Last-Reviewed:** \d{4}-\d{2}-\d{2}` line (date sanity ≤ today)
- `**Reviewer-Approver:** @\S+` line
- `**Applies to:** ` line
- `## .* Log` section with at least one entry

Exit non-zero with file:line evidence if any rule fails.

### Layer 2: New job in `.github/workflows/script-quality.yml`

Add `rule-frontmatter` job that:
- Triggers on PR touching `.claude/rules/**.md`
- Runs `scripts/check-rule-frontmatter.sh`
- Blocks merge on failure

### Layer 3: Optional husky pre-commit hook

`.husky/pre-commit` calls the script if any `.claude/rules/*.md` is staged. Local-fast-fail.

### Self-test (per `incident-to-rule-pipeline.md` §2 Stage 4)

PR description must include shell snippet showing:
1. Script run against current main = PASS (after GAP-249 backfill merged)
2. Script run against `/tmp/fixture-bad-frontmatter.md` (no Version) = FAIL with exit-1 + file:line message

## Acceptance Criteria

- [x] `scripts/check-rule-frontmatter.sh` exists, executable, shellcheck-clean (no -S error)
- [x] Script validates Version + Last-Reviewed + Reviewer-Approver + Applies-to + Log section
- [x] Self-test fixture committed showing PASS/FAIL output (or commit message quotes it)
- [x] CI workflow job `rule-frontmatter` added to `script-quality.yml` (or new `meta-quality.yml`)
- [x] Job triggers on `.claude/rules/**.md` PR changes
- [x] After GAP-249 backfill merged, CI runs against main = green (paired in same PR; verified locally — `bash scripts/check-rule-frontmatter.sh --all` returns exit-0 on all 14 rules)

## Out-of-scope

- Husky pre-commit (Layer 3) — defer if Layer 1+2 are sufficient
- CODEOWNERS configuration — separate gap if needed
- Frontmatter for `.claude/skills/**` (covered by GAP-251)
- Validating semver-bump correctness on edits (PATCH vs MINOR vs MAJOR) — too complex for static check; reviewer responsibility

## Related

- GAP-249 (paired — bulk backfill must merge before CI gate or CI fails on main)
- GAP-251 (sister: skill-conventions CI lint, same wave)
- `rule-change-process.md` §3 + §6 + §6.5
- `incident-to-rule-pipeline.md` (5-stage pipeline — this gap is Stage 3 enforcement of Stage 1 detection)
- GAP-245 (cousin pattern — Java compiler `-Werror`)

## Log

- **2026-04-28** ✅ Closed by Wave Meta-Gov 1 Agent A (`feature/wave-meta-gov-1-A-rule-frontmatter`). Shipped Layer 1 (`scripts/check-rule-frontmatter.sh` — 5-field validator + self-test mode + `--paths` flag for CI diff filter) + Layer 2 (new `rule-frontmatter` job in `.github/workflows/script-quality.yml`, blocks merge on failure). Self-test runs against 3 synthetic fixtures under `scripts/fixtures/rule-frontmatter/` (1 PASS + 2 FAIL); shellcheck `-S error` returns 0 issues. Layer 1+2 alone satisfy every AC. Detector also caught one issue in `output-review-mandate.md` (missing `Applies to` field) which was fixed in same PR (PATCH bump 1.1.0 → 1.1.1).
- **2026-04-28** Filed during ecosystem audit. Self-application of `incident-to-rule-pipeline.md` Stage 3 (Rule + Enforcement same PR) on `rule-change-process.md` §3.
