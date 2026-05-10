# GAP-269a: kc-student social-login backend wiring (Zalo OA + Google)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (existing username/password login works; social login is convenience UX)
**Domain:** Backend (kc-core auth providers) + Frontend (provider buttons wiring)
**Found:** 2026-05-10 (Wave 49 Bucket C PARTIAL exit-ramp per `gap-done-discipline.md` §3)
**Parent:** [GAP-269](GAP-269-track-2-port-kiteclass-student.md)
**Affects:** `kiteclass-core/.../auth/**` provider modules + `kiteclass-frontend/src/app/(auth)/**` provider button wiring

## Problem

Wave 49 Bucket C (PR #1093) shipped the kc-student route group reusing existing `/login` per plan. Login screen UI shows existing auth state only — Zalo OA and Google provider buttons are NOT wired (no provider keys configured in Phase 1 BETA MVP, no backend OAuth callback handlers for these providers).

Per `documents/00-brd/personas-catalog.md` Tier 2 Student persona, social login is preferred onboarding path for Vietnamese mobile users (Zalo dominant on VN mobile; Google ubiquitous). Current state forces username/password which adds friction.

## Current State (verified 2026-05-10)

| Artifact | Status |
|---|---|
| Existing `/login` username/password flow | ✅ works |
| Zalo OA provider button UI | ❌ not in current login UI |
| Google provider button UI | ❌ not in current login UI |
| Backend OAuth callback handler `/api/auth/oauth/{zalo|google}/callback` | ❌ does not exist |
| Provider API keys + secrets in `kite/prod/auth/zalo`, `kite/prod/auth/google` | ❌ not provisioned |

## Proposed Fix

### Phase 1: backend
1. Provision Zalo OA + Google OAuth apps; store `client_id` + `client_secret` per provider in AWS Secrets Manager (or Phase 1 BETA equivalent: `application.yml` + env vars per `release-deploy-standard.md` §3 secrets management)
2. Add Spring Security OAuth2 Client autoconfiguration; register both providers in `application.yml`
3. Add `OAuthCallbackController.handleCallback(provider, code, state)`; resolve `User` by provider-specific external-id lookup; auto-create `User` + `StudentProfile` if first-time + tenant context already known
4. Issue JWT post-callback per existing `AuthService` token contract

### Phase 2: frontend
1. Add Zalo OA + Google buttons to login screen with provider icons (matching kit prototype design)
2. On click: redirect to backend `/api/auth/oauth/{provider}/authorize`
3. Handle callback success/error in `(auth)/oauth-callback/page.tsx`
4. Redirect to `/student/today` on success; show error state on failure

### Phase 3: documentation
1. Update `documents/01-business/kiteclass/auth/api-contract.md` with new OAuth endpoints
2. Update `use-cases.md` UC-AUTH-* with OAuth flows
3. Update `rules.md` BR-AUTH-* with provider-specific rules (e.g., Zalo phone-required, Google email-required)
4. ADR if architectural choice (e.g., dedicated `kite-oauth-provider` service vs in-process kc-core)

## Acceptance Criteria

- [ ] Zalo OA + Google OAuth apps provisioned; secrets in store
- [ ] Backend OAuth callback handler ships + integration test
- [ ] FE Zalo OA + Google buttons functional (manual test pass on staging)
- [ ] First-time login auto-creates User + StudentProfile within tenant context
- [ ] JWT issued post-callback matches existing username/password JWT shape
- [ ] api-contract.md + use-cases.md + rules.md updated
- [ ] GAP-269 parent gap "Social login Zalo OA + Google" AC ✅ verifiable

## Related

- Parent: GAP-269
- Sibling: GAP-269b (real REST endpoints — separate concern but may share auth context)
- Cross-link: this is FUTURE-WORK per Wave 49 plan §3 Bucket C "social login (UI; backend wiring có thể deferred follow-up nếu provider chưa setup)"

## Log

- **2026-05-10**: Filed at Wave 49 closure as named follow-up promised in GAP-269 Log entry §"Deferred (explicit) → follow-up sub-gaps".
