# GAP-272: Track 2 Port — ai-branding-wizard v2 → production Next.js

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (UX growth — Direction C wizard refactor)
**Domain:** Frontend
**Found:** 2026-04-29
**Affects:** `kitehub-frontend/src/app/(customer)/branding/` — AI Branding wizard route

## Problem

HTML prototype `ai-branding-wizard-v2/` (avg **115.6/128** ⭐⭐, 28 screens, R2 PR #675) is the highest-scoring R2 kit. Direction C 6-step wizard refactor + ENTERPRISE Advanced Mode separate path + quality gate /100 widget + per-resource approve. Production AI Branding flow predates Round 2 redesign.

## Current State

KH branding hub exists at `kitehub-frontend/src/app/(customer)/branding/`. Existing flow may be partial/legacy. Per `ai-branding-guidelines.md` §2.4, Direction C is the canonical wizard pattern.

## Proposed Fix

Port 28 wizard screens covering 6-step provisioning + Enterprise Advanced Mode + quality gate.

**Scope:**
- 6-step wizard: Welcome → Logo upload → Audience → Tone → Template choose → Preview+approve
- Per-resource approve toggle (logo / colors / banner / hero separately) per `ai-branding-guidelines.md` §4.2
- Quality gate /100 widget with WCAG fail surface (G11 component)
- Tier-based regenerate counter visible (FREE 3 / PRO 10 / PREMIUM 30 / ENTERPRISE unlimited) per §4.3
- ENTERPRISE Advanced Mode separate path with free-prompt opt-in (gated by `ai.enterprise.advancedModeEnabled`)
- Input prompt token cap UI (FREE 2k / PRO 4k / PREMIUM 8k / ENTERPRISE 16k tokens) per `ai-branding-guidelines.md` §2.5
- Quality gate scoring transparency

## Acceptance Criteria

- [ ] All 28 screens ≥110/128 (kit was 115.6 ⭐⭐)
- [ ] 6-step wizard flow E2E (welcome → deploy)
- [ ] G11 theme preview component imported (post-GAP-273)
- [ ] WCAG warning surface with auto-suggested fixes (reflexive coverage)
- [ ] Per-resource approve toggle works
- [ ] Regenerate counter decrements + disables on quota exhaust
- [ ] Enterprise Advanced Mode toggle gated by `ai.enterprise.advancedModeEnabled` config
- [ ] Input token cap reject HTTP 400 with `AI_INPUT_TOO_LONG` (existing GAP-258 backend)
- [ ] Compliance with `ai-branding-guidelines.md` §2.1/§2.2/§2.4/§2.5/§4.1/§4.2/§4.3/§5/§6
- [ ] Vietnamese-only
- [ ] WCAG AA preserved

## Related

- HTML prototype: `ui_kits/ai-branding-wizard-v2/`
- Rule: `.claude/rules/ai-branding-guidelines.md`
- Component dependency: GAP-273 (G11 theme preview)
- Existing AI Branding governance: GAP-223 (audit-gate + skill + matrix), GAP-006 (Gemma 4 9B migration deferred)
- Sister gap: GAP-270 (kitehub-pro-v2 entry route to wizard)

## Sub-letters (Wave 32 + REWORK follow-ups)

Wave 32 v1 plan §7 pre-named planned letters (b–g). Wave 32 REWORK closure 2026-05-07 reconciled with planned letters + added new findings (h–m). Status as of 2026-05-07:

| Letter | Topic | Priority | Source | Status |
|--------|-------|:--------:|--------|--------|
| 272b | audience/tone/template persistence | — | Plan §7 | **Not filed** — Bucket B confirmed inline persistence via existing `BrandingGenerationRequest.targetAudience` |
| 272c | quality-gate score aggregator endpoint | 🟠 P1 | Plan §7 + Bucket D | 🟡 PARTIAL (Wave 34 PR #906 — orchestration done; sub-checks tracked GAP-226/227/228) |
| 272d | regenerate quota tracking endpoint | 🟠 P1 | Plan §7 + Bucket D | 🟢 DONE (Wave 34 PR #907+#910) |
| 272e | SSE deploying log streaming endpoint | 🟠 P1 | Plan §7 + Bucket D | 🟡 PARTIAL (Wave 34 PR #907+#910 — endpoint+hook done; orchestrator wiring → 272o; queue→poll swap deferred) |
| 272f | Wave 32 visual regression baseline | 🟡 P2 | Plan §7 | 🔵 OPEN |
| 272g | E2E test welcome→deploy happy path | 🟡 P2 | Plan §7 | 🔵 OPEN |
| 272h | Convert Wave 32 inline mocks → MSW + hook pattern | 🟠 P1 | Closure audit (user-flagged) | 🟢 DONE (Wave 34 PR #910) |
| 272i | Slug-availability backend endpoint | 🟠 P1 | Bucket A | 🟢 DONE (Wave 34 PR #907+#910) |
| 272j | Iframe live preview render endpoint | 🟠 P1 | Bucket C | 🟢 DONE (Wave 34 PR #906+#910) |
| 272k | Live brand colors from generate endpoint | 🟠 P1 | Bucket C | 🟡 PARTIAL (Wave 34 PR #906+#910 — DTO+FE done; real source from analyze pipeline deferred) |
| 272l | Real `InstanceLifecycleService` integration (§6 compliance) | 🔴 P0 | Bucket D | 🟢 DONE (Wave 34 PR #908+#910) |
| 272m | Server-side persistence for Advanced Mode | 🟡 P2 | Bucket D | 🔵 OPEN |
| 272n | Align `POST /regenerate` response on wrapper DTO | 🟡 P2 | Wave 34 closure | 🔵 OPEN (filed 2026-05-07) |
| 272o | Wizard orchestrator wires deploy-stream + regenerate-quota | 🟠 P1 | Wave 34 closure | 🔵 OPEN (filed 2026-05-07) |

**Cluster recommendation:** GAP-272c/d/e/i/j/k (6 backend endpoints, all P1) + GAP-272l (P0 §6 compliance) → "AI Branding wizard backend cluster" Wave 34 candidate. Pre-cluster requirement: api-contract.md update first per `feedback_fe_first_endpoint_proliferation.md` (post-rework retro memory).

**Tech debt cluster:** GAP-272h refactor → can ship as part of Wave 34 Bucket 0 (foundation) or sandwich into Wave 33 if BE work already in flight.

## Effort estimate

~1-2 weeks. Wave-pack candidate when sliced into wizard-flow / quality-gate / enterprise-mode.

## Log

- **2026-05-07** (Wave 34 backend cluster shipped — closes 5 sub-letters DONE + 3 PARTIAL + 2 new follow-ups): Wave 34 5 buckets shipped (PRs #905/#906/#907/#908/#910). **DONE:** 272d/h/i/j/l. **PARTIAL:** 272c (sub-check measurements deferred to GAP-226/227/228), 272e (orchestrator wiring → 272o; queue swap deferred), 272k (real-source from analyze pipeline deferred). **NEW follow-ups filed:** 272n (response shape mismatch), 272o (orchestrator wiring). Self-test §7.2 of `contract-first-for-cross-layer.md`: predicted ≤2 follow-ups vs Wave 32 v1's 8 — actual = 2 new. ✅ Rule effectiveness confirmed. Parent stays 🟡 PARTIAL until 272f/g (test deliverables) + 272m (Advanced Mode persistence) + 272n/o close.
- **2026-05-07** (REWORK shipped + sub-letters reconciled): Wave 32 REWORK 4/4 buckets shipped (PRs #887/#889/#888/#890). 11 sub-letter gaps filed: 272c/d/e/f/g (planned in v1 §7) + 272h/i/j/k/l/m (new findings). 272b not needed (Bucket B confirmed inline). User-flagged miss caught Step 2 duplicate check skip during initial gap filing — recovered via PR (this commit). Status: 🟡 PARTIAL post-rework (FE complete, 7 backend endpoints + tech debt + 2 test deliverables tracked in sub-letters).
- **2026-04-29:** Filed after user accepted Round 3 quality. HIGHEST-scoring kit Round 2 (115.6 ⭐⭐).
