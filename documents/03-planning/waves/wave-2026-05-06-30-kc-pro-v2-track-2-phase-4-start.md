---
title: Wave 30 — Phase 4 kit port start — GAP-266 KC pro v2 (4 page-cluster buckets)
status: complete
created: 2026-05-06
updated: 2026-05-06
waves: [30]
gaps: [GAP-266]
---

# Wave 30 — Phase 4 kit port start — KC pro v2 production port (4 buckets)

**Goal:** Khởi động Track 2 Phase 4 (kit ports) bằng cách port `kiteclass-pro-v2` HTML proto (avg 108.4/128, 10 screens) sang `kiteclass-frontend/src/app/(dashboard)/`. Validate `@kite/shared-ui` adoption pattern cho production app — Wave 27/28/29 đã ship 12 G* + D1 + ConsentBanner; Wave 30 là consumer đầu tiên ở dashboard scope. **Closes GAP-266 PARTIAL** (kit foundation + 4 page clusters; remaining polish/E2E/visual-regression → Wave 31+).
**Trigger:** Wave 29 plan §7 closure recommends Phase 4 kit ports start — all 7 kit gaps unblocked post-Wave-29. Drafted PIPELINED per `feedback_pipelined_wave_planning.md` + `wave-pack-planner` §Step 5.5 (Wave 29 Bucket A still in-flight).
**Estimated wall-clock:** ~15-20 min/agent parallel (kit port = wire shared-ui + apply tokens + adapt 2-3 pages per bucket; heavier than component port). Total ~60-80 min including coordinator merge + closure.

---

## 1. Brainstorm

**Q1 (alignment):**
- **Persona:** P2 Center Owner SaaS (flagship Phase 1) — owner dashboard surfaces (admin, classes, courses, students, teachers, billing, settings). Per `feedback_release_1_first_session_priority.md` MVP-Phase-1.
- **Domain:** Frontend production port (Next.js 15 App Router). KC frontend hiện tại baseline ~73/128 R1; Wave 30 đưa lên target ≥105/128 (per GAP-266 AC).
- **Phase progression:** Wave 30 = KC pro v2 *foundation*. Wave 31 candidates: KH pro v2 (GAP-270), ai-branding-wizard (GAP-272), teacher kit (GAP-268). Phase 4 hoàn tất khi 7 kits ship.
- **Adoption validation:** đây là production consumer đầu tiên cho `@kite/shared-ui` ở dashboard scope. Nếu adoption rough → file meta-gap chỉnh shared-ui API trước khi Wave 31.

**Q2 (trade-offs):**
- **4 buckets / 1 kit** vs 4 buckets / 4 kits: chọn **1 kit / 4 buckets** — single kit ships hoàn chỉnh, validate adoption thoroughly trước khi nhân rộng. 4-kits-parallel phân tán rủi ro nhưng làm mỗi gap stay PARTIAL → Wave 31 cleanup.
- **Cross-page primitives (theme, ⌘K palette, sparkline KPI):** Bucket A own — others consume. Risk: B/C/D depend on A's API. Mitigation: Bucket A briefing yêu cầu **stub interface ship sớm** trong PR (commit 1 = types only, commit 2+ = implementation). B/C/D import types tự type-check không cần A merge trước.
- **Dark mode + success confetti:** proto có `dashboard-dark.html` + `success-confetti.html`. Foundation Bucket A ship dark-mode toggle + confetti utility; cluster buckets hưởng lợi tự động.
- **Drag-drop reorder (classes):** GAP-266 AC liệt kê. Bucket B integrate (HTML5 DnD hoặc `@dnd-kit/core` nếu chưa có). Stub implementation OK nếu API backend thiếu PUT endpoint.
- **`@kite/shared-ui` consumption pattern:** import `import { G6InvoiceDetail, formatVNCurrency } from '@kite/shared-ui'` — đảm bảo workspace dep `kiteclass-frontend/package.json` đã có `@kite/shared-ui: workspace:*` (cần state-check).

**Q3 (risks):**
- **R1: KC frontend chưa có workspace dep cho @kite/shared-ui ở dashboard pages.** Public layout đã import — nghĩa là dep exists ở root `package.json`. Verify: state-check §4.
- **R2: Coordinator-applied CI fixes có thể cần (per `feedback_coordinator_ci_fix_pattern.md`).** Production port chạm SSR — `next build` strict hơn `tsc --noEmit`. Brief agents ship `pnpm build` verify, không chỉ type-check. Per `feedback_agent_local_verify_both_layers.md` + `feedback_agent_ts_strict_uncheckedindex.md`.
- **R3: Visual regression baseline.** GAP-266 AC yêu cầu `scripts/capture-screenshots.ts`. Wave 30 KHÔNG capture — defer GAP-266b (visual regression baseline) để cluster work tập trung port logic. Document in §7 closure.
- **R4: Dependency on Wave 29 G12 BulkActionsBar (Bucket C consumer).** Wave 30 spawn AFTER Wave 29 closure — verify G12 exported from `@kite/shared-ui`. Nếu Wave 29 stalls → defer Bucket C scope.
- **R5: 4 disjoint buckets vs cross-cutting layout.tsx.** Layout.tsx trong `(dashboard)/` chứa nav shell — Bucket A own (foundation). B/C/D chỉnh nav links thông qua addition không sửa core. Coordinator merge order A → B → C → D như Wave 27/28.

---

## 2. Task Breakdown

| Bucket | Scope | Owner | Effort | Disjoint? |
|--------|-------|-------|--------|-----------|
| A | Foundation: theme provider + dark-mode toggle + ⌘K palette + sparkline KPI primitive + dashboard home page + success confetti util | bg-agent | ~18-22 min | ✅ `_shared/dashboard-foundation/` + `(dashboard)/page.tsx` + `(dashboard)/layout.tsx` |
| B | Class + Course management: apply tokens + integrate G4 ClassScheduleManager + drag-drop reorder | bg-agent | ~15-18 min | ✅ `(dashboard)/classes/` + `(dashboard)/courses/` |
| C | Student + Teacher management: apply tokens + integrate G12 BulkActionsBar + G1 BulkImportDropzone (consume Wave 29) | bg-agent | ~15-18 min | ✅ `(dashboard)/students/` + `(dashboard)/teachers/` |
| D | Billing + Settings + Branding gateway: apply tokens + integrate G6 InvoiceDetail + G10 PaymentStatusTimeline + G11 ThemePreview (consume Wave 29) | bg-agent | ~15-18 min | ✅ `(dashboard)/billing/` + `(dashboard)/settings/` + `(dashboard)/branding/` |

**Disjoint check:** mỗi bucket touch riêng route dirs. Shared edit point = `(dashboard)/layout.tsx` nav links (Bucket A own; B/C/D không sửa). `package.json` không sửa (dep đã có). Coordinator giải quyết alphabetical order (A → B → C → D).

**Cross-bucket dependency:** B/C/D depend on Bucket A's foundation primitives (theme provider, palette hook). Mitigation: Bucket A briefing ship interface stubs ở commit đầu (`_shared/dashboard-foundation/types.ts`) trước implementation; B/C/D import types tự type-check không block.

---

## 3. Scope (per bucket)

### Bucket A — Foundation: theme + palette + KPI + home dashboard

- **Spec source:**
  - `documents/02-architecture/design-system/ui_kits/kiteclass-pro-v2/screens/dashboard-default.html` (24KB — KPI cards + sparklines + recent activity)
  - `dashboard-{loading,empty,error,success,dark}.html` (5 state variants)
  - `command-palette.html` (⌘K UI — fuzzy search + categories)
  - `dark-mode-toggle.html` (theme switcher animation)
  - `success-confetti.html` (micro-interaction)
  - `_shared/colors_and_type.css` (design tokens — apply via Tailwind extend hoặc CSS modules)
- **Files to create:**
  - `kiteclass-frontend/src/_shared/dashboard-foundation/ThemeProvider.tsx` — light/dark mode toggle, persist localStorage, applies `colors_and_type.css` tokens
  - `.../CommandPalette.tsx` — ⌘K dialog với fuzzy search; routes to dashboard pages
  - `.../useCommandPalette.ts` — keyboard shortcut hook (⌘K / Ctrl+K)
  - `.../KPICard.tsx` — KPI tile với sparkline (lightweight SVG, không thư viện mới)
  - `.../Sparkline.tsx` — pure SVG sparkline component
  - `.../SuccessConfetti.tsx` — confetti utility (canvas-based, fire-and-forget)
  - `.../types.ts` — `ThemeMode`, `DashboardCommand`, `KPIData`, etc. (SHIP COMMIT 1 — interface stubs cho B/C/D)
  - `.../index.ts` — barrel export
- **Files to modify:**
  - `kiteclass-frontend/src/app/(dashboard)/layout.tsx` — wrap với `<ThemeProvider>`, add `<CommandPalette>`, nav apply tokens
  - `kiteclass-frontend/src/app/(dashboard)/page.tsx` — home dashboard với KPI grid + sparklines + recent activity (refactor để match `dashboard-default.html`)
- **Tests:** ≥6 — ThemeProvider toggle persistence, CommandPalette ⌘K open, KPICard render với data, Sparkline empty state, layout integration smoke
- **Acceptance:**
  - Dashboard home /128 self-rescore ≥105 vs baseline
  - Dark mode toggle smooth (CSS transition)
  - ⌘K mở palette từ bất kỳ dashboard page nào
  - WCAG AA preserved (axe DevTools clean trên home page)
  - `pnpm build` clean (next build strict)

### Bucket B — Class + Course management

- **Spec source:** GAP-266 §Proposed Fix scope row "Class management (drag-drop reorder)" + "Course catalog"
- **Files to modify:**
  - `(dashboard)/classes/page.tsx`, `(dashboard)/classes/[id]/page.tsx`, `(dashboard)/classes/__tests__/`
  - `(dashboard)/courses/page.tsx`, `(dashboard)/courses/[id]/page.tsx` (nếu tồn tại)
- **Files to create:**
  - `kiteclass-frontend/src/_shared/class-reorder/DragDropList.tsx` — HTML5 DnD reorder (no new deps; nếu cần, propose `@dnd-kit/core` ở PR description)
  - Test files cho mỗi component mới
- **Integration with `@kite/shared-ui`:** import `ClassScheduleManager` (Wave 28 G4) — replace existing schedule UI nếu trùng pattern
- **Tests:** ≥5 — drag-drop reorder fires callback, API persist call, course catalog list render, class detail page render, integration smoke
- **Acceptance:**
  - Tokens applied (colors + typography matches proto)
  - Drag-drop reorder persists qua existing PUT endpoint
  - `pnpm build` clean
  - Per-page UI review ≥105/128 self-rescore

### Bucket C — Student + Teacher management

- **Spec source:** GAP-266 §Proposed Fix "Student management (table + bulk actions)" + "Teacher management"
- **Files to modify:**
  - `(dashboard)/students/page.tsx`, `(dashboard)/students/[id]/page.tsx`, `(dashboard)/students/new/page.tsx`
  - `(dashboard)/teachers/page.tsx`
- **Integration with `@kite/shared-ui`:**
  - `BulkActionsBar` (Wave 29 G12) — wire vào students table với selection state
  - `BulkImportDropzone` (Wave 29 G1) — wire vào students/new flow
  - `AttendanceRoster` (Wave 27 G2) — reference từ student detail nếu pattern fit
- **Tests:** ≥5 — students table render, bulk action callback (DELETE → ConfirmDialog), bulk import smoke, teacher list render, integration smoke
- **Acceptance:**
  - Tokens applied
  - BulkActionsBar wires cho 4 actions (Xuất CSV, Lưu trữ, Phân lớp, Xóa) — destructive Xóa trigger D1 ConfirmDialog
  - BulkImportDropzone wires cho `/students/new` flow
  - `pnpm build` clean

### Bucket D — Billing + Settings + Branding gateway

- **Spec source:** GAP-266 §Proposed Fix "Billing overview" + "Settings (theme + branding gateway)"
- **Files to modify:**
  - `(dashboard)/billing/page.tsx`
  - `(dashboard)/settings/page.tsx`
  - `(dashboard)/branding/page.tsx` (gateway link sang AI Branding wizard nếu tồn tại)
- **Integration with `@kite/shared-ui`:**
  - `InvoiceDetail` + `formatVNCurrency` (Wave 27 G6) — wire billing detail view
  - `PaymentStatusTimeline` (Wave 28 G10) — wire billing detail recent payments
  - `ThemePreview` (Wave 29 G11) — wire settings theme customization
- **Tests:** ≥5 — billing list render, invoice detail render, theme preview integration, settings smoke, branding gateway link
- **Acceptance:**
  - Tokens applied
  - InvoiceDetail + PaymentStatusTimeline + ThemePreview integrated
  - Branding page có CTA dẫn sang AI Branding wizard (placeholder OK nếu wizard chưa wired)
  - `pnpm build` clean

---

## 4. State-Check Evidence (per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Type | Verification | Verdict |
|--------|------|-------------|---------|
| `kiteclass-frontend/src/app/(dashboard)/{admin,classes,courses,students,teachers,billing,settings,branding}/` | route dirs | `ls` | ✅ exist |
| `kiteclass-pro-v2/screens/*.html` | HTML proto | `ls` | ✅ 10 files exist |
| `_shared/colors_and_type.css` | design tokens | `wc -l` | ✅ 6.5KB exists |
| `@kite/shared-ui` workspace dep in KC | pnpm dep | `grep '@kite/shared-ui' kiteclass/kiteclass-frontend/package.json` | needs verify trước spawn |
| `G2`/`G6`/`formatVNCurrency` exports (Wave 27) | shared-ui | `grep 'AttendanceRoster\|InvoiceDetail\|formatVNCurrency' packages/shared-ui/src/index.ts` | ✅ exist |
| `G3`/`G4`/`G8`/`G10`/`D1` exports (Wave 28) | shared-ui | grep | ✅ exist |
| `G1`/`G9`/`G11`/`G12` exports (Wave 29) | shared-ui | post-Wave-29-merge verify | 🔄 in-flight (Bucket A still running; B/C/D shipped) |
| `ThemeProvider`, `CommandPalette`, `KPICard`, `Sparkline`, `SuccessConfetti` | local primitives | grep KC src | 🆕 to-be-created (Bucket A) |
| `DragDropList` | local primitive | grep | 🆕 to-be-created (Bucket B) |
| `(dashboard)/classes/[id]/__tests__/` | existing tests | `ls` | ✅ exist (need not regress) |
| `(dashboard)/students/__tests__/` | existing tests | `ls` | ✅ exist |

**Pre-spawn verify (coordinator):**
1. Wave 29 closure SHIPPED + all 4 G* exports trong `@kite/shared-ui`
2. `pnpm -F @kite/kiteclass-frontend build` baseline clean (no pre-existing build errors)
3. Confirm `@kite/shared-ui` workspace dep ở `kiteclass-frontend/package.json` (nếu thiếu → coordinator add ở plan PR hoặc Bucket A)

---

## 5. Verification Gates (per bucket)

| Bucket | Local verify command | Notes |
|--------|---------------------|-------|
| A | `pnpm -F @kite/kiteclass-frontend type-check && pnpm -F @kite/kiteclass-frontend test && pnpm -F @kite/kiteclass-frontend build` | next build strict — per `feedback_agent_ts_strict_uncheckedindex.md` |
| B | same — focus suites: classes + courses | `next build` MUST pass |
| C | same — focus suites: students + teachers | next build + bulk actions integration |
| D | same — focus suites: billing + settings + branding | next build + shared-ui consumption verified |

Coordinator post-merge: full `pnpm -F @kite/kiteclass-frontend build` + `pnpm -F @kite/shared-ui test` MUST pass.

---

## 6. Agent Spawn Pattern

Per `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md`:
- All 4 buckets `run_in_background: true` + `isolation: worktree`
- RELATIVE paths only (per `feedback_worktree_absolute_path_contamination.md`)
- Coordinator merge sequential A → B → C → D (A foundation merges first; B/C/D can rebase nếu A's primitives drift)
- `(dashboard)/layout.tsx` shared edit: Bucket A own; B/C/D không sửa (route page additions only)

**Spawn timing:** Wave 30 plan PR drafted DURING Wave 29 Bucket A in-flight (pipelined per skill §Step 5.5). Wave 30 spawn happens AFTER Wave 29 closure ships. Per `feedback_token_quota_spawn_timing.md` — coordinator monitor token budget; nếu >250k post-Wave-29-closure, defer Wave 30 spawn → next session với `/clear`.

**Domain-milestone audit:** Wave 30 thuộc domain `phase-4-kit-ports` (per `post-wave-audit-mandate.md` §2.4.1 registry). Cluster gồm: Wave 30 (KC pro v2) + Wave 31+ (KH pro v2 / ai-branding-wizard / teacher / parent / student / K-12 admin). Trailer cho Wave 30 closure: `AUDIT_DEFER_DOMAIN_MILESTONE: phase-4-kit-ports — milestone Wave NN closes Track 2 Phase 4`. Milestone TBD (likely Wave 35-36 sau khi cả 7 kits ship).

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `feedback_post_merge_doc_sync.md` + `feedback_wave_history_append_required.md` + `post-wave-cleanup.md`:

- Mỗi bucket PR update GAP-266 Log
- **Status flip:** GAP-266 stays 🟡 PARTIAL post-Wave-30 (foundation + 4 page clusters shipped; remaining: visual regression baseline GAP-266b, E2E test GAP-266c, polish iterations TBD)
- ROADMAP §🚀 Next Action update — recommend Wave 31 candidates: KH pro v2 (GAP-270) + ai-branding-wizard (GAP-272) parallel; HOẶC Phase 1 BETA P0 deploy cluster (GAP-369/370/373)
- Wave plan frontmatter `status: complete` flip
- `wave-history.jsonl` append (Rule 15)
- `bash scripts/prune-merged-worktrees.sh --yes` sau merge 4 PR + trước closure PR
- `AUDIT_DEFER_DOMAIN_MILESTONE: phase-4-kit-ports` trailer

**Follow-up gaps to file at closure:**
- **GAP-266b** — KC pro v2 visual regression baseline (`scripts/capture-screenshots.ts` capture + score)
- **GAP-266c** — KC pro v2 E2E test (Playwright owner login → dashboard → manage class → drop class → toast)
- **GAP-266d** — Bundle size analysis (AC `<300KB First Load JS` verify)
- **Phase 4 progress:** Wave 31 candidates (KH pro v2 + ai-branding-wizard parallel — both Phase 1 critical, both unblocked post-Wave-29)

---

## 8. Log

- **2026-05-06 (draft):** Plan tạo PIPELINED trong khi Wave 29 Bucket A in-flight (Buckets B/C/D đã ship PR #864/#865/#866). Áp dụng `wave-pack-planner` §Step 5.5 + `feedback_pipelined_wave_planning.md` lần 2 (Wave 29 → Wave 30). State-check verified: dashboard routes + HTML proto + tokens tồn tại; KC frontend chưa consume `@kite/shared-ui` ở dashboard scope (chỉ public layout). Wave 30 = production consumer đầu tiên cho shared-ui ở dashboard — validate adoption pattern trước khi nhân rộng cho 6 kit khác. Spawn timing: SAU Wave 29 closure ships + token budget check (per `feedback_token_quota_spawn_timing.md`). Risk-mitigated cross-bucket dependency qua Bucket A interface stubs ship sớm.
