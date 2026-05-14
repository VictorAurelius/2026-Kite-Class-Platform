---
title: Wave Track 2 UI Kits Production Port — Multi-week Umbrella Plan (Phase 1+2+3 DONE; 5 kit OPEN + Phase 5+6 pending)
status: in-progress
created: 2026-04-30
updated: 2026-05-10
gaps: [GAP-266, GAP-267, GAP-268, GAP-269, GAP-270, GAP-271, GAP-272, GAP-273, GAP-274, GAP-275, GAP-276, GAP-277, GAP-278, GAP-279, GAP-280]
phase_1_done: [ADR-024, GAP-273-Phase-1]
phase_2_done: [GAP-273-Phase-2-G2, GAP-273-Phase-2-G6, GAP-273-Phase-2-G5, GAP-273-Phase-2-G7]
phase_3_done: [GAP-273-Phase-3-G3, GAP-273-Phase-3-G4, GAP-273-Phase-3-G8, GAP-273-Phase-3-G10, GAP-273-Phase-3-D1, GAP-273-Phase-3-G1, GAP-273-Phase-3-G9, GAP-273-Phase-3-G11, GAP-273-Phase-3-G12]
phase_4_partial: [GAP-266, GAP-270]
phase_4_open: [GAP-267, GAP-268, GAP-269, GAP-271, GAP-272]
phase_5_open: [GAP-274, GAP-275, GAP-276, GAP-277, GAP-278, GAP-279, GAP-280]
phase_6_open: [bundle-optimization, lighthouse-baseline, e2e-playwright, visual-regression-baseline]
deferred_separate_track: []
adr_reference: ADR-024
---

<!-- wave-plan-completeness-exempt: Pre-Wave-76 legacy plan — predates current _TEMPLATE.md structure -->

# Wave Track 2 UI Kits Production Port — Multi-week Umbrella

**Wave date range:** 2026-04-30 (Phase 1 ADR + scaffolding DONE) → MVP launch (~4-6 weeks projected)
**Cluster theme:** Port HTML prototype kits (Round 2 + Round 3) → production Next.js apps. Multi-week scope, NOT single wave-pack.
**Strategy reference:** ADR-024 ACCEPTED 2026-04-30 ([`documents/02-architecture/adr/ADR-024-shared-ui-lib-strategy.md`](../../02-architecture/adr/ADR-024-shared-ui-lib-strategy.md)) — pnpm workspace + `@kite/shared-ui` package.

---

## ⚠️ Status sync 2026-05-10

Per state-check audit `audit-to-gap-pipeline.md` §2.5/§2.7, Phase 2 + Phase 3 đã thực sự ship qua Waves 27/28/29 (2026-05-06) nhưng phần "Phase 2-6 Multi-week port roadmap" bên dưới vẫn đang được viết ở dạng "tương lai sẽ ship". Phần đó **đã thành lịch sử cho Phase 2 + 3**; chỉ còn pending Phase 4 (5 kit OPEN + 2 PARTIAL follow-up), Phase 5 (7 coverage gap), Phase 6 (hardening).

**Hiện trạng tổng hợp** (verified 2026-05-10):

| Phase | Trạng thái | Bằng chứng |
|-------|-----------|------------|
| 1 ADR + scaffolding | ✅ DONE 2026-04-30 PR #713 | `packages/shared-ui/` workspace + ADR-024 ACCEPTED |
| 2 — G2/G6/G5/G7 | ✅ DONE Wave 27 (2026-05-06) | 4 component thư mục đầy đủ + tests + exports trong `index.ts` |
| 3 — G3/G4/G8/G10/D1 + G1/G9/G11/G12 | ✅ DONE Waves 28/29 (2026-05-06) | 9 component thư mục + 23 test files × 307 tests PASS |
| 4 — 7 kit ports | 🟡 2/7 PARTIAL + 5/7 OPEN | GAP-266 (Wave 30) + GAP-270 (Wave 31) PARTIAL; GAP-267/268/269/271/272 OPEN |
| 5 — 7 coverage gap | ⏳ tất cả OPEN | GAP-274..280 chưa start |
| 6 — Hardening | ⏳ chưa start | Bundle / Lighthouse / E2E / visual regression baseline pending |

**Đường găng còn lại:** Phase 4 (5 kit OPEN) → Phase 4 PARTIAL follow-up close → Phase 5 → Phase 6. Ước tính ~28-39h (giảm so với ~30-40h ban đầu vì component lib đã sạch sẽ).

---

## Phase 1 — DONE (PR #713 merged 2026-04-30)

✅ **ADR-024 ACCEPTED** — pnpm workspace strategy
✅ **PR #713 SHIPPED** — workspace bootstrap + empty `@kite/shared-ui` package scaffolding
- `package.json` (root) + `pnpm-workspace.yaml`
- `packages/shared-ui/` với `package.json`, `tsconfig.json`, `src/index.ts`, `src/styles/tokens.css`, `README.md`
- `transpilePackages: ['@kite/shared-ui']` added to both Next.js `next.config.js`
- pnpm overrides moved root-level (per pnpm 10 workspace requirement)
- `.gitignore` excludes `/packages/*/node_modules/`

**Phase 1 status:** GAP-273 Phase 1 complete (12-component shared lib infrastructure ready, 0 components ported yet).

---

## Phase 2-6 — Multi-week port roadmap (15 OPEN gaps)

### Phase 2 — Component foundation (~3-5 days, 1 wave-pack)

**GAP-273 Phase 2:** Port 5 highest-priority shared components first (each as its own sub-wave or 1 component-per-agent batch):

| Priority | Component | Used by | Sub-wave candidate |
|:--------:|-----------|---------|---------------------|
| 1 | **G2 Attendance Roster** | KC daily ops, P5 K-12 critical | feature-tdd-agent (1 component) |
| 2 | **G6 Invoice Detail** | KC + KH billing flows | feature-tdd-agent |
| 3 | **G5 Payment Method Selector** | KC payment flows | feature-tdd-agent |
| 4 | **G7 Parent Invite** | KC parent enrollment | feature-tdd-agent |
| 5 | **D1 Generic Confirm Dialog** | both apps universal | feature-tdd-agent |

**Wave-pack candidate:** 4-5 components in parallel agents × 1 wave-pack ~120-180 min. Foundation ships shared component test infra (Playwright + Vitest in `packages/shared-ui/__tests__/`).

### Phase 3 — Component completion (~5-7 days, 2-3 wave-packs)

**GAP-273 Phase 3:** Remaining 7 components from Round 3:
- G1 Avatar + Initial
- G3 Class Schedule Cell
- G4 Tuition Status Badge
- G8 Calendar Compact
- G9 Instance Lifecycle Card
- G10 Wizard Step Indicator
- G11 Theme Switch Toggle
- G12 Bulk Actions Bar

Plus ~10 D* dialogs from `dossier/12-modal-dialog-inventory-{kc,kh}.md`.

**Wave-pack pattern:** 4 agents × 2-3 components each, 2-3 waves total.

### Phase 4 — Kit ports (~2-3 weeks, 4-5 wave-packs)

7 production Next.js apps to port from HTML prototypes. Each kit consumes shared components from Phase 2-3.

| Gap | Kit | Persona | Priority | Notes |
|-----|-----|---------|:--------:|-------|
| **GAP-266** | `kiteclass-pro v2` | P2 Owner + P3 Director (KC tenant admin) | 🔴 P0 | Direction B with ⌘K palette, sparklines |
| **GAP-267** | `kiteclass-parent` | Parent in K-12 (P5) + Center (P3) | 🔴 P0 | Direction D PWA-grade web; **LEGAL MANDATE per parent-in-P5 AC** |
| **GAP-268** | `kiteclass-teacher` | Teacher employee (P3 + P5 GVCN/bộ môn) | 🔴 P0 | Multi-class gradebook + GVCN workflow |
| **GAP-269** | `kiteclass-student` | Student in K-12 + Center (P3 + P5) | 🔴 P0 | Mobile PWA, route NEW for self-service |
| **GAP-270** | `kitehub-pro v2` | P2 Owner SaaS dashboard | 🔴 P0 | KH multi-tenant management |
| **GAP-271** | `kitehub-admin` | K-12 Principal (P5 USER PRIORITY) | 🔴 P0 | NEW K-12 scope; consumes admin-in-P5 AC |
| **GAP-272** | `ai-branding-wizard v2` | Owner provisioning flow | 🔴 P0 | Direction C 6-step refactor + ENTERPRISE Advanced Mode |

**Sub-wave pattern:** 4 kits per wave-pack × 2 waves to cover 7 kits.

### Phase 5 — Coverage gaps (~1-2 weeks, 2-3 wave-packs)

7 audit-driven gaps from Wave UI Coverage Audit (#710):

| Gap | Coverage scope |
|-----|----------------|
| **GAP-274** | KC public marketing kit |
| **GAP-275** | KH public marketing + blog kit |
| **GAP-276** | Auth flows kit (KC + KH) |
| **GAP-277** | Error pages kit + best-practice fixes |
| **GAP-278** | KH platform admin kit (KH ops, distinct from K-12 Principal) |
| **GAP-279** | Common modals + dialogs catalog (D1..D10) |
| **GAP-280** | Onboarding wizard kit (initial tenant first-run) |

### Phase 6 — Production hardening (~3-5 days, 1 wave-pack)

- Bundle size optimization (per `feedback_nextjs_rsc_array_regression.md` + GAP-127 code-splitting)
- Lighthouse CI baseline ≥90 across all kit routes
- E2E Playwright per critical user journey
- Visual regression baseline với Round 2/3 HTML prototypes
- Production environment variable wiring

---

## Dependencies + sequencing

```
Phase 1 (DONE) → Phase 2 (5 priority components)
                    ↓
                 Phase 3 (7 components + 10 dialogs)
                    ↓
                 Phase 4 (7 kit ports — consumes Phase 2-3)
                    ↓
                 Phase 5 (7 coverage kits)
                    ↓
                 Phase 6 (hardening)
```

**Hard dependencies:**
- Phase 4 BLOCKED by Phase 2-3 (kit ports import shared components)
- GAP-271 kitehub-admin (K-12 Principal) consumes admin-in-P5 AC + GVCN workflow components → ensure Phase 4 sequencing accounts for AC reference
- GAP-267 kiteclass-parent consumes parent-in-P5 AC (LEGAL MANDATE) → Phase 4 prioritize after Phase 2 G7 Parent Invite + G6 Invoice Detail ready

**Soft dependencies:**
- Phase 5 coverage gaps can interleave with Phase 4 if file-overlap manageable

---

## Wave-pack methodology applied

Per `wave-pack-planner` SKILL + lessons from Waves 13/14/15/16 (4 consecutive sub-35-min waves established methodology):

- 4-agent max-cap per wave-pack (`feedback_parallel_agent_strategy.md` rule #9)
- `feature-tdd-agent.md` template for component port (NOT skeleton — real code with tests)
- Worktree pre-prune BEFORE final merge (Wave 13/14/15/16 lesson)
- Foundation PR per Phase với scope + agent assignments
- Closure PR per wave với GAP status flips + ROADMAP

**Wall-clock estimate per phase:**
- Phase 2: 1 wave-pack ~3h (5 components × ~30 min/component, parallel)
- Phase 3: 2-3 wave-packs ~6-9h cumulative
- Phase 4: 4-5 wave-packs ~12-15h cumulative
- Phase 5: 2-3 wave-packs ~6-9h cumulative
- Phase 6: 1 wave-pack ~2-3h
- **Total Track 2 engineering: ~30-40h (~1-1.5 weeks at 4-5h/day solo-dev rate, OR ~2-3 weeks at 2-3h/day)**

**Compare with original `feedback_wave_pack_cross_gap_clustering.md` benchmark:** 3 gaps × ~75 min wave-pack = ~5x speedup vs serial. Track 2 has 15 gaps → expect ~10-15h total wave-pack effort vs ~50-75h serial.

---

## Acceptance criteria (umbrella-level)

- [ ] Phase 2 — 5 priority components ported + tested (1 wave-pack)
- [ ] Phase 3 — 7 remaining components + ~10 dialogs (2-3 wave-packs)
- [ ] Phase 4 — 7 kit ports complete (4-5 wave-packs)
- [ ] Phase 5 — 7 coverage gaps closed (2-3 wave-packs)
- [ ] Phase 6 — production hardening (1 wave-pack)
- [ ] All 15 gaps GAP-266..280 → 🟢 DONE
- [ ] GAP-273 → 🟢 DONE (when all 12 G* + ~10 D* shipped)
- [ ] Lighthouse CI ≥90 across critical routes
- [ ] E2E Playwright passes for parent-in-P5 + admin-in-P5 + student-in-P5 critical journeys

---

## Out of Scope (separate track)

- **Tier 2/3 persona kits** (P4/P7/P8/P9 production apps) — wave-pack post-MVP
- **Mobile native apps** (Flutter/React Native) — separate Track 3
- **Backend changes** (only frontend port; backend GAPs tracked separately)
- **AI Branding integration** (post-MVP; ai-branding-wizard v2 = HTML kit only this track)

---

## Cross-references

- **ADR:** [`ADR-024 Shared UI Library Strategy`](../../02-architecture/adr/ADR-024-shared-ui-lib-strategy.md)
- **Phase 1 PR:** [#713 pnpm workspace bootstrap](https://github.com/VictorAurelius/2026-Kite-Class-Platform/pull/713) (merged 2026-04-30)
- **Round 2 + 3 HTML kits:** `documents/02-architecture/design-system/ui_kits/`
- **Component dossier:** `documents/02-architecture/design-system/dossier/04-component-gaps.md`
- **Modal dossier:** `documents/02-architecture/design-system/dossier/12-modal-dialog-inventory-{kc,kh}.md`
- **Common components:** `documents/02-architecture/design-system/dossier/14-common-components-inventory-{kc,kh}.md`
- **Persona AC docs (consume in Phase 4):** `documents/00-brd/persona-criteria/` + `documents/00-brd/persona-criteria/secondary/`
- **Memory:** `feedback_nextjs_rsc_array_regression.md` (RSC pin rationale), `feedback_dependabot_pnpm_transitive.md` (workspace + dependabot)

---

## Log

- **2026-04-30** — Umbrella wave plan filed at session-cleanup time. Phase 1 (ADR + workspace scaffolding) DONE via PR #713. Phase 2-6 enumeration tracked here so future sessions don't re-derive scope. Wave-pack methodology proven (Waves 13-16 = 4 consecutive sub-35-min waves) — apply to Phase 2-6 sub-waves. Trigger to start Phase 2: when MVP-essential components (G2/G6) become blockers for Wave 17 review findings, OR explicit user kick-off.
