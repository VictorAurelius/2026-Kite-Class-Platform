# GAP-272m: Server-side persistence for Advanced Mode opt-in

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend (kitehub-subscription / preference service) + Frontend wiring
**Found:** 2026-05-07 (Wave 32 REWORK Bucket D — `(customer)/settings/branding/advanced/page.tsx`)
**Affects:** Settings Advanced Mode opt-in — currently localStorage; cross-device persistence missing
**Related:** GAP-272 (parent — Track 2 port for ai-branding-wizard v2)

## Problem

Wave 32 Bucket D shipped Settings Advanced Mode page with opt-in toggle for
Enterprise tier users (gates §2.1 free-form prompt access). Toggle state
persists via `localStorage` keyed by user ID. This is per-browser only —
user opts in on desktop, toggle resets when they open mobile.

State-check 2026-05-07:
- No `user_preferences` or `settings` API endpoint for branding-specific
  preferences
- `kitehub-subscription` has `Tenant` + `User` entities but no
  `UserPreferences` aggregate

Inline TODO: `TODO(GAP-272m): server-side persistence for Advanced Mode`.

## Root Cause

Advanced Mode is a per-user preference, not a tenant-level config. Backend
preference storage is sparse. Bucket D used localStorage as PARTIAL exit
ramp per `gap-done-discipline.md` §3 — UX functional, persistence gap
flagged.

## Proposed Fix

Two options (decide at fix time):

**Option A — User preferences table (lightweight):**
- New table `user_preferences (user_id, key, value, updated_at)`
- Endpoints: `GET/PUT /api/v1/users/me/preferences/{key}`
- Frontend: replace localStorage with `useUserPreference('branding.advancedMode')`

**Option B — Subscription metadata (Enterprise-bound):**
- Add `advanced_mode_enabled boolean` to subscription entity
- Toggle becomes a subscription mutation
- More aligned with §2.4 "Enterprise tier opt-in"; harder to extend to
  non-branding preferences later

Recommend Option A — generic preferences mechanism reusable for future
per-user settings.

## Acceptance Criteria

- [ ] Backend `/users/me/preferences` endpoints live
- [ ] Frontend `Settings/branding/advanced` page uses real persistence
- [ ] localStorage fallback preserved as offline cache
- [ ] Cross-device test: toggle on desktop persists on mobile after login

## Related

- GAP-272 (parent)
- Wave 32 rework Bucket D (PR #890) — Advanced Mode page scaffolding
- `ai-branding-guidelines.md` §2.4 (Enterprise Advanced Mode)
