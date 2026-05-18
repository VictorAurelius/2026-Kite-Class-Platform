---
title: Wave 27 — Track 2 Phase 2 — port 4 priority shared-ui components (G2 + G5 + G6 + G7)
status: complete
created: 2026-05-06
updated: 2026-05-06
waves: [27]
gaps: [GAP-273]
---

# Wave 27 — Track 2 Phase 2 component port

**Goal:** Port 4 highest-priority shared-ui components from HTML prototypes → React/TypeScript inside `packages/shared-ui` per ADR-024, mirroring the proven `ConsentBanner` reference structure.
**Trigger:** ROADMAP §🚀 Next Action recommends Track 2 FE start. User picked "Option A" (umbrella plan §Phase 2 — safer, GAP-273 closes Wave 28-29). D1 dropped (no spec) → 4 G* components fit 4-agent cap cleanly.
**Estimated wall-clock:** ~60-90 min agent work, longest-bucket ~50 min. Closure ~15 min.

---

## 1. Brainstorm (5-10 min)

**Q1 (alignment):**
- Personas: P3 Center Owner/Admin, P5 K-12 (parent + teacher + principal), P3 Teacher, P3 Parent.
- Domains: KC daily ops (G2), KC + KH billing (G5/G6), KC parent enrollment (G7).
- Foundation for Phase 4 kit ports — 7 kit gaps (GAP-266..272) consume these components, 5 of 7 kits import ≥1 of these 4 (GAP-267 parent uses G7+G6, GAP-268 teacher uses G2, GAP-269 student uses G6, GAP-271 admin uses G2/G5/G6).

**Q2 (trade-offs):**
- 4 components × 1 agent vs 12 components × 4 buckets ×3 components: chose 4×1 per `feedback_token_quota_spawn_timing.md` + Wave 25 first-spawn quota-hit precedent. Single-component agents = ~120-180k tokens each = stays well below quota envelope. 12 components in one wave = ~1.8M tokens with HIGH PARTIAL risk.
- Drop D1 vs ship aspirational: D1 has no formal spec (only dossier note "destructive-action AlertDialog used by 3 flows in `courses/[id]/page.tsx`"). Agent designing spec from scratch ≠ proven port pattern. Per `audit-to-gap-pipeline.md` §2.6 wave-plan state-check, aspirational symbols without 🆕 to-be-created flag fail. Defer D1 to Wave 28 with explicit spec-creation Phase 0.
- pnpm workspace already bootstrapped (PR #713 merged 2026-04-30) — no infra setup risk.

**Q3 (risks):**
- **R1: `packages/shared-ui/src/index.ts` merge conflict.** All 4 buckets append export lines. **Mitigation:** coordinator concatenates in alphabetical order at merge time; each agent appends only their component's exports.
- **R2: HTML proto state-set incomplete.** Round 2 layout has `default + loading + empty + error + success` (5 states); spec.md lists 5-6 states. **Mitigation:** agent maps spec states ↔ HTML files; missing states ship as TODO + mention in PR body.
- **R3: VN-specific format gotchas.** G5 Payment Method (VNPay/MoMo/ZaloPay/Bank/Cash/QR strings), G6 Invoice (VN tax format `%`, currency `1.500.000đ`), G7 Parent Invite (Zalo OA share). **Mitigation:** agent reads spec.md verbatim; Vietnamese labels copy-pasted, never translated.
- **R4: ConsentBanner pattern variance.** ConsentBanner uses `Component.tsx + index.tsx + types.ts + storage.ts + useConsent.ts + __tests__/`. ADR-024 example shows `index.tsx + states.tsx + spec.md`. **Mitigation:** agents follow ConsentBanner shipped pattern (it's the production proof) — minimum required: `Component.tsx + index.tsx + types.ts + __tests__/Component.test.tsx`. spec.md mirror is optional but recommended.

---

## 2. Task Breakdown

| Bucket | Component | Owner | Effort | Disjoint? |
|--------|-----------|-------|--------|-----------|
| A | G2 Attendance Roster | bg-agent | ~50 min | ✅ `packages/shared-ui/src/components/G2-attendance-roster/` |
| B | G6 Invoice Detail | bg-agent | ~45 min | ✅ `packages/shared-ui/src/components/G6-invoice-detail/` |
| C | G5 Payment Method Selector | bg-agent | ~45 min | ✅ `packages/shared-ui/src/components/G5-payment-method-selector/` |
| D | G7 Parent Invite Flow | bg-agent | ~45 min | ✅ `packages/shared-ui/src/components/G7-parent-invite/` |

**Disjoint check:** each bucket touches only its component subfolder under `packages/shared-ui/src/components/`. Shared file `packages/shared-ui/src/index.ts` modified additively (each adds 1-3 export lines) — coordinator resolves alphabetical order at merge.

---

## 3. Scope (per bucket)

### Bucket A — G2 Attendance Roster (P/V/M/L per-student grid)

- **Spec source:** `documents/02-architecture/design-system/dossier/04-component-gaps.md` §G2 + `ui_kits/components/G2-attendance-roster/spec.md` (130 lines) + 5 HTML state files (`default.html`, `loading.html`, `empty.html`, `error.html`, `success.html`).
- **Files to create:**
  - `packages/shared-ui/src/components/G2-attendance-roster/AttendanceRoster.tsx` — main component
  - `packages/shared-ui/src/components/G2-attendance-roster/index.tsx` — public re-export
  - `packages/shared-ui/src/components/G2-attendance-roster/types.ts` — `AttendanceStatus = 'P' | 'V' | 'M' | 'L'`, `AttendanceRosterProps`, `StudentRecord`
  - `packages/shared-ui/src/components/G2-attendance-roster/__tests__/AttendanceRoster.test.tsx` — Vitest + RTL tests covering 5 states + per-student toggle + sticky save bar
  - `packages/shared-ui/src/components/G2-attendance-roster/spec.md` — mirror of HTML proto spec
- **Tests (per ConsentBanner reference pattern):** ≥6 tests — render 5 states + interaction (toggle status + save bar visibility on dirty).
- **`src/index.ts` exports added:** `AttendanceRoster`, `AttendanceRosterProps`, `AttendanceStatus`, `StudentRecord`.
- **Acceptance:** all 5 states render without console error, P/V/M/L cycle on click, save bar appears when ≥1 row dirty, Vietnamese labels (`Có mặt`, `Vắng`, `Muộn`, `Lý do`) copy-pasted from spec, WCAG AA color contrast on all 4 status badges.

### Bucket B — G6 Invoice Detail (VN tax + currency format)

- **Spec source:** dossier 04 §G6 + `ui_kits/components/G6-invoice-detail/spec.md` (118 lines) + 5 HTML state files.
- **Files to create:**
  - `.../G6-invoice-detail/InvoiceDetail.tsx`
  - `.../G6-invoice-detail/index.tsx`
  - `.../G6-invoice-detail/types.ts` — `InvoiceLineItem`, `InvoiceDetailProps`, `InvoiceTaxBreakdown`
  - `.../G6-invoice-detail/utils.ts` — `formatVNCurrency(amount: number): string` (VN format `1.500.000đ`), `formatVNTax(rate: number): string`
  - `.../G6-invoice-detail/__tests__/InvoiceDetail.test.tsx`
  - `.../G6-invoice-detail/__tests__/utils.test.ts` — VN currency edge cases (0, decimals, 1B+)
  - `.../G6-invoice-detail/spec.md`
- **Tests:** ≥8 — 5 state renders + currency format edge cases (0 đ, 1.500.000đ, decimals truncated) + tax breakdown summation.
- **`src/index.ts` exports added:** `InvoiceDetail`, `InvoiceDetailProps`, `InvoiceLineItem`, `InvoiceTaxBreakdown`, `formatVNCurrency`, `formatVNTax`.
- **Acceptance:** print-friendly (no fixed pixel widths), VN currency `1.500.000đ` (period thousands separator + đ suffix, not VND), VAT % displayed `8%` not `0.08`.

### Bucket C — G5 Payment Method Selector (VN multi-gateway)

- **Spec source:** dossier 04 §G5 + `ui_kits/components/G5-payment-method-selector/spec.md` (108 lines) + 5 HTML state files.
- **Files to create:**
  - `.../G5-payment-method-selector/PaymentMethodSelector.tsx`
  - `.../G5-payment-method-selector/index.tsx`
  - `.../G5-payment-method-selector/types.ts` — `PaymentMethod = 'VNPAY' | 'MOMO' | 'ZALOPAY' | 'BANK_TRANSFER' | 'CASH' | 'QR'`, `PaymentMethodSelectorProps`
  - `.../G5-payment-method-selector/__tests__/PaymentMethodSelector.test.tsx`
  - `.../G5-payment-method-selector/spec.md`
- **Tests:** ≥7 — 5 state renders + selection (single-pick semantics) + onChange callback.
- **`src/index.ts` exports added:** `PaymentMethodSelector`, `PaymentMethodSelectorProps`, `PaymentMethod`.
- **Acceptance:** all 6 method labels in Vietnamese (`Chuyển khoản ngân hàng`, `Tiền mặt`, `Mã QR`, etc.), keyboard navigation (radio-group semantics), selected state visible, disabled state for unavailable methods.

### Bucket D — G7 Parent Invite Flow (email/token + Zalo OA)

- **Spec source:** dossier 04 §G7 + `ui_kits/components/G7-parent-invite/spec.md` (124 lines) + 5 HTML state files.
- **Files to create:**
  - `.../G7-parent-invite/ParentInvite.tsx`
  - `.../G7-parent-invite/index.tsx`
  - `.../G7-parent-invite/types.ts` — `InviteChannel = 'EMAIL' | 'ZALO_OA'`, `ParentInviteProps`, `InviteState = 'idle' | 'sending' | 'sent' | 'error'`
  - `.../G7-parent-invite/__tests__/ParentInvite.test.tsx`
  - `.../G7-parent-invite/spec.md`
- **Tests:** ≥7 — 5 state renders + channel toggle + error message rendering.
- **`src/index.ts` exports added:** `ParentInvite`, `ParentInviteProps`, `InviteChannel`, `InviteState`.
- **Acceptance:** Zalo OA share button visually distinct (Zalo blue brand), email validation (Zod schema), token copy-to-clipboard works (use `navigator.clipboard`), Vietnamese labels (`Mời phụ huynh`, `Gửi qua Zalo OA`).

---

## 4. State-Check Evidence (BẮT BUỘC per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Type | Verification command | Evidence | Verdict |
|--------|------|----------------------|----------|---------|
| `packages/shared-ui/` workspace | pnpm package | `cat packages/shared-ui/package.json` | `@kite/shared-ui v0.1.0` workspace package present (PR #713) | ✅ exists |
| `ConsentBanner` reference pattern | shipped component | `ls packages/shared-ui/src/components/ConsentBanner/` | 6 files: `ConsentBanner.tsx`, `__tests__/`, `api.ts`, `index.tsx`, `storage.ts`, `types.ts`, `useConsent.ts` | ✅ exists (Wave 23 GAP-353) |
| `ui_kits/components/G2-attendance-roster/spec.md` | HTML proto spec | `wc -l documents/02-architecture/design-system/ui_kits/components/G2-attendance-roster/spec.md` | 130 lines | ✅ exists |
| `ui_kits/components/G5-payment-method-selector/spec.md` | HTML proto spec | `wc -l .../G5-payment-method-selector/spec.md` | 108 lines | ✅ exists |
| `ui_kits/components/G6-invoice-detail/spec.md` | HTML proto spec | `wc -l .../G6-invoice-detail/spec.md` | 118 lines | ✅ exists |
| `ui_kits/components/G7-parent-invite/spec.md` | HTML proto spec | `wc -l .../G7-parent-invite/spec.md` | 124 lines | ✅ exists |
| `dossier/04-component-gaps.md` §G2/G5/G6/G7 | dossier entries | `grep -nE "^### G[2567]\\." documents/02-architecture/design-system/dossier/04-component-gaps.md` | lines 38, 75, 87, 98 | ✅ exists |
| `vitest` + `@testing-library/react` | dev deps | `cat packages/shared-ui/package.json \| grep -E "vitest\|testing-library"` | vitest ^4.1.5, @testing-library/react ^16.3.2, jsdom ^27.4.0 | ✅ exists |
| `transpilePackages: ['@kite/shared-ui']` in both `next.config.js` | workspace consumer | `grep -l "@kite/shared-ui" kiteclass/kiteclass-frontend/next.config.js kitehub/kitehub-frontend/next.config.js` | both files reference | ✅ exists (PR #713) |
| ADR-024 ACCEPTED | architecture decision | `grep "^**Status:**" documents/02-architecture/adr/ADR-024-shared-ui-lib-strategy.md` | "ACCEPTED 2026-04-30" | ✅ exists |
| `AttendanceRoster.tsx` | React component | `grep -rn "AttendanceRoster" packages/shared-ui/src/` | 0 matches | 🆕 to-be-created (Bucket A) |
| `InvoiceDetail.tsx` | React component | `grep -rn "InvoiceDetail" packages/shared-ui/src/` | 0 matches | 🆕 to-be-created (Bucket B) |
| `PaymentMethodSelector.tsx` | React component | `grep -rn "PaymentMethodSelector" packages/shared-ui/src/` | 0 matches | 🆕 to-be-created (Bucket C) |
| `ParentInvite.tsx` | React component | `grep -rn "ParentInvite" packages/shared-ui/src/` | 0 matches | 🆕 to-be-created (Bucket D) |
| `formatVNCurrency` | utility function | `grep -rn "formatVNCurrency" packages/` | 0 matches | 🆕 to-be-created (Bucket B) |

D1 Generic Confirm Dialog explicitly **not in scope** — no formal spec.md or HTML proto. Tracked in follow-up Wave 28 Phase 0 (design D1 spec from existing `(dashboard)/courses/[id]/page.tsx` 3 destructive-flow usage pattern).

---

## 5. Verification Gates (per bucket)

| Bucket | Local verify command | CI gate |
|--------|---------------------|---------|
| A | `pnpm -F @kite/shared-ui type-check && pnpm -F @kite/shared-ui test -- AttendanceRoster` | shared-ui CI (TBD — see §7 follow-up) |
| B | `pnpm -F @kite/shared-ui type-check && pnpm -F @kite/shared-ui test -- InvoiceDetail` | shared-ui CI |
| C | `pnpm -F @kite/shared-ui type-check && pnpm -F @kite/shared-ui test -- PaymentMethodSelector` | shared-ui CI |
| D | `pnpm -F @kite/shared-ui type-check && pnpm -F @kite/shared-ui test -- ParentInvite` | shared-ui CI |

Each agent must run `pnpm -F @kite/shared-ui test` (full suite) before commit to ensure no regression on `ConsentBanner` tests (47/47 baseline).

**Cross-app smoke test (coordinator at closure):** import 1 component into each frontend's a dev page (gated `process.env.NEXT_PUBLIC_DEV_DEMO === '1'`) to verify `transpilePackages` works end-to-end. If demo route addition seems heavy, defer to Wave 28 closure.

---

## 6. Agent Spawn Pattern

Per `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md`:
- All 4 buckets spawned with `run_in_background: true`
- Worktree isolation (`isolation: worktree`) for parallel safety
- RELATIVE paths in agent prompts per `feedback_worktree_absolute_path_contamination.md`
- Each agent ships 1 PR (4 PRs total)
- Coordinator merges sequentially A→B→C→D after all 4 background completions
- `src/index.ts` conflicts resolved by coordinator (additive concatenation, alphabetical order)

**Agent briefing template** (use per-bucket):
> Port `<G#-component-name>` from HTML proto → React/TypeScript inside `packages/shared-ui/src/components/<G#-component-name>/`.
>
> **Pattern reference:** `packages/shared-ui/src/components/ConsentBanner/` is the production-proof structure. Mirror it: `Component.tsx` + `index.tsx` + `types.ts` + `__tests__/Component.test.tsx`. Optionally add `spec.md` mirror.
>
> **Spec sources (READ FIRST):** `dossier/04-component-gaps.md` §G# + `ui_kits/components/<G#-folder>/spec.md` + 5 HTML state files (`default.html`, `loading.html`, `empty.html`, `error.html`, `success.html`).
>
> **TDD discipline:** test-first per state. Vitest + RTL. Vietnamese labels copy-pasted from spec.md verbatim — never translate.
>
> **Verify before commit:** `pnpm -F @kite/shared-ui type-check && pnpm -F @kite/shared-ui test`. Full suite must pass (ConsentBanner 47 tests must stay green).
>
> **Exports:** append your component's public API to `packages/shared-ui/src/index.ts`. If merge conflict at coordinator-side, agent's section is the canonical version.
>
> **Status flip:** GAP-273 stays 🟡 PARTIAL after this PR (1 of 12 G* shipped). Do NOT flip to 🟢 DONE.

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `feedback_post_merge_doc_sync.md` + `feedback_wave_history_append_required.md` + `post-wave-cleanup.md`:

- Each bucket PR updates `GAP-273` Log (+ "1/12 ported via Wave 27 Bucket X" entry); **status stays 🟡 PARTIAL** (only 4/12 of full GAP-273 scope).
- ROADMAP §🚀 Next Action updated in closure PR — recommend Wave 28 = next 4 components OR D1 spec creation Phase 0.
- Wave plan frontmatter `status: complete` flip in closure PR.
- `wave-history.jsonl` append in closure PR (Rule 15 — `session-docs-check` Rule N enforcement).
- Sub-gaps file IF: any bucket ships PARTIAL (e.g., 4 of 5 states implemented). Per `gap-done-discipline.md` §3 PARTIAL exit ramp.
- Run `bash scripts/prune-merged-worktrees.sh --yes` after all 4 bucket PRs merged + before drafting closure PR (per `post-wave-cleanup.md`).

**Follow-up gaps to file at closure (anticipated):**
- Cross-app dev demo route(s) for shared-ui smoke test (deferred from §5).
- Visual regression baseline capture for the 4 components (`Lighthouse` / Playwright).
- D1 Generic Confirm Dialog spec creation (Wave 28 Phase 0 prerequisite).
- shared-ui dedicated CI workflow (currently no gate runs `pnpm -F @kite/shared-ui test` on PR) — meta-P1 candidate per `meta-gap-priority.md`.

---

## 8. Log

- **2026-05-06 (draft):** Plan created on branch `wave/27-plan`. State-check verified all 4 spec sources + ConsentBanner reference pattern + workspace infra. D1 dropped (no formal spec, deferred to Wave 28 Phase 0). 4-agent cap respected (1 component per agent). Token risk LOW per `feedback_token_quota_spawn_timing.md` (single-component scope = ~120-180k tokens/agent vs ~450k/agent for 3-component scope rejected in option C).
- **2026-05-06 (complete):** Wave SHIPPED. 4 PRs (#848/#849/#850/#851) squash-merged after coordinator-resolved 4 additive `packages/shared-ui/src/index.ts` conflicts (B/C/D all touched same alphabetical export region — 4 conflicts predicted, 4 resolved). Final shared-ui state: 108/108 tests (47 ConsentBanner baseline + 12 G2 + 27 G6 + 9 G5 + 13 G7) + type-check clean. Wall-clock ~7-8min/agent parallel vs ~50min plan estimate. Token cost: A 300k / B 313k / C 192k / D 297k = ~1.1M total + coordinator. Bucket C narrower than briefing scope (5 methods per spec vs briefing-claimed 6, `QR` is a state not a method); agent correctly read spec authoritative. 4 follow-up items: shared-ui dedicated CI workflow (no remote CI runs on `packages/shared-ui/**` yet — meta-P1), cross-app smoke test dev demo route, D1 Confirm Dialog spec creation (Wave 28 Phase 0 prerequisite), visual regression baseline. GAP-273 stays 🟡 PARTIAL (4/12 G* shipped). 63rd consecutive 0-clarification streak.
