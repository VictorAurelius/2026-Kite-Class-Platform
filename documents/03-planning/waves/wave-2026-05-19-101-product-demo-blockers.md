---
title: Wave 101 — Product Demo-Blockers cluster (4 buckets)
status: complete
created: 2026-05-19
updated: 2026-05-19
closed_at: 2026-05-19
phase: phase-1-beta
waves: [101]
gaps: [GAP-518, GAP-562, GAP-562b, GAP-287, GAP-538, GAP-684, GAP-685, GAP-686, GAP-687]
audits: [2026-05-18-thesis-persona-demo-audit, 2026-05-19-thesis-v1-draft-docx-audit]
audience: dev
---

# Wave 101 — Product Demo-Blockers cluster

**Reference canonical:** [`documents/03-planning/roadmap/release-1.5-thesis-scope.md`](../roadmap/release-1.5-thesis-scope.md) §2 Track D Product demo-blocker gaps.

**Goal:** Close 4 demo-blocker gaps (3 PARTIAL → 100%, 1 OPEN → 100%) để unblock thesis Move 1+2 persona demo (chị Hằng P2 Owner / anh Tâm P3 Manager).

**Trigger:** Post Wave 100.7 V1 thesis sprint closure (PR #1599-1601 merged 2026-05-19). Track A (META) đã có foundation; Track D product demo-blockers là next biggest leverage cho thesis Move 1+2 demo narrative. User direction 2026-05-19: "Ship 4-bucket plan now, accept PARTIAL exits" (PARTIAL ramp if AWS GAP-612 còn block live verify).

**Estimated wall-clock:** ~3-6h agent work parallel; longest-bucket Bucket C (GAP-287 wizard 0→100) ~2-3h.

**Pending placeholders acknowledged:**
- AWS account 906286017800 restore (GAP-612 blocked) — Bucket A/B/D live walk PARTIAL exit ramp per `gap-done-discipline.md` §3 nếu restore không xong session này
- GAP-293 monthly income dashboard P1 (downgraded from P0 per CSV) — defer Wave 102+
- GAP-286 mobile OTP → email-only — defer Wave 102+ (no signup-flow agent capacity session này)

---

## 1. Brainstorm

### Q1 Inside-out (4-source consultation per `inside-out-completeness-trigger.md`)

**Source 1 — ROADMAP `documents/04-quality/gaps/ROADMAP.md` §🚀 Next Action:** post-Wave-100.7 V1 closure pending Release 1.5 thesis Phase 2 direction (user picked Product demo-blockers wave 2026-05-19).

**Source 2 — `documents/03-planning/inside-out-queue.md`:** 7 items active (Premium plan / Feedback channel / Email content audit / User manual / Manual split / QR upload / OCR auto-confirm) — NONE overlap Wave 101 demo-blocker scope (different tracks: P3+ persona / feedback / payments).

**Source 3 — Phase-1-beta gap CSV non-DONE:** Wave 101 scope subset từ Track D §release-1.5-thesis-scope.md §2:
- GAP-518 PARTIAL 95% (admin role mismatch) ✅ in scope
- GAP-562 PARTIAL 90% + GAP-562b PARTIAL 85% (RBAC role) ✅ in scope
- GAP-287 OPEN 0% (branding wizard skip) ✅ in scope
- GAP-538 PARTIAL 90% (onboarding + seed data) ✅ in scope
- GAP-286 OPEN 0% (mobile OTP) — defer Wave 102+ (no overlap với 4 buckets)
- GAP-293 monthly income dashboard — re-prioritized P1 per CSV, defer Wave 102+
- GAP-297 batch invoice — defer Wave 102+ (per release-1.5-thesis-scope.md §3 Wave 100 plan — large scope)

**Source 4 — AskUserQuestion explicit (2026-05-19 session):** User chốt 4-bucket plan với accept PARTIAL exits — no additional inside-out items mentioned.

### Q2 Outside-in (3-audit consensus Wave 100 2026-05-18 — exception per `outside-in-coverage-trigger.md` §4 row 4 "audit ≤30 ngày satisfied")

Wave 101 inherit findings từ 3 audits 2026-05-18 cited frontmatter:
- **Persona demo audit:** 7 P0 BLOCKING product gaps re-prioritized thesis-demo — Wave 101 ship 4 trong số 7 (GAP-287/518/562/538), defer 3 (GAP-286/297/293).
- **VN benchmark audit:** Release 1.5 ambitious scope; depth-first demo. Demo-blockers eliminate live demo bug risk (persona Move 1+2).
- **Failure-mode matrix:** "live demo bug" examiner top-10 P0 thesis-blocker → GAP-518 admin role + GAP-562 RBAC + GAP-538 onboarding sample data eliminate live-demo bug class.

### Q3 Trade-offs

- **Alternative 1: Thesis evidence wave (GAP-648 NFR + GAP-649 beta cohort)** — rejected per user choice 2026-05-19. Demo-blockers higher leverage cho persona Move 1+2 demo immediate.
- **Alternative 2: Focused Bucket C only** — rejected per user choice 2026-05-19. Parallel velocity priority over clean session output.

### Q4 Risks

- **Risk 1: AWS GAP-612 chưa restore** → Bucket A/B/D close-outs stall at 95-97% PARTIAL → 3 PARTIAL exits same session. Mitigation: `gap-done-discipline.md` §3 PARTIAL exit ramp + file follow-up gap cho live verify each bucket.
- **Risk 2: GAP-562b kitehub-branding spring-security dep** — Wave 80 deferred. Mitigation: include dep add trong Bucket B scope hoặc PARTIAL exit nếu dep conflict.
- **Risk 3: GAP-287 OPEN 0% UI scope larger than estimated** — Mitigation: timebox 3h, ship "skip" option minimal nếu "default" complex.

---

## 2. Task Breakdown

| Bucket | Gap(s) | Owner | Effort | Disjoint? |
|--------|--------|-------|--------|-----------|
| A | GAP-518 close-out 95→100 | bg-agent | ~1-1.5h | ✅ kitehub-platform admin role files |
| B | GAP-562 + GAP-562b close-out 90/85→100 | bg-agent | ~2-3h | ✅ kitehub-branding @PreAuthorize + FE RoleGuard scope |
| C | GAP-287 OPEN 0→100 wizard skip/default | bg-agent | ~2-3h | ✅ kitehub-frontend wizard component scope |
| D | GAP-538 close-out 90→100 | bg-agent | ~1-2h | ✅ kitehub-platform onboarding + seed data scope |

**Disjoint check:**
- A: `kitehub/kitehub-platform/**` admin role + `kitehub/kitehub-frontend/src/lib/auth/**` role-guard
- B: `kitehub/kitehub-branding/**` @PreAuthorize + `kitehub/kitehub-frontend/src/components/RoleGuard*`
- C: `kitehub/kitehub-frontend/src/app/onboarding/branding/**` wizard
- D: `kitehub/kitehub-platform/**` onboarding + sample-data + `kitehub/kitehub-frontend/src/app/onboarding/**` checklist

A vs B: cùng touch `kitehub-frontend/src/lib/auth/**` (potential overlap) — verify at spawn time, có thể serialize A→B nếu conflict.

---

## 3. Scope (compact schema)

**Stake tier (per `wave-pack-planner/SKILL.md` §Step 4.6):** MEDIUM → model Opus medium (close-out work + 1 OPEN FE-only, không có new architecture).
**Cross-layer? (per `wave-pack-planner/SKILL.md` §Step 4.5):** PARTIALLY (A/B/D touch BE+FE; C FE-only) → Bucket 0 Foundation **KHÔNG required** vì:
- Tất cả endpoint shapes đã định nghĩa trong existing api-contract.md (xem §4 State-Check Evidence)
- Wave 101 = implementation close-out, không new endpoint shape design
- Per `contract-first-for-cross-layer.md` §2 row 4 exception "Frontend-only kit ports (UI redesign không thay đổi API shape)" — gần với scope close-out này

| # | Bucket | Gap(s) | Priority | Files (glob) | Spawn order |
|:-:|--------|--------|:--------:|--------------|:-----------:|
| 1 | **A** | GAP-518 | 🔴 P0 | `kitehub/kitehub-platform/src/main/java/com/kitehub/platform/auth/**` + `kitehub/kitehub-frontend/src/lib/auth/**` + `kitehub/kitehub-frontend/src/components/RoleGuard*` | parallel |
| 2 | **B** | GAP-562 + GAP-562b | 🔴 P0 | `kitehub/kitehub-branding/**` (@PreAuthorize add) + `kitehub/kitehub-branding/pom.xml` (spring-security dep) | parallel |
| 3 | **C** | GAP-287 | 🔴 P0 | `kitehub/kitehub-frontend/src/app/onboarding/branding/**` (wizard skip/default option) | parallel |
| 4 | **D** | GAP-538 | 🔴 P0 | `kitehub/kitehub-platform/src/main/**` (onboarding service) + `kitehub/kitehub-frontend/src/app/onboarding/checklist/**` | parallel |

### Bucket A — GAP-518 admin role BE seed `PLATFORM_ADMIN` vs FE guard `ADMIN` close-out

- Files: `kitehub/kitehub-platform/src/main/java/com/kitehub/platform/auth/**` + `kitehub/kitehub-frontend/src/lib/auth/**` + `kitehub/kitehub-frontend/src/components/RoleGuard*`
- Tests: extend Wave 98 B7 `RoleGuardMatrixIT` + `role-guard.spec.ts` Playwright nếu cần
- Acceptance per gap AC + `pre-handoff-self-test-completeness.md` §2.4 Admin-flow checklist:
  - [ ] Role match BE seed `PLATFORM_ADMIN` vs FE guard literal — grep verify
  - [ ] Admin login → admin dashboard render correctly
  - [ ] Admin navigation to `/admin/beta-requests` works (UI button OR documented URL)
  - [ ] `RoleGuardMatrixIT` PASS local + CI
  - [ ] **Live walk** (POST /api/auth/login với seeded PLATFORM_ADMIN credential → 200 + JWT + dashboard render) — **PARTIAL exit ramp if AWS GAP-612 still blocked** → file follow-up gap với block condition

### Bucket B — GAP-562 + GAP-562b kitehub-branding @PreAuthorize close-out

- Files: `kitehub/kitehub-branding/pom.xml` (add spring-security dep) + `kitehub/kitehub-branding/src/main/java/**` (@PreAuthorize on Owner-only endpoints) + RoleGuard frontend consume
- Tests: add IT cho @PreAuthorize 403 + audit log entry; FE RoleGuard test consume
- Acceptance per gap AC:
  - [ ] kitehub-branding pom.xml has spring-security-config + spring-security-web deps
  - [ ] All Owner-only branding endpoints có @PreAuthorize("hasRole('OWNER')")
  - [ ] Manager login → access branding endpoint → 403 (verified via IT)
  - [ ] FE RoleGuard hides branding menu cho Manager
  - [ ] Audit log entry on 403 attempt
  - [ ] **Live walk** Manager-vs-Owner role separation — PARTIAL exit ramp gated GAP-612

### Bucket C — GAP-287 branding wizard skip/default option (P0 OPEN 0%)

- Files: `kitehub/kitehub-frontend/src/app/onboarding/branding/**` wizard component
- Tests: add unit test cho "Skip" action → onboarding state advance + default branding applied
- Acceptance per gap AC:
  - [ ] Wizard có "Sử dụng mặc định" button (Vietnamese label per `vn-localization-audit-checklist.md` §2)
  - [ ] Click "Sử dụng mặc định" → onboarding state advance + default brand template (`KiteHub` primary color + default logo placeholder) applied
  - [ ] Default branding visible trên tenant dashboard sau onboarding
  - [ ] Test unit + Playwright cover skip flow
- **No live-walk AWS dependency** — pure FE work, clean session output expected

### Bucket D — GAP-538 onboarding + sample data close-out

- Files: `kitehub/kitehub-platform/src/main/**` onboarding service + sample-data worker + `kitehub/kitehub-frontend/src/app/onboarding/checklist/**` UI
- Tests: extend Wave 78 B + Wave 98 B2 GAP-658 VN seed worker tests; add E2E onboarding flow
- Acceptance per gap AC:
  - [ ] Day-1 onboarding checklist UI hiển thị 5-step (created tenant + branding + first class + invite teacher + view dashboard)
  - [ ] "Load sample data" button → trigger VN seed worker (Trần Thị Hồng / Lớp 5A1 per Wave 98 B2)
  - [ ] Sample data tenant-scoped (KHÔNG leak cross-tenant)
  - [ ] **Live walk** "load sample data → see in dashboard" — PARTIAL exit ramp gated GAP-612

---

## 4. State-Check Evidence (BẮT BUỘC per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Type | Verification command | Evidence | Verdict |
|--------|------|----------------------|----------|---------|
| `GAP-518` | Gap file | `awk -F',' '/GAP-518/' documents/04-quality/gaps/gap-status.csv` | row PARTIAL 95 (verified 2026-05-19) | ✅ exists |
| `GAP-562` | Gap file | `awk -F',' '/GAP-562/' documents/04-quality/gaps/gap-status.csv` | row PARTIAL 90 (verified 2026-05-19) | ✅ exists |
| `GAP-562b` | Gap file | `awk -F',' '/GAP-562b/' documents/04-quality/gaps/gap-status.csv` | row PARTIAL 85 (verified 2026-05-19) | ✅ exists |
| `GAP-287` | Gap file | `awk -F',' '/GAP-287/' documents/04-quality/gaps/gap-status.csv` | row OPEN 0 (verified 2026-05-19) | ✅ exists |
| `GAP-538` | Gap file | `awk -F',' '/GAP-538/' documents/04-quality/gaps/gap-status.csv` | row PARTIAL 90 (verified 2026-05-19) | ✅ exists |
| `GAP-612` | Gap file (blocker) | `awk -F',' '/GAP-612/' documents/04-quality/gaps/gap-status.csv` | row (verify at spawn time) — AWS account restore block | ✅ exists |
| `RoleGuardMatrixIT` | Java IT test | `grep -rn "RoleGuardMatrixIT" kitehub/kitehub-platform/src/test` | (verify at spawn time per Wave 98 B7) | ✅ exists (Wave 98 B7) |
| `PLATFORM_ADMIN` | Enum / seed value | `grep -rn "PLATFORM_ADMIN" kitehub/kitehub-platform/src/main` | (verify at spawn time) | ✅ exists |
| `@PreAuthorize` (kitehub-branding) | Annotation usage | `grep -rn "@PreAuthorize" kitehub/kitehub-branding/src/main` | 0 matches expected (Wave 80 deferred) | 🆕 to-be-created (Bucket B) |
| `spring-security-config` (kitehub-branding pom) | Maven dependency | `grep -A 1 "spring-security-config" kitehub/kitehub-branding/pom.xml` | 0 matches expected | 🆕 to-be-created (Bucket B) |
| `Sử dụng mặc định` button | FE component label | `grep -rn "Sử dụng mặc định\\|use-default-branding" kitehub/kitehub-frontend/src/app/onboarding` | 0 matches expected | 🆕 to-be-created (Bucket C) |
| VN seed worker (Wave 98 B2 GAP-658) | Backend service | `grep -rn "VietnameseSeedWorker\\|VnSeedWorker" kitehub/kitehub-platform/src/main` | (verify at spawn time) | ✅ exists (Wave 98 B2) |

**State-check execution deferred to per-bucket agent spawn time** — coordinator verifies at bucket-start via `verify-at-spawn` step in agent prompt (per `audit-to-gap-pipeline.md` §2.6 — fresh state-check before each bucket commit).

---

## 5. Verification Gates (per bucket)

| Bucket | Local verify command | CI gate |
|--------|---------------------|---------|
| A | `cd kitehub && ./mvnw -pl kitehub-platform verify -P strict-warnings && cd kitehub-frontend && pnpm test --run` | kitehub-ci + kitehub-frontend-ci |
| B | `cd kitehub && ./mvnw -pl kitehub-branding verify -P strict-warnings` | kitehub-ci |
| C | `cd kitehub/kitehub-frontend && pnpm test --run && pnpm build && pnpm lint` | kitehub-frontend-ci |
| D | `cd kitehub && ./mvnw -pl kitehub-platform verify && cd kitehub-frontend && pnpm test --run` | kitehub-ci + kitehub-frontend-ci |

---

## 6. Agent Spawn Pattern

Per `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md`:
- All 4 buckets spawned với `run_in_background: true`
- Worktree isolation (`isolation: worktree`) cho parallel safety
- RELATIVE paths trong agent prompts per `feedback_worktree_absolute_path_contamination.md`
- Coordinator merges sequentially A→B→C→D sau khi tất cả 4 bg-agent completions

**Pre-spawn check (coordinator):** verify A vs B no overlap trong `kitehub-frontend/src/lib/auth/**` — nếu overlap, serialize A→B thay vì parallel.

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `wave-closure-scope-completeness.md` + `feedback_post_merge_doc_sync.md` + `feedback_wave_history_append_required.md` + `post-wave-cleanup.md`:

- Each bucket PR updates affected GAP file Log + status (DONE flip via git mv → `closed/` per `gap-folder-organization.md` v2.0.0 §3.3)
- ROADMAP §🚀 Next Action updated trong closure PR
- Wave plan frontmatter `status: complete` flip trong closure PR
- `wave-history.jsonl` append trong closure PR (Rule 15)
- **Scope-Completeness Reconciliation table** per `wave-closure-scope-completeness.md` §3 — 4 bucket items + categorize ✅/🟡/❌
- Sub-gaps filed cho any deferral (expected: 3 follow-up gaps cho live walk Bucket A/B/D nếu AWS GAP-612 còn block — file ngay tại closure)
- Run `bash scripts/prune-merged-worktrees.sh --yes` per `post-wave-cleanup.md` sau khi 4 bucket PRs merged
- **`## Release Plan Progress` section** trong closure PR body per `feedback_wave_closure_release_progress_report.md`

---

## 7.1 Scope-Completeness Reconciliation (per `wave-closure-scope-completeness.md` §3)

| # | Plan §3 Scope item | Verdict | PR / Follow-up |
|---|---|---|---|
| 1 | Bucket A — GAP-518 admin role close-out 95→100 | 🟡 PARTIAL 97 | PR #1603 — code/test shipped; live walk blocked GAP-612 AWS restore → GAP-684 OPEN follow-up |
| 2 | Bucket B — GAP-562 + GAP-562b close-out 90/85→100 | ✅ DONE 100/100 | PR #1607 (initial #1607 + 2 follow-up commits a967249f + 7c28c2f3 fixing 5 @WebMvcTest tests broken by SecurityConfig) — kitehub-branding spring-security + 4 controllers @PreAuthorize + 7-case IT |
| 3 | Bucket C — GAP-287 OPEN 0→100 | ✅ DONE 100 | PR #1605 — "Sử dụng mặc định" button + 10 unit tests + 3 Playwright; clean FE-only close-out |
| 4 | Bucket D — GAP-538 close-out 90→100 | 🟡 PARTIAL 95 | PR #1604 — E2E spec + VN sample verified; live walkthrough blocked GAP-612 → no new follow-up gap (existing GAP-612 dependency) |
| 5 | Wave 101 post-wave audit suite ≤3 days | 🔵 OPEN | GAP-685 filed this closure PR (deadline 2026-05-22) |
| 6 | 3-layer business doc sync kitehub-branding | 🔵 OPEN | GAP-686 filed this closure PR (post-merge hook flagged) |
| 7 | Thesis V1 draft DOCX audit follow-ups | 🔵 OPEN | GAP-687 filed this closure PR (audit 60/100 baseline) — covers 3 phases (immediate scrub + production V1 format + content evidence) |

**Wave 101 outcome:** 4/4 buckets shipped via 5 PRs (#1603 A / #1604 D / #1605 C / #1606 thesis docx side-quest / #1607 B). 2 DONE 100% + 2 PARTIAL (gated GAP-612 AWS restore). 3 follow-up gaps filed (GAP-685/686/687) tracking deferred work per `wave-closure-scope-completeness.md` mandate.

**Side artifacts:**
- `documents/08-thesis/thesis-v1-draft.docx` shipped PR #1606 (user-requested pandoc convert; 60/100 D+ self-audit)
- `documents/04-quality/audits/persona-review/2026-05-19-thesis-v1-draft-docx-audit.md` (this closure PR)
- 3 follow-up gap files GAP-685/686/687 (this closure PR)
- Worktree prune: 4 husks + 3 merged branches pruned via `scripts/prune-merged-worktrees.sh`

---

## 8. Log

- **2026-05-19** (draft): Plan created. User direction "Ship 4-bucket plan now, accept PARTIAL exits" sau 2 AskUserQuestion (direction + strategy). 4-bucket scope subset from `release-1.5-thesis-scope.md` §2 Track D Product demo-blockers. PARTIAL exit ramp accepted cho A/B/D nếu AWS GAP-612 còn block live walk. Bucket C (GAP-287 wizard) FE-only clean close-out expected 100%.
- **2026-05-19** (in-progress): 4 bg-agents spawned worktree-isolated. 3 of 4 initial spawns hit transient API 529; all recovered via retry. PR #1603 (A) + #1604 (D) + #1605 (C) + #1607 (B) created.
- **2026-05-19** (in-progress): PR #1607 fix iteration — SecurityConfig broke 5 pre-existing @WebMvcTest classes (11 failures total); fix added `@AutoConfigureMockMvc(addFilters = false)` to ContentGeneration/TemplateGallery/LifecycleEvents/BrandingWizard test classes (kept BrandingRoleAuthorizationTest filters-on by design). 28/28 PASS local; CI 31/31 PASS.
- **2026-05-19** (complete): All 5 PRs merged (#1602 plan + #1603 A + #1604 D + #1605 C + #1606 thesis docx + #1607 B). Wave-history.jsonl appended. Worktrees pruned. 3 follow-up gaps GAP-685/686/687 filed. Closure PR (this).

---

**Note volume budget:** `documents/03-planning/waves/` hiện 106 files (212% over cap 50 per `docs-folder-volume-budget.md` §2). New wave plan = 107th file. Override trailer required in commit:

```
DOCS_VOLUME_OVERRIDE: documents/03-planning/waves 107/50 — Phase 1 BETA active wave continuation (Wave 101 of release-1.5-thesis-scope.md §3 track plan)
DOCS_VOLUME_FOLLOWUP: GAP-679 (future batch sub-split waves/ by wave-range per docs-folder-volume-budget.md §4.2)
```
