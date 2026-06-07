---
title: Wave NN — <topic>
status: draft
created: YYYY-MM-DD
updated: YYYY-MM-DD
waves: [NN]
gaps: [GAP-XXX, GAP-YYY]
---

# Wave NN — <topic>

**Goal:** one-sentence outcome.
**Trigger:** what made this wave the next wave.
**Estimated wall-clock:** ~Xh agent work, longest-bucket ~Ymin.

---

## 1. Brainstorm (5-10 min)

**Q1 (alignment):** which personas / domains / waves does this serve?
**Q2 (trade-offs):** what alternatives were considered and rejected, and why?
**Q3 (risks):** what could go wrong; how does each bucket recover?

---

## 2. Task Breakdown

| Bucket | Gap(s) | Owner | Effort | Disjoint? |
|--------|--------|-------|--------|-----------|
| A | GAP-XXX | bg-agent | est. Nh | ✅ FE only |
| B | GAP-YYY | bg-agent | est. Nh | ✅ BE module X |

Disjoint check: confirm no two buckets touch the same package / file.

---

## 3. Scope (compact schema — Strategy B+C proven Wave 33)

**Stake tier (per `wave-pack-planner/SKILL.md` §Step 4.6):** <HIGH | MEDIUM | LOW> → model: <Opus 4.7 full | Opus medium | Sonnet/Haiku>
**Cross-layer? (per `wave-pack-planner/SKILL.md` §Step 4.5):** <YES → Bucket 0 Foundation required per `contract-first-for-cross-layer.md` | NO → skip foundation>

> **Gap referencing convention** (per `.claude/rules/gap-architecture-v2.md`): use the canonical id from `documents/04-quality/gaps/gap-status.csv` column 1. For collision-stem ids (multiple files sharing a numeric prefix), use the full stem (e.g. `GAP-116-pii-scrubbing-logs`). Query gap state via `bash scripts/query-gaps.sh <prefix>` before referencing — confirms `status`/`priority`/`phase` match what the wave assumes.

| # | Bucket | Gap(s) | Priority | Files (glob) | Spawn order |
|:-:|--------|--------|:--------:|--------------|:-----------:|
| 0 | **Foundation** | (contract + mock infra — only if cross-layer=YES) | 🟠 P1 | `documents/01-business/{domain}/api-contract.md` + `{frontend}/src/test/msw/handlers/{domain}.ts` | MERGE FIRST |
| 1 | **A** | GAP-XXX | <prio> | `path/to/area/` | parallel after Bucket 0 |
| 2 | **B** | GAP-YYY | <prio> | `path/to/other/` | parallel after Bucket 0 |

### Bucket 0 — Foundation (Contract + Mock Infrastructure) — only if cross-layer=YES

Per `.claude/rules/contract-first-for-cross-layer.md` v1.0.0:
- Files: `documents/01-business/{domain}/api-contract.md` (CREATE/UPDATE) + `{frontend}/src/test/msw/handlers/{domain}.ts` setup
- Acceptance: api-contract.md exists + lists all endpoints consumed by FE+BE buckets; MSW handlers consumable
- Spawn order: MERGE FIRST trước khi spawn FE+BE buckets parallel

### Bucket A — <one-line>

- Files: `path/to/area/` (RELATIVE paths only per `feedback_worktree_absolute_path_contamination.md`)
- Tests: which test files added/modified
- Acceptance: bucket-level AC subset of gap AC
- (Cross-layer FE bucket): "Endpoint consumption tuân thủ schema trong `documents/01-business/{domain}/api-contract.md`"
- (Cross-layer BE bucket): "Controller signature + DTO match `documents/01-business/{domain}/api-contract.md` schema"

### Bucket B — ...

---

## 4. State-Check Evidence (BẮT BUỘC per `audit-to-gap-pipeline.md` §2.6)

Every code-symbol-shaped reference (in backticks) used in §3 Scope must appear here with verification evidence. Symbols intentionally absent because the wave WILL CREATE them get verdict `🆕 to-be-created` and a Bucket owning the creation. Symbols referenced as if existing but absent → revise §3 Scope.

| Symbol | Type | Verification command | Evidence | Verdict |
|--------|------|----------------------|----------|---------|
| `ClassName.fieldName` | Java field | `grep -rn "ClassName" kiteclass-core/src/main/java` | 17 matches in `.../module/foo/` | ✅ exists |
| `BR-FOO-007` | Business rule | `grep -rn "BR-FOO-007" documents/01-business/` | 3 matches in `.../foo/rules.md` | ✅ exists |
| `V42__add_bar.sql` | Migration | `ls kiteclass-core/src/main/resources/db/migration/V42*` | 1 file | ✅ exists |
| `<NewWidget>` | FE component | `grep -rn "NewWidget" kiteclass-frontend/src` | 0 matches | 🆕 to-be-created (Bucket B) |
| `kite.foo.threshold` | Config key | `grep -rn "kite.foo.threshold" kiteclass-core/src/main/resources` | 0 matches | 🆕 to-be-created (Bucket A) |
| `documents/01-business/{domain}/api-contract.md` | API contract doc (cross-layer only) | `ls documents/01-business/{domain}/api-contract.md` | <result> | ✅ exists / 🆕 to-be-created (Bucket 0 Foundation) — only if cross-layer=YES |

Banned shortcuts (mirror §2.5):
- `| head` truncation on grep/find
- Skipping verification "because agents will check at execution"
- Aspirational references without 🆕 flag

### 4.1 Bucket-Completion Check (per `audit-to-gap-pipeline.md` §2.6.1)

For EACH bucket targeting an EXISTING gap (has a CSV row), query `bash scripts/query-gaps.sh <gap-id>` and classify — symbol-exists ≠ work-remaining. Greenfield buckets (no pre-existing gap) skip this table.

| Bucket | Gap | completion_pct (CSV) | Residual (from §Current State) | Verdict |
|--------|-----|:--------------------:|--------------------------------|---------|
| A | GAP-XXX | 0 | full greenfield build | 🆕 Greenfield |
| B | GAP-YYY | 60 | only `<exact residual>` — dependency `<symbol>` already shipped | 🔨 Delta (scope to residual) |
| C | GAP-ZZZ | 90 | live walk only — code 100% shipped | ⚠️ Already-shipped → reframe verify-only / drop |

- 🆕 **Greenfield** — deliverable absent, bucket creates it → scope as-is
- 🔨 **Delta** — symbol present as dependency; scope bucket to the EXACT residual only (cite completion_pct + residual in §3)
- ⚠️ **Already-shipped** — symbol present AND implements the bucket AC → reframe to verify-only (fold into G3 walk), drop, or narrow; correct expected P0-count delta

If any bucket is ⚠️ Already-shipped, revise §3 Scope + correct the wave's expected outcome before merging the plan PR.

---

## 5. Verification Gates (per bucket)

| Bucket | Local verify command | CI gate |
|--------|---------------------|---------|
| A | `pnpm -F kiteclass-frontend test:unit && pnpm -F kiteclass-frontend build` | frontend-ci |
| B | `./mvnw -pl kiteclass-core clean verify -Dcheckstyle.skip=true` | core-ci |

---

## 6. Agent Spawn Pattern

Per `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md`:
- All buckets spawned with `run_in_background: true`
- Worktree isolation (`isolation: worktree`) for parallel safety
- RELATIVE paths in agent prompts per `feedback_worktree_absolute_path_contamination.md`
- Coordinator merges sequentially after all background completions

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `feedback_post_merge_doc_sync.md` + `feedback_wave_history_append_required.md` + `post-wave-cleanup.md` + `feedback_wave_closure_release_progress_report.md`:
- Each bucket PR updates affected GAP file Log + status
- ROADMAP §🚀 Next Action updated in closure PR
- Wave plan frontmatter `status: complete` flip in closure PR
- `wave-history.jsonl` append in closure PR (Rule 15 enforcement)
- Sub-gaps filed for any deferral; PARTIAL exit-ramp per `gap-done-discipline.md` §3
- Run `bash scripts/prune-merged-worktrees.sh --yes` to prune worktree husks + merged branches per `post-wave-cleanup.md` (after all bucket PRs merged, before drafting closure PR)
- **`## Release Plan Progress` section in closure PR body** — per `feedback_wave_closure_release_progress_report.md` rules #1-6: current Phase + milestone progress + wave contribution + trigger gates + estimated remaining wall-clock + **Waves Remaining table** (3 rows: strict-min v0.9.0-beta / practical v0.9.0-beta / v1.0.0 PROD with explicit wave numbers + GAP IDs + PR #s)

---

## 8. Log

- **YYYY-MM-DD** (draft): Plan created.
- **YYYY-MM-DD** (in-progress): Agents spawned.
- **YYYY-MM-DD** (complete): Wave SHIPPED. Outcomes: ...
