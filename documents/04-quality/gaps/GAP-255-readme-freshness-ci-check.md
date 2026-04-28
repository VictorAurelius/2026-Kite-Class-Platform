# GAP-255: No CI check for README staleness — `Last Updated` dates rot silently

**Status:** 🟢 DONE 2026-04-28 — `scripts/check-readme-freshness.sh` + 5 self-test fixtures + CI job shipped
**Priority:** 🟡 P2 (Meta — supportive force-multiplier; prerequisite for GAP-256)
**Domain:** DevOps / CI / Documentation governance
**Detected:** 2026-04-28 (ecosystem audit — README sweep)
**Affects:** 43 README files across the repo

## Problem

Audit 2026-04-28 found 43 READMEs total: 9 FRESH, 34 STALE (mostly 11d old from a 2026-04-17 batch), 0 OUTDATED. **3 critical staleness issues** caught manually:
- Root `README.md` — Spring Boot version drift (3.5.13 stated → actual 3.5.14)
- `kiteclass/README.md` — `Last Updated: 2026-02-27` (60 days stale at audit), Spring Boot 3.5.11
- `kitehub/README.md` — `Last Updated: 2026-03-09` (50 days stale at audit), Spring Boot 3.5.11, service status frozen at "🚧 PR 4.X" while shipped

These were caught by manual audit, not CI. Memory rule + reviewer attention is the only enforcement layer.

State-check 2026-04-28: no `.github/workflows/*.yml` job inspects README age. No husky pre-commit checks `**Last Updated:**` lines. `script-quality.yml` lints `.sh`/`.py` only.

## Root Cause

`docs-folder-structure.md` mandates README structure but not freshness. `output-review-mandate.md` §3 has rows for code/UI/docs review processes but no row for "README freshness." The `audit-gate.py` hook fires on code patterns, not on doc-only patterns where the doc itself becomes the bug.

Result: a README touched once on 2026-02-27 silently misleads dev onboarding 60 days later. The fix is so cheap (touch a date line) that the absence of a forcing function is the actual bug.

## Proposed Fix

### Layer 1: `scripts/check-readme-freshness.sh`

Script that scans all `**/README.md` (excluding `node_modules/`, `target/`, `.git/`, `documents/07-archived/`) and:
- Greps `**Last Updated:** YYYY-MM-DD` line
- Compares to today's date
- WARN if >30 days, FAIL if >90 days
- Skips files explicitly opted out via `<!-- readme-freshness-exempt: <reason> -->` HTML comment

### Layer 2: New advisory job in `meta-quality.yml` (or `script-quality.yml`)

- Triggers on PR touching any `**/README.md`
- Runs the script
- Posts WARN as PR comment (non-blocking) for staleness 30–90 days
- BLOCKS merge for >90 days
- Reports with file:line evidence

### Layer 3 (optional): version-drift sub-check

When README mentions `Spring Boot X.Y.Z`, optionally validate against actual `pom.xml`. Defer if Layer 1+2 cover most pain.

### Self-test fixtures

Commit 2 fixture READMEs under `data/fixtures/`:
- `fresh.md` — `Last Updated: <today>` → PASS
- `stale-60d.md` — `Last Updated: <today − 60d>` → WARN
- `outdated-100d.md` — `Last Updated: <today − 100d>` → FAIL exit-1

## Acceptance Criteria

- [x] `scripts/check-readme-freshness.sh` exists, executable, shellcheck-clean (`-S error` + `-S warning` both 0 issues)
- [x] Script honors `<!-- readme-freshness-exempt -->` opt-out (verified via T4 fixture)
- [x] CI workflow job `readme-freshness` triggers on `**/README.md` + `**/_README*.md` + script + fixtures path changes
- [x] WARN at 30d / BLOCK at 90d behavior verified via 3 dynamic fixtures + 2 committed fixtures (T1–T5 PASS in `--self-test`)
- [x] Local full-repo scan = exit 0 baseline 4 PASS / 42 WARN / 0 FAIL across 46 READMEs
- [x] §3 matrix in `output-review-mandate.md` gets new row "README freshness" linking here (PATCH bump v1.1.1 → v1.1.2)

## Out-of-scope

- Auto-update bot (e.g., `actions/github-script` to auto-bump dates) — defer; manual touch + this CI is sufficient
- Multilingual README freshness (`README.vi.md`) — none exist now
- Validating internal cross-references (broken links) — separate gap if filed
- Refactoring CONTRIBUTING.md / CODE_OF_CONDUCT.md / etc. — only README scoped here

## Related

- GAP-256 (paired — read-first rule depends on freshness enforcement; otherwise stale README misleads AI navigation)
- `output-review-mandate.md` §3 — needs new row "README freshness"
- `incident-to-rule-pipeline.md` — this gap closes Stage 3 enforcement on `docs-folder-structure.md`
- `audit-to-gap-pipeline.md` Step 2.5 — README staleness is exactly the kind of doc-state mismatch §2.5 wants caught earlier
- GAP-249/250 (sister CI-gate gaps for rule frontmatter) — same pattern, different artifact
- GAP-101 (docs-folder README standardization — closed; this is its enforcement layer)

## Log

- **2026-04-28 (DONE):** `scripts/check-readme-freshness.sh` (~225 LOC, shellcheck-clean both severity levels) + 2 committed fixtures (`exempt.md` + `no-date.md`) + 3 dynamic-date fixtures (generated at `--self-test` runtime to avoid temporal drift). New CI job `readme-freshness` in `script-quality.yml` runs self-test + validates full repo on any `**/README.md` PR. Bug found & fixed during dev: original regex `^\*\*Last[ -]?Updated\*\*` required closing `**` immediately after "Updated", which failed for project's `**Last Updated:** YYYY-MM-DD` convention (colon inside bold) — relaxed to `Last[ -]?Updated` non-anchored. Local self-test 5/5 PASS; full-repo scan baseline 4 PASS / 42 WARN / 0 FAIL across 46 READMEs. AC 6/6 ✅; flipped to DONE per `gap-done-discipline.md` §2 (no banned phrases verified). Unblocks GAP-256 (read-first rule) — gating clock starts now, eligible to file after ≥7d active.
- **2026-04-28** Filed during ecosystem audit (Wave Meta-Gov 1, Phase 1E follow-up). Triggered by 3 critical README staleness findings. Bundled into foundation PR alongside the 3 fix-on-the-spot README rewrites + GAP-256.
