# User Preferences — API Contract

**Domain:** User preference state — dismissible banners + onboarding phase tracking (Wave 98 GAP-656 UI Coordinator)
**Source-of-truth controller:** `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/preferences/controller/PreferencesController.java`
**Last verified:** 2026-05-18 (Wave 98 Bucket B0 — GAP-656)

Contract này là source-of-truth cross-layer cho Wave 98 Bucket B0 UI Coordinator. Consumed bởi:
- FE `useOnboardingPhase` hook đọc dismissal state qua cookie (server-set, httpOnly mặc định, cross-tab sync)
- FE `OnboardingCoordinator` component sequence reveal banner → modal → support menu
- FE `SupportMenu` không cần endpoint riêng (mailto + link routing only)

---

## Endpoint

### POST /api/v1/preferences/dismiss-banner-state

**Use case:** UC-PREF-001 — User dismiss banner (vd: beta disclaimer banner, day-1 onboarding modal). Server set httpOnly cookie `kite-banner-dismissed-{bannerKey}` 30-day expiry để cross-tab + cross-browser sync (per GAP-656 §Proposed Fix Step 5).

**Auth:** Public (authenticated optional). Anonymous user vẫn dismiss được vì cookie set theo browser session. Authenticated user JWT context tự động attach qua `SecurityContextHolder` để future persistence vào user preferences table (Wave 99+ scope).

**Request body (`DismissBannerStateRequest`):**

```json
{
  "bannerKey": "beta-disclaimer-2026-q2",
  "dismissed": true
}
```

**Field constraints:**

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `bannerKey` | string | yes | Non-blank, 3-100 chars, kebab-case `[a-z0-9-]+`. Identify banner. Ví dụ: `beta-disclaimer-2026-q2`, `day-1-onboarding`. |
| `dismissed` | boolean | yes | `true` để dismiss; `false` để reset (uncommon, admin scenario). |

**Response 204 No Content:**

Server set HTTP header `Set-Cookie: kite-banner-dismissed-{bannerKey}=1; Path=/; Max-Age=2592000; HttpOnly; SameSite=Lax; Secure`. Không body trả về.

Cookie name pattern: `kite-banner-dismissed-{bannerKey}` (slug-safe). Max-Age 30 ngày (2592000 giây).

**Errors:**

| HTTP | Error code | Trigger |
|------|------------|---------|
| 400 | `PREF_INVALID_BANNER_KEY` | `bannerKey` blank, sai format kebab-case, hoặc vượt 100 chars |
| 400 | `PREF_MISSING_DISMISSED` | `dismissed` field missing |
| 429 | `RATE_LIMITED` | Per-IP gateway rate limit exceeded (60 req/min/IP) |

---

## Side effects

- Server set httpOnly cookie response header — cross-tab sync via document.cookie không hoạt động vì httpOnly, nhưng cross-tab sync work qua HTTP request next page-load (cookie tự động gửi lên server, server có thể inject vào HTML hoặc FE đọc dismissal qua GET endpoint future scope).
- Wave 98 scope: chỉ set cookie. Future Wave 99+ scope: persist trong `user_preferences` table khi user authenticated.
- KHÔNG emit outbox event (preference state là per-user transient, không cross-service).

---

## Wave 98 implementation notes

- Phase 1 (this PR): in-memory `dismissed` mapping per server instance (acceptable cho beta scale, no persistence across restart yet)
- Phase 2 (Wave 99+): persist `user_preferences` table với `user_id + banner_key + dismissed_at` columns
- TODO cookie name slug sanitization: stripped non-alphanumeric chars, lowercased, max 100 chars

---

## Related

- GAP-656 (this contract source)
- GAP-540 (support widget — separate scope, no endpoint needed, mailto + link routing only)
- GAP-542 (feedback widget — uses `/api/v1/feedback` per `documents/01-business/kitehub/feedback/api-contract.md`)
- Wave 98 plan: `documents/03-planning/waves/wave-2026-05-18-98-cluster-b-beta-cohort-polish.md` §Bucket B0
- Rule `contract-first-for-cross-layer.md` v1.0.1 — contract ship cùng PR với controller
- Rule `pre-launch-auth-hardening-checklist.md` — httpOnly cookie mandate

---

## Log

- **2026-05-18** — Initial contract create cho GAP-656 Wave 98 Bucket B0 UI Coordinator. Phase 1 in-memory cookie set; Phase 2 user_preferences persistence Wave 99+ deferred per scope.
