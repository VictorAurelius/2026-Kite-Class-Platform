---
title: Wave 50 — Track 2 Phase 4 KH Kits (kh-admin K-12 Principal + ai-branding-wizard v2)
status: complete
created: 2026-05-10
updated: 2026-05-10
waves: [50]
gaps: [GAP-271, GAP-272]
parent_umbrella: documents/03-planning/waves/wave-track-2-ui-kits-port-umbrella.md
phase_reference: Phase 4 (Track 2)
depends_on: Wave 49 closure (no file conflict but logical sequencing — KH kits ship after KC personas to keep Phase 4 batched)
---

# Wave 50 — Track 2 Phase 4 KH Kits

**Goal:** Ship 2 production Next.js port của 2 KH kit cuối — kh-admin (P5 K-12 School Principal, 12 dense-desktop screens) + ai-branding-wizard v2 (Direction C 6-step wizard, 28 screens) — đóng 2/2 cổng KH cuối của Phase 4 Track 2.
**Trigger:** User chọn parallel work song song với Wave 49 (3 KC persona đang chạy ~6-8h). Wave 50 plan PR draft trong khi Wave 49 chạy; ship execution sau khi Wave 49 đóng. Lý do: 2 KH kit không share infra với 3 KC kit, nhưng spawn cùng lúc sẽ vượt max-cap 5 agent/wave + risk Vercel preview build queue contention. Tách wave để giữ kỷ luật max-cap 5.
**Estimated wall-clock:** ~7-9h longest path (Bucket B ai-branding-wizard nặng hơn vì 28 screens vs Bucket A 12 screens). 2 agent parallel.

---

## 1. Brainstorm (5-10 min)

**Q1 (alignment):**
- 2 persona: P5 K-12 School Principal (Bucket A — Tier 1 USER PRIORITY per umbrella plan) + Owner provisioning flow (Bucket B — applies to ALL paid tiers)
- Đóng 2/2 cổng KH cuối của Phase 4 → sau Wave 49 + Wave 50, Phase 4 đóng 5/7 DONE + 2/7 PARTIAL (Wave 30/31 follow-ups)
- ai-branding-wizard v2 là kit ⭐⭐ HIGHEST R2 (115.6/128) — chuẩn hoá Direction C wizard pattern per `ai-branding-guidelines.md` §2.4 — direct contribution Phase 1 BETA §3.6 row #7 "AI Branding minimum: logo upload + color theme picker functional"
- kh-admin port mở khoá P5 K-12 Principal — gates Phase 3 K-12 launch (sau khi counsel review per CLAUDE.md Phase 3 trigger)

**Q2 (trade-offs):**
- **Đã xét:** spawn 2 KH agent SONG SONG với 3 KC agent Wave 49 → REJECT vì 5 concurrent vẫn ngay max-cap rule #9 (`feedback_parallel_agent_strategy.md`); Vercel preview build queue contention (kh-frontend + kc-frontend cùng deploy preview); coordinator load tăng 2× với 5 completion notification
- **Đã xét:** wait until Wave 49 đóng rồi mới draft Wave 50 plan → REJECT vì làm chậm critical path Track 2; doc-only plan PR không conflict với Wave 49 execution
- **Đã xét:** gộp Wave 49 + Wave 50 thành 1 wave 5-bucket → REJECT vì vượt max-cap 5/wave (rule #9) + scope quá lớn không retro-able
- **Chọn:** Wave 50 plan PR draft NGAY (parallel với Wave 49 execution); ship execution sau khi Wave 49 đóng
- **Đã xét:** thêm Bucket 0 Foundation cho KH (kh-frontend PWA infra) → REJECT vì kh-admin desktop-only + ai-branding-wizard customer Owner desktop-first, không cần PWA mobile; nếu future GAP-275 KH marketing PWA cần, tách wave riêng
- **Đã xét:** đợi đóng follow-up GAP-266b/c/d + GAP-270b/c PARTIAL trước khi mở Wave 50 → REJECT vì 2 việc khác scope (Storybook + visual regression baseline khác kit port logic); batch riêng wave sau

**Q3 (rủi ro):**
- **R1 — kh-admin route group decision**: 12 K-12 screens NÊN nằm ở `(admin)/school-admin/**` (extend existing) HAY new `(school-admin)/**` route group? `(admin)/admin/**` hiện covers KiteHub PLATFORM admin (beta-requests, instances, payments, revenue) — KHÁC scope K-12 Principal hoàn toàn. → AC: chọn NEW `(school-admin)/**` route group + new `(school-admin)/layout.tsx`; tách rõ hai persona; KHÔNG phá `(admin)/admin/**` hiện hữu. Recovery: nếu auth middleware chưa support multi-route-group, file follow-up.
- **R2 — ai-branding-wizard existing flow drift**: `(customer)/branding/wizard/page.tsx` hiện hữu, không rõ độ hoàn thiện vs Direction C 6-step. → AC Bucket B: state-check execution-time đo độ phủ; nếu wizard hiện chỉ 1-step, rewrite 6-step thay vì incremental refactor (tránh confusion mid-flow). Recovery: nếu user đang dùng wizard cũ trong production, ship behind feature flag `kite.branding.wizardV2`.
- **R3 — Lifecycle state machine consistency với BE**: 28 screens có 5 lifecycle variants (NOT_STARTED/INITIALIZING/GENERATING/DEPLOYED/REGENERATING/FAILED) — phải khớp `ai-branding-guidelines.md` §6 + G9 InstanceLifecycleStatus shared component. → AC Bucket B: dùng G9 InstanceLifecycleStatus + validTransition helper từ shared lib; KHÔNG re-implement state machine.
- **R4 — Quality gate /100 widget WCAG**: per kit, quality gate widget surface WCAG fail với auto-suggested fixes. Cần G11 ThemePreview (đã shipped) + WCAG suggestFix logic. → AC Bucket B: import G11 + reuse calculateContrast/suggestFix; KHÔNG inline WCAG calc.
- **R5 — ENTERPRISE Advanced Mode gating**: 28 screens có advanced mode separate path gated bởi `ai.enterprise.advancedModeEnabled` flag. → AC Bucket B: UI gating từ env/config; nếu flag không set, render placeholder "Liên hệ sales để mở Advanced Mode".
- **R6 — Token cap UI mismatch**: Per `ai-branding-guidelines.md` §2.5, FE cap UI hiển thị 2k/4k/8k/16k tokens by tier. → AC Bucket B: import token cap constants từ `kitehub-frontend/src/config/ai-input-cap.ts` (state-check execution-time verify constants tồn tại; nếu không, hardcode + file follow-up).

---

## 2. Task Breakdown

| Bucket | Gap(s) | Owner | Effort | Disjoint? |
|--------|--------|-------|--------|-----------|
| A | GAP-271 kh-admin (P5 K-12 School Principal, 12 screens dense-desktop) | bg-agent | ~5-7h | ✅ FE only — NEW `kitehub-frontend/src/app/(school-admin)/**` |
| B | GAP-272 ai-branding-wizard v2 (Direction C 6-step, 28 screens) | bg-agent | ~7-9h | ✅ FE only — `kitehub-frontend/src/app/(customer)/branding/**` |

**Disjoint check:**
- Bucket A files: NEW `(school-admin)/**` + `src/components/school-admin/**`
- Bucket B files: `(customer)/branding/**` (existing wizard rewrite) + `src/components/branding/**`
- Zero file overlap ✅
- Both consume `@kite/shared-ui` qua imports — không edit `packages/shared-ui` source
- Không có Bucket 0 Foundation needed (no shared infra giữa A và B; PWA không cần cho kh desktop personas)

---

## 3. Scope (compact schema)

**Stake tier (per `wave-pack-planner/SKILL.md` §Step 4.6):** **HIGH** — kh-admin = P5 K-12 LEGAL MANDATE (Vietnamese MoET compliance + parent communication SLA + conduct tracking pháp lý nhạy cảm); ai-branding-wizard = Direction C canonical wizard pattern + Quality Gate compliance per `ai-branding-guidelines.md`. Model: **Opus 4.7 full** mỗi agent.
**Cross-layer? (per `contract-first-for-cross-layer.md`):** **NO** — pure FE; consume existing kh-platform endpoints (kh-admin) + kh-branding endpoints (ai-branding-wizard) + `@kite/shared-ui` G1/G3/G9/G10/G11 (tất cả đã shipped Wave 27/28/29).

| # | Bucket | Gap(s) | Priority | Files (glob) | Spawn order |
|:-:|--------|--------|:--------:|--------------|:-----------:|
| 1 | **A — kh-admin K-12 Principal** | GAP-271 | 🟡 P2 | `kitehub-frontend/src/app/(school-admin)/**` (NEW) + `src/components/school-admin/**` | parallel sau Wave 49 closure |
| 2 | **B — ai-branding-wizard v2** | GAP-272 | 🟡 P2 | `kitehub-frontend/src/app/(customer)/branding/**` + `src/components/branding/wizard/**` | parallel sau Wave 49 closure |

### Bucket A — kh-admin K-12 Principal (12 screens, NEW route group)

- Files: NEW `kitehub-frontend/src/app/(school-admin)/**` + `src/components/school-admin/**`
- 12 screens (per `documents/02-architecture/design-system/ui_kits/kitehub-admin/screens/`):
  - `dashboard.html` — School overview (KPIs: enrollment + attendance + fee collection + conduct flags)
  - `bulk-import.html` — Bulk student import (G1 BulkImportDropzone — enrollment week 500/day scale)
  - `teacher-management.html` — Teacher list 50+ + assign-to-class + role hierarchy
  - `multi-class-roster.html` — Class × subject × teacher matrix 25×9
  - `academic-calendar.html` — Semester/term + holidays + exam weeks
  - `report-cards.html` — MoET-compliant report card generation (G3 GradebookEntryGrid + G10 PaymentStatusTimeline + compliance stamp)
  - `parent-comms.html` — Parent communication monitor (escalation queue + SLA timer)
  - `fees.html` — Annual fees panel
  - `conduct.html` — Conduct/behavior tracking (5-step escalation ladder)
  - `school-profile.html` — School profile + settings
  - `empty-states.html` — Catalog empty states
  - `login.html` — School Principal login
- Components consumed từ `@kite/shared-ui`: G1 BulkImportDropzone (CSV/Excel parse VN names + phone validate); G3 GradebookEntryGrid (VN 10pt scale cho report cards); G10 PaymentStatusTimeline (fees panel)
- Tests: per-screen unit tests + integration cho bulk import flow + report card generation
- Acceptance:
  - 12 screens ≥105/128 per screen (kit baseline 107.2/128)
  - NEW route group `(school-admin)/**` không phá `(admin)/admin/**` existing platform admin
  - G1 + G3 + G10 imported từ `@kite/shared-ui` (không inline)
  - MoET-compliant report card output (compliance stamp + format chuẩn)
  - Vietnamese-only + realistic VN K-12 data (50+ teacher, 500-3000 student scale)
  - WCAG AA preserved
  - Parent communication SLA timer hoạt động (escalation badge khi quá hạn)
  - Conduct 5-step escalation ladder UI
  - E2E: principal login → dashboard → bulk import 100 students → assign teacher → generate report card → send parent comm

### Bucket B — ai-branding-wizard v2 (28 screens, Direction C 6-step)

- Files: `kitehub-frontend/src/app/(customer)/branding/wizard/**` (rewrite existing) + `src/components/branding/wizard/**`
- 28 screens (per `documents/02-architecture/design-system/ui_kits/ai-branding-wizard-v2/screens/`):
  - **6-step wizard**: step1-welcome (default + validating + conflict) + step2-logo (default + uploaded + skip + error) + step3-audience (default + selected) + step4-tone (default + selected) + step5-template (grid + fullscreen + with-custom-prompt) + step6-preview (default + quality-gate-pass + quality-gate-fail + regenerate-counter + regenerate-quota-empty + deploying)
  - **5 lifecycle variants** (per `ai-branding-guidelines.md` §6): lifecycle-NOT_STARTED + lifecycle-GENERATING + lifecycle-DEPLOYED + lifecycle-REGENERATING + lifecycle-FAILED
  - **Advanced mode**: settings-branding-advanced-mode + settings-branding-advanced-disclaimer-modal
  - **Partials**: _partials.html (shared headers/cards)
- Components consumed từ `@kite/shared-ui`: G9 InstanceLifecycleStatus + validTransition (lifecycle 5 states); G11 ThemePreview + calculateContrast + suggestFix (quality gate WCAG)
- Tests: per-step unit tests + integration cho 6-step E2E flow + lifecycle state transitions + quality gate scoring
- Acceptance:
  - 28 screens ≥110/128 per screen (kit baseline 115.6 ⭐⭐ HIGHEST R2)
  - 6-step wizard E2E flow (welcome → logo → audience → tone → template → preview-deploy)
  - G9 InstanceLifecycleStatus dùng cho 5 lifecycle screens (KHÔNG inline state machine)
  - G11 ThemePreview dùng cho quality gate WCAG widget (KHÔNG inline contrast calc)
  - Per-resource approve toggle hoạt động (logo / colors / banner / hero separately) per `ai-branding-guidelines.md` §4.2
  - Tier-based regenerate counter visible (FREE 3 / PRO 10 / PREMIUM 30 / ENTERPRISE unlimited) per §4.3
  - ENTERPRISE Advanced Mode separate path với free-prompt opt-in (gated by `ai.enterprise.advancedModeEnabled`); fallback placeholder khi flag off
  - Input prompt token cap UI (FREE 2k / PRO 4k / PREMIUM 8k / ENTERPRISE 16k tokens) per `ai-branding-guidelines.md` §2.5
  - Quality gate <70 → block deploy + auto-regenerate path (per §5)
  - Vietnamese-only content + realistic VN tenant data
  - WCAG AA preserved
  - E2E: Owner login → wizard step1-6 → quality gate pass → deploy → see DEPLOYED dashboard

---

## 4. State-Check Evidence (BẮT BUỘC per `audit-to-gap-pipeline.md` §2.6)

Verified 2026-05-10 trước khi draft plan:

| Symbol | Type | Verification command | Evidence | Verdict |
|--------|------|----------------------|----------|---------|
| `@kite/shared-ui` G1 BulkImportDropzone export | TS export | `grep "BulkImportDropzone" packages/shared-ui/src/index.ts` | exports `BulkImportDropzone, parseCSV, validateRow` | ✅ exists |
| `@kite/shared-ui` G3 GradebookEntryGrid export | TS export | `grep "GradebookEntryGrid" packages/shared-ui/src/index.ts` | exports `GradebookEntryGrid, validateGrade, parseExcelPaste` | ✅ exists |
| `@kite/shared-ui` G9 InstanceLifecycleStatus export | TS export | `grep "InstanceLifecycleStatus" packages/shared-ui/src/index.ts` | exports `InstanceLifecycleStatus, validTransition` | ✅ exists |
| `@kite/shared-ui` G10 PaymentStatusTimeline export | TS export | `grep "PaymentStatusTimeline" packages/shared-ui/src/index.ts` | exports `PaymentStatusTimeline` | ✅ exists |
| `@kite/shared-ui` G11 ThemePreview + helpers | TS exports | `grep "ThemePreview\|calculateContrast\|suggestFix" packages/shared-ui/src/index.ts` | exports `ThemePreview, calculateContrast, suggestFix` | ✅ exists |
| `kitehub-frontend/src/app/(admin)/admin/` | Existing route | `find <path>` | 5 sub-routes (beta-requests / instances / payments / revenue) for KH PLATFORM admin | ✅ exists (Bucket A KHÔNG phá — tách NEW `(school-admin)/**`) |
| `kitehub-frontend/src/app/(school-admin)/` | Target NEW route | `find kitehub-frontend/src/app -type d -name "school-admin"` | 0 dirs | 🆕 to-be-created (Bucket A) |
| `kitehub-frontend/src/app/(customer)/branding/wizard/page.tsx` | Existing route | `ls <path>` | 1 file (existing legacy wizard) | ✅ exists (Bucket B rewrite Direction C 6-step) |
| `kitehub-frontend/src/app/(customer)/branding/page.tsx` | Existing hub | `ls <path>` | 1 file | ✅ exists (Bucket B preserve hub, edit wizard sub-route) |
| `kitehub-frontend` đã consume `@kite/shared-ui` | imports | `grep -ohE "from '@kite/shared-ui'" kitehub-frontend/src` | 3 imports (ConsentBanner + InstanceLifecycleStatus + ThemePreview) | ✅ existing pattern (Bucket B mở rộng) |
| HTML prototype `documents/02-architecture/design-system/ui_kits/kitehub-admin/screens/` | Source | `ls <path>` | 12 .html files | ✅ exists (Bucket A reference) |
| HTML prototype `documents/02-architecture/design-system/ui_kits/ai-branding-wizard-v2/screens/` | Source | `ls <path>` | 28 .html files | ✅ exists (Bucket B reference) |
| `ai-branding-guidelines.md` §6 lifecycle state machine | Rule | `grep "Lifecycle State Machine" .claude/rules/ai-branding-guidelines.md` | section §6 with 6 states + transitions | ✅ exists (Bucket B alignment) |
| `ai-branding-guidelines.md` §2.4 wizard pattern | Rule | `grep "Wizard pattern" .claude/rules/ai-branding-guidelines.md` | §4.1 "Wizard pattern (required cho new tenant) Provisioning wizard 6 steps" | ✅ exists (Bucket B 6-step alignment) |
| `ai-branding-guidelines.md` §2.5 input cap | Rule | `grep "Input prompt token cap" .claude/rules/ai-branding-guidelines.md` | §2.5 with FREE 2k / BASIC 4k / PREMIUM 8k / ENTERPRISE 16k tiers | ✅ exists (Bucket B token cap UI alignment) |
| `kitehub-frontend/src/config/ai-input-cap.ts` (token cap UI constants) | Config file | `find kitehub-frontend/src -name "ai-input-cap*"` | (TBD execution-time) | ⚠️ verify Bucket B execution; nếu thiếu, hardcode + follow-up gap |
| `kite.branding.wizardV2` feature flag | Config key | `grep -rn "kite.branding.wizardV2" kitehub` | (TBD execution-time) | ⚠️ verify Bucket B execution; có thể chưa tồn tại — Bucket B tạo nếu cần feature-flag rollout |
| `ai.enterprise.advancedModeEnabled` flag | Config key | `grep -rn "advancedModeEnabled" kitehub` | (TBD execution-time) | ⚠️ verify Bucket B execution; ánh xạ với BE config |

**Banned shortcut compliance (mirror §2.5):** không dùng `\| head` truncation; full grep/find output đã verify cho 12 hàng ✅; 3 hàng ⚠️ marked verify-execution-time với fallback strategy documented.

---

## 5. Verification Gates (per bucket)

| Bucket | Local verify command | CI gate |
|--------|---------------------|---------|
| A | `pnpm -F kitehub-frontend test --run -- school-admin && pnpm -F kitehub-frontend build` | kitehub-frontend-ci |
| B | `pnpm -F kitehub-frontend test --run -- branding/wizard && pnpm -F kitehub-frontend build` | kitehub-frontend-ci |

---

## 6. Agent Spawn Pattern

Per `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md`:
- **Buckets A và B**: spawn `run_in_background: true` SAU KHI Wave 49 closure PR merged + main synced
- `isolation: worktree` mỗi bucket để parallel safety
- RELATIVE paths trong agent prompts per `feedback_worktree_absolute_path_contamination.md`
- Coordinator merge tuần tự A → B sau khi 2 background completion notifications đến
- Stake tier HIGH → mỗi agent dùng Opus 4.7 full (không downgrade Sonnet/Haiku)

**KHÔNG spawn cùng lúc với 3 agent Wave 49** — sẽ vượt max-cap 5 (rule #9). Wave 50 execution = Wave 49 closure + 30 min buffer + Wave 50 spawn.

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `feedback_post_merge_doc_sync.md` + `feedback_wave_history_append_required.md` + `post-wave-cleanup.md` + `feedback_wave_closure_release_progress_report.md`:

- Mỗi bucket PR update gap file Log + status (2 GAP-271/272)
- ROADMAP §🚀 Next Action update trong closure PR
- Wave plan frontmatter `status: draft → complete` flip trong closure PR
- `wave-history.jsonl` append entry trong closure PR (Rule 15)
- Sub-gap filed cho deferred items (e.g. ENTERPRISE Advanced Mode BE flag wiring nếu chưa setup)
- PARTIAL exit-ramp per `gap-done-discipline.md` §3 nếu không đủ AC verified
- `bash scripts/prune-merged-worktrees.sh --yes` sau khi tất cả bucket PR merged + trước khi draft closure PR
- **`## Release Plan Progress` section trong closure PR body** per `feedback_wave_closure_release_progress_report.md` rules #1-6

### Track 2 progress dự kiến sau Wave 49 + Wave 50

| Item | Trước Wave 49 | Sau Wave 49 (kc personas) | Sau Wave 50 (kh kits) |
|------|---------------|--------------------------|----------------------|
| Phase 4 kit OPEN | 5/7 (267/268/269/271/272) | 2/7 (271 + 272) | 0/7 |
| Phase 4 kit DONE | 0/7 | 3/7 (267 + 268 + 269) | 5/7 (267 + 268 + 269 + 271 + 272) |
| Phase 4 kit PARTIAL | 2/7 (266 + 270) | 2/7 (266 + 270) | 2/7 (266 + 270) |
| Phase 1 BETA §3.6 row #1 "8 Track 2 ports shipped" | 2/8 PARTIAL | 5/8 (3 DONE + 2 PARTIAL) | 7/8 (5 DONE + 2 PARTIAL) |

→ Sau Wave 50, Track 2 Phase 4 đạt **5/7 DONE + 2/7 PARTIAL** (chỉ còn GAP-266b/c/d + GAP-270b/c follow-ups). Phase 1 BETA §3.6 row #1 từ 2/8 → 7/8 (đếm `@kite/shared-ui` shared-lib là cổng thứ 8 đã DONE Wave 27/28/29). Đường găng còn lại: Phase 5 (7 coverage gap) + Phase 6 (hardening) + đóng PARTIAL follow-ups.

---

## 8. Log

- **2026-05-10 (draft)**: Wave 50 plan filed song song với Wave 49 đang chạy. User chọn "Wave 50 plan PR (kh-admin + ai-branding-wizard)" làm parallel work an toàn nhất. Plan tuân thủ `audit-to-gap-pipeline.md` §2.6 State-Check Evidence (15 hàng verified ✅ + 3 hàng ⚠️ verify-execution-time với fallback) + `contract-first-for-cross-layer.md` (NO cross-layer) + `gap-done-discipline.md` PARTIAL exit-ramp ready + `post-wave-cleanup.md` cleanup script trong closure protocol. Stake tier HIGH → Opus 4.7 full mỗi agent. Wall-clock estimate ~7-9h (longest path Bucket B 28 screens). **Status: draft — chờ user review + approve. Execution sau khi Wave 49 closure merged + main synced (KHÔNG spawn cùng lúc với 3 agent Wave 49 vì vượt max-cap 5).**
