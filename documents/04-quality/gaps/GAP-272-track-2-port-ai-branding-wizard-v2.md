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

## Effort estimate

~1-2 weeks. Wave-pack candidate when sliced into wizard-flow / quality-gate / enterprise-mode.

## Log

- **2026-04-29:** Filed after user accepted Round 3 quality. HIGHEST-scoring kit Round 2 (115.6 ⭐⭐).
