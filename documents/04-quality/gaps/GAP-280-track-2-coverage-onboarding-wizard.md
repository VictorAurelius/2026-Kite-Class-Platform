# GAP-280: Track 2 Coverage — Onboarding wizard kit (initial tenant first-run)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (UX growth — first-time tenant experience, conversion-critical moment)
**Domain:** Frontend / Design System
**Found:** 2026-04-29 via audit §2.7 (Agent A flagged distinct from AI Branding wizard)
**Affects:** Onboarding components in both apps + initial-setup pages

## Problem

R2/R3 wizard kit (`ai-branding-wizard-v2/`) is brand-asset generation. Production code has **separate onboarding/welcome wizard** for tenant initial setup (create first class / invite teacher / configure schedule etc.). Audit found ❌ NO kit coverage for onboarding.

## Current State (verified 2026-04-29)

**KC components/onboarding:**
| File | Use case |
|------|---------|
| `components/onboarding/DashboardWelcome.tsx` | First-run welcome card on dashboard |
| `components/onboarding/OnboardingWizard.tsx` | Multi-step initial-setup wizard |

**KH components/onboarding/** (similar pattern, ~3 files).

## Why distinct from `ai-branding-wizard-v2`

| Concern | AI Branding Wizard | Onboarding Wizard |
|---------|--------------------|-------------------|
| Trigger | After tenant exists, before brand generation | First tenant login post-signup |
| Goal | Generate brand assets (logo + colors + hero + AI-generated banners) | Walk through tenant setup essentials (name school + first class + invite teacher + configure schedule) |
| Output | Branded theme + assets | Configured tenant ready to use |
| Persona moment | Mid-funnel (post-signup, pre-deploy) | Top-of-funnel (just signed up) |
| Frequency | Once per branding refresh | Once per new tenant |

Both needed — they cover different moments.

## Proposed Fix

Create `ui_kits/onboarding-wizard/` HTML kit:

**Screens (~6-8):**
- Welcome card (post-signup landing on dashboard)
- Step 1: Tenant info (school/center name + address + contact)
- Step 2: First class creation (pre-fill from tenant data + smart defaults)
- Step 3: Invite teacher(s) — optional, batch invite
- Step 4: Schedule template (recurring rules — references G4 component)
- Step 5: Branding gateway (link to AI Branding wizard OR skip-for-now)
- Step 6: Done + dashboard tour CTA
- Empty states: incomplete-setup recovery
- Error states: invalid input / network failure

## Acceptance Criteria

- [ ] HTML kit ≥105/128 across all wizard steps
- [ ] Different visual tone from AI Branding wizard (clearly separate UX moments)
- [ ] Skip-for-now option per step (not all required upfront)
- [ ] Resume-from-where-left-off (tenant logs back in mid-onboarding)
- [ ] References G4 component (schedule template — post-GAP-273)
- [ ] Bridges to `ai-branding-wizard-v2` at step 5
- [ ] Production ported KC + KH onboarding components ≥105/128
- [ ] VN-realistic mock data
- [ ] WCAG AA preserved

## Related

- Audit evidence: §2.7
- Sister: `ai-branding-wizard-v2` (covers different moment), GAP-272 (port AI branding wizard)

## Effort estimate

~1 wave (kit + port). Wave-pack candidate.

## Log

- **2026-04-29:** Filed from audit synthesis. Agent A flagged as distinct from AI Branding wizard — different UX moment, different goal.
