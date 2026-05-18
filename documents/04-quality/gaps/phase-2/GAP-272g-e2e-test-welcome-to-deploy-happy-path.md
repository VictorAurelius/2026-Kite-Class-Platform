# GAP-272g: E2E test for AI Branding Wizard happy path (welcome → deploy)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Frontend tests (Playwright E2E)
**Found:** 2026-05-07 (Wave 32 REWORK closure)
**Affects:** AI Branding Wizard end-to-end flow validation
**Related:** GAP-272 (parent), GAP-272d/e/i/j/k/l/m (backend dependencies)

## Problem

Wave 32 REWORK shipped 4 buckets covering 6 steps + Step 6 sub-states.
Per Wave 32 v1 plan §7 closure protocol, E2E test cho happy path
"welcome → deploy" phải validate full wizard flow integration.

State-check 2026-05-07:
- No `tests/e2e/branding-wizard-happy-path.spec.ts` or similar
- Bucket-level unit tests cover individual components (10/9/25/33 = 77 tests)
  but no test exercises Step1 → Step2 → Step3 → Step4 → Step5 → Step6
  completion flow end-to-end
- Integration tests verify reducer state transitions (Bucket A) but not
  network calls + real navigation

## Root Cause

Wave 32 REWORK focused on per-bucket scope. E2E test phải coordinate
across all 4 buckets + use real (or properly mocked) backend endpoints.
Required GAP-272d/e/i/j/k/l (backend endpoints) + GAP-272f (visual
baseline) ship trước để E2E test stable.

## Proposed Fix

1. **Test scope:** happy path scenario
   - Welcome (slug "test-tenant" → available)
   - Logo (skip — AI generated)
   - Audience (mầm non)
   - Tone (Thân thiện)
   - Template (pick first matching)
   - Step 6 preview → approve all 4 resources
   - Deploy → wait DEPLOYED state
2. **Mocks:** MSW handlers for all backend endpoints (GAP-272i/j/k/d/e/l)
3. **Assertions:**
   - StepIndicator advances correctly
   - WizardState reducer reflects each step
   - Final URL navigates to dashboard post-deploy
4. **Run CI:** on every wizard-touching PR

## Acceptance Criteria

- [ ] E2E test file commits + passes locally
- [ ] CI gate active
- [ ] MSW handlers cover all backend deps
- [ ] Test runs <30s (Playwright trace cleanup)

## Related

- GAP-272 (parent)
- GAP-272d/e/i/j/k/l (backend dependencies — needed for non-MSW production E2E later)
- Wave 32 v1 plan §7 (this letter pre-named there)
