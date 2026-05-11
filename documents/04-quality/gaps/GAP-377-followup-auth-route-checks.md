# GAP-377-followup: Smoke test auth-route checks (`/auth/signup`, `/auth/request-beta-access`)

**Status:** 🔵 OPEN
**Priority:** 🟢 P3 (non-blocking — substitutes already in place)
**Domain:** DevOps / QA
**Found:** 2026-05-06 (state-check during GAP-377 implementation)
**Affects:** Smoke test coverage; matches AC text vs current FE routing

## Problem

The original GAP-377 Acceptance Criteria + Proposed Fix references routes that do not exist in the kitehub-frontend codebase as of 2026-05-06:

- `/auth/signup`
- `/auth/request-beta-access`
- `/api/v1/health`

Current state (verified 2026-05-06 via `ls kitehub/kitehub-frontend/src/app/`):

- Auth route group: `(auth)/` containing **only** `login/`, `register/`, `verify-email/` — no top-level `/auth/...` paths or beta-access form.
- Health endpoint: `app/api/health/route.ts` (NOT `/api/v1/health`).

To satisfy the AC "15+ assertions" without referencing 404s, the GAP-377 closure substituted:

- `/login` (closest analogue to `/auth/signup` — covers KH login surface)
- `/register` (covers public registration surface)
- `/api/health` (FE health endpoint actually shipped)

## Proposed Fix (when underlying routes ship)

Three independent items, any/all may close this gap:

1. If a public-facing `/auth/signup` (separate from `/register`) is added to kitehub-frontend, swap the smoke-test substitute back to the original route + add a new follow-up if `/register` should also stay.
2. If `/auth/request-beta-access` (or equivalent beta-invite-request form) ships, add it as a smoke-test assertion.
3. If `/api/v1/health` is introduced as a versioned alias of the FE health endpoint, add an assertion for it (keep the existing `/api/health` assertion until the alias is canonical).

## Acceptance Criteria

- [ ] One of: routes exist + smoke test updated to assert on them, OR documented decision that `/login` + `/register` + `/api/health` are the canonical surfaces and original AC text superseded.

## Related

- Parent: `documents/04-quality/gaps/closed/GAP-377-smoke-test-post-deploy-automation.md` (closed Wave 26 Bucket C)
- Wave plan: `documents/03-planning/waves/wave-2026-05-06-26-pdpl-phase-2-closeout-smoke-test.md`

## Log

- **2026-05-06:** Filed during GAP-377 implementation per `audit-to-gap-pipeline.md` Step 2.5 state-check (banned routes substituted; original AC routes tracked here).
