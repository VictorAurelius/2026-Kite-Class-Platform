# GAP-487: MEMORY.md orphan backfill — 7 memory files NOT indexed

**Status:** 🟢 DONE 2026-05-12 (state-corrected — symptom resolved via Wave 64 PRs before Wave 65 spawn)
**Priority:** 🟠 P1 (Wave 65 Bucket A — sync target #1)
**Domain:** Meta
**Found:** 2026-05-12 (Wave 64 close meta-audit)
**Affects:** Memory discoverability across sessions

## Problem

Audit scan found 7 memory files exist in `~/.claude/projects/-home-nguyenvankiet-projects-2026-Kite-Class-Platform/memory/` but NOT listed in MEMORY.md index:

```
feedback_dev_stack_cold_setup_5_gaps.md
feedback_e2e_scaffold_pattern_universal.md
feedback_gap006_infra_blocker.md
feedback_objectmapper_test_jsr310.md
feedback_phase_0_governance_violation.md
feedback_release_1_first_session_priority.md
feedback_test_hostnames_rfc2606.md
```

These files were created in prior sessions, content valid, but discoverability broken — future Claude won't see them in MEMORY.md auto-loaded index.

## Proposed Fix

Add 7 one-line entries to MEMORY.md index under appropriate section (Feedback section, in date order).

## Acceptance Criteria

- [x] All 7 orphan files have MEMORY.md index entries (verified via grep — all 7 present)
- [x] `comm -23 <(memory files) <(MEMORY.md entries)` returns empty (88=88, 0 orphans)
- [x] Entries follow MEMORY.md convention: `- [Short title](filename.md) — one-line hook`

## Related

- Wave 65 Bucket A
- Sibling: GAP-488 (wave-history orphan — separate, still real), GAP-486 (sync detector to prevent recurrence)

## Log

- **2026-05-12:** Filed Wave 64 close audit.
- **2026-05-12** (Wave 65 fix-time state-check per `audit-to-gap-pipeline.md` §2.8): Symptom self-corrected. Verification:
  ```
  Files on disk (memory/*.md, excl MEMORY.md+README): 88
  Files referenced in MEMORY.md: 88
  comm -23 orphans: 0
  ```
  All 7 originally-listed orphan files (`feedback_dev_stack_cold_setup_5_gaps.md`, `feedback_e2e_scaffold_pattern_universal.md`, `feedback_gap006_infra_blocker.md`, `feedback_objectmapper_test_jsr310.md`, `feedback_phase_0_governance_violation.md`, `feedback_release_1_first_session_priority.md`, `feedback_test_hostnames_rfc2606.md`) verified present in MEMORY.md index. Backfill happened incidentally via Wave 64 closure PRs. **No code change needed — flip DONE.** Saves Bucket A scope ~1h of redundant work.
