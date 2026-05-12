# GAP-487: MEMORY.md orphan backfill — 7 memory files NOT indexed

**Status:** 🔵 OPEN
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

- [ ] All 7 orphan files have MEMORY.md index entries
- [ ] `comm -23 <(memory files) <(MEMORY.md entries)` returns empty
- [ ] Entries follow MEMORY.md convention: `- [Short title](filename.md) — one-line hook`

## Related

- Wave 65 Bucket A
- Sibling: GAP-488 (wave-history orphan), GAP-486 (sync detector to prevent recurrence)

## Log

- **2026-05-12:** Filed Wave 64 close audit.
