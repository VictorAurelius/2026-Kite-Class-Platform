# GAP-199: Rework Audit for Context-Degraded PRs

**Status:** 🟢 DONE (Phase 1 — skill design) / 🟡 PARTIAL (Phase 2 — pilot + automation deferred)
**Priority:** 🟠 P1 (meta tier — retroactive quality assurance)
**Domain:** Meta / Quality / Audit
**Found:** 2026-04-20 (action-1 §12 + §15.J)
**Wave:** Wave 8b (meta)
**Affects:** PRs merged during high-context-pressure sessions (Wave 6-8 era suspected)

## Problem

User explicit concern (action-1 line 611): "chất lượng output giảm do context quá đầy, làm thế nào để đánh giá PR/wave gần đây và xem có phải rework hay không?"

No mechanism currently exists to:
- Identify which merged PRs were produced under context-degradation conditions (high token usage, late in session, post-multiple-compact)
- Re-audit those PRs systematically rather than ad-hoc
- Track "rework debt" as a separate category from normal gaps
- Correlate PR merge quality score against session context pressure

## Context

Anecdotal evidence from memory (`feedback_audit_calibration`): self-audit overstates 15–20 pts vs specialist. Hypothesis: context-degraded PRs have higher calibration drift. Without this gap, drift stays hidden.

## Proposed Fix

1. **Detection heuristic** — mine `documents/03-planning/pr-logs/PR-*.json` for:
   - Session turn-count at merge time (if logged)
   - Time-since-last-/compact
   - PRs missing audit evidence (e.g., no UI /128, no quality /100)
   - PRs merged within short window of each other (fatigue window)
2. **Rework audit skill** — `.claude/skills/quality/rework-audit/SKILL.md`
   - Input: PR list
   - Output: prioritized rework backlog (P0: functional regression, P1: incomplete tests/docs, P2: style)
3. **Pilot** on Wave 6–8 PRs (user suspected window)
4. **Gap output** — rework items become new gaps (follow `audit-to-gap-pipeline.md`)
5. **Prevention** — tie detection into GAP-193 session-orchestration skill so future sessions self-flag

## Acceptance Criteria

### Phase 1 (DONE — this PR)
- [x] Skill `.claude/skills/quality/rework-audit/SKILL.md` drafted
- [x] Detection heuristic documented (`reference/heuristics.md` §Signal Catalog + §Detection Commands)
- [x] Severity rubric documented (`reference/scoring-rubric.md` P0/P1/P2 + calibration guidance)
- [x] Output template defined (reporting format + rework-ID convention)
- [x] Integration path to `audit-to-gap-pipeline.md` specified

### Phase 2 (DEFERRED — follow-up PR)
- [ ] `scripts/detect-candidates.sh` — automate signal scoring to TSV
- [ ] Pilot run on Wave 6–8 (≥5 rework items OR "none found" evidence)
- [ ] Each pilot rework item → gap file per `audit-to-gap-pipeline.md`
- [ ] Session-lock archival hook (depends on GAP-193 Phase 2)
- [ ] Turn-count telemetry in `PR-{N}.json` (depends on GAP-193 Phase 2)
- [ ] Post-wave-audit checklist adds "rework check" in `post-wave-audit-mandate.md` §4
- [ ] Integration into `/repo-status` "rework suspicion" factor

## Related

- action-1 §12 + §15.J
- Memory `feedback_audit_calibration`
- GAP-193 session orchestration (detection sibling)
- `.claude/rules/post-wave-audit-mandate.md`
- `.claude/skills/workflow/pr-health.md`
- Rule: `.claude/rules/meta-gap-priority.md` §3 (Meta P1)

## Log

- 2026-04-20 — Created from action-1 §15.J.
- 2026-04-20 — Phase 1 CLOSED via Wave 8b Agent E. Delivered: `.claude/skills/quality/rework-audit/{SKILL.md, reference/heuristics.md, reference/scoring-rubric.md}` + skills index entry. Heuristics v1 defined (8 signals, weighted scoring, ≥5 threshold). Phase 2 pilot + automation deferred — depends on GAP-193 Phase 2 (turn-count telemetry, session-lock archival).
