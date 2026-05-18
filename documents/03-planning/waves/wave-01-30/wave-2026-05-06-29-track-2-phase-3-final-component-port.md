---
title: Wave 29 — Track 2 Phase 3 final — port last 4 G* (G1 + G9 + G11 + G12)
status: complete
created: 2026-05-06
updated: 2026-05-06
waves: [29]
gaps: [GAP-273]
---

# Wave 29 — Track 2 Phase 3 final component port (4 buckets)

**Goal:** Port last 4 G* components → `@kite/shared-ui`. **Closes G* portion of GAP-273** (12/12 G* shipped post-wave). Fully unblocks GAP-270 (KH pro v2) + GAP-272 (ai-branding-wizard v2) for Phase 4 kit ports.
**Trigger:** Wave 28 plan §7 closure roadmap recommends Wave 29 = remaining 4 G*. Drafted pipelined per `feedback_pipelined_wave_planning.md` (auto-loaded) + `wave-pack-planner` SKILL §Step 5.5 — coordinator drafts Wave 29 plan while Wave 28 agents run background. 0 dead-time between waves.
**Estimated wall-clock:** ~10-15 min/agent parallel (Wave 27/28 retro: ~7-8 min/agent for full G* port; G11 reflexive WCAG demo + G1 file-upload likely longest at ~12-15 min). Total session ~45-60 min including coordinator merge + closure.

---

## 1. Brainstorm (5-10 min)

**Q1 (alignment):**
- Personas: P2 Owner SaaS (G9 + G11 = KH pro dashboard surface), Admin (G1 + G12 = bulk-import + bulk-actions for K-12 admin GAP-271 Phase 3 scope), Wizard tenant (G11 used by AI Branding wizard).
- Domains: KH SaaS lifecycle management (G9), KH theme/branding live preview (G11), K-12 admin bulk operations (G1 + G12).
- Phase 4 kit-port unblock impact (after Wave 29 closure):
  - GAP-266 KC pro v2: no G* dependencies — already unblocked
  - GAP-267 parent kit (P5 K-12): G7✅ + G6✅ — already unblocked post-Wave-27
  - GAP-268 teacher kit: G2✅ + G3 + G4 + G8 — fully unblocked post-Wave-28
  - GAP-269 student kit: G6✅ + G8 + G10 — fully unblocked post-Wave-28
  - GAP-270 KH pro v2: G9 + G10 + G11 — **fully unblocked post-Wave-29** ✅
  - GAP-271 K-12 admin: G1 + G3 + G4 + G8 + G10 — **fully unblocked post-Wave-29** ✅
  - GAP-272 ai-branding-wizard: G11 — **fully unblocked post-Wave-29** ✅
- All 7 Phase 4 kit ports become unblocked simultaneously after Wave 29 ships.

**Q2 (trade-offs):**
- 4 buckets vs include D* dialogs: chose 4 G* only. D* (D2..D10 from `dossier/12-modal-dialog-inventory-{kc,kh}.md`) lacks formal specs/protos — would need spec creation Phase 0 each. Ship clean G* completion Wave 29; D* dialogs = Wave 30+ scope (state-check + design specs first).
- Pipelined plan PR drafted while Wave 28 agents run: per skill §Step 5.5 + `feedback_pipelined_wave_planning.md`. First time applying pipeline pattern formally; demonstrates the new workflow.
- G11 reflexive WCAG coverage: spec lists "WCAG fail demonstration + auto-suggested fixes" — meta-level test (component must SHOW its own contrast violations). Bucket C briefing makes this explicit.
- G1 file upload UX: Round 3 spec lists 5 states (idle/drag-over/parsing/partial-success/done). Real file upload = browser File API. Tests use mocked File objects.

**Q3 (risks):**
- **R1: `packages/shared-ui/src/index.ts` merge conflict.** All 4 buckets append exports. Wave 27 + Wave 28 patterns established Python concatenation script — coordinator reuses. Predicted 4 conflicts, all additive, alphabetical resolution.
- **R2: Wave 28 still in-flight when Wave 29 plan drafts.** Wave 28 buckets touch G3/G4/G8/G10/D1 component dirs + same `index.ts` + Bucket E touches `package.json`. Wave 29 plan PR is docs-only (`documents/03-planning/waves/wave-29...md` only) — file-disjoint from Wave 28 agents.
- **R3: Coordinator context compounding.** Currently ~150-160k post-Wave-28-spawn. Drafting Wave 29 plan adds ~30-40k. Wave 28 agent completion notifications add ~30-50k. Coordinator merge resolution adds ~30-50k. Total trajectory: ~250-300k by closure of Wave 28. Risk: hitting Opus 1M context budget late session. **Mitigation:** ship Wave 29 plan PR quickly, don't merge yet (per `feedback_wave_plan_through_pr.md`); after Wave 28 closure, decide if Wave 29 spawns this session or next session based on remaining budget.
- **R4: G11 reflexive WCAG complexity.** Component must demonstrate fail + auto-fix. Heaviest UI logic in Wave 29. **Mitigation:** allow PARTIAL ship if core preview + WCAG measurement works + auto-fix deferred.
- **R5: G1 file upload mocking.** Tests must mock browser `File` + `FileReader`. **Mitigation:** brief explicit; jsdom supports basic File mocking; agent flags if test setup blocked.

---

## 2. Task Breakdown

| Bucket | Component | Owner | Effort | Disjoint? |
|--------|-----------|-------|--------|-----------|
| A | G1 Bulk Import Drop-zone + Job Tracker | bg-agent | ~12-15 min | ✅ `packages/shared-ui/src/components/G1-bulk-import-dropzone/` |
| B | G9 Instance Lifecycle Status | bg-agent | ~10-12 min | ✅ `packages/shared-ui/src/components/G9-instance-lifecycle/` |
| C | G11 Theme Customization Live Preview | bg-agent | ~12-15 min | ✅ `packages/shared-ui/src/components/G11-theme-preview/` |
| D | G12 Student List with Bulk Actions | bg-agent | ~10-12 min | ✅ `packages/shared-ui/src/components/G12-bulk-actions-bar/` |

**Disjoint check:** each bucket touches only its component subfolder. Shared file `packages/shared-ui/src/index.ts` modified additively — coordinator resolves alphabetical (G1 → G9 → G11 → G12 placement after G10).

---

## 3. Scope (per bucket)

### Bucket A — G1 Bulk Import Drop-zone + Job Tracker (file upload + parse + commit)

- **Spec source:** `dossier/04-component-gaps.md` §G1 (line 25) + `ui_kits/components/G1-bulk-import-dropzone/README.md` (109 lines) + `states/*.html` (5 files: idle, drag-over, parsing, partial-success, done — Round 3 layout).
- **Files to create:**
  - `packages/shared-ui/src/components/G1-bulk-import-dropzone/BulkImportDropzone.tsx`
  - `packages/shared-ui/src/components/G1-bulk-import-dropzone/index.tsx`
  - `packages/shared-ui/src/components/G1-bulk-import-dropzone/types.ts` — `ImportJobStatus = 'idle' | 'drag-over' | 'parsing' | 'partial-success' | 'done' | 'error'`, `ImportRow`, `ImportError`, `BulkImportDropzoneProps`, `JobProgress`
  - `packages/shared-ui/src/components/G1-bulk-import-dropzone/utils.ts` — `parseCSV(text: string): { rows: ImportRow[]; errors: ImportError[] }`, `validateRow(row: ImportRow, schema: 'students'): { valid: boolean; errors: string[] }`
  - `packages/shared-ui/src/components/G1-bulk-import-dropzone/__tests__/BulkImportDropzone.test.tsx`
  - `packages/shared-ui/src/components/G1-bulk-import-dropzone/__tests__/utils.test.ts` — CSV parse edge cases (UTF-8 BOM, Vietnamese names, comma-in-quoted-field, empty rows) + row validation (phone format `0\d{9,10}`, date `dd/mm/yyyy`)
  - `packages/shared-ui/src/components/G1-bulk-import-dropzone/spec.md`
- **Tests:** ≥9 — 5 state renders + drag-over interaction + file selection + parse-success path + parse-error path + commit-progress visible
- **`src/index.ts` exports:** `BulkImportDropzone`, `BulkImportDropzoneProps`, `ImportJobStatus`, `ImportRow`, `ImportError`, `JobProgress`, `parseCSV`, `validateRow`
- **Acceptance:** ≤5 MB file limit, ≤10k rows display warning, drag-drop + click-to-browse, sample file CTA (`Tải file mẫu (.xlsx)`), validation errors localized (`Dòng 23: Số điện thoại không hợp lệ`), partial-success download CTA, batch insert progress 500/txn step.

### Bucket B — G9 Instance Lifecycle Status (6-state machine per ai-branding-guidelines)

- **Spec source:** dossier §G9 (line 120) + `G9-instance-lifecycle/README.md` (100 lines) + `states/*.html` (6 files matching 6 lifecycle states) + `ai-branding-guidelines.md` §6 Lifecycle State Machine.
- **Files to create:**
  - `.../G9-instance-lifecycle/InstanceLifecycleStatus.tsx`
  - `.../G9-instance-lifecycle/index.tsx`
  - `.../G9-instance-lifecycle/types.ts` — `InstanceLifecycleState = 'NOT_STARTED' | 'INITIALIZING' | 'GENERATING' | 'DEPLOYED' | 'REGENERATING' | 'FAILED'`, `LifecycleEvent`, `InstanceLifecycleStatusProps`
  - `.../G9-instance-lifecycle/utils.ts` — `validTransition(from: InstanceLifecycleState, to: InstanceLifecycleState): boolean` (per §6 state machine: NOT_STARTED → INITIALIZING → GENERATING → DEPLOYED ⇄ REGENERATING; any → FAILED)
  - `.../G9-instance-lifecycle/__tests__/InstanceLifecycleStatus.test.tsx`
  - `.../G9-instance-lifecycle/__tests__/utils.test.ts` — state machine valid + invalid transitions
  - `.../G9-instance-lifecycle/spec.md`
- **Tests:** ≥9 — 6 state renders + valid transitions (NOT_STARTED→INITIALIZING ✓, GENERATING→DEPLOYED ✓, DEPLOYED→REGENERATING ✓) + invalid (NOT_STARTED→DEPLOYED ✗, FAILED→DEPLOYED ✗ direct, FAILED→GENERATING ✓ via retry)
- **`src/index.ts` exports:** `InstanceLifecycleStatus`, `InstanceLifecycleStatusProps`, `InstanceLifecycleState`, `LifecycleEvent`, `validTransition`
- **Acceptance:** state machine matches `ai-branding-guidelines.md` §6 verbatim, status badge with VN labels (`Chưa khởi tạo`/`Đang khởi tạo`/`Đang tạo`/`Đã triển khai`/`Đang tạo lại`/`Lỗi`), retry CTA only when state=FAILED + lastSuccessful=GENERATING (not DEPLOYED), event timeline scroll showing transitions.

### Bucket C — G11 Theme Customization Live Preview (reflexive WCAG)

- **Spec source:** dossier §G11 (line 142) + `G11-theme-preview/README.md` (110 lines) + `states/*.html` (5 files).
- **Files to create:**
  - `.../G11-theme-preview/ThemePreview.tsx`
  - `.../G11-theme-preview/index.tsx`
  - `.../G11-theme-preview/types.ts` — `ThemeMode = 'light' | 'dark'`, `BrandColors`, `ContrastWarning`, `ThemePreviewProps`
  - `.../G11-theme-preview/utils.ts` — `calculateContrast(fg: string, bg: string): number` (WCAG luminance formula), `suggestFix(brandColors: BrandColors): { bg: string; fg: string; reason: string }` (auto-fix to AA 4.5:1)
  - `.../G11-theme-preview/__tests__/ThemePreview.test.tsx`
  - `.../G11-theme-preview/__tests__/utils.test.ts` — contrast calc edge cases (white-on-white = 1, black-on-white = 21, mid-gray pairs) + suggestFix produces AA-compliant pairs
  - `.../G11-theme-preview/spec.md`
- **Tests:** ≥10 — 5 state renders + light/dark toggle + WCAG fail demonstration (low-contrast input → warning visible) + auto-fix suggestion applied → recalc pass + brand color picker integration smoke test
- **`src/index.ts` exports:** `ThemePreview`, `ThemePreviewProps`, `ThemeMode`, `BrandColors`, `ContrastWarning`, `calculateContrast`, `suggestFix`
- **Acceptance:** light + dark mode toggle with smooth transition, WCAG warning badge appears on contrast <4.5:1, auto-fix CTA suggests AA-compliant alternative + applies on click, before/after preview side-by-side, Vietnamese labels (`Xem trước giao diện`, `Cảnh báo độ tương phản`, `Tự động sửa`).
- **Reflexive coverage** (per dossier §G11): the component must demonstrate WCAG fail + auto-fix on its OWN preview surface. Tests must verify the warning fires when input colors fail AA.

### Bucket D — G12 Student List with Bulk Actions (sticky multi-select bar)

- **Spec source:** dossier §G12 (line 154) + `G12-bulk-actions-bar/spec.md` (111 lines, **Round 2 layout** — has root spec.md + 5 root HTML state files like Wave 27 components).
- **Files to create:**
  - `.../G12-bulk-actions-bar/BulkActionsBar.tsx`
  - `.../G12-bulk-actions-bar/index.tsx`
  - `.../G12-bulk-actions-bar/types.ts` — `BulkAction = 'EXPORT_CSV' | 'ARCHIVE' | 'ASSIGN' | 'DELETE'`, `BulkActionsBarProps`, `SelectedCount`
  - `.../G12-bulk-actions-bar/__tests__/BulkActionsBar.test.tsx`
  - `.../G12-bulk-actions-bar/spec.md` (mirror)
- **Tests:** ≥7 — 5 state renders + multi-select count display + each action callback fires + sticky positioning verified (CSS class assertion)
- **`src/index.ts` exports:** `BulkActionsBar`, `BulkActionsBarProps`, `BulkAction`, `SelectedCount`
- **Acceptance:** sticky-position bottom or top (configurable), multi-select count `Đã chọn N` localized, 4 actions with VN labels (`Xuất CSV`, `Lưu trữ`, `Phân lớp`, `Xóa`), destructive action (`Xóa`) shows ConfirmDialog (use Wave 28 D1 ConfirmDialog from `@kite/shared-ui` — cross-component re-use). If D1 not yet available at Wave 29 spawn (Wave 28 still in-flight), use placeholder `<button>` + comment `TODO: integrate ConfirmDialog after Wave 28 ships`.

**Round 2 layout note:** G12 spec.md exists at root (Wave 27 pattern). Other Wave 29 buckets (A/B/C) use Round 3 layout (README + states/). Bucket D's agent briefing reflects Round 2 source.

---

## 4. State-Check Evidence (BẮT BUỘC per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Type | Verification command | Evidence | Verdict |
|--------|------|----------------------|----------|---------|
| `packages/shared-ui/` workspace | pnpm package | `cat packages/shared-ui/package.json` | post-Wave-27/28 — `@kite/shared-ui` with G2/G5/G6/G7 + (Wave 28 in-flight: G3/G4/G8/G10/D1) | ✅ exists |
| Wave 27 + 28 reference patterns | shipped components | `ls packages/shared-ui/src/components/` | 4 dirs from Wave 27 (post-merge); Wave 28 adds 5 more (in-flight) | ✅ exists / 🔄 in-flight |
| `dossier/04-component-gaps.md` §G1/G9/G11/G12 | dossier entries | `grep -nE "^### G(1\|9\|11\|12)\\." documents/02-architecture/design-system/dossier/04-component-gaps.md` | lines 25, 120, 142, 154 | ✅ exists |
| `ui_kits/components/G1-bulk-import-dropzone/README.md` | Round 3 spec | `wc -l` | 109 lines | ✅ exists |
| `ui_kits/components/G9-instance-lifecycle/README.md` | Round 3 spec | `wc -l` | 100 lines | ✅ exists |
| `ui_kits/components/G11-theme-preview/README.md` | Round 3 spec | `wc -l` | 110 lines | ✅ exists |
| `ui_kits/components/G12-bulk-actions-bar/spec.md` | **Round 2** spec | `wc -l` | 111 lines | ✅ exists (Round 2 — different layout from buckets A/B/C) |
| `ui_kits/components/G[1,9,11]-*/states/` subfolders | HTML state files | `ls .../states/*.html` | 5/6/5 files respectively | ✅ exists |
| `ui_kits/components/G12-bulk-actions-bar/{default,loading,empty,error,success}.html` | Round 2 state files | `ls` | 5 files (root level, not states/ subfolder) | ✅ exists |
| `ai-branding-guidelines.md` §6 Lifecycle State Machine | architecture rule | `grep -nE "^## 6\.|6-state" .claude/rules/ai-branding-guidelines.md` | §6 establishes state machine NOT_STARTED → INITIALIZING → GENERATING → DEPLOYED ⇄ REGENERATING + FAILED | ✅ exists (Bucket B references) |
| `ConfirmDialog` from `@kite/shared-ui` | Wave 28 Bucket E (in-flight) | `gh pr view <wave-28-E>` | Wave 28 Bucket E shipping `ConfirmDialog` to shared-ui | 🔄 in-flight (Bucket D Wave 29 has fallback if not ready) |
| `BulkImportDropzone.tsx` | React component | `grep -rn "BulkImportDropzone" packages/shared-ui/src/` | 0 matches | 🆕 to-be-created (Bucket A) |
| `InstanceLifecycleStatus.tsx` | React component | `grep -rn "InstanceLifecycleStatus" packages/shared-ui/src/` | 0 matches | 🆕 to-be-created (Bucket B) |
| `ThemePreview.tsx` | React component | `grep -rn "ThemePreview" packages/shared-ui/src/` | 0 matches | 🆕 to-be-created (Bucket C) |
| `BulkActionsBar.tsx` | React component | `grep -rn "BulkActionsBar" packages/shared-ui/src/` | 0 matches | 🆕 to-be-created (Bucket D) |
| `parseCSV` / `validateRow` / `validTransition` / `calculateContrast` / `suggestFix` | utility functions | grep across `packages/shared-ui/src/` | 0 matches each | 🆕 to-be-created (per bucket) |

**Wave 28 in-flight dependency note:** Bucket D references `ConfirmDialog` from Wave 28 Bucket E. Wave 29 plan PR drafts during Wave 28 agent run; spawn Wave 29 agents AFTER Wave 28 closure ships (per `feedback_wave_plan_through_pr.md` + skill §Step 5.5 constraints). At Wave 29 spawn time, D1 will be available. If Wave 29 spawns BEFORE Wave 28 closure (unlikely per workflow), Bucket D agent uses placeholder `<button>` + TODO comment.

---

## 5. Verification Gates (per bucket)

| Bucket | Local verify command | Notes |
|--------|---------------------|-------|
| A | `pnpm -F @kite/shared-ui type-check && pnpm -F @kite/shared-ui test -- BulkImportDropzone` | jsdom supports basic File mocking; flag if blocked |
| B | `pnpm -F @kite/shared-ui type-check && pnpm -F @kite/shared-ui test -- InstanceLifecycleStatus` | verify state machine matches `ai-branding-guidelines.md` §6 verbatim |
| C | `pnpm -F @kite/shared-ui type-check && pnpm -F @kite/shared-ui test -- ThemePreview` | reflexive WCAG: tests verify warning fires on bad contrast input |
| D | `pnpm -F @kite/shared-ui type-check && pnpm -F @kite/shared-ui test -- BulkActionsBar` | conditionally consumes Wave 28 D1 ConfirmDialog if available |

Coordinator at closure: full suite must show Wave 28 baseline + ~30-40 new tests = ~180-200+ total.

---

## 6. Agent Spawn Pattern

Per `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md` + Wave 27/28 lessons:
- All 4 buckets spawned with `run_in_background: true`
- Worktree isolation (`isolation: worktree`) for parallel safety
- RELATIVE paths in agent prompts per `feedback_worktree_absolute_path_contamination.md`
- Each agent ships 1 PR (4 PRs total)
- Coordinator merges sequentially A→B→C→D after all 4 background completions
- `src/index.ts` conflicts resolved by coordinator (alphabetical: G1 → G9 → G11 → G12 placement after Wave 28's G3/G4/G8/G10)

**Spawn timing:** Wave 29 plan PR drafted DURING Wave 28 agent run (pipelined per skill §Step 5.5). Wave 29 spawn happens AFTER Wave 28 closure ships (per `feedback_wave_plan_through_pr.md` + token-budget caution). If coordinator hits ~250k+ context post-Wave-28-closure, defer Wave 29 spawn to next session per `feedback_token_quota_spawn_timing.md`.

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `feedback_post_merge_doc_sync.md` + `feedback_wave_history_append_required.md` + `post-wave-cleanup.md`:

- Each bucket PR updates `GAP-273` Log.
- **Status flip eligible:** GAP-273 = 12/12 G* shipped post-Wave-29 → can flip 🟡 PARTIAL → 🟢 DONE for **G* portion**. BUT umbrella GAP-273 also lists ~10 D* dialogs as scope; if strict reading "ALL G* + D*" required → stays PARTIAL until D* shipped (Wave 30+). Coordinator decides at closure based on gap acceptance criteria reading.
- ROADMAP §🚀 Next Action updated in closure PR — recommend Wave 30 candidates (D2..D10 dialogs design + port OR Phase 4 kit ports start since all G* unblock complete).
- Wave plan frontmatter `status: complete` flip in closure PR.
- `wave-history.jsonl` append in closure PR (Rule 15).
- Run `bash scripts/prune-merged-worktrees.sh --yes` after all 4 bucket PRs merged + before drafting closure PR.

**Follow-up gaps to file at closure:**
- **D2..D10 dialog inventory port** — design specs first (state-check Phase 0), then port ~9 dialogs from `dossier/12-modal-dialog-inventory-{kc,kh}.md`.
- **Phase 4 kit ports start** — all 7 kit gaps (GAP-266..272) unblocked; can begin first kit (GAP-268 teacher OR GAP-267 parent recommended).
- Carry-overs from Waves 27/28: shared-ui CI workflow, cross-app smoke test demo route, visual regression baseline, D1 callsite migration.

---

## 8. Log

- **2026-05-06 (draft):** Plan created PIPELINED during Wave 28 agents running background — first formal application of `wave-pack-planner` SKILL §Step 5.5 (added v1.1 same session). Demonstrates the workflow: coordinator drafts Wave N+1 plan PR while Wave N agents in-flight, plan PR merges only AFTER Wave N closure ships. State-check verified all 4 G* + dossier 04 entries + Round 2/3 layout differences (G12 = Round 2 spec.md; G1/G9/G11 = Round 3 README.md + states/). Wave 28 in-flight dependency: Bucket D Wave 29 references Wave 28 Bucket E's `ConfirmDialog` — agent has fallback if not yet available. Token budget caution: if coordinator hits ~250k+ post-Wave-28-closure, defer Wave 29 spawn to next session per `feedback_token_quota_spawn_timing.md`.
- **2026-05-06 (SHIPPED):** All 4 buckets shipped + merged. PR #867 G1 BulkImportDropzone (31 tests), PR #864 G9 InstanceLifecycleStatus (28 tests), PR #865 G11 ThemePreview (23 tests, reflexive WCAG), PR #866 G12 BulkActionsBar (15 tests, D1 cross-component re-use). Coordinator merge order A → B → C → D as planned; 2 conflicts on `src/index.ts` (C and D, predicted) resolved via rebase keeping all G* exports alphabetical. Final shared-ui state: 12/12 G* shipped (Wave 27 = G2/G5/G6/G7, Wave 28 = G3/G4/G8/G10, Wave 29 = G1/G9/G11/G12) + D1 ConfirmDialog + ConsentBanner. Total tests post-merge: ~265+ (241 from Bucket A + Wave 29 baseline). 65th consecutive 0-clarification streak (4 agents 0-clarif). Wall-clock: ~7-12 min/agent parallel; coordinator merge resolution ~10 min including 2 rebases. Post-wave cleanup ran (4 worktree husks pruned). GAP-273 stays 🟡 PARTIAL — G* portion 12/12 done but Storybook/demo route + production ≥105/128 verification + visual regression baseline + D2..D10 dialogs remain. Phase 4 kit ports (GAP-266..272) fully unblocked from G* standpoint. Domain-milestone audit deferred per `post-wave-audit-mandate.md` §2.4 trailer `AUDIT_DEFER_DOMAIN_MILESTONE: track-2-shared-ui` (cluster milestone TBD when Storybook + visual regression land).
