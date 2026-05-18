---
title: Wave 31 — Phase 4 kit port — GAP-270 KH pro v2 (4 page-cluster buckets)
status: complete
created: 2026-05-06
updated: 2026-05-06
waves: [31]
gaps: [GAP-270]
---

# Wave 31 — Phase 4 kit port — KH pro v2 production port (4 buckets)

**Goal:** Port `kitehub-pro-v2` HTML proto (avg 107.8/128, 24 screens) sang production `kitehub-frontend/src/app/(customer)/`. P2 Center Owner SaaS control plane (subscription health + billing + branding hub + instance lifecycle). **Closes GAP-270 PARTIAL** (foundation + 4 page clusters; remaining polish/E2E/visual-regression → follow-ups).
**Trigger:** Wave 30 plan §7 closure recommends KH pro v2 + ai-branding-wizard parallel. Drafted PIPELINED per `feedback_pipelined_wave_planning.md` + `wave-pack-planner` §Step 5.5 (Wave 30 agents in-flight). 3rd consecutive pipelined application — pattern stable.
**Estimated wall-clock:** ~15-20 min/agent parallel (mirror Wave 30 estimate; KH dashboard scope similar — page-cluster + token-apply + shared-ui integration).

---

## 1. Brainstorm

**Q1 (alignment):**
- **Persona:** P2 Center Owner SaaS — owner-side KH control plane (subscription health, billing, theme management, instance lifecycle). Phase 1 critical per `release-1-plan-2026.md` §3.
- **Domain:** Frontend production port (Next.js 15). KH `(customer)/` baseline predates R2 → Wave 31 brings to ≥105/128 target.
- **Scope vs ai-branding-wizard:** Wave 31 ports KH pro v2 (24 screens covering dashboard/billing/branding-HUB/instances/settings); ai-branding-wizard-v2 (28 screens — Direction C 6-step + Enterprise Advanced + Quality Gate widget) deferred Wave 32 due to size (highest-scoring kit ⭐ 115.6/128, deserves dedicated wave).
- **Adoption validation #2:** Wave 30 KC pro v2 = first @kite/shared-ui dashboard consumer; Wave 31 KH = second consumer → confirm cross-app pattern works (any divergence = file meta-gap on shared-ui API).

**Q2 (trade-offs):**
- **4 buckets / 1 kit** giống Wave 30 — clean focus, single kit ships hoàn chỉnh.
- **Cross-page primitives reuse:** KH có thể tái sử dụng dashboard-foundation từ Wave 30 (KC) NẾU API generic enough. Bucket A briefing yêu cầu IF Wave 30 Bucket A đã merged + foundation generic → reuse via cross-app workspace pattern; ELSE create KH-specific copy at `kitehub-frontend/src/_shared/dashboard-foundation/`. Decision deferred to Bucket A agent based on what they find.
- **G9 InstanceLifecycleStatus integration:** Wave 29 G9 component shipped specifically cho AI Branding lifecycle — Bucket D wires nó vào KH `instances/` page (primary use case).
- **Bucket split:** A = foundation + customer dashboard home; B = billing (invoices + payment methods + tier upgrade); C = branding hub (theme + logo + wizard entry — light touch, defer wizard-internals to Wave 32); D = instances + settings (G9 integration).

**Q3 (risks):**
- **R1: Wave 30 dashboard-foundation API drift.** Bucket A của Wave 31 cần foundation primitives. Nếu Wave 30 Bucket A's API quá KC-specific → KH bucket A duplicates. Mitigation: brief Bucket A agent đánh giá generic-vs-specific tại spawn time.
- **R2: 24 screens spread 4 buckets = ~6 screens/bucket** — heavier than Wave 30 (~2-3 pages/bucket). Token-by-token apply sẽ tốn time. Mitigation: focus per-cluster ≥105/128 self-rescore on PRIMARY pages (dashboard home, billing default, branding hub default, instance default); empty/error/loading states minimum-viable per-cluster.
- **R3: ai-branding-wizard entry from branding hub.** Bucket C porting branding hub có CTA "Khởi động wizard" — sẽ point sang Wave 32 wizard route. Placeholder href OK in Wave 31; Wave 32 wires.
- **R4: G9 InstanceLifecycleStatus state machine wiring.** Bucket D integrates G9 với real backend `/api/instances/{id}/status` polling. Không rõ backend endpoint exists — Bucket D state-check tại spawn; nếu missing → mock state + flag follow-up.
- **R5: Coordinator merge order.** Same Wave 30 pattern — A→B→C→D sequential. `(customer)/layout.tsx` shared edit (Bucket A own). Predicted 1-2 conflicts.

---

## 2. Task Breakdown

| Bucket | Scope | Owner | Effort | Disjoint? |
|--------|-------|-------|--------|-----------|
| A | KH foundation (eval reuse vs copy of Wave 30's) + customer dashboard home (subscription health KPIs + usage charts) | bg-agent | ~18-22 min | ✅ `_shared/dashboard-foundation/` + `(customer)/dashboard/` + `(customer)/layout.tsx` |
| B | Billing: invoices + payment methods + tier upgrade flow | bg-agent | ~15-18 min | ✅ `(customer)/billing/` |
| C | Branding hub: theme + logo + wizard entry (CTA placeholder for Wave 32) | bg-agent | ~12-15 min | ✅ `(customer)/branding/` (HUB only — wizard internals = Wave 32) |
| D | Instances + Settings (G9 InstanceLifecycleStatus integration) | bg-agent | ~15-18 min | ✅ `(customer)/instances/` + `(customer)/settings/` |

**Disjoint check:** mỗi bucket touch riêng route dirs. Shared edit = `(customer)/layout.tsx` (Bucket A own).

**Cross-bucket dependency:** B/C/D phụ thuộc Bucket A foundation. Mitigation giống Wave 30: Bucket A ship interface stubs commit đầu.

---

## 3. Scope (per bucket)

### Bucket A — KH foundation + customer dashboard home

- **Spec source:**
  - `documents/02-architecture/design-system/ui_kits/kitehub-pro-v2/screens/dashboard-{default,loading,empty,error,success,dark}.html` (6 state variants)
  - `documents/02-architecture/design-system/ui_kits/_shared/colors_and_type.css`
- **Foundation reuse evaluation:**
  - First state-check: `kiteclass-frontend/src/_shared/dashboard-foundation/` (Wave 30 Bucket A output). Đánh giá generic-vs-KC-specific.
  - Decision A: nếu generic (ThemeProvider + KPICard + Sparkline reusable) → factor sang `packages/shared-ui-app/dashboard-foundation/` workspace package + both apps consume. File ADR-XXX nếu workspace package mới.
  - Decision B: nếu KC-specific → duplicate ở `kitehub-frontend/src/_shared/dashboard-foundation/` (copy-paste OK; refactor sang workspace nếu drift painful sau Wave 32).
- **Files (depending on Decision A/B):**
  - Decision A: cross-app workspace package + KH consumer wiring
  - Decision B: KH-local primitives (mirror Wave 30 Bucket A list — ThemeProvider, KPICard, Sparkline, CommandPalette, SuccessConfetti)
- **Files to modify:**
  - `kitehub-frontend/src/app/(customer)/layout.tsx` — wrap với ThemeProvider, add CommandPalette
  - `kitehub-frontend/src/app/(customer)/dashboard/page.tsx` — subscription health KPIs + usage sparklines + tier-status card
- **Tests:** ≥6 — foundation reuse smoke, dashboard home render, KPI cards với KH-specific data shape (subscription health vs KC class metrics)
- **Acceptance:**
  - Customer dashboard home /128 ≥105 self-rescore
  - Subscription tier card shows current plan + usage progress
  - `pnpm -F @kite/kitehub-frontend build` clean

### Bucket B — Billing

- **Spec source:** `kitehub-pro-v2/screens/billing-{default,loading,empty,payment,dark}.html` (5 variants)
- **Files to modify:**
  - `(customer)/billing/page.tsx` — invoices list + payment methods + tier upgrade entry
- **Integration với @kite/shared-ui:**
  - `InvoiceDetail` + `formatVNCurrency` (Wave 27 G6) — đã ship cho KC Wave 30 Bucket D, KH consumes same components
  - `PaymentMethodSelector` (Wave 27 G5) — wire tier upgrade flow
- **Tests:** ≥4 — billing list render, invoice detail render via G6, PaymentMethodSelector tier upgrade smoke, empty-state render
- **Acceptance:** tokens applied + `pnpm build` clean

### Bucket C — Branding hub

- **Spec source:** `kitehub-pro-v2/screens/branding-hub-{default,dark,loading,quota-empty}.html` (4 variants) + branding-wizard-step* (4 placeholders, internals defer Wave 32)
- **Files to modify:**
  - `(customer)/branding/page.tsx` — branding hub: theme + logo upload + wizard entry CTA (placeholder href to `/branding/wizard` for Wave 32)
- **Integration với @kite/shared-ui:**
  - `ThemePreview` (Wave 29 G11) — wire vào branding hub theme customization (giống KC settings)
- **Tests:** ≥3 — branding hub render, ThemePreview integration, wizard CTA link present (href = placeholder)
- **Acceptance:** tokens applied + ThemePreview integrated + wizard CTA placeholder + `pnpm build` clean
- **Note:** wizard internals (Direction C 6-step + Enterprise Advanced + Quality Gate widget) DEFER Wave 32 (ai-branding-wizard-v2 standalone wave — 28 screens too big to bundle here).

### Bucket D — Instances + Settings

- **Spec source:**
  - `kitehub-pro-v2/screens/instance-{NOT_STARTED,GENERATING,DEPLOYED,REGENERATING,FAILED}.html` (5 lifecycle states)
  - Settings screens (assume baseline)
- **Files to modify:**
  - `(customer)/instances/page.tsx` — instance list với G9 InstanceLifecycleStatus per row
  - `(customer)/instances/[id]/page.tsx` — instance detail với full G9 timeline
  - `(customer)/settings/page.tsx` — settings basic (KH-side: notification prefs, locale, etc.)
- **Integration với @kite/shared-ui:**
  - `InstanceLifecycleStatus` (Wave 29 G9) — primary use case, wire với `/api/instances/{id}/status` polling (or mock if endpoint absent — flag follow-up)
- **Tests:** ≥4 — instances list render với G9 per row, instance detail với full G9 + timeline, settings render, mock status polling cho G9 transitions
- **Acceptance:** tokens applied + G9 wired (real or mock per state-check) + `pnpm build` clean

---

## 4. State-Check Evidence (per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Type | Verification | Verdict |
|--------|------|-------------|---------|
| `kitehub-frontend/src/app/(customer)/{dashboard,billing,branding,instances,settings}/` | route dirs | `ls` | ✅ exist |
| `kitehub-pro-v2/screens/*.html` | HTML proto | `ls` | ✅ 24 files exist |
| `_shared/colors_and_type.css` | design tokens | wc -l | ✅ exists |
| `@kite/shared-ui` workspace dep ở KH | pnpm dep | `grep '@kite/shared-ui' kitehub/kitehub-frontend/package.json` | needs verify trước spawn |
| `G6 InvoiceDetail` + `formatVNCurrency` (Wave 27) | shared-ui | `grep 'InvoiceDetail\|formatVNCurrency' packages/shared-ui/src/index.ts` | ✅ exist |
| `G5 PaymentMethodSelector` (Wave 27) | shared-ui | grep | ✅ exists |
| `G9 InstanceLifecycleStatus` (Wave 29) | shared-ui | grep | ✅ exists |
| `G11 ThemePreview` (Wave 29) | shared-ui | grep | ✅ exists |
| Wave 30 Bucket A foundation (`kiteclass-frontend/src/_shared/dashboard-foundation/`) | KC primitives | `ls` post-Wave-30-merge | 🔄 in-flight (Wave 30 Bucket A still running) |
| KH `(customer)/branding/wizard/` route | wizard route | `ls` | needs verify (Wave 32 scope — Bucket C just placeholder href) |
| `/api/instances/{id}/status` endpoint | backend | grep `instances` controllers | needs verify (Bucket D state-check; mock if absent) |

**Pre-spawn verify (coordinator):**
1. Wave 30 closure SHIPPED (Wave 30 Buckets A/B/C/D + closure PR all merged) — Wave 31 spawn AFTER
2. `pnpm -F @kite/kitehub-frontend build` baseline clean
3. Confirm `@kite/shared-ui` workspace dep ở `kitehub-frontend/package.json`

---

## 5. Verification Gates (per bucket)

| Bucket | Local verify command | Notes |
|--------|---------------------|-------|
| A | `pnpm -F @kite/kitehub-frontend type-check && test && build` | next build strict; foundation reuse decision documented in PR body |
| B | same — focus suites: billing | InvoiceDetail + PaymentMethodSelector consumed |
| C | same — focus suites: branding hub | ThemePreview consumed; wizard route placeholder OK |
| D | same — focus suites: instances + settings | G9 wired (real or mock) |

Coordinator post-merge: full `pnpm -F @kite/kitehub-frontend build` MUST pass.

---

## 6. Agent Spawn Pattern

Per `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md`:
- All 4 buckets `run_in_background: true` + `isolation: worktree`
- RELATIVE paths only
- Coordinator merge sequential A→B→C→D
- `(customer)/layout.tsx` shared edit: Bucket A own; B/C/D không sửa

**Spawn timing:** Wave 31 plan PR drafted DURING Wave 30 agents in-flight (3rd pipelined application). Spawn happens AFTER Wave 30 closure ships + token budget verify per `feedback_token_quota_spawn_timing.md`. **Khuyến nghị `/clear` giữa Wave 30 closure và Wave 31 spawn cùng session** (per `feedback_token_quota_spawn_timing.md` mid-session pattern).

**Domain-milestone audit:** Wave 31 thuộc cluster `phase-4-kit-ports` (per `post-wave-audit-mandate.md` §2.4.1). Trailer: `AUDIT_DEFER_DOMAIN_MILESTONE: phase-4-kit-ports — milestone TBD when 7 kits ship`. Milestone TBD — likely Wave 35-36 sau khi cả 7 kits ship.

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `feedback_post_merge_doc_sync.md` + `feedback_wave_history_append_required.md` + `post-wave-cleanup.md` + `feedback_wave_closure_release_progress_report.md` (NEW 2026-05-06):

- Mỗi bucket PR update GAP-270 Log
- **Status flip:** GAP-270 stays 🟡 PARTIAL post-Wave-31 (foundation + 4 page clusters; remaining: visual regression, E2E, polish iterations, ai-branding-wizard internals → Wave 32)
- ROADMAP §🚀 Next Action update — Wave 32 candidates: ai-branding-wizard (GAP-272) standalone OR Phase 1 BETA P0 deploy cluster
- **NEW:** Closure PR body PHẢI có "## Release Plan Progress" section (per memory `feedback_wave_closure_release_progress_report.md`):
  - Current Phase: Phase 1 BETA
  - Track 2 progress: 2 of 7 kits ported (KC pro v2 Wave 30 + KH pro v2 Wave 31); remaining 5 (ai-branding-wizard, teacher, parent K-12 SKIP, student K-12 SKIP, K-12 admin SKIP for Phase 1)
  - PDPL deadline countdown 2026-07-01 (~7 weeks)
  - BETA P0 BLOCKING gaps status (GAP-369/370/372/373/376/379)
  - Estimated waves còn lại đến Phase 1 BETA launch
- Wave plan frontmatter `status: complete` flip
- `wave-history.jsonl` append
- `bash scripts/prune-merged-worktrees.sh --yes` sau merge
- AUDIT trailer

**Follow-up gaps to file at closure:**
- GAP-270b — KH pro v2 visual regression baseline
- GAP-270c — KH pro v2 E2E test (owner login → dashboard → upgrade tier → see invoice)
- Wave 32 plan kickoff: ai-branding-wizard-v2 (GAP-272) — 28 screens, 6-step wizard + Enterprise Advanced + Quality Gate widget

---

## 8. Log

- **2026-05-06 (draft):** Plan tạo PIPELINED trong khi Wave 30 4 agents in-flight (Buckets A/B/C/D đang chạy). 3rd consecutive `wave-pack-planner` §Step 5.5 application — pattern stable across 3 waves (29→30, 30→31). State-check verified: KH `(customer)/` 5 dir exist; kitehub-pro-v2 24 screens; ai-branding-wizard-v2 separately (115.6/128 ⭐, 28 screens) → Wave 32 standalone scope (too big to bundle). Wave 30 dashboard-foundation reuse decision deferred to Bucket A agent runtime evaluation. Spawn timing: AFTER Wave 30 closure + `/clear` recommended per `feedback_token_quota_spawn_timing.md`. **NEW closure protocol bullet** per `feedback_wave_closure_release_progress_report.md` (saved 2026-05-06 same session) — Wave 31 closure PR sẽ là first wave applying release-plan-progress section.
