---
title: Wave 32 — Phase 4 kit port — GAP-272 ai-branding-wizard v2 (4 buckets, Direction C 6-step refactor)
status: draft
created: 2026-05-06
updated: 2026-05-06
waves: [32]
gaps: [GAP-272]
---

# Wave 32 — Phase 4 kit port — ai-branding-wizard v2 (Direction C 6-step refactor)

**Goal:** Refactor existing legacy 4-step wizard at `kitehub-frontend/src/app/(customer)/branding/wizard/page.tsx` (237 LOC, predates Round 2) → Direction C 6-step provisioning wizard per `ui_kits/ai-branding-wizard-v2/` (avg **115.6/128** ⭐⭐, highest-scoring R2 kit, 28 screens). **Closes GAP-272 PARTIAL** (6-step flow + Quality Gate widget + Regenerate Counter + Enterprise Advanced Mode + 5 lifecycle inline shipped; backend endpoint gaps for audience/tone/template persistence + quality-gate score → follow-ups GAP-272b/c if endpoints absent at agent runtime).
**Trigger:** Wave 31 plan §7 closure recommends ai-branding-wizard standalone (28 screens too big to bundle với KH pro v2 hub). Drafted PIPELINED per `feedback_pipelined_wave_planning.md` + `wave-pack-planner` §Step 5.5 (Wave 31 4 agents in-flight at draft time; Bucket C completed during draft writing — pattern stable across 4 consecutive waves).
**Estimated wall-clock:** ~25-30 min/agent parallel (heavier than Wave 30/31 — refactor + 6 wizard primitives `🆕 to-be-created` + complex Step 6 với 6 screens + tier-based gating + lifecycle integration).

---

## 1. Brainstorm

**Q1 (alignment):**
- **Persona:** P2 Center Owner first-time setup + P3 Medium Center Admin rebrand. Phase 1 BETA critical — branding wizard is the highest-friction onboarding step; broken UX = trial→paid conversion drop.
- **Domain:** Frontend production refactor (Next.js 15). Legacy 4-step (Upload → Analyze → Generate → Review) replaced với Direction C 6-step (Welcome → Logo → Audience → Tone → Template → Preview+approve+deploy). 28 screens covering 6 steps + Settings Advanced Mode + 5 lifecycle inline.
- **Compliance gates:** `ai-branding-guidelines.md` §2.1 (free-form prompt BANNED for non-Enterprise), §2.4 (Enterprise Advanced Mode separate path), §2.5 (input token cap UI), §4.2 (per-resource approve), §4.3 (tier-based regenerate counter). Wave 32 makes these compliant — Round 1 violated §2.1.
- **Scope vs Wave 31 Bucket C:** Wave 31 Bucket C ported branding HUB (theme + logo + wizard CTA placeholder href). Wave 32 wires the wizard internals — Bucket C's CTA placeholder becomes live entry point.

**Q2 (trade-offs):**
- **Refactor vs greenfield:** existing 237-LOC `wizard/page.tsx` + 4 step components (`UploadStep.tsx` 4.2K, `AnalyzeStep.tsx` 5.4K, `GenerateStep.tsx` 4.6K, `ReviewStep.tsx` 5.9K) + `useBrandingJob`/`useAnalyzeLogo`/`useCreateBrandingJob` hooks already shipped. Direction C reuses Logo step (Step 2 ≈ legacy Upload) but adds Audience/Tone/Template/QualityGate/Regenerate. Strategy: **rename + extend, not greenfield** — preserve dynamic-import code-splitting (Wave GAP-236 perf optimization) + existing react-query hooks. Delete `AnalyzeStep`/`GenerateStep`/`ReviewStep` AFTER confirming Direction C subsumes them.
- **6 wizard primitives 🆕 to-be-created** (StepIndicator, AudienceSelector, ToneSelector, TemplatePicker, QualityGateWidget, RegenerateCounter) — built **app-local** trong `kitehub-frontend/src/components/branding/wizard/` (NOT shared-ui workspace). Rationale: wizard-specific to AI Branding domain, single consumer (KH `(customer)/branding/wizard`), no KC reuse expected. If KC later adds branding wizard → factor to shared-ui then. Shipping app-local is faster + avoids workspace churn. Per `feedback_dedicated_tools_first.md` + Wave 30/31 lessons (Decision B duplicate-first beats premature workspace abstraction).
- **Backend endpoint state-check at agent runtime:** Direction C needs audience/tone/template persistence + quality-gate score endpoints. State-check existence at agent runtime — IF missing → mock + flag follow-up gap (GAP-272b/c).
- **Bucket split (4 buckets disjoint):**
  - **A:** Wizard shell refactor (route page.tsx + StepIndicator primitive + Step 1 welcome/slug-validation + Step 2 logo upload — preserve UploadStep). Owns `wizard/page.tsx` + `wizard/layout.tsx`.
  - **B:** Steps 3-4 (AudienceSelector 4 VN cards + ToneSelector 4 cards với rendered preview). 4 screens.
  - **C:** Step 5 (TemplatePicker grid + fullscreen + custom-prompt variant) + Step 6 main preview (G11 ThemePreview integration + iframe + 4 resource toggles). 5 screens.
  - **D:** Step 6 QualityGateWidget + RegenerateCounter + deploying screen + 5 G9 lifecycle inline + Settings Advanced Mode + disclaimer modal. 11 screens (heaviest bucket — but most rendering is just states of QualityGateWidget + RegenerateCounter + already-built G9).

**Q3 (risks):**
- **R1: Backend endpoint absence.** Audience/tone/template persistence + quality-gate score may not exist (state-check at agent runtime). Mitigation: agent mocks + flags follow-up. WAVE 32 ships UI even if backend stubs.
- **R2: Legacy step component deletion.** `AnalyzeStep.tsx` + `GenerateStep.tsx` + `ReviewStep.tsx` replaced by Direction C — Bucket A must verify NO other consumer imports them before delete (`grep -rn 'AnalyzeStep\|GenerateStep\|ReviewStep' kitehub-frontend/src/`). Bucket A delete AFTER all 4 PRs ready.
- **R3: Bundle size regression.** Wave GAP-236 Sub-PR B code-split wizard via `dynamic()`. Direction C must preserve dynamic imports per step. Bucket A's wizard shell sets the pattern.
- **R4: 6 primitives concurrent.** Buckets B/C/D each ship 1-2 new primitives — minor risk of duplicate utility (e.g. card-with-preview component). Mitigation: Bucket A also creates a small `wizard-shared.tsx` for common card/badge/preview wrappers; B/C/D import.
- **R5: Step 6 complexity.** 6 sub-screens (preview-default, qgate-pass, qgate-fail, regenerate-counter, regenerate-quota-empty, deploying) + iframe + per-resource approve + tier upgrade modal. Bucket C handles preview-default + iframe; Bucket D handles 5 sub-states. Risk = double-edit on `step6/index.tsx`. Mitigation: Bucket C ships skeleton with state-driven render; Bucket D adds state branches.
- **R6: Tier-gating logic spread.** Enterprise Advanced Mode opt-in (Settings) + per-tier regenerate counter (Step 6) + token cap UI (Step 5/6). All gated by tier from auth-store. Bucket D consolidates tier logic in a `useBrandingTier()` hook to avoid duplication.

---

## 2. Task Breakdown

| Bucket | Scope | Owner | Effort | Disjoint? |
|--------|-------|-------|--------|-----------|
| A | Wizard shell refactor + StepIndicator + Step 1 (welcome/slug) + Step 2 (logo) + `wizard-shared.tsx` | bg-agent | ~25-30 min | ✅ `wizard/page.tsx` + `components/branding/wizard/{StepIndicator,WelcomeStep,LogoStep,wizard-shared}.tsx` |
| B | Steps 3-4: AudienceSelector + ToneSelector (4 cards each, VN audience labels, tone with preview) | bg-agent | ~20-25 min | ✅ `components/branding/wizard/{AudienceStep,ToneStep,AudienceCard,ToneCard}.tsx` |
| C | Step 5: TemplatePicker (grid + fullscreen + custom-prompt) + Step 6 main preview (G11 ThemePreview + iframe + 4 resource toggles) | bg-agent | ~25-30 min | ✅ `components/branding/wizard/{TemplateStep,Step6Preview,TemplateGrid,ResourceToggle}.tsx` |
| D | Step 6 sub-states: QualityGateWidget + RegenerateCounter + deploying + 5 G9 lifecycle inline + Settings Advanced Mode + disclaimer modal + `useBrandingTier` hook | bg-agent | ~30-35 min | ✅ `components/branding/wizard/{QualityGateWidget,RegenerateCounter,DeployingStep,LifecycleInline}.tsx` + `(customer)/settings/branding/advanced/page.tsx` + `hooks/use-branding-tier.ts` |

**Disjoint check:** mỗi bucket touch riêng component subdirs trong `components/branding/wizard/`. Shared edits:
- `wizard/page.tsx` (Bucket A own — orchestrates step rendering; B/C/D wire steps via Bucket A's exported step-registry pattern)
- `step6/index.tsx` (Bucket C ships skeleton + preview default state; Bucket D adds qgate/regenerate/deploying state branches — coordinate via state-machine in Bucket A's `wizard-shared.tsx`)
- Settings Advanced route (`(customer)/settings/branding/advanced/page.tsx`) — Bucket D solo own.

**Cross-bucket dependency:** B/C/D depend on Bucket A's `StepIndicator` + `wizard-shared.tsx` exports. Bucket A ships interface stubs commit đầu (mirror Wave 30/31 pattern).

**Legacy delete (post-merge):** Bucket A's PR removes `AnalyzeStep.tsx`, `GenerateStep.tsx`, `ReviewStep.tsx` AFTER state-check confirms no other consumer + Direction C subsumes their behavior. `UploadStep.tsx` PRESERVED as Step 2 LogoStep base.

---

## 3. Scope (per bucket)

### Bucket A — Wizard shell + StepIndicator + Steps 1-2

- **Spec source:**
  - `documents/02-architecture/design-system/ui_kits/ai-branding-wizard-v2/screens/step1-welcome-{default,validating,conflict}.html` (3 variants)
  - `documents/02-architecture/design-system/ui_kits/ai-branding-wizard-v2/screens/step2-logo-{default,uploaded,skip,error}.html` (4 variants)
  - `documents/02-architecture/design-system/ui_kits/_shared/colors_and_type.css`
  - `documents/02-architecture/design-system/ui_kits/ai-branding-wizard-v2/app.jsx` (React skeleton sketch)
- **State-check first:**
  - Read existing `kitehub-frontend/src/app/(customer)/branding/wizard/page.tsx` (237 LOC, 4-step legacy)
  - Read `components/branding/UploadStep.tsx` — extract reusable upload logic for new Step 2 LogoStep
  - Verify no consumers of `AnalyzeStep`/`GenerateStep`/`ReviewStep` outside wizard: `grep -rn 'AnalyzeStep\|GenerateStep\|ReviewStep' kitehub-frontend/src/` (expect: only `wizard/page.tsx` imports them)
  - Verify slug-validation endpoint exists: `grep -rn 'slug.*availab\|tenant.*slug\|/api/.*slug' kitehub-frontend/src/lib/api/endpoints.ts kitehub/kitehub-subscription/src/main/java/`
- **Files to create:**
  - `kitehub-frontend/src/components/branding/wizard/StepIndicator.tsx` — 6-step progress indicator với current-step + completed-step states
  - `kitehub-frontend/src/components/branding/wizard/wizard-shared.tsx` — shared types, state machine reducer, common card/badge wrappers
  - `kitehub-frontend/src/components/branding/wizard/WelcomeStep.tsx` — Step 1 welcome + slug input + tenant validation (3 sub-states: default/validating/conflict)
  - `kitehub-frontend/src/components/branding/wizard/LogoStep.tsx` — Step 2 logo upload (4 sub-states: default/uploaded/skip/error). Reuse `useUploadAsset` + `useAnalyzeLogo` hooks from `use-branding.ts`.
- **Files to modify:**
  - `kitehub-frontend/src/app/(customer)/branding/wizard/page.tsx` — REFACTOR from 4-step to 6-step orchestrator. Preserve `dynamic()` code-split per step. Use Bucket A's `wizard-shared.tsx` state machine. Pass step-rendering responsibility to step components (B/C/D imports).
- **Files to delete (POST-merge by Bucket A's PR):**
  - `kitehub-frontend/src/components/branding/AnalyzeStep.tsx` (replaced by direct backend job analyze in Step 6)
  - `kitehub-frontend/src/components/branding/GenerateStep.tsx` (replaced by Step 5 TemplatePicker + Step 6 generate)
  - `kitehub-frontend/src/components/branding/ReviewStep.tsx` (replaced by Step 6 preview + per-resource approve)
- **Tests:** ≥6
  - StepIndicator renders 6 steps + highlights current
  - WelcomeStep slug validation: default → validating → conflict transition
  - LogoStep upload: drag-drop + format error + skip path
  - Wizard shell: dynamic imports per step still work (no hydration issues)
  - State machine: transitions step1 → step2 only after slug valid; step2 → step3 after logo OR skip
  - Legacy components deletion: `wizard/page.tsx` no longer imports `AnalyzeStep`/`GenerateStep`/`ReviewStep`
- **Acceptance:**
  - Wizard shell renders Step 1 + Step 2 functional
  - StepIndicator + wizard-shared.tsx exported for B/C/D consumption
  - 7 screens self-rescore ≥110/128 (per dossier rubric)
  - `pnpm -F @kite/kitehub-frontend type-check && test --run && build` clean
  - Bundle size: wizard route First Load JS ≤ pre-refactor baseline (preserve code-split)

### Bucket B — Steps 3-4 (Audience + Tone)

- **Spec source:**
  - `documents/02-architecture/design-system/ui_kits/ai-branding-wizard-v2/screens/step3-audience-{default,selected}.html` (2 variants)
  - `documents/02-architecture/design-system/ui_kits/ai-branding-wizard-v2/screens/step4-tone-{default,selected}.html` (2 variants)
  - 4 audience cards: mầm non / THCS / trung tâm tiếng Anh / luyện thi đại học
  - 4 tone cards: Chuyên nghiệp / Thân thiện / Năng động / Cao cấp — each với TINY rendered preview (button + heading)
- **State-check first:**
  - Read Bucket A output: `components/branding/wizard/wizard-shared.tsx` exports + `StepIndicator` interface
  - Verify backend audience/tone persistence: `grep -rn 'audience\|tone' kitehub/kitehub-branding/src/main/java/` — IF endpoint absent → mock + follow-up gap
- **Files to create:**
  - `kitehub-frontend/src/components/branding/wizard/AudienceStep.tsx` — Step 3 với 4-card grid + "không thuộc nhóm?" details disclosure + AI reasoning preview after select
  - `kitehub-frontend/src/components/branding/wizard/AudienceCard.tsx` — single card với emoji + label + description + selected state
  - `kitehub-frontend/src/components/branding/wizard/ToneStep.tsx` — Step 4 với 4-card grid + tiny rendered preview per card + reasoning preview after select
  - `kitehub-frontend/src/components/branding/wizard/ToneCard.tsx` — single card với rendered preview (button + heading) + selected state
- **Tests:** ≥4
  - AudienceStep renders 4 cards + selection persists
  - ToneStep renders 4 cards với tiny preview elements
  - AudienceCard selected state visible
  - ToneCard preview renders sample button + heading
- **Acceptance:**
  - 4 screens self-rescore ≥115/128 (high-scoring kit screens)
  - Audience persistence wired (real API or mock with flag)
  - Tone persistence wired (real API or mock with flag)
  - All verify commands pass
- **Cross-bucket:** import `StepIndicator` + `wizard-shared.tsx` from Bucket A. NO touch on `wizard/page.tsx`.

### Bucket C — Step 5 (Template) + Step 6 main preview

- **Spec source:**
  - `documents/02-architecture/design-system/ui_kits/ai-branding-wizard-v2/screens/step5-template-{grid,fullscreen,with-custom-prompt}.html` (3 variants)
  - `documents/02-architecture/design-system/ui_kits/ai-branding-wizard-v2/screens/step6-preview-default.html` (HEADLINE screen — 122/128)
  - 6 REAL SVG template previews filtered by audience+tone
- **State-check first:**
  - Read Bucket A output (StepIndicator + wizard-shared)
  - Verify G11 ThemePreview export: `grep ThemePreview packages/shared-ui/src/index.ts`
  - Verify template list endpoint: `grep -rn 'template' kitehub-frontend/src/lib/api/endpoints.ts` — IF absent → mock 6 templates + follow-up
  - Verify `kitehub.ai.enterprise.advancedModeEnabled` config key exists: `grep -rn 'advancedModeEnabled\|enterprise.advancedMode' kitehub/kitehub-branding/src/main/resources/`
- **Files to create:**
  - `kitehub-frontend/src/components/branding/wizard/TemplateStep.tsx` — Step 5 với grid view + fullscreen modal toggle + custom-prompt variant (Enterprise gated)
  - `kitehub-frontend/src/components/branding/wizard/TemplateGrid.tsx` — 6-card grid với SVG previews
  - `kitehub-frontend/src/components/branding/wizard/TemplateFullscreen.tsx` — fullscreen preview với WCAG/responsive/text-safety badges
  - `kitehub-frontend/src/components/branding/wizard/Step6Preview.tsx` — HEADLINE Step 6 default state. Iframe live preview + G11 ThemePreview integration + 4 resource toggles (logo/colors/banner/hero) + per-resource approve checkboxes
  - `kitehub-frontend/src/components/branding/wizard/ResourceToggle.tsx` — single resource toggle với approve checkbox + thumbnail
- **Tests:** ≥4
  - TemplateGrid renders 6 templates filtered by audience+tone props
  - TemplateFullscreen modal opens + WCAG badges render
  - TemplateStep custom-prompt variant ONLY visible if tier === 'ENTERPRISE'
  - Step6Preview iframe renders + 4 resource toggles
- **Acceptance:**
  - 4 screens self-rescore ≥115/128 (Step 6 default = HEADLINE 122)
  - G11 ThemePreview integrated
  - Iframe live preview functional
  - Per-resource approve toggles work (state managed in wizard-shared state machine)
  - Custom-prompt only visible for Enterprise tier
  - All verify commands pass
- **Cross-bucket:**
  - Import StepIndicator + wizard-shared (Bucket A)
  - Step6Preview ships preview-default state ONLY; Bucket D adds qgate/regenerate/deploying state branches via shared state machine
  - NO touch on `wizard/page.tsx` (orchestrator owned by A)

### Bucket D — Step 6 sub-states + Settings Advanced + lifecycle inline

- **Spec source:**
  - `documents/02-architecture/design-system/ui_kits/ai-branding-wizard-v2/screens/step6-quality-gate-{pass,fail}.html` (2 variants — 95/100 and 65/100)
  - `documents/02-architecture/design-system/ui_kits/ai-branding-wizard-v2/screens/step6-regenerate-{counter,quota-empty}.html` (2 variants)
  - `documents/02-architecture/design-system/ui_kits/ai-branding-wizard-v2/screens/step6-deploying.html` (1 — SSE-driven log)
  - `documents/02-architecture/design-system/ui_kits/ai-branding-wizard-v2/screens/lifecycle-{NOT_STARTED,GENERATING,DEPLOYED,REGENERATING,FAILED}.html` (5 lifecycle states — G9 already in shared-ui)
  - `documents/02-architecture/design-system/ui_kits/ai-branding-wizard-v2/screens/settings-branding-advanced-{mode,disclaimer-modal}.html` (2 — Enterprise opt-in flow)
- **State-check first:**
  - Read Bucket A output (state machine API)
  - Verify G9 InstanceLifecycleStatus export: `grep InstanceLifecycleStatus packages/shared-ui/src/index.ts`
  - Verify quality-gate score endpoint: `grep -rn 'quality.gate\|qualityScore\|/api/.*quality' kitehub/kitehub-branding/src/main/java/ kitehub-frontend/src/lib/api/endpoints.ts` — IF absent → mock + follow-up
  - Verify regenerate quota tracking: `grep -rn 'regenerate.count\|regenerate.quota' kitehub-frontend/src/lib/api/endpoints.ts kitehub/kitehub-branding/src/main/java/`
  - Verify SSE endpoint for deploying log: `grep -rn 'sse\|EventSource\|/branding.*stream' kitehub-frontend/src/lib/api/endpoints.ts`
  - Verify Enterprise tier in auth-store: `grep -rn 'tier\|ENTERPRISE\|subscription.*tier' kitehub-frontend/src/stores/auth-store.ts`
- **Files to create:**
  - `kitehub-frontend/src/components/branding/wizard/QualityGateWidget.tsx` — /100 score + 5 check breakdown (WCAG contrast, CSS vars, broken URLs, visual regression, logo placement). Pass + fail variants.
  - `kitehub-frontend/src/components/branding/wizard/RegenerateCounter.tsx` — counter + tier comparison + quota-empty PRO upsell modal trigger
  - `kitehub-frontend/src/components/branding/wizard/DeployingStep.tsx` — SSE log streaming display + G9 inline G9 InstanceLifecycleStatus component
  - `kitehub-frontend/src/components/branding/wizard/LifecycleInline.tsx` — wraps G9 với Step 6 deploy-context (5 states: NOT_STARTED, GENERATING, DEPLOYED, REGENERATING, FAILED)
  - `kitehub-frontend/src/app/(customer)/settings/branding/advanced/page.tsx` — Settings Advanced Mode entry (Enterprise tier gate + opt-in toggle)
  - `kitehub-frontend/src/components/branding/wizard/AdvancedModeDisclaimer.tsx` — disclaimer modal trước khi enable Advanced Mode
  - `kitehub-frontend/src/hooks/use-branding-tier.ts` — consolidate tier-gating logic (returns `{ tier, regenerateQuota, advancedModeEnabled, canUseCustomPrompt }`)
- **Tests:** ≥6
  - QualityGateWidget pass variant: 5 checks all green + PASS badge
  - QualityGateWidget fail variant: 65/100 + auto-regen action button
  - RegenerateCounter decrements on regenerate + disables at quota-empty
  - RegenerateCounter quota-empty triggers upsell modal
  - DeployingStep SSE log appends new lines (mock EventSource)
  - LifecycleInline renders all 5 states correctly
  - Settings Advanced Mode toggle gated by tier === 'ENTERPRISE'
  - AdvancedModeDisclaimer modal blocks toggle until consent checked
- **Acceptance:**
  - 11 screens (5 step6 sub-states + 5 lifecycle + 2 settings) self-rescore ≥110/128 average
  - QualityGate widget functional (mock score if backend absent + follow-up flag)
  - Regenerate counter tier-gated (FREE 3 / PRO 10 / PREMIUM 30 / ENT unlimited per `ai-branding-guidelines.md` §4.3)
  - Deploying SSE log functional (mock if endpoint absent)
  - 5 G9 lifecycle states render inline trong Step 6
  - Settings Advanced Mode page Enterprise-gated
  - Disclaimer modal blocks opt-in until consent
  - All verify commands pass
- **Cross-bucket:**
  - Import StepIndicator + wizard-shared (Bucket A)
  - Imports Bucket C's Step6Preview as base; ADDS state branches via shared state machine
  - NO touch on Buckets B/C step components

---

## 4. State-Check Evidence (per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Type | Verification | Verdict |
|--------|------|-------------|---------|
| `kitehub-frontend/src/app/(customer)/branding/wizard/page.tsx` | Existing route (237 LOC, 4-step legacy) | `wc -l` returned 237 | ✅ exists (refactor target — Bucket A) |
| `kitehub-frontend/src/components/branding/{Upload,Analyze,Generate,Review}Step.tsx` | Existing step components | `ls` returned 5 files | ✅ exist (UploadStep preserved as base for Step 2; Analyze/Generate/Review deleted by Bucket A post-state-check) |
| `kitehub-frontend/src/hooks/use-branding.ts` | Existing react-query hooks | grep showed `useUploadAsset`, `useAnalyzeLogo`, `useCreateBrandingJob`, `useBrandingJob` | ✅ exists (preserved + extended) |
| `documents/02-architecture/design-system/ui_kits/ai-branding-wizard-v2/screens/*.html` | 28 HTML proto screens | `ls | wc -l` = 28 | ✅ exist |
| `_shared/colors_and_type.css` | design tokens | exists | ✅ |
| `@kite/shared-ui` G9 InstanceLifecycleStatus | Wave 29 component | grep showed export | ✅ exists (Bucket D LifecycleInline wraps it) |
| `@kite/shared-ui` G11 ThemePreview | Wave 29 component | confirmed in shared-ui index | ✅ exists (Bucket C Step6Preview integrates) |
| Backend POST `/branding/ai/analyze-logo` | logo analyze endpoint | grep `endpoints.ts` line 40 | ✅ exists (LogoStep uses) |
| Backend POST `/branding/jobs` | generate job endpoint | grep `endpoints.ts` line 41 | ✅ exists (Step 6 generate uses) |
| Backend GET `/branding/jobs/:id` | job polling endpoint | grep `endpoints.ts` line 42 | ✅ exists (DeployingStep polls) |
| `StepIndicator` component | wizard primitive | ❌ not in shared-ui or app | 🆕 to-be-created (Bucket A) |
| `AudienceSelector`/`AudienceStep`/`AudienceCard` | wizard primitives | ❌ not present | 🆕 to-be-created (Bucket B) |
| `ToneSelector`/`ToneStep`/`ToneCard` | wizard primitives | ❌ not present | 🆕 to-be-created (Bucket B) |
| `TemplatePicker`/`TemplateGrid`/`TemplateFullscreen` | wizard primitives | ❌ not present | 🆕 to-be-created (Bucket C) |
| `Step6Preview`/`ResourceToggle` | wizard primitives | ❌ not present | 🆕 to-be-created (Bucket C) |
| `QualityGateWidget` | wizard primitive | ❌ not present | 🆕 to-be-created (Bucket D) |
| `RegenerateCounter` | wizard primitive | ❌ not present | 🆕 to-be-created (Bucket D) |
| `DeployingStep`/`LifecycleInline` | wizard primitives | ❌ not present | 🆕 to-be-created (Bucket D) |
| `useBrandingTier` hook | tier gating hook | ❌ not present | 🆕 to-be-created (Bucket D) |
| `(customer)/settings/branding/advanced/` route | Enterprise opt-in route | ❌ not present | 🆕 to-be-created (Bucket D) |
| Backend audience/tone/template persistence endpoints | Step 3/4/5 persistence | needs verify at Bucket B/C agent runtime | ⚠️ likely absent — mock + follow-up gap if so |
| Backend quality-gate score endpoint | Step 6 score | needs verify at Bucket D agent runtime | ⚠️ likely absent — mock + follow-up gap if so |
| Backend regenerate quota tracking | Step 6 counter | needs verify at Bucket D agent runtime | ⚠️ likely absent — mock + follow-up gap if so |
| Backend SSE deploying log | Step 6 deploying | needs verify at Bucket D agent runtime | ⚠️ likely absent — mock + follow-up gap if so |
| `kitehub.ai.enterprise.advancedModeEnabled` config key | Enterprise gate | needs verify at Bucket C agent runtime | ⚠️ likely defined per `ai-branding-guidelines.md` §2.4 — verify |
| `BR-INPUT-CAP-001..007` (input token cap UI) | business rules | ✅ present in `ai-branding-guidelines.md` §2.5 | ✅ exist (Bucket C/D enforce on custom-prompt + UI hint) |

**Pre-spawn verify (coordinator):**
1. Wave 31 SHIPPED (all 4 buckets merged + closure) — Wave 32 spawn AFTER
2. `pnpm -F @kite/kitehub-frontend build` baseline clean
3. `git pull --ff-only origin main` to ensure latest
4. State-check `(customer)/branding/wizard/` route still has legacy 4-step (sanity check — should be unchanged from this draft time)

---

## 5. Verification Gates (per bucket)

| Bucket | Local verify command | Notes |
|--------|---------------------|-------|
| A | `pnpm -F @kite/kitehub-frontend type-check && test --run && build` | next build strict; bundle size ≤ pre-refactor baseline (verify First Load JS for `/branding/wizard`) |
| B | same — focus suites: AudienceStep + ToneStep | imports Bucket A wizard-shared |
| C | same — focus suites: TemplateStep + Step6Preview | G11 ThemePreview consumed; iframe + custom-prompt Enterprise-gated |
| D | same — focus suites: QualityGateWidget + RegenerateCounter + LifecycleInline + Settings advanced | tier-gating consolidated in `useBrandingTier` |

Coordinator post-merge: full `pnpm -F @kite/kitehub-frontend build` MUST pass + bundle size regression check.

---

## 6. Agent Spawn Pattern

Per `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md`:
- All 4 buckets `run_in_background: true` + `isolation: worktree`
- RELATIVE paths only (per `feedback_worktree_absolute_path_contamination.md`)
- Coordinator merge sequential A→B→C→D
- `wizard/page.tsx` shared edit: Bucket A own; B/C/D import wizard-shared.tsx interface
- `step6/index.tsx` shared edit: Bucket C ships skeleton; Bucket D adds state branches (state-machine-driven, not direct edits)
- `(customer)/settings/branding/advanced/` solo own by Bucket D

**Spawn timing:** Wave 32 plan PR drafted DURING Wave 31 4 agents in-flight (4th consecutive `wave-pack-planner` §Step 5.5 application — pattern stable). Spawn happens AFTER Wave 31 closure ships + token budget verify per `feedback_token_quota_spawn_timing.md`. Recommend `/clear` between Wave 31 closure and Wave 32 spawn cùng session.

**Domain-milestone audit:** Wave 32 thuộc cluster `phase-4-kit-ports` (per `post-wave-audit-mandate.md` §2.4.1). Trailer: `AUDIT_DEFER_DOMAIN_MILESTONE: phase-4-kit-ports — milestone TBD when 7 kits ship`. Milestone TBD — likely Wave 35-36 sau khi cả 7 kits ship.

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `feedback_post_merge_doc_sync.md` + `feedback_wave_history_append_required.md` + `post-wave-cleanup.md` + `feedback_wave_closure_release_progress_report.md`:

- Mỗi bucket PR update GAP-272 Log
- **Status flip:** GAP-272 stays 🟡 PARTIAL post-Wave-32 (6-step flow + Quality Gate + Regenerate Counter + Enterprise Advanced + 5 lifecycle inline shipped; remaining: backend persistence endpoints if mocked + visual regression baseline + E2E test → follow-ups)
- ROADMAP §🚀 Next Action update — Wave 33 candidates: Phase 1 BETA P0 BLOCKING deploy cluster (GAP-369 DNS + GAP-370 email + GAP-372 beta tenant + GAP-376 prod data seed) OR remaining Phase 4 kit ports (teacher GAP-268 P3, public marketing GAP-274/275 P3)
- Closure PR body PHẢI có "## Release Plan Progress" section per memory `feedback_wave_closure_release_progress_report.md`:
  - Current Phase: Phase 1 BETA
  - Track 2 progress: 3 of 7 kits ported (KC pro v2 Wave 30 + KH pro v2 Wave 31 + ai-branding-wizard v2 Wave 32); remaining 4 (teacher P3 + 3 K-12 SKIP for Phase 1)
  - PDPL deadline countdown 2026-07-01
  - BETA P0 BLOCKING gaps status (GAP-369/370/372/376 OPEN — recommend Wave 33 spawn)
  - Estimated waves còn lại đến Phase 1 BETA launch (target: ~3-5 waves more)
- Wave plan frontmatter `status: complete` flip
- `wave-history.jsonl` append (per Rule 15)
- `bash scripts/prune-merged-worktrees.sh --yes` sau merge (per `post-wave-cleanup.md`)
- AUDIT_DEFER_DOMAIN_MILESTONE trailer

**Follow-up gaps to file at closure (likely):**
- GAP-272b — backend audience/tone/template persistence endpoints (if absent at agent runtime)
- GAP-272c — backend quality-gate score endpoint (if absent at agent runtime)
- GAP-272d — backend regenerate quota tracking endpoint (if absent at agent runtime)
- GAP-272e — backend SSE deploying log endpoint (if absent at agent runtime)
- GAP-272f — Wave 32 visual regression baseline
- GAP-272g — Wave 32 E2E test (welcome → deploy happy path)

---

## 8. Log

- **2026-05-06 (draft):** Plan tạo PIPELINED trong khi Wave 31 4 agents in-flight (Bucket C completed during draft writing — PR #876 ⭐). 4th consecutive `wave-pack-planner` §Step 5.5 application — pattern stable across 4 waves (29→30, 30→31, 31→32). State-check found 🟡 PARTIAL implementation: legacy 4-step wizard at `(customer)/branding/wizard/page.tsx` (237 LOC) + 4 step components (Upload/Analyze/Generate/Review) + `use-branding.ts` hooks (4 functions) + 3 backend endpoints (`/branding/ai/analyze-logo`, `/branding/jobs`, `/branding/jobs/:id`). NO wizard primitives in shared-ui — 13 new app-local components Wave 32 ships. Refactor strategy: rename + extend (preserve UploadStep as Step 2 base, preserve dynamic-import code-split per Wave GAP-236), delete `AnalyzeStep`/`GenerateStep`/`ReviewStep` post-state-check. 4 buckets disjoint: A (shell + Steps 1-2 + StepIndicator + wizard-shared), B (Steps 3-4), C (Step 5 + Step 6 main preview + G11 integration), D (Step 6 sub-states + 5 lifecycle inline + Settings Advanced + tier hook). 6+ backend endpoint state-checks deferred to agent runtime per Bucket B/C/D briefings — mock + follow-up gaps if absent.
