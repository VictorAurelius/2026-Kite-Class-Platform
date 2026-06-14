# GAP-520: JWT signing secret rotation runbook + dual-key support

**Status:** 🟡 PARTIAL (90% — Wave 72a Bucket B PR #1287)
**Priority:** 🟠 P1
**Domain:** Backend / DevOps
**Found:** 2026-05-13 (Wave 71c per `pre-launch-auth-hardening-checklist.md` §2.6)

## Problem

JWT secret stored in env var. No rotation runbook. If secret leaks (worktree commit, log, etc.) every existing JWT remains valid until expiry — no force-logout mechanism.

## Proposed Fix

1. `jwt.secret.current` + `jwt.secret.previous` config slots
2. Verifier tries current first, falls back to previous (covers in-flight tokens during rotation)
3. Rotation runbook `documents/05-guides/operations/jwt-rotation-runbook.md`: AWS Secrets Manager versioned secret + redeploy pulls new current
4. Recommend quarterly cadence

## Acceptance Criteria

- [x] 2-slot verifier + tests (`JwtKeyService` + `JwtKeyServiceTest` 7 cases — Wave 72a Bucket B PR #1287)
- [x] Rotation runbook with grace window (`documents/05-guides/operations/jwt-rotation-runbook.md`)
- [ ] AWS Secret `kitehub/production/jwt-signing-secret` versioned — provisioning step, executed at first real rotation (deferred to ops follow-up)

## Related

- Rule: `pre-launch-auth-hardening-checklist.md` §2.6
- PR: #1287 (Wave 72a Bucket B)

## Log


- 2026-06-14: phase re-triage — phase-1-beta→phase-1.5-paid (notes 'AWS rotation pending Phase 1.5').
- **2026-05-14** Wave 72a Bucket B PR #1287 ships dual-key code + runbook: `JwtKeyService` (current + optional previous + `jwt.verify.fallback` counter), `AuthService.refresh` + `TokenService` delegate, new `jwt.previous-secret` config slot (env `JWT_SECRET_PREVIOUS`), 7 unit tests + 1 quarterly rotation runbook. Status → 🟡 PARTIAL — last AC (AWS Secret versioning) ships at first real rotation per runbook §3.
