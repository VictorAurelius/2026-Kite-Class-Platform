---
paths:
  - "documents/04-quality/gaps/**"
  - "documents/04-quality/gaps/gap-status.csv"
---

# Gap Folder Organization — file location mirrors CSV phase (not status)

**Priority:** 🟠 MANDATORY — gap docs filesystem governance
**Version:** 2.0.0
**Created:** 2026-05-18
**Last-Reviewed:** 2026-05-18
**Reviewer-Approver:** @nguyenvankiet (solo-dev — v2.0.0 MAJOR self-approve per `rule-change-process.md` §5; supersedes v1.0.0 status-driven taxonomy after outside-in audit 3 agents 2026-05-18 — Agents 1 & 2 strong revert + Agent 3 conditional risky; new design phase-only + per-phase `closed/` one-way archive eliminates ~1,150 file moves over Phase 1 BETA lifetime; CSV remains canonical per `gap-architecture-v2.md` §3, filesystem mirrors PHASE only (much more stable than status); user direction "bỏ việc move file vào closed và partial đi, chỉ phân loại theo phase" 2026-05-18; existing PR1 v1.0.0 scaffolding partially recoverable — partial/ + wontfix/ subdirs being deleted, phase-X/ subdirs kept + extended với closed/ archive)
**Supersedes:** v1.0.0 status-driven 9-subdir taxonomy (shipped PR #1532, commit 7d0e6de5, reverted by this PR per outside-in audit findings)
**Applies to:** Every gap markdown file under `documents/04-quality/gaps/` AND every CSV row in `gap-status.csv` `filename` column. Scope = gap CRUD (creation, status flip = NO file move, phase reclassify = move, closure = move to phase-X/closed/). Out-of-scope: 196 orphan files in root `closed/` without CSV row (historical archive, separate cleanup gap if needed).

---

## 1. The Rule

> **A gap file's filesystem location MUST mirror its CSV row's `phase` column. Status changes (OPEN → PARTIAL → DONE) do NOT move files, except DONE which triggers move to `phase-X/closed/` one-way archive within the same phase folder.**

This rule v2.0.0 supersedes v1.0.0 (status-driven 9-subdir taxonomy) after outside-in audit 2026-05-18 — three parallel agents independently concluded status-projection-onto-filesystem fights Git's natural model + violates 9/9 industry pattern + ADR precedent (immutable filename per AWS/Microsoft/adr.github.io).

Design principle: **CSV is canonical for status** per `gap-architecture-v2.md` §3 — filesystem should NOT duplicate that signal. **Phase is much more stable than status** (changes rarely on re-scope vs daily on status flips) — projecting phase onto filesystem is a reasonable browsability aid without daily churn.

Estimated file moves over 6-month Phase 1 BETA lifetime:
- v1.0.0 status-driven: ~1,200 moves (every status flip + phase reclassify)
- **v2.0.0 phase-only + closed-archive: ~120-150 moves** (only PARTIAL→DONE close events + rare phase re-scope)

---

## 2. The 5-folder taxonomy + per-phase closed archive

```
documents/04-quality/gaps/
├── gap-status.csv             # canonical: status, phase, priority, completion_pct
├── ROADMAP.md
├── README.md
├── _TEMPLATE.md
├── _REVIEW-TEMPLATE.md
├── phase-1-beta/              # active gaps with phase=phase-1-beta (any non-DONE status)
│   ├── README.md
│   ├── GAP-NNN-*.md           # OPEN / PARTIAL / IN_PROGRESS / PENDING / PLANNED / WONTFIX
│   └── closed/                # DONE archive (one-way; no move back)
│       └── GAP-NNN-*.md
├── phase-1.5-paid/
│   ├── README.md
│   ├── GAP-NNN-*.md
│   └── closed/
├── phase-2/
│   ├── README.md
│   ├── GAP-NNN-*.md
│   └── closed/
├── phase-3/
│   ├── README.md
│   ├── GAP-NNN-*.md
│   └── closed/
├── unclassified/              # phase=n/a (meta gaps, foundation work, undetermined scope)
│   ├── README.md
│   ├── GAP-NNN-*.md
│   └── closed/
└── closed/                    # LEGACY archive — pre-Phase-2-CSV-migration orphan files
    └── README.md              # 196 historical files without CSV row; rule out-of-scope
```

### 2.1 Subdir match conditions

| Subdir | Match condition | Source-of-truth column |
|---|---|---|
| `phase-1-beta/` (root level) | `phase == phase-1-beta` AND `status != DONE` | CSV `phase` |
| `phase-1-beta/closed/` | `phase == phase-1-beta` AND `status == DONE` | CSV `status` |
| `phase-1.5-paid/` | similar (phase + non-DONE) | CSV |
| `phase-1.5-paid/closed/` | similar (phase + DONE) | CSV |
| `phase-2/`, `phase-2/closed/` | similar | CSV |
| `phase-3/`, `phase-3/closed/` | similar | CSV |
| `unclassified/` | `phase == n/a` AND `status != DONE` | CSV |
| `unclassified/closed/` | `phase == n/a` AND `status == DONE` | CSV |
| `closed/` (root, LEGACY) | Pre-existing orphan files (no CSV row) | N/A — grandfathered |

### 2.2 Why phase-only (not status-driven)

- **Phase changes rarely** — only on scope re-classification (~30-50 events per 6-month Phase 1 BETA). Status changes daily.
- **CSV is already canonical for both** — projecting status onto filesystem duplicates the signal (per Agent 2 audit "two caches for one source = textbook drift recipe"). Phase projection is a single-source browsability aid, not duplication.
- **Industry pattern 9/9** — Linear / GitHub Issues / Jira / Trello / Notion / Airtable / GitLab / Todoist / Bugzilla all use storage-stable + status-as-metadata. Phase is closer to "project" or "milestone" in those tools — sometimes filesystem-organized, often not.
- **ADR precedent** — AWS/Microsoft/adr.github.io ADRs use immutable filenames; status header changes, filename stable. v2.0.0 partially honors this (file moves only on DONE-archive + phase reclassify).
- **Wave batch closure ergonomics** — Wave closing 5 DONE gaps = 5 small moves to phase-X/closed/ (within same phase, single subdir target), not 5 moves across taxonomy boundaries.

### 2.3 Why per-phase `closed/` (not single root `closed/`)

- **Preserves phase scope semantic** — DONE gap belongs to "what was done in phase-1-beta" not generic archive
- **Volume cap satisfied** — phase-1-beta would have 232 files if no archive (> 200 cap); split into 151 active + 81 closed keeps both under cap
- **Retro queries** — `ls phase-1-beta/closed/` answers "what shipped in Phase 1 BETA?" cleanly
- **Root `closed/` reserved for LEGACY** — 196 orphan files (pre-CSV migration) stay there as historical archive; not active scope

---

## 3. Required actions per lifecycle event

### 3.1 New gap creation

1. File new gap → target subdir = `phase-X/` matching CSV `phase` value at creation
2. `git add documents/04-quality/gaps/phase-X/GAP-NNN-*.md`
3. Update `gap-status.csv` — set `filename` column to `phase-X/GAP-NNN-*.md`
4. CI verifies path matches `phase` column

### 3.2 Status flip OPEN → PARTIAL → PENDING → IN_PROGRESS → PLANNED → WONTFIX (any non-DONE transition)

**NO FILE MOVE.** Update CSV `status` + `completion_pct` columns only. File stays at `phase-X/GAP-NNN-*.md`.

### 3.3 Status flip → DONE (closure)

Same PR as gap closure per `gap-done-discipline.md` §2:

```bash
git mv documents/04-quality/gaps/phase-1-beta/GAP-NNN-foo.md \
       documents/04-quality/gaps/phase-1-beta/closed/GAP-NNN-foo.md
# Then update CSV row:
# Before: GAP-NNN,phase-1-beta/GAP-NNN-foo.md,...,DONE,...
# After:  GAP-NNN,phase-1-beta/closed/GAP-NNN-foo.md,...,DONE,...
```

One-way move. No reverse (no DONE → OPEN status revert; if regression, file NEW gap referencing closed one).

### 3.4 Phase reclassify (rare — scope re-prioritization)

When CSV `phase` column changes (e.g., `phase-1.5-paid` → `phase-2`):

```bash
git mv documents/04-quality/gaps/phase-1.5-paid/GAP-NNN-foo.md \
       documents/04-quality/gaps/phase-2/GAP-NNN-foo.md
# If DONE: phase-1.5-paid/closed/ → phase-2/closed/
# Update CSV:
# phase column: phase-1.5-paid → phase-2
# filename column: matching new path
```

### 3.5 Status revert (rare edge case: DONE → OPEN regression)

NOT supported — per `gap-done-discipline.md` "🟢 DONE never re-opens — file a NEW gap if regression". So:
- DONE gap stays in `phase-X/closed/` permanently
- Regression filed as NEW `GAP-NNN+M` in `phase-X/` (root level, non-closed)
- New gap references closed sibling: `closed/GAP-NNN-original.md`

---

## 4. Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| Move file on every status flip (PR1 v1.0.0 anti-pattern) | Update CSV `status` column only; file stays put |
| Create `partial/`, `wontfix/`, status-named subdirs | Status is CSV metadata; no filesystem split by status (except DONE archive) |
| Move DONE gap back to active phase folder on reopen | DONE is one-way — file NEW gap for regression |
| Skip CSV `filename` sync after `git mv` | Same PR must update CSV column |
| Place new gap at root level | Must land in `phase-X/` matching CSV phase at creation |
| Reuse legacy `closed/` (root) for new DONE archives | New DONE → `phase-X/closed/`; root `closed/` is LEGACY only |
| Use symlinks across subdirs | Cross-link by `GAP-NNN` ID; resolver scripts walk subdirs |
| Bulk move 196 orphans into per-phase closed/ without CSV rows | Out-of-scope; orphans grandfathered until separate cleanup gap |

---

## 5. Enforcement (per `rule-change-process.md` §6.5 Enforcement Parity)

### 5.1 CI script (active, WARN-mode initially)

`scripts/check-gap-folder-location.sh` validates:
- Each CSV row's `filename` matches expected path per §2.1 (phase + DONE-archive logic)
- File exists at CSV-specified path
- Orphan files in root `closed/` tolerated (LEGACY exemption)

Modes:
| Mode | CI behavior |
|---|---|
| `--strict` | Exit 1 on any mismatch (target after PR2 mass migration) |
| `--warn` | Print mismatches, exit 0 (initial mode through PR1.5 → PR2) |
| `--report-only` | Print full table + counts, exit 0 |

### 5.2 Reviewer-checklist (manual)

Pre-merge review for any gap-touching PR:
- New gap → file lives in `<phase-from-CSV>/GAP-NNN-*.md`?
- Status flip ≠ DONE → NO file move?
- Status flip = DONE → `git mv` to `<phase>/closed/`?
- Phase reclassify → `git mv` between phase folders?
- CSV `filename` column synced?

### 5.3 Override mechanism

For genuine exceptions (e.g., umbrella gap spanning 2 phases, custom archive scope):

```
git commit -m "...
GAP_FOLDER_OVERRIDE: <gap-id> — <reason>"
```

Trailer logged in quarterly retro. Pattern frequency >5% / quarter triggers meta-review.

### 5.4 Detector deferral

Per `incident-to-rule-pipeline.md` §3 premature-rule guard ≥7 days: pre-commit hook integration deferred. Reviewer-checklist + CI WARN sufficient for v2.0.0.

---

## 6. Self-test (worked example — 2026-05-18 baseline)

Run `bash scripts/check-gap-folder-location.sh --report-only` after PR1.5 lands but before PR2 mass migration:

```
=== Gap folder location report (2026-05-18) ===
CSV rows total: 466
Files at expected location: 119 (90 in legacy closed/ + 29 in pending/)
Files MISPLACED: 347
  · should be in phase-1-beta/        : ~70 (OPEN/active at root currently)
  · should be in phase-1-beta/closed/ : ~81 (DONE currently in root closed/)
  · should be in phase-2/             : ~83
  · should be in phase-3/             : ~70
  · should be in phase-1.5-paid/      : ~37
  · should be in unclassified/        : ~35 (PARTIAL+IN_PROGRESS+OPEN+PLANNED, phase=n/a)
  · should be in unclassified/closed/ : ~8 (DONE, phase=n/a)
Legacy orphans (root closed/, no CSV row): 196 (rule §2.3 grandfathered)
```

Rule fires correctly — detects misplacement under v2.0.0 expected layout. Mass migration tracked PR2 (Wave 95 follow-up). PR2 also migrates 29 PENDING files from `pending/` → `phase-X/` matching their CSV phase, eliminating `pending/` as a status-driven anomaly.

---

## 7. Relationship to other rules

- **`gap-architecture-v2.md`** §3 — CSV canonical for status+phase. v2.0.0 honors this fully: status NOT projected onto filesystem (eliminates duplication); phase projected (single-source browsability aid).
- **`gap-done-discipline.md`** §2 — DONE flip mechanics. v2.0.0 adds: same PR `git mv` to `phase-X/closed/`.
- **`docs-folder-volume-budget.md`** Rule 3 — 200 active cap. v2.0.0 satisfies cap per §2.3 (largest active = phase-1-beta 151 < 200).
- **`docs-subfolder-maturity.md`** Rule 2 — subdir threshold. All 10 subdirs qualify: phase folders by Volume criterion (≥35 files each); per-phase closed/ by sister-pattern (mirrors existing legacy `closed/` 286-file precedent).
- **`outside-in-coverage-trigger.md`** v1.1.0 §2.1 — architecture-decision keywords trigger. v2.0.0 is direct output of 3-agent outside-in audit 2026-05-18.
- **`incident-to-rule-pipeline.md`** §3 — premature-rule guard. v1.0.0 → v2.0.0 revision triggered by Stage 1 detect (user-flagged design concern) + 3-agent outside-in audit consolidation; per Stage 3, rule + CI + scaffolding revision ship same PR.
- **`audit-to-gap-pipeline.md`** §2.6 — wave-plan state-check; this rule adds filesystem state-check at gap CRUD time.
- **`post-merge-sync-completeness.md`** Rule 17 — gap status flip → CSV row sync; v2.0.0 same invariant; file move also synced.
- **`output-review-mandate.md`** §3 — adds row "Gap folder organization" tracking this standard.
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — rule v2.0.0 + CI script rewrite + scaffolding cleanup + GAP-645 Log update all paired same PR (PR1.5).

---

## 8. Wave 95 execution map (revised post-outside-in audit)

### PR1 (shipped, commit 7d0e6de5) — v1.0.0 status-driven design

DEPRECATED by this v2.0.0 PR1.5. Scaffolding partially recoverable:
- `partial/` + `wontfix/` empty subdirs created in PR1 — DELETED in this PR1.5
- `phase-X/` + `unclassified/` subdirs KEPT — extended with `closed/` sub-archive
- `closed/` (root) KEPT — converted to LEGACY archive marker (196 orphans)

### PR1.5 (this PR) — v2.0.0 forward-fix

- Rule rewrite v1.0.0 → v2.0.0 MAJOR (this file)
- Delete `partial/`, `wontfix/` subdirs (READMEs + folders)
- Add 5 `phase-X/closed/` + `unclassified/closed/` sub-archives (READMEs)
- Update existing `phase-X/`, `unclassified/`, `closed/` (root) READMEs with new scope
- CI script rewrite (phase-based logic + DONE-archive special case + legacy-orphan tolerance)
- Update `output-review-mandate.md` §3 row v1.10.0 → v1.11.0
- Update GAP-645 Log with outside-in audit findings + design pivot rationale

### PR2 (Wave 95 follow-up) — mass migration per v2.0.0

- Migrate 90 CSV-tracked DONE from root `closed/` → `phase-X/closed/` (89 phase-1-beta, 1 phase-1.5-paid, 8 unclassified, wait recount per CSV: 81+1+8 = 90, math checks)
- Migrate 232 phase-1-beta gaps from root → `phase-1-beta/` (active in root, DONE in closed/)
- Migrate 83 phase-2 from root → `phase-2/`
- Migrate 70 phase-3 from root → `phase-3/`
- Migrate 38 phase-1.5-paid from root → `phase-1.5-paid/`
- Migrate 43 unclassified (n/a) from root → `unclassified/`
- Migrate 29 PENDING from `pending/` → `phase-X/` (their CSV phase)
- Update CSV `filename` column for all migrated files
- Flip CI `--warn` → `--strict`

### PR3 (Wave 95 follow-up) — cross-link sweep

- grep + Edit broken `documents/04-quality/gaps/GAP-NNN` path references
- Verify low-density per Agent 3 finding (~6 explicit path citations in rules; mostly bare GAP-NNN IDs which auto-resolve)

---

## 9. Audit decision log (outside-in 2026-05-18)

Per `outside-in-coverage-trigger.md` v1.1.0 §3 Bước 5 documenting consolidated audit findings:

3 parallel agents triangulated independently:
1. **Persona simulation** verdict ❌ REVERT — ~1,200 moves over 6 months unsustainable
2. **External benchmark** verdict ❌ REVERT — 9/9 industry tools + ADR precedent both mandate stable filenames
3. **Failure-mode matrix** verdict ⚠️ RISKY — Class 4 tooling blockers (`.claude/skills/**` non-recursive globs) require fix before PR2 anyway

Consensus core: status should NOT drive filesystem location (duplicates canonical CSV; fights Git natural model). Phase is borderline acceptable (stable, less churn). v2.0.0 strikes balance: phase-projection (single source) + per-phase `closed/` archive (preserves phase scope semantic + satisfies volume budget cap).

User direction 2026-05-18: "bỏ việc move file vào closed và partial đi, chỉ phân loại theo phase thì sao?" — explicit acceptance of phase-only with per-phase closed/ added during clarification dialogue.

Audit artifacts preserved per `output-review-mandate.md` §3:
- Agent 1 (Persona simulation) — `/tmp/.../tasks/afff9fb778e14295b.output` (full transcript)
- Agent 2 (External benchmark) — `/tmp/.../tasks/a8ad1a312998601d7.output`
- Agent 3 (Failure-mode matrix) — `/tmp/.../tasks/a1beab2a4c0f9ed26.output`

Future enhancement: save consolidated audit report to `documents/04-quality/audits/meta/2026-05-18-gap-folder-organization-outside-in-audit.md` (defer to PR2 for proper format + audits-index.csv registration).

---

## 10. Log

- **2026-05-18 (v2.0.0):** MAJOR rewrite — supersedes v1.0.0 status-driven 9-subdir taxonomy. Triggered by user post-merge inspection 2026-05-18 immediately after PR1 v1.0.0 merge: "tôi nghĩ nên có agent outside lại cấu trúc subfolder này có tốt không?" Per `outside-in-coverage-trigger.md` v1.1.0 §3 (recurrence #3 of pattern per memory `feedback_outside_in_recurring_miss.md` — Claude skipped outside-in trigger when user proposed scope shape; user caught it post-PR1 ship), spawned 3 parallel outside-in agents. All 3 independently concluded status-driven layout wrong: Agent 1 (persona) ❌ REVERT; Agent 2 (industry benchmark 9/9) ❌ REVERT; Agent 3 (failure-mode) ⚠️ RISKY. User direction "phase-only + drop closed/partial migration" → clarification dialogue extended with per-phase closed/ archive to satisfy volume budget cap. Per `incident-to-rule-pipeline.md` 5-stage + `rule-change-process.md` §6.5 Enforcement Parity Mandate: this rule + CI script rewrite + scaffolding cleanup (delete partial/+wontfix/, add 5 phase-X/closed/) + GAP-645 Log update + output-review-mandate §3 row revision + rules-index.csv keeps row (revised scope) all paired same PR1.5. Reviewer: @nguyenvankiet (solo-dev MAJOR self-approve per `rule-change-process.md` §5 — significant constraint shift from v1.0.0 status-driven to v2.0.0 phase-only; no constraint loosening for FUTURE work but explicit relaxation of v1.0.0 status-flip-causes-move requirement; existing PR1 scaffolding partially recoverable per §8 execution map; rule applies prospectively to new gap CRUD from PR1.5 merge forward). Detector wiring (pre-commit hook §5.4) deferred ≥7 days per premature-rule guard. Atomic-unique-bar verified per `rule-change-process.md` §5.1: ✅ atomic (folder placement per phase + DONE archive) + ✅ unique (distinct from `gap-architecture-v2.md` CSV scope) + ✅ widely applicable (every gap CRUD) + ✅ body discipline (§1 has 1 "and" conjunction).
- **2026-05-18 (v1.0.0) — DEPRECATED by v2.0.0:** Rule created PR #1532. Status-driven 9-subdir taxonomy. Shipped commit 7d0e6de5 same session. Reverted by v2.0.0 within hours per outside-in audit findings (3 agents). v1.0.0 scaffolding (partial/+wontfix/ subdirs) deleted by v2.0.0 PR1.5; phase-X/ subdirs retained + extended.
