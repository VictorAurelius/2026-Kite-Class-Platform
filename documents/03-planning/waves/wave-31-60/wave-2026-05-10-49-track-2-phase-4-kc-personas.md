---
title: Wave 49 — Track 2 Phase 4 KC Personas (kc-parent + kc-teacher + kc-student)
status: complete
created: 2026-05-10
updated: 2026-05-10
waves: [49]
gaps: [GAP-267, GAP-268, GAP-269]
parent_umbrella: documents/03-planning/waves/wave-track-2-ui-kits-port-umbrella.md
phase_reference: Phase 4 (Track 2)
---

# Wave 49 — Track 2 Phase 4 KC Personas

**Goal:** Ship 3 production Next.js port của 3 KC persona cuối — kc-parent (mobile PWA) + kc-teacher (desktop) + kc-student (mobile PWA, route NEW) — đóng 3/5 kit OPEN của Phase 4 Track 2.
**Trigger:** User request "Hoàn tất 8 cổng Track 2 (FE production)" 2026-05-10. State-check confirm Phase 2+3 components đã ship đầy đủ qua Waves 27/28/29 → Phase 4 hoàn toàn unblock dependency. Ưu tiên 3 kit kc-* trước GAP-271 kh-admin + GAP-272 ai-branding-wizard vì shared PWA infra của parent + student tận dụng wave-pack tốt hơn.
**Estimated wall-clock:** ~8-9h (Bucket 0 Foundation ~1h + parallel agents A/B/C max ~7-8h longest path + ~30 min closure overhead).

---

## 1. Brainstorm (5-10 min)

**Q1 (alignment):**
- 3 persona Tier 2 thuộc P3 Center (parent có thể từ P5 K-12) — checklist Phase 1 BETA §3.6 row #1 "8 Track 2 ports shipped — production FE matches kit canon"
- Trực tiếp đóng 3/8 cổng Track 2 còn lại; sau wave này còn 5 hạng mục: GAP-271 (kh-admin) + GAP-272 (ai-branding-wizard v2) + GAP-266b/c/d follow-ups + GAP-270b/c follow-ups + Phase 5+6
- kc-parent có LEGAL MANDATE cho parent-in-P5 K-12 AC (per umbrella plan) — ưu tiên cao

**Q2 (trade-offs):**
- **Đã xét:** spawn 3 agents song song không có Bucket 0 → REJECT vì conflict file `public/manifest.json` + `public/sw.js` (cả parent + student đều cần). Coordinator-fix sau-rebase sẽ tốn thời gian gấp đôi savings parallelization.
- **Đã xét:** sequential A → C, B parallel → REJECT vì wall-clock tăng từ ~8-9h lên ~12-16h, defeats wave-pack purpose
- **Chọn:** Bucket 0 Foundation FE-only (PWA infra) merge FIRST, sau đó A+B+C parallel. Pattern này KHÔNG vi phạm `contract-first-for-cross-layer.md` (rule đó cho cross-layer FE+BE; wave này pure FE) — nhưng tận dụng cùng nguyên lý "shared infra trước"
- **Đã xét:** cũng port GAP-271 kh-admin + GAP-272 ai-branding-wizard v2 trong wave này → REJECT vì 5 bucket vượt max-cap 5 agent (sau Bucket 0) + 2 kit đó không share PWA với 3 KC kit
- **Đã xét:** thêm follow-up close GAP-266 + GAP-270 PARTIAL → REJECT vì khác bucket scope (Storybook + visual regression baseline khác kit port logic); sẽ batch riêng wave sau

**Q3 (rủi ro):**
- **R1 — Parent skeleton hiện hữu drift**: `(dashboard)/parent/page.tsx` đã có 174 LOC từ Wave 18b1 (`useMyChildren` + transcript link). Bucket A phải decide: rewrite hay extend? → AC: extend, không phá `useMyChildren` hook + `parent/transcript/[childId]` route hiện hữu. Recovery: nếu Wave 18b1 logic không khớp prototype, file sub-gap follow-up reconcile sau.
- **R2 — Teacher route fragmentation**: `(dashboard)/teacher/dashboard` + `(teacher)/attendance/period/[classId]/[periodNo]/[date]` là 2 route group khác nhau. Bucket B phải clarify nên consolidate vào `(teacher)/*` (mobile-first new prefix) hay rebuild trong `(dashboard)/teacher/*`. → AC: chọn `(teacher)/*` route group consistent với attendance route hiện có; reuse `(teacher)/layout.tsx`. Recovery: nếu route group conflict, file follow-up reconcile.
- **R3 — Student route NEW từ scratch**: thấp rủi ro reuse, nhưng có thể bị block nếu auth flow `(dashboard)/student/*` cần `student-ui` route group middleware mới chưa có. → AC: dùng `(dashboard)/student/*` cùng route group với `(dashboard)/parent/*` (per-tenant auth context); không tạo route group mới.
- **R4 — PWA infra rủi ro xung đột với Vercel rewrite/middleware**: manifest + sw deploy phải tương thích với existing middleware. → Bucket 0 verify build + dev server start clean trước khi merge.
- **R5 — Web Push backend chưa có**: GAP-267/269 AC yêu cầu "Web Push permission UI works (subscribe/unsubscribe round-trip)" nhưng backend endpoint `/api/push/subscribe` có thể chưa tồn tại. → State-check Bucket 0 verify; nếu thiếu, Bucket 0 mock endpoint qua MSW (test) + UI gọi endpoint giả; AC adjust thành "UI có; backend wiring deferred follow-up".

---

## 2. Task Breakdown

| Bucket | Gap(s) | Owner | Effort | Disjoint? |
|--------|--------|-------|--------|-----------|
| 0 | (PWA infra foundation) | bg-agent or coordinator | ~1-1.5h | ✅ shared infra `public/` + `lib/web-push.ts` + `next.config.js` PWA |
| A | GAP-267 kc-parent | bg-agent | ~6-8h | ✅ FE only — `(dashboard)/parent/**` + `(auth)/parent-invite/**` |
| B | GAP-268 kc-teacher | bg-agent | ~6-8h | ✅ FE only — `(dashboard)/teacher/**` + `(teacher)/**` |
| C | GAP-269 kc-student | bg-agent | ~5-7h | ✅ FE only — NEW `(dashboard)/student/**` |

**Disjoint check:**
- Bucket A files: `(dashboard)/parent/**` + `(auth)/parent-invite/**` + `src/components/parent/**`
- Bucket B files: `(dashboard)/teacher/**` + `(teacher)/**` + `src/components/teacher/**`
- Bucket C files: NEW `(dashboard)/student/**` + `src/components/student/**`
- Shared (covered by Bucket 0): `public/manifest.json` + `public/sw.js` + `src/lib/web-push.ts` + `next.config.js`
- Zero file overlap ✅

---

## 3. Scope (compact schema)

**Stake tier (per `wave-pack-planner/SKILL.md` §Step 4.6):** **HIGH** — 3 user-facing personas critical to Phase 1 BETA, Vietnamese content mandate, PWA semantics, accessibility WCAG AA. Model: **Opus 4.7 full** mỗi agent.
**Cross-layer? (per `contract-first-for-cross-layer.md`):** **NO** — pure FE; consumes existing kc-core endpoints (no new BE contract); uses `@kite/shared-ui` G2/G3/G4/G7/G8/G10 đã shipped. **Bucket 0 Foundation tồn tại nhưng KHÔNG phải cross-layer foundation** — mục đích là shared FE infra (PWA) tránh conflict A+C parallel.

| # | Bucket | Gap(s) | Priority | Files (glob) | Spawn order |
|:-:|--------|--------|:--------:|--------------|:-----------:|
| 0 | **Foundation (PWA infra)** | shared by GAP-267 + GAP-269 | 🟠 P1 | `kiteclass-frontend/public/manifest.json` + `public/sw.js` + `src/lib/web-push.ts` + `next.config.js` headers + `src/app/layout.tsx` PWA meta tags | **MERGE FIRST** |
| 1 | **A — kc-parent** | GAP-267 | 🟡 P2 | `kiteclass-frontend/src/app/(dashboard)/parent/**` + `(auth)/parent-invite/**` + `src/components/parent/**` | parallel sau Bucket 0 |
| 2 | **B — kc-teacher** | GAP-268 | 🟡 P2 | `kiteclass-frontend/src/app/(dashboard)/teacher/**` + `(teacher)/**` + `src/components/teacher/**` | parallel sau Bucket 0 |
| 3 | **C — kc-student** | GAP-269 | 🟡 P2 | `kiteclass-frontend/src/app/(dashboard)/student/**` (NEW) + `src/components/student/**` | parallel sau Bucket 0 |

### Bucket 0 — Foundation (PWA infra)

- Files:
  - `kiteclass-frontend/public/manifest.json` (Web App Manifest — name/icons/theme_color/start_url/display=standalone)
  - `kiteclass-frontend/public/sw.js` (service worker — basic install + activate + offline fallback)
  - `kiteclass-frontend/src/lib/web-push.ts` (subscribe/unsubscribe helpers + permission flow + MSW handler stub for `/api/push/subscribe`)
  - `kiteclass-frontend/src/app/layout.tsx` (add `<link rel="manifest">` + `<meta name="theme-color">`)
  - `kiteclass-frontend/next.config.js` (headers for sw scope + manifest cache-control)
- Tests: `src/lib/__tests__/web-push.test.ts` (subscribe/unsubscribe contract; MSW mock); manifest validation test
- Acceptance:
  - `pnpm build` succeeds
  - `pnpm dev` starts clean, `curl http://localhost:3001/manifest.json` returns valid JSON
  - `curl http://localhost:3001/sw.js` returns 200 with `Content-Type: application/javascript`
  - Lighthouse PWA category ≥80 trên localhost
- Spawn order: **MERGE FIRST** trước khi spawn A+B+C parallel
- Reference HTML prototype source: `documents/02-architecture/design-system/ui_kits/kiteclass-parent/{manifest.json,sw.js}` + `kiteclass-student/{manifest.json,sw.js}` (cùng nội dung core, có thể tổng hợp 1 file tối thiểu shared)

### Bucket A — kc-parent (mobile PWA, 17 screens)

- Files: `kiteclass-frontend/src/app/(dashboard)/parent/**` + `(auth)/parent-invite/**` + `src/components/parent/**`
- 17 screens (per `documents/02-architecture/design-system/ui_kits/kiteclass-parent/screens/`):
  - home (default + dark + empty + error + loading variants — 5 states)
  - attendance-calendar + attendance-day
  - billing-list + billing-pay + billing-success
  - grades-overview + grades-subject-detail + grades-empty
  - settings + push-notification-card + pwa-install-prompt + web-push-permission
- Components consumed từ `@kite/shared-ui`: G7 ParentInvite (parent-invite redemption flow); G8 AttendanceCalendar; G6 InvoiceDetail (billing)
- Tests: per-screen unit tests + integration test cho parent-invite redemption flow
- Acceptance:
  - 17 screens match HTML prototype ≥110/128 per screen (kit baseline 114/128)
  - PWA: manifest + sw từ Bucket 0 hoạt động; `pwa-install-prompt.html` UI được port
  - Web Push permission UI hoạt động (subscribe/unsubscribe round-trip qua MSW stub)
  - Zalo OA primary card visible above Web Push fallback (per kit prototype)
  - Bottom tab nav 44px+ tap targets
  - Vietnamese content + realistic VN data
  - WCAG AA preserved
  - **KHÔNG phá** existing Wave 18b1 logic (`useMyChildren` hook + `(dashboard)/parent/transcript/[childId]` route)
  - E2E: parent-invite token → child binding → child detail → pay tuition

### Bucket B — kc-teacher (24 screens, desktop-first)

- Files: `kiteclass-frontend/src/app/(dashboard)/teacher/**` + `(teacher)/**` + `src/components/teacher/**`
- 24 screens (per `documents/02-architecture/design-system/ui_kits/kiteclass-teacher/screens/`):
  - attendance (default + dark + empty + error + marking + saved — 6 states)
  - grade-entry (default + dark + editing + finalize-confirm + finalized + validation-error — 6 states)
  - schedule (week-view + dark + create-slot + conflict-error — 4 states)
  - reports (overview-default + detail-class + dark + empty + loading — 5 states)
  - settings (default + dark + payroll — 3 states)
- Components consumed từ `@kite/shared-ui`: G2 AttendanceRoster; G3 GradebookEntryGrid (VN 10pt scale + Excel paste); G4 ClassScheduleManager (conflict detection); G8 AttendanceCalendar
- Tests: per-screen unit tests + integration cho attendance + grade entry workflows
- Acceptance:
  - 24 screens ≥105/128 per screen (kit baseline 108/128)
  - G2/G3/G4/G8 imported từ `@kite/shared-ui` (không inline copy)
  - VN 10pt grade scale validation working (G3 native)
  - Daily attendance saves to existing kc-core endpoint
  - Recurring schedule rules (G4 conflict detection working)
  - Subject teacher MoET-format report card output (per `attendance-saved.html` + `reports-detail-class.html`)
  - Vietnamese-only + realistic VN teacher data
  - WCAG AA preserved
  - Route consolidation decision: gộp vào `(teacher)/*` route group (existing `(teacher)/layout.tsx` reuse) HOẶC giữ `(dashboard)/teacher/*` — agent quyết tại execution + document trong PR description
  - E2E: teacher login → mark attendance Lớp 6A1 → enter grades → see report

### Bucket C — kc-student (mobile PWA, 13 screens, route NEW)

- Files: NEW `kiteclass-frontend/src/app/(dashboard)/student/**` + `src/components/student/**`
- 13 screens (per `documents/02-architecture/design-system/ui_kits/kiteclass-student/screens/`):
  - today + login (entry points)
  - my-classes + class-detail
  - assignments + assignment-detail
  - grades + grade-detail
  - attendance + payments + notifications + profile
  - empty-states (catalog of empty states)
- Components consumed từ `@kite/shared-ui`: G6 InvoiceDetail (payments); G8 AttendanceCalendar (attendance); G10 PaymentStatusTimeline (payments)
- Tests: per-screen unit tests + integration cho assignment submit (offline draft + sync)
- Acceptance:
  - 13 screens ≥110/128 per screen (kit baseline 116/128 ⭐⭐ HIGHEST)
  - PWA installable từ Bucket 0 manifest + sw
  - Web Push permission UI hoạt động
  - Saved-draft submit recovers offline submissions when back online (sw cache + queue + retry)
  - Bottom 5-tab nav, 44px+ tap targets
  - Social login Zalo OA + Google (UI; backend wiring có thể deferred follow-up nếu provider chưa setup)
  - Vietnamese-only + realistic VN student data
  - WCAG AA preserved
  - E2E: student login → today → submit assignment offline → see synced when online

---

## 4. State-Check Evidence (BẮT BUỘC per `audit-to-gap-pipeline.md` §2.6)

Verified 2026-05-10 trước khi draft plan:

| Symbol | Type | Verification command | Evidence | Verdict |
|--------|------|----------------------|----------|---------|
| `@kite/shared-ui` package | Workspace | `cat packages/shared-ui/package.json` | exists, exports 12/12 G* + D1 | ✅ exists |
| `AttendanceRoster`, `GradebookEntryGrid`, `ClassScheduleManager`, `AttendanceCalendar`, `ParentInvite`, `InvoiceDetail`, `PaymentStatusTimeline` exports | TS exports | `grep "^export" packages/shared-ui/src/index.ts` | tất cả 7 exports đã có | ✅ exists |
| `kiteclass-frontend/src/app/(dashboard)/parent/page.tsx` | Existing route | `wc -l <path>` | 174 LOC (Wave 18b1) | ✅ exists (Bucket A extends, không phá) |
| `kiteclass-frontend/src/app/(dashboard)/parent/transcript/[childId]/page.tsx` | Existing route | `ls <path>` | 1 file | ✅ exists (Bucket A preserves) |
| `kiteclass-frontend/src/app/(auth)/parent-invite/[token]/` | Existing route | `find <path>` | 1 dir | ✅ exists (Bucket A reuses) |
| `kiteclass-frontend/src/app/(dashboard)/teacher/dashboard/page.tsx` | Existing route | `ls <path>` | 1 file | ✅ exists (Bucket B extends/redesigns) |
| `kiteclass-frontend/src/app/(teacher)/layout.tsx` | Existing route group layout | `ls <path>` | 1 file | ✅ exists (Bucket B reuses) |
| `kiteclass-frontend/src/app/(teacher)/attendance/period/[classId]/[periodNo]/[date]/page.tsx` | Existing route | `ls <path>` | 1 file + tests | ✅ exists (Bucket B reuses) |
| `kiteclass-frontend/src/app/(dashboard)/student/` | Target route | `find kiteclass-frontend/src/app/(dashboard)/student -type d` | 0 dirs | 🆕 to-be-created (Bucket C) |
| HTML prototype `documents/02-architecture/design-system/ui_kits/kiteclass-parent/screens/` | Source | `ls <path>` | 17 .html files | ✅ exists (Bucket A reference) |
| HTML prototype `documents/02-architecture/design-system/ui_kits/kiteclass-teacher/screens/` | Source | `ls <path>` | 24 .html files | ✅ exists (Bucket B reference) |
| HTML prototype `documents/02-architecture/design-system/ui_kits/kiteclass-student/screens/` | Source | `ls <path>` | 13 .html files | ✅ exists (Bucket C reference) |
| `kiteclass-frontend/public/manifest.json` | PWA manifest | `ls <path>` | 0 files | 🆕 to-be-created (Bucket 0) |
| `kiteclass-frontend/public/sw.js` | Service worker | `ls <path>` | 0 files | 🆕 to-be-created (Bucket 0) |
| `kiteclass-frontend/src/lib/web-push.ts` | Web Push helper | `ls <path>` | 0 files | 🆕 to-be-created (Bucket 0) |
| `useMyChildren` React Query hook | Existing FE hook | `grep -rn "useMyChildren" kiteclass-frontend/src` | (exists per Wave 18b1, Bucket A reuses) | ✅ exists |

**Banned shortcut compliance (mirror §2.5):** không dùng `\| head` truncation; full `find`/`grep` output đã verify; không có aspirational reference.

---

## 5. Verification Gates (per bucket)

| Bucket | Local verify command | CI gate |
|--------|---------------------|---------|
| 0 | `pnpm -F kiteclass-frontend test --run && pnpm -F kiteclass-frontend build && pnpm -F kiteclass-frontend dev &` + `curl http://localhost:3001/manifest.json` | frontend-ci |
| A | `pnpm -F kiteclass-frontend test --run -- parent && pnpm -F kiteclass-frontend build` | frontend-ci |
| B | `pnpm -F kiteclass-frontend test --run -- teacher && pnpm -F kiteclass-frontend build` | frontend-ci |
| C | `pnpm -F kiteclass-frontend test --run -- student && pnpm -F kiteclass-frontend build` | frontend-ci |

---

## 6. Agent Spawn Pattern

Per `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md`:
- **Bucket 0 Foundation**: ship FIRST qua single PR, coordinator merge sau khi CI green; không spawn agent (scope nhỏ ~1h, coordinator làm trực tiếp hiệu quả hơn overhead spawn)
- **Buckets A/B/C**: tất cả spawn `run_in_background: true` SAU KHI Bucket 0 merged + main synced
- `isolation: worktree` mỗi bucket để parallel safety
- RELATIVE paths trong agent prompts per `feedback_worktree_absolute_path_contamination.md`
- Coordinator merge tuần tự A → B → C sau khi tất cả background completion notifications đến
- Stake tier HIGH → mỗi agent dùng Opus 4.7 full (không downgrade Sonnet/Haiku)

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `feedback_post_merge_doc_sync.md` + `feedback_wave_history_append_required.md` + `post-wave-cleanup.md` + `feedback_wave_closure_release_progress_report.md`:

- Mỗi bucket PR update gap file Log + status (3 GAP-267/268/269)
- ROADMAP §🚀 Next Action update trong closure PR
- Wave plan frontmatter `status: draft → complete` flip trong closure PR
- `wave-history.jsonl` append entry trong closure PR (Rule 15)
- Sub-gap filed cho bất kỳ deferral nào (e.g. Web Push backend wiring nếu provider chưa setup)
- PARTIAL exit-ramp per `gap-done-discipline.md` §3 nếu không đủ AC verified
- `bash scripts/prune-merged-worktrees.sh --yes` sau khi tất cả bucket PR merged + trước khi draft closure PR
- **`## Release Plan Progress` section trong closure PR body** per `feedback_wave_closure_release_progress_report.md` rules #1-6: Phase 1 BETA progress + 3 cổng Track 2 đóng + Waves Remaining table cập nhật

### Track 2 progress sau Wave 49 (dự kiến)

| Item | Trước Wave 49 | Sau Wave 49 (nếu success) |
|------|---------------|---------------------------|
| Phase 4 kit OPEN | 5/7 (267/268/269/271/272) | 2/7 (271 + 272) |
| Phase 4 kit DONE | 0/7 | 3/7 (267 + 268 + 269) |
| Phase 4 kit PARTIAL | 2/7 (266 + 270) | 2/7 (266 + 270, không thay đổi) |
| Track 2 tổng tiến độ | Phase 1+2+3 DONE; Phase 4 2/7 PARTIAL | Phase 1+2+3 DONE; Phase 4 3/7 DONE + 2/7 PARTIAL + 2/7 OPEN |

---

## 8. Log

- **2026-05-10 (draft)**: Wave 49 plan filed sau state-check phát hiện Phase 2+3 đã ship đầy đủ (PR #1088 sync). User chọn "Fix doc drift + spawn Phase 4 wave-pack ngay" với 3 KC personas ưu tiên. Plan tuân thủ `audit-to-gap-pipeline.md` §2.6 State-Check Evidence + `contract-first-for-cross-layer.md` (NO cross-layer, không cần Bucket 0 contract; nhưng có Bucket 0 FE-only PWA infra để tránh conflict A+C parallel) + `gap-done-discipline.md` PARTIAL exit-ramp ready + `post-wave-cleanup.md` cleanup script trong closure protocol. Stake tier HIGH → Opus 4.7 full mỗi agent. Wall-clock estimate ~8-9h (Bucket 0 ~1h + parallel max ~7-8h longest path). **Status: draft — chờ user review + approve trước khi merge plan PR + spawn agents.**
- **2026-05-10 (complete)**: Wave 49 SHIPPED. 4 PRs merged sequentially: plan #1089 → Bucket 0 PWA infra #1090 → A kc-parent #1092 → B kc-teacher #1094 → C kc-student #1093. 3 background agents Opus full với worktree isolation chạy parallel ~24min wall-clock vs ~8-9h estimate (≥20× speedup vs serial). Tất cả 3 GAP-267/268/269 flipped 🔵 OPEN → 🟡 PARTIAL per `gap-done-discipline.md` §3 honest exit-ramp:
  - GAP-267 (kc-parent): 14/17 AC verified; 3 deferred (Lighthouse PWA ≥90 needs HTTPS deploy, Playwright E2E parent-invite spec, G7 redeem variant in GAP-273) — sub-gap GAP-267a follow-up logged
  - GAP-268 (kc-teacher): route consolidation `(teacher)/teacher/*` shipped; 3 follow-ups (per-screen /128 audit, attendance overview API extension, E2E flow spec) logged in gap file
  - GAP-269 (kc-student): 7/10 AC; deferred social-login backend, real REST endpoints, Lighthouse, E2E, login screen — logged in gap Log
  - **Phase 4 Track 2 progress**: 0/7 DONE → **3/7 PARTIAL** (267+268+269) + 2/7 PARTIAL pre-existing (266+270) + 2/7 OPEN (271+272 Wave 50)
  - **Discipline wins**: zero file conflicts (state-check disjoint paths verified pre-spawn); Wave 18b1 logic preserved verbatim (Bucket A); Bucket C `sw.js` extension architecturally clean (page-context queue module, additive); coordination note Bucket A↔C honored — zero merge conflicts; 3/3 buckets shipped CI green (Frontend Tests + Build pass on all, E2E Playwright pass on A+C, B Frontend Tests passed before merge); local verify on each bucket per `admin-merge-discipline.md` (711+705+707 tests respectively, builds clean)
  - **Cleanup**: 1 worktree husk + 2 merged branches pruned via `scripts/prune-merged-worktrees.sh --yes`
