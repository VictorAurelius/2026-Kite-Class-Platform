# GAP-486: Post-merge sync mandate — extend rule to cover all 4 sync targets + CI detector

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (meta-governance — recurring miss pattern user-flagged)
**Domain:** Meta / Governance
**Found:** 2026-05-12 (Wave 64 session close — user-flagged recurring miss)
**Affects:** Every PR that touches gap files / wave plans / new memory entries / new rules

## Problem

Wave 64 session-close audit (user-flagged) found 4 sync misses recurring across same session — pattern indicates governance gap, not isolated mistakes:

| # | Miss | Sync target | Detection |
|---|------|-------------|-----------|
| 1 | GAP-482 status OPEN→PARTIAL after fixes shipped | `gap-status.csv` row | User nudge |
| 2 | Cutover work missing from ROADMAP §🚀 | `documents/04-quality/gaps/ROADMAP.md` | User nudge |
| 3 | Wave 64 cutover phase not in wave-history | `documents/03-planning/waves/wave-history.jsonl` | Coordinator self-catch (during sync audit) |
| 4 | `feedback_pre_mutation_state_check.md` not indexed | `~/.claude/.../memory/MEMORY.md` | Coordinator self-catch |

Existing rules cover PARTS of this:
- `gap-architecture-v2.md` mandates CSV row sync but no CI detector
- `feedback_post_merge_doc_sync.md` mandates ROADMAP + gap Log sync but doesn't enumerate ALL 4 targets
- `feedback_wave_history_append_required.md` covers wave-history but only on wave closure (not mid-wave overflow work)
- No rule covers MEMORY.md index sync on new memory entry creation

Result: every PR creates risk of stale sync target. 4-miss pattern in 1 session = systemic.

## Proposed Fix

### Option A — Extend existing `feedback_post_merge_doc_sync.md` rule

Add explicit "4 sync targets" checklist + reviewer-checklist line covering all:

```
- [ ] **Post-merge sync (per `post-merge-doc-sync.md`)** — if PR changes a gap status / closes work / adds memory entry:
  - [ ] `gap-status.csv` row updated (status + priority + completion_pct + last_verified)
  - [ ] `ROADMAP.md` §🚀 Next Action reflects current state
  - [ ] `wave-history.jsonl` entry appended/updated (if wave-scoped work)
  - [ ] `MEMORY.md` index updated (if new memory entry added)
```

### Option B — New rule `post-merge-sync-completeness.md`

Standalone rule consolidating all 4 sync targets + override mechanism + worked self-test on the 4 misses from this session.

### Option C — Add CI detector

`scripts/check-post-merge-sync.sh` runs in CI on PRs:
- If diff modifies `documents/04-quality/gaps/*.md` (status change) → verify `gap-status.csv` row updated
- If diff modifies `documents/03-planning/waves/wave-*.md` (status: complete flip) → verify `wave-history.jsonl` appended
- If diff adds memory file (path under user-memory dir) → verify `MEMORY.md` index entry added
- WARN/BLOCK per `audit-gate.py` patterns

Recommend Option B + C ship same PR per `rule-change-process.md` §6.5 Enforcement Parity.

## Acceptance Criteria

- [ ] New rule `.claude/rules/post-merge-sync-completeness.md` v1.0.0 codifies 4 sync targets
- [ ] PR template Output Review section adds explicit checkbox per 4-target matrix
- [ ] `scripts/check-post-merge-sync.sh` shipped — detects each of 4 miss patterns + exits non-zero on stale targets
- [ ] CI workflow integrates the script (e.g. extend `script-quality.yml`)
- [ ] Worked self-test: rule applied retroactively to 4 misses from Wave 64 session — verify all 4 would be caught
- [ ] Memory entry `feedback_post_merge_sync_completeness.md` paired same-PR per `rule-change-process.md` §6.5

## Out-of-scope

- Other sync targets beyond 4 (CHANGELOG, ADR cross-refs) — defer to later
- Migration of existing stale entries — focus on prospective enforcement only
- Replacing `feedback_post_merge_doc_sync.md` — that memory stays; this rule extends

## Related

- **Surfaced by:** User-flagged audit during Wave 64 session close 2026-05-12
- **Reference rules:**
  - `feedback_post_merge_doc_sync.md` (existing memory — to be extended/superseded)
  - `feedback_wave_history_append_required.md`
  - `gap-architecture-v2.md` v1.0.0
  - `rule-change-process.md` §6.5 Enforcement Parity Mandate
  - `incident-to-rule-pipeline.md` 5-stage applied retroactively
- **Companion gap:** GAP-485 (CSV canonical for meta enumerations) — same theme of canonical metadata

## Log

- **2026-05-12:** Filed at user request after Wave 64 session-close audit found 4 recurring sync misses (CSV / ROADMAP / wave-history / MEMORY.md). Pattern systemic, not isolated.
