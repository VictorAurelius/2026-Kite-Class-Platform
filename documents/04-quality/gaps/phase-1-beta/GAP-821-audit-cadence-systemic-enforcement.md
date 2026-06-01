# GAP-821 — Audit-cadence systemic enforcement (META P1)

**Status:** 🟡 PARTIAL
**Priority:** P1 (META force-multiplier)
**Phase:** phase-1-beta
**Domain:** Meta/Governance
**Found:** 2026-06-01 (Wave meta-7 Bucket B audit)
**Last-Verified:** 2026-06-01

## Problem

`post-wave-audit-mandate.md` §2.2 mandates post-wave audit suite ≤3 days after merge. Wave meta-7 Bucket B audit surfaced 5/7 audit-cadence-related gaps (GAP-678/685/691/698/708 — flagged "Wave NN post-wave audit suite deadline"), and Wave meta-8 Bucket C detector run surfaces **76 stale-cadence waves** in `wave-history.jsonl` with no matching audit row in `audits-index.csv`.

Pattern: post-wave audit mandate is honored when wave coordinator remembers, ignored when context flushes. No automated enforcement → drift compounds wave-over-wave → audit suite becomes after-the-fact retrospective instead of within-cycle gate.

## Root Cause

1. **Mandate without detector** — `post-wave-audit-mandate.md` §3 enforcement = `audit-gate.py` hook on PR commit, but the hook checks file-pattern triggers per PR, NOT wave-level cadence. Wave closure can flip `status: complete` without ever triggering audit suite if subsequent PRs don't touch matching path patterns.
2. **Bookkeeping fragmentation** — wave closure recorded in `wave-history.jsonl` (skill data file), audit recorded in `audits-index.csv` (governance file). No cross-validator.
3. **Solo-dev context flush** — coordinator session that ships wave doesn't always reach audit-suite step before session-end / `/clear`.

## Proposed Fix

### Phase 1 (this PR — Wave meta-8 Bucket C)

- [x] Ship `scripts/check-post-wave-audit-cadence.sh` cross-validator:
  - Parse wave-history.jsonl, extract (wave, date) per entry
  - For each wave aged ≥7 days, scan `audits-index.csv` column `wave` for matching audit row
  - WARN per stale wave; HARD STOP optional flag
- [x] Self-test: 3 synthetic fixtures (fresh skip / compliant skip / stale WARN) — PASS
- [x] CI wire: `.github/workflows/quality-docs.yml` job `post-wave-audit-cadence` WARN-mode
- [x] Baseline: 76 stale-cadence waves surfaced — documented in commit body for retrospective

### Phase 2 (Wave meta-9 candidate)

- [ ] After 30-day grace through 2026-07-01, flip CI to `--hard-stop` mode
- [ ] Audit backfill batch: file follow-up gap for systematic audit-suite catch-up across 76 stale waves OR document domain-milestone deferral per `post-wave-audit-mandate.md` §2.4

## Acceptance Criteria

- [x] Detector script `scripts/check-post-wave-audit-cadence.sh` shipped + self-test PASS
- [x] CI job `post-wave-audit-cadence` wired in `.github/workflows/quality-docs.yml` WARN-mode
- [x] Real-data baseline run documents 76 stale-cadence wave findings
- [ ] HARD STOP flip post-30-day grace (Wave meta-9)
- [ ] Stale-cadence backlog triage: per-wave audit deferral classification (domain-milestone vs genuinely missed)

## Notes

Per `meta-gap-priority.md` §3 META P1 force-multiplier — detector fix 1 lần → mọi future wave subsequent auto-comply prospectively → eliminate cadence-drift class permanently.

Detector deliberately permissive at v1.0.0 (WARN-only, simple wave-name string match) per `incident-to-rule-pipeline.md` §3.1 tightened legitimate-deferral conditions — accommodates legacy entries without backfill cost. Tightening (domain-milestone awareness + audit category requirement) deferred Phase 2.

## Related

- Rule: `.claude/rules/post-wave-audit-mandate.md` §2.2 (3-day cadence)
- Rule: `.claude/rules/post-wave-audit-mandate.md` §2.4 (domain-milestone exception)
- Sister gaps (audit-cadence missed): GAP-678 / GAP-685 / GAP-691 / GAP-698 / GAP-708
- Audit source: `documents/04-quality/audits/meta/2026-06-01-wave-meta-7-bucket-b-p1-open-1.md`
- Sister rule: `meta-gap-priority.md` §3

## Log

- 2026-06-01 — Gap filed Wave meta-8 Bucket C. Phase 1 shipped: detector script + self-test + CI wire WARN-mode + 76-wave baseline. Phase 2 HARD STOP flip target Wave meta-9 after 30-day grace through 2026-07-01.
