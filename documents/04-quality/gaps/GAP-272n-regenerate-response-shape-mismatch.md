# GAP-272n: Align `POST /regenerate` response shape on `BrandingJobResponse` wrapper

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (contract drift — non-blocking but inconsistent)
**Domain:** Backend (kitehub-branding)
**Found:** 2026-05-07 (Wave 34 Bucket D coordinator-flagged inconsistency)
**Affects:** API contract consistency between A's regenerate POST and B's GET wrapper at `/api/v1/branding/jobs/{jobId}`
**Related:** GAP-272 (parent), GAP-272d, GAP-272k, Wave 34 Buckets A+B+D PRs #907 / #906 / #910

## Problem

Wave 34 Buckets A and B independently shipped two different response shapes for `BrandingJob` at `/api/v1/`:

- **Bucket A (PR #907):** `POST /api/v1/branding/jobs/{jobId}/regenerate` returns the raw `BrandingJob` entity JSON (no `brandColors`, internal field shape).
- **Bucket B (PR #906):** `GET /api/v1/branding/jobs/{jobId}` returns `BrandingJobResponse` wrapper with `brandColors` + curated public field set.

Bucket D's `useRegenerateQuota` hook handles both shapes via loose typing — works but masks the contract drift. Future consumers paginating job state across the two endpoints will see field-name divergence.

## Root Cause

Wave 34 wave plan §3 §8 R3 explicitly warned of `BrandingJobResponse` shared-edit risk; coordinator briefed B as DTO owner, A as non-owner. A interpreted that as "do not extend" and returned entity directly. B introduced the wrapper but didn't (and shouldn't have) modified A's controller. Outcome: contract drift not caught at briefing time.

## Proposed Fix

Refactor `BrandingWizardController.regenerate(...)` (Bucket A's) to return the same `BrandingJobResponse` wrapper Bucket B introduced. Inject the response-mapper service B created. Keep test parity.

Steps:
1. Inject `BrandingJobResponse` mapper or factory from Bucket B's package into `BrandingWizardController`.
2. Update return type + JSON serialization for `POST /regenerate`.
3. Update controller test to assert `brandColors` field present in regenerate response.
4. Update Bucket D's `useRegenerateQuota` hook to use the strict wrapper type (drop loose typing).
5. Remove the loose-type compat path with brief grep; ensure no caller relies on legacy entity shape.

## Acceptance Criteria

- [ ] `POST /api/v1/branding/jobs/{jobId}/regenerate` returns `BrandingJobResponse` (same shape as `GET /jobs/{jobId}`)
- [ ] `useRegenerateQuota` hook uses strict typed wrapper
- [ ] Controller integration test asserts wrapper field set
- [ ] `pnpm test --run` + `mvn verify -pl kitehub-branding -am` green
- [ ] api-contract.md `POST /regenerate` row updates response schema reference

## Out of scope

- Refactor `BrandingJobController` legacy `/api/platform/...` path (separate concern)
- Per-field migration if wrapper omits any client-needed field — file separate gap if surfaced

## Related

- Parent: GAP-272
- Prior: GAP-272d (regenerate endpoint), GAP-272k (brandColors source)
- Wave 34 plan §8 R3 (the warned risk)
- Bucket D PR #910 body (where mismatch was first surfaced)

## Log

- **2026-05-07:** Filed at Wave 34 closure (this PR). Coordinator-flagged after Bucket D agent reported handling inconsistency via loose typing. Self-test §7.2 of `contract-first-for-cross-layer.md` rule §7.2: this is 1 of ≤2 expected sub-gap follow-ups (counted with GAP-272o); confirms rule effectiveness vs Wave 32 v1's 8 sub-gaps.
