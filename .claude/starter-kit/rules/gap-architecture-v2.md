# Gap Architecture v2 — single canonical source (Design A)

**Priority:** 🟠 MANDATORY — gap docs governance
**Version:** 1.0.3
**Created:** 2026-05-11
**Last-Reviewed:** 2026-05-14
**Reviewer-Approver:** @nguyenvankiet (starter-kit upstream maintainer)
**Applies to:** `documents/04-quality/gaps/*.md` files + `documents/04-quality/gaps/gap-status.csv` canonical store + tools that query/update gap status

---

## 1. The Rule

> **`documents/04-quality/gaps/gap-status.csv` là single canonical source for gap status, priority, phase, completion_pct, last_verified.** Gap markdown files describe SCOPE (Problem + AC + Log) — status/priority field trong frontmatter là cache informational, có thể drift, KHÔNG canonical.

Cố gắng trust status field trong gap file = trust-the-document trap (lesson từ 2026-05-11 GAP-450 + GAP-353b + 9 emoji-variant gaps). CSV eliminates plan-vs-gap drift, emoji-format drift, audit-of-status-claims overhead.

---

## 2. CSV schema

Located at `documents/04-quality/gaps/gap-status.csv`.

| Column | Type | Description |
|---|---|---|
| `id` | `GAP-NNN[a-z]*` | Match filename prefix; alphanumeric suffix OK (`GAP-353b`, `GAP-005a`) |
| `filename` | string | Relative path from `documents/04-quality/gaps/` (e.g., `GAP-005-ai-queue-fair-scheduling.md` or `closed/GAP-002-ai-async-pipeline.md`) |
| `title_short` | string | One-line summary ≤80 chars |
| `status` | enum | `OPEN \| PARTIAL \| PLANNED \| IN_PROGRESS \| DONE \| WONTFIX` |
| `priority` | enum | `P0 \| P1 \| P2 \| P3` |
| `domain` | enum | `Frontend \| Backend \| DevOps \| Compliance \| Meta \| AI \| Mixed` |
| `phase` | enum | `phase-1-beta \| phase-1.5-paid \| phase-2 \| phase-3 \| n/a` |
| `completion_pct` | 0-100 | For PARTIAL granularity; 0=OPEN, 100=DONE; 1-99 = PARTIAL/IN_PROGRESS |
| `found_date` | YYYY-MM-DD | Initial filing date |
| `last_verified` | YYYY-MM-DD | Per `audit-to-gap-pipeline.md` §2.8 — last empirical state-check date |
| `notes` | string | Optional 1-line context (DEFERRED reason, blocked-by ref) |

Header line `id,filename,title_short,status,...` required. Comment lines start `#`.

### Status ↔ completion_pct consistency rules

| Status | completion_pct | Validation |
|---|---|---|
| OPEN | 0 | enforced |
| DONE | 100 | enforced |
| PARTIAL / IN_PROGRESS | 1-99 | enforced |
| PLANNED | 0-50 | informational |
| WONTFIX | n/a | informational |

CI script (`scripts/check-gap-status-csv.sh`) validates enums + format + file-exists.

---

## 3. Authority

For these fields, **CSV beats markdown frontmatter**:

| Field | Canonical |
|---|---|
| Status | **CSV** (markdown field deprecated, informational) |
| Priority | **CSV** (markdown field deprecated) |
| Phase classification | **CSV** (plan + gap file Phase fields deprecated) |
| Completion % | **CSV** (no equivalent in markdown) |
| Last verified | **CSV** (no equivalent in markdown) |

Markdown gap file remains canonical for:
- Title + Problem description
- Acceptance Criteria
- Proposed Fix
- Log entries
- Related gaps + PR refs (narrative)

---

## 4. Migration phases

### Phase 1 — Pilot (PR #1159, 2026-05-11) ✅ DONE

- CSV created với 5 sample rows
- CI script `check-gap-status-csv.sh` validates rows present
- `GAP_FILES_OPTIONAL=true` — not yet requiring 100% coverage
- Markdown files unchanged — still có Status/Priority frontmatter
- Query helper `scripts/query-gaps.sh` proves token-savings claim

### Phase 2 — Bulk migration (this PR, 2026-05-11) ✅ DONE

- `scripts/migrate-gaps-to-csv.py` extractor — parses Status/Priority/Domain/Found
  from every `documents/04-quality/gaps/GAP-*.md` frontmatter; handles multi-line
  Status blocks (GAP-436), date-field variants (Found/Detected/Created), and
  filename-prefix collisions (GAP-116, GAP-200, GAP-321b-1, GAP-353b each have 2
  files — bulk migrator emits full-stem ids for all colliding files; pilot rows
  remap accordingly)
- CSV grown từ 5 → 289 rows (100% coverage of active gap files)
- Final distribution: Status `OPEN 186 / PARTIAL 98 / IN_PROGRESS 3 / PLANNED 2`;
  Priority `P0 67 / P1 114 / P2 104 / P3 4`; Phase `phase-1-beta 35 /
  phase-1.5-paid 4 / phase-2 1 / phase-3 3 / n/a 246` (conservative — bulk
  migrate does NOT infer phase unless content explicitly cites it; reclassify
  Phase 1 BETA-relevant gaps post-merge)
- `check-gap-status-csv.sh` flipped to Phase 2 mode (`GAP_FILES_OPTIONAL=false`
  default; coverage check matches by exact filename to support collision-stem
  ids); CI job `gap-status-csv` wired in `.github/workflows/script-quality.yml`

### Phase 3 — Frontmatter strip (post bulk-migrate validated)

- Strip Status/Priority field from gap files
- Add header comment: "Canonical status: gap-status.csv. To update, edit CSV."
- ROADMAP §🚀 Next Action auto-derived from CSV via script

### Phase 4 — Tooling integration

- [x] `start-session` collect-state.sh queries CSV as fallback for blockers + surfaces Phase 1 BETA P0 count ✅ (PR #1162)
- [x] `audit-to-gap-pipeline.md` §2.8 step 0 recommends `query-gaps.sh` ✅ (this PR)
- [x] Wave plan template `_TEMPLATE.md` §3 — gap referencing convention via CSV id ✅ (this PR)
- [ ] ROADMAP §🚀 Next Action auto-derive from CSV — deferred (curated doc, risky to autogen)

---

## 5. Token cost analysis (pilot validation)

Test: list all P0 OPEN gaps for Phase 1 BETA.

| Method | Token cost | Speed |
|---|---|---|
| Read each gap file + grep Status field | ~500 tokens × N gaps | Slow |
| `bash scripts/query-gaps.sh P0 OPEN phase-1-beta` | ~50 tokens (1 CSV row + filter) | Fast |
| `bash scripts/query-gaps.sh --count P0 OPEN phase-1-beta` | ~10 tokens (count only) | Fastest |

For 187 active gaps: state-check session token cost reduces ~50× via CSV.

---

## 6. Enforcement

### 6.1 CI gate

`scripts/check-gap-status-csv.sh` validates:
- CSV well-formed (header + valid enums + dates)
- Every CSV row's `filename` points to existing gap file (top-level OR closed/)
- Status ↔ completion_pct consistency
- Phase 2+: every active gap file has CSV row (`GAP_FILES_OPTIONAL=false`)

Add to `.github/workflows/script-quality.yml` paired same-PR:

```yaml
- name: Gap status CSV validation
  run: bash scripts/check-gap-status-csv.sh
```

### 6.2 PR-template checkbox

Add to `.github/PULL_REQUEST_TEMPLATE.md` Output Review Checklist:

> - [ ] **Gap status change** — if PR closes a gap OR changes status/priority/completion, gap-status.csv row updated correspondingly (CSV is canonical per `gap-architecture-v2.md`)

### 6.3 Memory auto-load

Memory `feedback_gap_status_csv_canonical.md` (paired same-PR) reminds at session start to query CSV before reading gap files.

### 6.4 Override mechanism

For genuine cases where CSV update can't happen same PR (e.g., emergency hotfix):

```
git commit -m "...
GAP_STATUS_CSV_DEFER: <reason — explain why CSV update is in follow-up PR linked to GAP-XXX>"
```

Trailer logged in quarterly retro.

---

## 7. Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| Read gap file Status field cho status check | `bash scripts/query-gaps.sh <id-prefix>` |
| Update Status in markdown file without CSV row sync | Update CSV row; markdown Status field is cache |
| Add new gap file without CSV row | Add CSV row in same PR creating the file |
| Trust plan document Priority field over gap file | Trust CSV. Both plan + gap file are caches. |
| Bulk-strip markdown Status field at pilot time | Wait Phase 3 post bulk-migrate validation |

---

## 8. Self-test (worked example — 5 pilot gaps)

Test 1 — CI script validates clean:

```bash
bash scripts/check-gap-status-csv.sh
```

Expected: `PASS: 5 CSV rows validated`. ✅ Actual: passes.

Test 2 — Query script returns correct counts:

```bash
bash scripts/query-gaps.sh --count P0 OPEN phase-1-beta
```

Expected: 1 (GAP-137). ✅ Actual: 1.

```bash
bash scripts/query-gaps.sh --count "" "" phase-1-beta
```

Expected: 4 (005, 114, 137, 353b — all phase-1-beta). ✅ Actual: 4.

Test 3 — Pilot CSV catches representative drift scenarios:

| Gap | Drift case demonstrated |
|---|---|
| GAP-005 | IN_PROGRESS với completion_pct=40 (Phase 1 done, Phase 2 open) — granularity field captures what markdown PARTIAL/IN_PROGRESS can't |
| GAP-006 | OPEN + notes="DEFERRED..." — deferred reason captured structurally, not buried in markdown Log |
| GAP-114 | PARTIAL với completion_pct=80 — granularity for near-DONE PARTIAL |
| GAP-137 | OPEN + completion_pct=0 — baseline simple case |
| GAP-353b | PARTIAL với completion_pct=73 (8/11 AC verified) — structured progress vs prose "8/11 AC" buried in Status field |

→ Rule + CSV format captures all 5 representative cases. Self-test PASS.

---

## 9. Relationship to other rules

- **`audit-to-gap-pipeline.md`** §2.5 (filing-time state-check) + §2.6 (wave-plan) + §2.7 (decision-doc) + §2.8 (fix-time) — those rules describe WHEN to state-check; this rule says WHERE canonical state lives
- **`gap-done-discipline.md`** §2 — DONE flip = update CSV row status field + completion_pct=100; markdown Status field update follows (cache)
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — this rule + CSV + CI script + query helper + worked self-test all ship same PR
- **`incident-to-rule-pipeline.md`** — this rule is output of 2026-05-11 user-flagged retro on docs trust ("docs có vấn đề, không trust được" → architectural fix not patch)
- **`docs-only-pr-auto-merge.md`** — gap status updates are docs-only edits → auto-merge eligible per §2 scope (CSV + .md + rules + skills)
- **`output-review-mandate.md`** §3 — adds new row "Gap status" — review standard = CSV + check-script
- **`feedback_gap_status_csv_canonical.md`** (memory, paired same-PR)

---

## 10. Open Items / Follow-ups (per `gap-done-discipline.md` §3 PARTIAL exit ramp)

Phase 1 pilot + Phase 2 bulk migration + Phase 4 governance integration shipped (this PR + #1159 + #1161 + #1162). Tracked separately:

- [x] ~~Phase 2 bulk migration~~ ✅ (PR #1161)
- [x] ~~CI wiring~~ ✅ (PR #1161)
- [x] ~~Phase 2.1 phase reclassification (auto-fill safe cases)~~ ✅ — this PR extended `migrate-gaps-to-csv.py` with richer keyword + filename-pattern (`GAP-NNN-p3-*` → phase-3 K-12); n/a count dropped 246 → 219 (27 gaps reclassified to phase-3 K-12 LEGAL). Remaining 219 n/a are genuinely ambiguous — leave to user judgment per gap.
- [x] ~~Phase 4 tooling integration~~ ✅ — `collect-state.sh` (PR #1162) + `audit-to-gap-pipeline.md` §2.8 step 0 + wave template §3 convention note (this PR). ROADMAP §🚀 auto-derive remains deferred (curated doc, risky).
- [ ] **Phase 2.2 completion_pct refinement** — PARTIAL/IN_PROGRESS rows default to 50/40; can be refined per gap when context known (e.g., GAP-005 Phase 1 done = ~40; GAP-114 ~80). Iterative, no urgency.
- [ ] **Phase 3 markdown frontmatter strip** — remove Status/Priority field from gap files; add header comment "Canonical status: gap-status.csv". Premature until ROADMAP auto-derive + every consumer (runbooks, skills) migrated off Status grep.
- [ ] **ROADMAP §🚀 Next Action auto-derive** — deferred (curated doc, risky to autogen). File follow-up gap when need outweighs maintenance cost.
- [ ] **Filename-collision cleanup** — 4 prefix-collision pairs surfaced during bulk migration (GAP-116 / GAP-200 / GAP-321b-1 / GAP-353b each have 2 files). Currently handled via full-stem ids; consider renaming one side of each pair to disambiguate filename too. Low priority.

---

## 11. Log

- **2026-05-11 (v1.0.3):** PATCH — Phase 2.1 auto-fill + Phase 4 doc closeout. `migrate-gaps-to-csv.py` `infer_phase()` extended with richer keyword set + filename pattern (`GAP-NNN-p3-*` → phase-3 K-12 LEGAL, captures author convention). Re-ran extractor: 27 gaps reclassified from `n/a` to specific phase (mostly phase-3 K-12). Final distribution: phase-1-beta 35 / phase-1.5-paid 4 / phase-2 1 / **phase-3 30** (was 3) / n/a 219 (was 246). `audit-to-gap-pipeline.md` v1.4.0 → v1.4.1 — adds §2.8 step 0 "Canonical-status lookup first" recommending `query-gaps.sh` before heavier state-check. `documents/03-planning/waves/_TEMPLATE.md` §3 — adds gap-referencing convention note (use CSV canonical id + `query-gaps.sh` to verify status before wave plan references). §10 Open Items updated: only ROADMAP auto-derive + Phase 3 frontmatter strip + Phase 2.2 completion refinement + filename rename remain (all genuinely lower priority or risky). No constraint change. Reviewer: @nguyenvankiet (solo-dev PATCH self-approve per `rule-change-process.md` §5 — additive automation + doc cross-references).

- **2026-05-11 (v1.0.2):** PATCH — Phase 4 partial integration. `collect-state.sh` (used by `/start-session`) now queries `gap-status.csv` as fallback for blocker IDs (replacing the slower grep-each-gap-file path) and surfaces a new line `Phase 1 BETA P0: N active (M PARTIAL)` per session start. JSON output adds `phase_1_beta_p0_active` + `phase_1_beta_p0_partial` keys. §4 Phase 4 marked partial; remaining items (audit-pipeline §2.8 CSV query, wave plans by CSV id, ROADMAP §🚀 auto-derive) tracked. No constraint change. Reviewer: @nguyenvankiet (solo-dev PATCH self-approve per `rule-change-process.md` §5 — additive tool integration, no constraint loosening).

- **2026-05-11 (v1.0.1):** PATCH — Phase 2 bulk migration shipped (PR #1161). `scripts/migrate-gaps-to-csv.py` extracted Status/Priority/Domain/Found from 289 active gap files into `gap-status.csv`; CSV grown 5 → 289 rows. `scripts/check-gap-status-csv.sh` flipped to Phase 2 mode (`GAP_FILES_OPTIONAL=false` default) — coverage match by exact filename to support filename-prefix collision pairs (GAP-116 / GAP-200 / GAP-321b-1 / GAP-353b each have 2 files; both members get full-stem ids; pilot row GAP-353b remapped to GAP-353b-server-consent-api-audit-log). CI job `gap-status-csv` added to `.github/workflows/script-quality.yml`. §4 Phase 1/2 marked DONE; §10 Open Items updated. Reviewer: @nguyenvankiet (solo-dev PATCH self-approve per `rule-change-process.md` §5 — Phase 2 was already mandated in v1.0.0 §4 as "follow-up wave"; this PR is the closeout sync). No constraint change. Phase 3 (frontmatter strip) + Phase 4 (tooling integration) remain deferred per §10.

- **2026-05-11 (v1.0.0):** Rule created. Triggered by 2026-05-11 user-flagged retro on docs trust ("tinh giảm docs xuống để có thể sync đơn giản, đỡ tốn token không?"). 5 drift classes documented empirically (plan vs gap Priority disagreement, status stale, obsolete refs, PARTIAL granularity, ROADMAP §🚀 stale). Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (user-flagged systemic doc-trust failure) → Classify ✓ (no existing rule mandates single canonical source for gap status; CLAUDE.md §Living Documents addresses sync but doesn't eliminate multi-source) → Rule+Enforce ✓ (this file + gap-status.csv + scripts/check-gap-status-csv.sh CI validator + scripts/query-gaps.sh helper + paired memory + 5-gap worked self-test per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (§8 worked example on 5 representative gaps — IN_PROGRESS, DEFERRED, PARTIAL near-DONE, OPEN, PARTIAL mid) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — new rule with built-in enforcement, no constraint loosening for prior work; existing gap files grandfathered; rule applies prospectively). Phase 2-4 bulk migration deferred to follow-up gaps per §10. Phase 1 pilot proves concept on 5 representative gaps spanning full status/priority/completion enum range.
