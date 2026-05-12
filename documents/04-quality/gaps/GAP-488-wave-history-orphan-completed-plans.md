# GAP-488: wave-history.jsonl orphan backfill — 15+ wave plans status:complete missing entries

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (Wave 65 Bucket A — sync target #2)
**Domain:** Meta
**Found:** 2026-05-12 (Wave 64 close meta-audit)
**Affects:** Wave history completeness; pattern lookup for past waves

## Problem

Audit scan found 15+ wave plan files with `status: complete` frontmatter NOT having corresponding entry in `documents/03-planning/waves/wave-history.jsonl`:

```
wave-2026-04-28-gap-122-platform-alerts.md
wave-2026-04-29-business-correctness.md
wave-2026-04-29-dr-backup.md
wave-2026-04-29-legal-brd-phase1-5.md
wave-2026-04-29-legal-brd-phase1.md
wave-2026-04-29-meta-gov-2.md
wave-2026-04-29-meta-phase2-cleanup.md
wave-2026-04-29-observability.md
wave-2026-04-29-review-process-improvement.md
wave-2026-04-29-ui-coverage-audit.md
wave-2026-04-29-ui-kits-round-2.md
wave-2026-04-29-ui-kits-round-3.md
wave-2026-05-04-18a-keystones.md
wave-2026-05-04-18b1-k12-legal-phase-1a.md
wave-2026-05-04-18b2-k12-legal-phase-1b-foundation.md
... (more)
```

Pattern recurring for weeks — `feedback_wave_history_append_required.md` rule + `session-docs-check` Rule 15 detector exist but only fire on STATUS FLIP commits, not on past wave plans that were marked complete without jsonl append.

## Proposed Fix

Backfill jsonl entries for each orphan wave plan. Source data from wave plan frontmatter + Log section. Format per existing entries.

If full per-wave detail not available (some old plans), use minimal stub:
```json
{"wave":"...", "date":"...", "title":"...", "status":"complete-backfilled","outcome":"shipped","note":"Backfilled by GAP-488 2026-05-12; original session jsonl append was missed"}
```

## Acceptance Criteria

- [ ] All wave plan files with `status: complete` have corresponding jsonl entry (full OR stub)
- [ ] No new orphan after backfill: `grep -l "status: complete" wave-*.md | xargs basename → all in jsonl`
- [ ] Stub entries clearly marked `status:complete-backfilled` so future readers know data is partial
- [ ] `session-docs-check` Rule 15 detector validated on backfilled state (no false positive)

## Related

- Wave 65 Bucket A
- Sibling: GAP-487 (memory orphan), GAP-486 (sync detector to prevent recurrence — extend Rule 15 to catch prior misses?)

## Log

- **2026-05-12:** Filed Wave 64 close audit.
