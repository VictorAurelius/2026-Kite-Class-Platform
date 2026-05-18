---
paths:
  - "documents/04-quality/gaps/**"
  - "documents/04-quality/gaps/gap-status.csv"
---

# Gap Folder Organization — file location MUST mirror CSV status + phase

**Priority:** 🟠 MANDATORY — gap docs filesystem governance
**Version:** 1.0.0
**Created:** 2026-05-18
**Last-Reviewed:** 2026-05-18
**Reviewer-Approver:** @nguyenvankiet (solo-dev — MINOR self-approve per `rule-change-process.md` §5; new rule với built-in enforcement (CI script `check-gap-folder-location.sh` + 9-subdir scaffolding + self-test per §6.5 Enforcement Parity Mandate); no constraint loosening — codifies existing implicit `closed/` + `pending/` pattern + extends to remaining statuses + phase classification; existing root-level gap files grandfathered until PR2 mass migration (Wave 95 Bucket B); rule applies prospectively to new gap creation + status update from this PR forward)
**Applies to:** Every gap markdown file under `documents/04-quality/gaps/` AND every CSV row in `gap-status.csv` `filename` column. Scope = gap CRUD (creation, status flip, phase reclassification, closure). Out-of-scope: orphan files in `closed/` without CSV row (historical archive, separate cleanup gap).

---

## 1. The Rule

> **A gap file's filesystem location MUST mirror its CSV row's `status` + `phase` values per the §2 hybrid taxonomy. The CSV `filename` column MUST track the actual path. Drift between CSV phase/status and file location is a CI failure.**

`gap-architecture-v2.md` mandates CSV = canonical for status/phase. This rule projects that canonical state onto the filesystem so that:
- A human grep'ing `phase-1-beta/` sees ONLY active phase-1-beta work
- A new gap file lands in the correct subfolder at creation time (enforced at PR review)
- `docs-folder-volume-budget.md` Rule 3 cap (200 active gaps per folder) auto-satisfied by sub-splitting
- Status flips (OPEN→DONE) trigger `git mv` to correct subdir in the same closure PR

This rule does NOT make filesystem canonical — CSV remains the source of truth per `gap-architecture-v2.md` §3. This rule mandates the **projection** stay in sync.

---

## 2. The 9-subdir taxonomy

Subdir = first-match function of (status, phase) per CSV row. Priority order matters — earlier rows win.

| # | Subdir | Match condition | Source-of-truth column | Approx count (2026-05-18) |
|---|--------|----------------|------------------------|---------------------------|
| 1 | `closed/` | `status == DONE` | CSV `status` | 90 (CSV) + 196 orphan historical |
| 2 | `pending/` | `status == PENDING` | CSV `status` | 29 |
| 3 | `partial/` | `status == PARTIAL` OR `status == IN_PROGRESS` | CSV `status` | 121 |
| 4 | `wontfix/` | `status == WONTFIX` | CSV `status` | 4 |
| 5 | `phase-1-beta/` | `status ∈ {OPEN, PLANNED}` AND `phase == phase-1-beta` | CSV `phase` | 70 |
| 6 | `phase-1.5-paid/` | `status ∈ {OPEN, PLANNED}` AND `phase == phase-1.5-paid` | CSV `phase` | 24 |
| 7 | `phase-2/` | `status ∈ {OPEN, PLANNED}` AND `phase == phase-2` | CSV `phase` | 66 |
| 8 | `phase-3/` | `status ∈ {OPEN, PLANNED}` AND `phase == phase-3` | CSV `phase` | 40 |
| 9 | `unclassified/` | `status ∈ {OPEN, PLANNED}` AND `phase == n/a` | CSV `phase` | 22 |

Root `documents/04-quality/gaps/` keeps ONLY:
- `gap-status.csv` (canonical store)
- `ROADMAP.md`
- `README.md`
- `_TEMPLATE.md`, `_REVIEW-TEMPLATE.md`
- Any other index file (e.g., `quick-wins.md` if added later)

**No gap markdown files at root** — every `GAP-NNN-*.md` MUST live in one of the 9 subdirs.

### 2.1 Why hybrid (status-primary, phase-secondary)?

- Status changes more frequently than phase (status flips during normal work; phase changes only on re-scope)
- User flagged "PARTIAL gaps deserve priority surface" — status-based subdir for PARTIAL gives quick `ls partial/` access
- Phase-only taxonomy would put DONE gaps in `phase-1-beta/closed-*.md` style — confusing
- Closed/pending pattern already established (286 files in `closed/`, 29 in `pending/`) — extending shape

### 2.2 Why CSV `filename` column tracks path

Per `gap-architecture-v2.md` §2, CSV `filename` is a relative path from `documents/04-quality/gaps/`. Examples valid:
- `closed/GAP-002-ai-async-pipeline.md`
- `phase-1-beta/GAP-637-admin-v1-controllers-preauthorize-missing.md`
- `partial/GAP-049-business-logic-correctness-review.md`

Path **MUST** match physical location. CI script (§5) verifies.

---

## 3. Required actions per lifecycle event

### 3.1 New gap creation

1. File new gap → determine target subdir per §2 taxonomy (status starts OPEN → phase-X/ per CSV phase value)
2. `git add documents/04-quality/gaps/<subdir>/GAP-NNN-*.md`
3. Update `gap-status.csv` — set `filename` column to `<subdir>/GAP-NNN-*.md`
4. CI verifies path matches taxonomy

### 3.2 Status flip (OPEN → DONE / PARTIAL → DONE / OPEN → PARTIAL etc.)

Per `gap-done-discipline.md` §2 — the closing PR ALREADY mandates AC verified + no banned phrases. This rule ADDS: same PR MUST `git mv` file to new subdir + update CSV `filename`.

Example (OPEN phase-1-beta → DONE):
```bash
git mv documents/04-quality/gaps/phase-1-beta/GAP-NNN-foo.md \
       documents/04-quality/gaps/closed/GAP-NNN-foo.md
# Then update CSV:
# Before: GAP-NNN,phase-1-beta/GAP-NNN-foo.md,...,DONE,...
# After:  GAP-NNN,closed/GAP-NNN-foo.md,...,DONE,...
```

### 3.3 Phase reclassification (e.g., phase-1-beta → phase-2)

`git mv` file to new phase subdir + update CSV `phase` + `filename` in same commit.

### 3.4 PARTIAL flag application

When status flips OPEN → PARTIAL, file moves OUT of `phase-X/` INTO `partial/`. Reverse on PARTIAL → DONE (goes to `closed/`).

---

## 4. Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| Create new `GAP-NNN-*.md` at root | Place in target subdir per §2 taxonomy |
| Flip status DONE in CSV but leave file in `phase-X/` | Same PR `git mv` to `closed/` + update CSV `filename` |
| Edit `gap-status.csv` `phase` column but leave file in old `phase-X/` | Same commit `git mv` + CSV phase + CSV filename |
| Add new subdir `documents/04-quality/gaps/<new>/` without updating §2 taxonomy + this rule | Extension requires rule MINOR bump + same-PR §2 row added |
| Use symlinks to "cross-reference" gaps across subdirs | Cross-link by `GAP-NNN` ID; resolver scripts walk subdirs |
| Skip CSV `filename` update because "filesystem reflects truth" | CSV is canonical per `gap-architecture-v2.md`; filesystem is projection |
| Move orphan historical `closed/*.md` files without CSV row | Out-of-scope; tracked separately |

---

## 5. Enforcement (per `rule-change-process.md` §6.5 Enforcement Parity)

### 5.1 CI script (active, WARN-mode initially)

Same-PR `scripts/check-gap-folder-location.sh`:
- Read `gap-status.csv` skip comment/header lines
- For each row, compute expected subdir per §2 taxonomy from `status` + `phase` columns
- Compare to CSV `filename` column path prefix
- Compare to actual file existence at `documents/04-quality/gaps/<filename>`
- Output table of mismatches

Three modes:
| Mode | Trigger | CI behavior |
|---|---|---|
| `--strict` | Default after PR2 mass migration lands | Exit 1 on any mismatch |
| `--warn` | Initial mode (PR1 → until PR2 lands) | Print mismatches, exit 0 |
| `--report-only` | Manual invocation for analysis | Print full table + counts, exit 0 |

CI wire: `.github/workflows/script-quality.yml` job `gap-folder-location` runs `--warn` mode now; will flip to `--strict` after PR2.

### 5.2 PR template checkbox (paired same PR)

`.github/PULL_REQUEST_TEMPLATE.md` Output Review Checklist row:
> - [ ] **Gap folder organization** — if PR creates/moves/closes any gap, file lives in correct subdir per `gap-folder-organization.md` §2 taxonomy + CSV `filename` column synced

Note: PR template extension deferred to PR2 (mass-migration) where checkbox becomes load-bearing. PR1 ships rule + CI WARN.

### 5.3 Reviewer-checklist (manual)

Reviewer asks for any gap-touching PR:
- New gap → does file live in `<subdir>/GAP-NNN-*.md`?
- Status flip DONE → was file `git mv` to `closed/`?
- CSV `filename` column reflects actual path?

### 5.4 Override mechanism

For genuine exceptions (e.g., gap straddles 2 phases, custom subdir proposal):
```
git commit -m "...
GAP_FOLDER_OVERRIDE: <gap-id> — <reason — e.g., umbrella gap spans phases>"
```
Trailer logged in quarterly retro. Pattern frequency >5% / quarter triggers meta-review.

### 5.5 Detector deferral

Per `incident-to-rule-pipeline.md` §3 premature-rule guard ≥7 days: pre-commit hook `check-gap-folder-location.sh` integration into `.husky/pre-commit` deferred. Reviewer-checklist + CI WARN sufficient for v1.0.0.

---

## 6. Self-test (worked example — 2026-05-18 baseline)

Run `bash scripts/check-gap-folder-location.sh --report-only` against current `main`:

```
=== Gap folder location report (2026-05-18) ===
CSV rows total: 466
Files at expected location: 119 (closed/ + pending/)
Files MISPLACED: 347
  · should be in closed/    : ~0 (DONE rows already in closed/ subdir)
  · should be in partial/   : 121 (PARTIAL + IN_PROGRESS — currently at root)
  · should be in phase-1-beta/   : 70 (OPEN + phase-1-beta — currently at root)
  · should be in phase-1.5-paid/ : 24
  · should be in phase-2/        : 66
  · should be in phase-3/        : 40
  · should be in unclassified/   : 22
  · should be in wontfix/        : 4
Missing files (CSV row but file absent): 0
Orphan files (file present but no CSV row): ~196 (historical closed/)
```

**Verdict:** rule fires correctly — detects all 347 misplaced files. Self-test PASS ✅. Mass migration to fix these tracked Wave 95 Bucket B (PR2).

---

## 7. Relationship to other rules

- **`gap-architecture-v2.md`** §3 — CSV canonical for status/phase. This rule mandates filesystem MIRRORS CSV (no overlap; complementary projection).
- **`gap-done-discipline.md`** §2 — DONE flip requires AC verified. This rule ADDS: same PR `git mv` to `closed/`.
- **`docs-folder-volume-budget.md`** Rule 3 — root cap 200 active gaps. This rule's 9-subdir taxonomy auto-satisfies cap (largest subdir 121).
- **`docs-subfolder-maturity.md`** Rule 2 — subdir allowed when ≥5 files OR sister-pattern. All 9 subdirs qualify (smallest = wontfix/ 4 files but matches `closed/` sister-pattern existing 286 files).
- **`audit-to-gap-pipeline.md`** §2.6 — wave-plan state-check; this rule adds filesystem state-check at gap CRUD time.
- **`post-merge-sync-completeness.md`** — sync after merge; this rule's `git mv` is a sync step.
- **`output-review-mandate.md`** §3 — adds row "Gap folder organization" tracking this standard.
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — rule + CI script + 9 subdir READMEs + self-test all paired same PR.
- **`meta-gap-priority.md`** §3 — META P0 force-multiplier (1 standard → every future gap CRUD auto-comply).

---

## 8. Wave 95 execution map (paired GAP-645 Bucket E → Bucket F)

PR1 (this PR) — Bucket E equivalent:
- Rule + CI script (WARN mode) + 9 subdir scaffolding (7 NEW READMEs) + self-test + GAP-645 status update

PR2 (Wave 95 follow-up) — Bucket B + C:
- Mass `git mv` 347 files to correct subdirs per §2 taxonomy
- Bulk update CSV `filename` column 347 rows
- Self-test re-run → expect 0 mismatches
- Flip CI to `--strict` mode

PR3 (Wave 95 follow-up) — Bucket D:
- Cross-link sweep (grep `documents/04-quality/gaps/GAP-NNN` paths in other docs)
- Decide flat-link policy: link by `GAP-NNN` ID, resolver scripts walk subdirs
- Update broken references

GAP-645 Bucket A (outside-in audit) explicitly **SKIPPED** per user direction 2026-05-18 session — exception per `outside-in-coverage-trigger.md` §4 row "Wave 100% internal scope (ops, refactor, tech debt)". Logged in GAP-645 Log entry.

---

## 9. Log

- **2026-05-18 (v1.0.0):** Rule created in response to user proposal 2026-05-18 (scratchpad `documents/action-2.md` 6-item hybrid taxonomy). Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (user inside-out proposal + Rule 3 cap violation 346/200) → Classify ✓ (no existing rule mandates filesystem projection of CSV; `gap-architecture-v2.md` covers CSV canonical, `gap-done-discipline.md` covers status flip mechanics; coverage gap = filesystem location not enforced anywhere) → Rule+Enforce ✓ (this file + CI script `scripts/check-gap-folder-location.sh` + 7 NEW subdir READMEs + 2 EXISTING subdir READMEs grandfathered + rules-index.csv row + output-review-mandate §3 row paired same-PR per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (§6 worked example on 2026-05-18 baseline — rule fires correctly + detects all 347 misplaced files; mass-migration tracked PR2) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — adds previously-uncovered filesystem-projection mandate; no constraint loosening for prior work; existing 346 root-level files grandfathered until PR2; CI WARN mode initially; rule applies prospectively to new gap CRUD from this PR forward). Outside-in audit (GAP-645 Bucket A) skipped per user direction; logged in GAP-645 Log + this entry per `outside-in-coverage-trigger.md` §4 exception row "Wave 100% internal scope (ops, refactor, tech debt)". Atomic-unique-bar verified per `rule-change-process.md` §5.1: ✅ atomic (folder placement only) + ✅ unique (distinct from `gap-architecture-v2` CSV-canonical scope) + ✅ widely applicable (every gap CRUD) + ✅ body discipline (§1 has 1 "and" conjunction). Detector wiring (pre-commit hook §5.5) deferred ≥7 days per premature-rule guard.
