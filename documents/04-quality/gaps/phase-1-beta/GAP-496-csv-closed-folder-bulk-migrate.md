# GAP-496: gap-status.csv Phase 2 bulk migrate skipped `closed/` folder — 191 rows missing

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (CSV coverage gap; not blocking but breaks `query-gaps.sh` for closed-gap lookup → causes path-to-invite drift like Wave 66 closure citing GAP-372 as pending when it was DONE Wave 45)
**Domain:** Meta / DevOps
**Found:** 2026-05-12 (Wave 66 closure verification — user-flagged path-to-invite check surfaced GAP-372 referenced as pending Wave 68 but actually DONE Wave 45, file in `closed/`, no CSV row)
**Affects:** Every `bash scripts/query-gaps.sh <id>` lookup for closed gaps; every audit/plan that needs to know historical gap state; trust in `gap-architecture-v2.md` Phase 2 100%-coverage claim

## Problem

`gap-architecture-v2.md` v1.0.x §4 Phase 2 claims "100% coverage of active gap files". Bulk migrate `scripts/migrate-gaps-to-csv.py` scanned only `documents/04-quality/gaps/GAP-*.md` (top-level), skipping `documents/04-quality/gaps/closed/GAP-*.md`.

**Coverage audit (this gap):**
- Active gap files: 282
- Closed gap files: 200
- Closed gaps WITH CSV row: 6 (`GAP-050, GAP-114, GAP-321b, GAP-377, GAP-430, GAP-470` — added piecemeal as gaps closed post Phase 2)
- Closed gaps MISSING CSV row: **194**

Worked example of cost: Wave 66 closure PR #1229 path-to-invite Wave 68 cited "GAP-372 beta tenant invite mechanism + smoke E2E" — but GAP-372 was DONE Wave 45 (2026-05-08, file moved to `closed/`). `query-gaps.sh GAP-372` returns nothing → coordinator copied stale ROADMAP text → Wave 68 scope inflated by 1 work item.

## Root Cause

`scripts/migrate-gaps-to-csv.py` line ~XX (gap file glob): `documents/04-quality/gaps/GAP-*.md` matches top-level only. `closed/` subfolder excluded.

CSV `filename` column schema per `gap-architecture-v2.md` §2 SUPPORTS `closed/GAP-XXX.md` paths — but bulk migrate didn't emit them.

CSV validator `scripts/check-gap-status-csv.sh` `GAP_FILES_OPTIONAL=false` Phase 2 mode validates active files only, not closed/ — so the drift is invisible to CI.

## Proposed Fix

### Phase 1 (this gap — backfill)

Extend `scripts/migrate-gaps-to-csv.py`:
- Add second glob: `documents/04-quality/gaps/closed/GAP-*.md`
- Emit row with `filename=closed/GAP-XXX-...md`
- Default status=DONE, completion_pct=100 (closed folder convention)
- Extract `Status:` from frontmatter — if WONTFIX/SUPERSEDED, use that
- Extract `last_verified` from git log of file rename to closed/ (or use closure date from gap's last Log entry)

### Phase 2 (validator extension)

Extend `scripts/check-gap-status-csv.sh`:
- Also count closed/ files
- Verify CSV has row per closed file
- New mode `GAP_CLOSED_COVERAGE=true` for 100% coverage including closed/

### Phase 3 (rule update)

Update `gap-architecture-v2.md` §4 Phase 2 description to explicitly state "active + closed" coverage. Update §10 Open Items.

## Acceptance Criteria

- [ ] `scripts/migrate-gaps-to-csv.py` glob extended to include `closed/`
- [ ] Re-run migrator → CSV grows from 315 → ~505 rows (315 active + ~190 backfilled closed)
- [ ] `bash scripts/query-gaps.sh GAP-372` returns DONE row (worked example)
- [ ] `bash scripts/check-gap-status-csv.sh` PASS in mode that counts closed/ coverage
- [ ] `gap-architecture-v2.md` §4 Phase 2 description updated to reflect closed/ inclusion
- [ ] `gap-architecture-v2.md` §10 Open Items list updated

## Related

- **Origin:** Wave 66 closure PR #1229 verify session 2026-05-12 (user-flagged path-to-invite drift)
- **Parent rule:** `.claude/rules/gap-architecture-v2.md` v1.0.3 — Phase 2 bulk migration
- **Sister gap:** GAP-490 (Tier 3 skills + audits CSV indexes — separate scope per `meta-csv-index-pattern.md`)
- **Worked example:** GAP-372 DONE Wave 45 not in CSV → Wave 66 closure cited it as pending Wave 68 (cost: 1 wave scope row drift, eliminated via hotfix PR same day)

## Log


- 2026-06-14: phase re-triage — n/a→phase-1-beta (gap-status.csv closed-folder bulk migrate; meta data hygiene).
- **2026-05-12:** Filed during Wave 66 closure verification. CSV coverage audit found 194/200 closed gaps missing rows. Backfill scope ~190 rows. Concurrent hotfix adds GAP-372 row manually to unblock current path-to-invite update.
