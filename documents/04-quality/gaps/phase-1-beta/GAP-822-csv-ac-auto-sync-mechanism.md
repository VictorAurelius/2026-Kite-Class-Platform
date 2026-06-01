# GAP-822 — CSV ↔ AC auto-sync mechanism (META P1)

**Status:** 🟡 PARTIAL
**Priority:** P1 (META force-multiplier)
**Phase:** phase-1-beta
**Domain:** Meta/Governance
**Found:** 2026-06-01 (Wave meta-7 Bucket D audit)
**Last-Verified:** 2026-06-01

## Problem

Wave meta-7 Bucket D audit (54 P1 PARTIAL gaps) surfaced systemic pattern: gap files có Status field "🔵 OPEN" + AC `- [ ]` 0/N unchecked **nhưng** CSV says PARTIAL with completion_pct > 0. Counter-pattern also observed: DONE gaps (CSV pct=100) với AC 0/N checked.

Two failure modes:

1. **Under-reporting** — work shipped (AC ticks added in some Sub-PR) nhưng CSV `completion_pct` không update → coordinator catalog apply backlog grows wave-over-wave
2. **Stale-checkbox** — CSV flipped DONE/PARTIAL at wave closure but AC checkboxes never ticked in gap file body → reader can't tell scope-completion from gap file alone

Baseline run of `sync-gap-csv-from-ac.sh --warn` surfaces **226 of 641 gaps** with drift ≥10pp between AC-derived pct and CSV `completion_pct`.

## Root Cause

1. **Two sources of truth** — gap file AC checkbox bitmap + CSV `completion_pct` column. Per `gap-architecture-v2.md` v2.0.0 §3, CSV is canonical, but no detector enforces sync; gap file body drifts.
2. **Audit cost amortization** — Wave meta-N audit cycles surface the drift catalog-style but require coordinator catalog-apply (manual). No CI-time detection.
3. **No directional discipline** — coordinator doesn't always know which direction (CSV→AC vs AC→CSV) is canonical for a given gap.

## Proposed Fix

### Phase 1 (this PR — Wave meta-8 Bucket D)

- [x] Ship `scripts/sync-gap-csv-from-ac.sh` drift detector:
  - Parse each gap file under `documents/04-quality/gaps/**/GAP-*.md` for AC `- [x]` vs `- [ ]` counts
  - Compute AC-derived completion_pct = checked/total × 100
  - WARN per gap với delta ≥10pp vs CSV `completion_pct`
- [x] Self-test: 2 synthetic fixtures (1 compliant + 1 drift) — PASS
- [x] Output baseline drift report `documents/04-quality/audits/meta/2026-06-01-csv-ac-drift-baseline.md` (226 gaps surfaced)
- [x] Interpretation header documents bi-directional drift classification (under-reporting vs stale-checkbox)
- [x] CI wire: `.github/workflows/quality-docs.yml` job `csv-ac-sync` WARN-mode

### Phase 2 (Wave meta-9 candidate)

- [ ] Split detector output: under-reporting (AC > CSV) vs stale-checkbox (CSV > AC)
- [ ] HARD STOP only on under-reporting direction (sources of true work-not-tracked)
- [ ] Backlog triage: 226 baseline drift gaps → catalog apply via Wave meta-N

## Acceptance Criteria

- [x] Detector `scripts/sync-gap-csv-from-ac.sh` shipped + self-test PASS
- [x] CI job `csv-ac-sync` wired in quality-docs.yml WARN-mode
- [x] Baseline drift report shipped (226 gaps surfaced)
- [ ] Direction-aware classification (Phase 2)
- [ ] HARD STOP under-reporting after 30-day grace (Phase 2)

## Notes

Per `meta-gap-priority.md` §3 META P1 force-multiplier — single chuẩn detector eliminates retroactive Wave meta-N audit cost permanently. CSV remains canonical per `gap-architecture-v2.md` v2.0.0 §3; detector is bi-directional advisory only.

Threshold 10pp chosen conservatively to avoid noise on small-AC-count gaps where 1-checkbox tick = >10pp delta. Phase 2 may tune per-AC-count tier (≤5 AC → 20pp / >5 AC → 10pp).

## Related

- Rule: `.claude/rules/gap-architecture-v2.md` v2.0.0 §3 (CSV canonical)
- Sister gap: GAP-821 (audit-cadence detector — paired Wave meta-8 Bucket C)
- Audit source: `documents/04-quality/audits/meta/2026-06-01-wave-meta-7-bucket-d-p1-partial.md`
- Baseline drift report: `documents/04-quality/audits/meta/2026-06-01-csv-ac-drift-baseline.md`
- Sister rule: `meta-gap-priority.md` §3

## Log

- 2026-06-01 — Gap filed Wave meta-8 Bucket D. Phase 1 shipped: detector + self-test + CI wire WARN-mode + baseline drift report (226 gaps). Phase 2 direction-aware classification + HARD STOP under-reporting target Wave meta-9.
