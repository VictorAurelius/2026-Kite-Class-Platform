# GAP-520: JWT signing secret rotation runbook + dual-key support

**Status:** 🔵 OPEN
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

- [ ] 2-slot verifier + tests
- [ ] Rotation runbook with grace window
- [ ] AWS Secret `kitehub/production/jwt-signing-secret` versioned

## Related

- Rule: `pre-launch-auth-hardening-checklist.md` §2.6
