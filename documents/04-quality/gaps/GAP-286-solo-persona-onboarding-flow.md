# GAP-286: Solo persona onboarding flow + skip AI branding

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Frontend (kitehub-frontend) + UX
**Found:** 2026-05-04 (Wave 17 Bucket A — P1 Solo Teacher Round 1 review)
**Affects:** P1 Solo Teacher persona onboarding (FREE/PRO tier signup flow)

## Problem

Signup + provisioning flow at `kitehub/kitehub-frontend/src/app/(auth)/register/page.tsx` is hardcoded for "trung tâm" (center) framing — placeholder `Trung tâm Anh ngữ ABC`, requires `organizationName` + `subdomain`. Solo teacher persona has no dedicated path. After signup, the AI branding wizard at `kitehub/kitehub-frontend/src/app/(customer)/branding/wizard/page.tsx` forces logo upload before continuing — `UploadStep` (line 158) has no "Skip" button; `OnboardingWizard.tsx:199` has skip on the welcome dialog but NOT on the wizard route.

P1 AC-ONBOARD-002 explicitly requires "Wizard branding cho phép Skip / Use default ở mọi step, không force hoàn tất AI branding flow" — currently FAIL. Estimated friction: 10+ minutes overhead just for AI branding step that solo teacher doesn't need (theme defaults are sufficient).

P1 AC §0 critical concern #1: "Ease of setup ≤30 min" → unachievable when AI branding alone consumes 10+ minutes.

## Root Cause

Branding wizard designed for center/school personas where custom branding has business value (parent perception, school identity). Solo teacher personas were not in v1 design scope — single-persona onboarding flow inherits center constraints.

## Proposed Fix

1. Add account-type selector to `register/page.tsx` (3 options: Solo Teacher / Center / School); persist to user profile.
2. Branch onboarding routing post-signup based on account type:
   - Solo → skip branding wizard entirely; default theme applied
   - Center/School → existing branding wizard path
3. Add "Skip / Use default theme" button to every `UploadStep` / `AnalyzeStep` / `GenerateStep` of branding wizard for users who land there but want out.
4. Update copy: replace "Trung tâm Anh ngữ ABC" placeholder with persona-aware variant.
5. Add E2E test: Solo signup → dashboard in ≤5 screens, ≤2 min wall-clock.

## Acceptance Criteria

- [ ] Account-type selector at `/register` with 3 options
- [ ] Solo path skips branding wizard (verified by Playwright E2E)
- [ ] "Skip" button visible at every wizard step (any persona may exit anytime)
- [ ] Skip → default theme applied + dashboard renders correctly
- [ ] Persona-aware placeholder copy
- [ ] E2E test: Solo signup → dashboard ≤5 screens, ≤2 min
- [ ] AC-ONBOARD-002 passes when re-tested in Round 2

## Related

- Review: [`documents/00-brd/persona-reviews/P1-solo-teacher-round-1-2026-05-04.md`](../../00-brd/persona-reviews/P1-solo-teacher-round-1-2026-05-04.md) §1
- AC: [P1-solo-teacher.md AC-ONBOARD-001/002](../../00-brd/persona-criteria/P1-solo-teacher.md)
- Sibling: GAP-287 (phone-as-primary auth), GAP-288 (student form gating), GAP-289 (in-app tour), GAP-293 (feature gating)
- Wave plan: [`documents/03-planning/waves/wave-2026-05-04-persona-review-round-1.md`](../../03-planning/waves/wave-2026-05-04-persona-review-round-1.md)

## Log

- 2026-05-04 — Created from Wave 17 Bucket A P1 review. State-check confirmed: no skip button in `BrandingWizardPage` (verified `UploadStep` lazy-load at line 20-23, no Skip prop); no account-type field in `registerSchema`. Build-from-scratch (🔵 OPEN per `audit-to-gap-pipeline.md` Step 2.5 matrix).
