# GAP-255: No CI check for README staleness — `Last Updated` dates rot silently

**Status:** 🔵 OPEN
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

- [ ] `scripts/check-readme-freshness.sh` exists, executable, shellcheck-clean
- [ ] Script honors `<!-- readme-freshness-exempt -->` opt-out
- [ ] CI workflow job triggers on `**/README.md` PR changes
- [ ] WARN at 30d / BLOCK at 90d behavior verified via 3 fixtures (PR description quotes output)
- [ ] After foundation PR merge (with 3 fresh READMEs), CI job runs against main = PASS
- [ ] §3 matrix in `output-review-mandate.md` gets new row "README freshness" with status linking here

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

- **2026-04-28** Filed during ecosystem audit (Wave Meta-Gov 1, Phase 1E follow-up). Triggered by 3 critical README staleness findings. Bundled into foundation PR alongside the 3 fix-on-the-spot README rewrites + GAP-256.
