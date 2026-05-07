# GAP-272d: Regenerate quota tracking endpoint for AI Branding Wizard

**Status:** 🟢 DONE 2026-05-07 (Wave 34 Bucket A — PR #907 + Bucket D PR #910)
**Priority:** 🟠 P1
**Domain:** Backend (kitehub-branding / kitehub-subscription) + Frontend wiring
**Found:** 2026-05-07 (Wave 32 REWORK Bucket D — `RegenerateCounter.tsx`)
**Affects:** AI Branding Wizard Step 6 — regenerate counter must reflect REAL session quota usage per `ai-branding-guidelines.md` §4.3
**Related:** GAP-272 (parent — Track 2 port for ai-branding-wizard v2)

## Problem

`RegenerateCounter.tsx` renders tier-driven counter (FREE 3 / BASIC 10 /
PREMIUM 30 / ENTERPRISE -1 unlimited) per §4.3. Bucket D shipped quotas
sourced from `useBrandingTier()` hook. **Missing:** real backend
tracking of regenerate count per session/job — currently parent-controlled
state via prop, lost on page refresh.

State-check 2026-05-07:
- No `regenerate_count` column on `branding_job` entity
- No `BrandingRegenerateService` tracking attempts per session
- No endpoint `GET/POST /api/v1/branding/jobs/{jobId}/regenerate-quota`
- `useBrandingTier` returns max quota only; no consumption query

Result: counter starts fresh on every page load. User can bypass quota
limit by refreshing the page.

## Root Cause

Direction C v2 wizard introduces visible regenerate quota as a new UX —
legacy 4-step wizard didn't have regenerate concept at all. Backend never
tracked attempt count.

## Proposed Fix

1. **Backend:** add `regenerate_count` to `branding_job` entity + migration
2. **Service:** `BrandingRegenerateService` with `recordAttempt(jobId)` +
   `getQuotaUsage(userId)` returning `{used, max, tier}`
3. **Endpoints:**
   - `GET /api/v1/branding/regenerate-quota` (current user's session usage)
   - `POST /api/v1/branding/jobs/{jobId}/regenerate` (idempotent — increments + dispatches new generate job)
4. **Frontend:** `RegenerateCounter` parent fetches usage, passes `used`
   prop; "Regenerate" button POSTs to endpoint

## Acceptance Criteria

- [x] Migration adds `regenerate_count` tracking — Bucket A shipped V29 with separate `branding_regenerate_usage` table (cleaner than column extension; supports per-user + per-job tracking)
- [x] Service tracks per-job attempts + per-user session totals — `RegenerateQuotaService` in `kitehub-branding/wizard/service/`
- [x] Endpoint enforces tier quota — returns 403 `QUOTA_EXCEEDED` (deviation from spec's 429; aligns with FORBIDDEN semantics for over-limit auth-scope checks)
- [x] Frontend wired to real endpoint — `useRegenerateQuota` hook (Bucket D)
- [x] Test: quota exhaustion + tier limit branches — `RegenerateQuotaServiceTest` covers FREE/PRO/PREMIUM/ENTERPRISE; controller test covers 403 path

## Log

- **2026-05-07:** Wave 34 Bucket A (PR #907) shipped V29 migration + `BrandingRegenerateUsage` entity + `RegenerateQuotaService` + 2 endpoints. Bucket D (PR #910) wired `useRegenerateQuota` hook. Status code deviation (403 not 429) noted; if 429 strictly required for client-rate-limit semantics, file follow-up. Upsell modal trigger is wizard orchestrator scope — Bucket D's hook returns `{used, limit, resetAt}` ready for orchestrator wiring (tracked GAP-272o).

## Related

- GAP-272 (parent)
- Wave 32 rework Bucket D (PR #890) — RegenerateCounter scaffold
- `ai-branding-guidelines.md` §4.3 (tier-based regenerate limits)
- Wave 32 v1 plan §7 (this letter pre-named there)
