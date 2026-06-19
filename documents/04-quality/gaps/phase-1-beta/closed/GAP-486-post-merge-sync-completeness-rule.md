# GAP-486: Post-merge sync mandate — extend rule to cover all 4 sync targets + CI detector

**Status:** 🟢 DONE 2026-05-12 — Rule + Rule 17 detector + 3 fixtures + PR template + worked self-test shipped Wave 65 Bucket B (this PR)
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

- [x] New rule `.claude/rules/post-merge-sync-completeness.md` v1.0.0 codifies 4 sync targets
- [x] PR template Output Review section adds explicit checkbox per 4-target matrix
- [x] Detector shipped — `session-docs-check` Rule 17 in `scripts/check-docs.sh` flags status flip without CSV sync (with override-trailer downgrade FAIL→WARN). Rule 18 (memory mirror) scoped as PARTIAL per `post-merge-sync-completeness.md` §5 (no in-repo mirror exists — reviewer manual until future scope)
- [x] CI integration via existing `session-docs-check` skill (already wired in audit-gate.py hook); standalone `scripts/check-post-merge-sync.sh` superseded by Rule 17 in `check-docs.sh` per `rule-change-process.md` §6.5 (single detection surface preferred over duplicating logic)
- [x] Worked self-test: rule §8 retroactively applied to Wave 64 GAP-482 incident — Rule 17 fires correctly; 3 fixtures (good / bad / bad-with-override) all green (`bash .claude/skills/workflow/session-docs-check/test/run-rules.sh` → 9/9 pass)
- [x] Memory entry text embedded in PR description (per rule §7.5) — repo has no in-repo memory mirror; user copies to `~/.claude/projects/.../memory/feedback_post_merge_sync_completeness.md` + updates MEMORY.md index

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
- **2026-05-12:** 🟢 DONE — Wave 65 Bucket B shipped: (a) new rule `.claude/rules/post-merge-sync-completeness.md` v1.0.0 codifying the 4 sync targets + §4 decision flow + §5 memory-mirror scope clarification + §6 anti-patterns + §7 enforcement + §8 worked self-test; (b) `session-docs-check` Rule 17 in `scripts/check-docs.sh` detects gap Status flip without `gap-status.csv` row update (FAIL strict, WARN otherwise; override trailer `POST_MERGE_SYNC_OVERRIDE: GAP-NNN — <reason>` downgrades FAIL→WARN); (c) 3 fixtures under `test/fixtures/post-merge-sync/{good-status-flip-with-csv-sync,bad-status-flip-no-csv-sync,bad-status-flip-no-csv-sync-with-override}` + run-rules.sh expectations + commit-message.txt override-injection support — 9/9 fixtures green; (d) PR template Output Review checkbox added; (e) doc-rules-matrix.md gains Rule 17 (full) + Rule 18 (PARTIAL with deferred scope) entries; (f) SKILL.md rule count bumped 14→17; (g) gap-status.csv row for GAP-486 synced to DONE/100 per the rule it ships. Rule 18 (memory mirror) PARTIAL per §5 — repo has no in-repo memory mirror; enforcement via reviewer manual + PR-description embedding until future scope adoption.
