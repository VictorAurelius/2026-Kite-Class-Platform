# Cookie Consent — Use Cases

**Domain:** Anonymous + authenticated user cookie consent flow (PDPL-compliant)
**Last verified:** 2026-05-14 (Wave 79 Bucket 0 Foundation)

> **Wave 79 Bucket 0 status:** 4 use cases describe target Bucket B Wave 79 implementation.

---

## UC-COOKIE-BANNER-DISPLAY — First visit hiển thị banner

**Actor:** Anonymous visitor (chưa có `kh_consent` cookie) HOẶC user mới quay lại sau >12 tháng (consent expired).
**Trigger:** Page load bất kỳ route trên domain `kitehub.me`.
**Rules:** BR-COOKIE-001 (consent first), BR-COOKIE-002 (3 categories), BR-COOKIE-004 (analytics gated).

### Happy path

1. Browser load page (vd `https://kitehub.me/`).
2. FE Next.js layout `_app.tsx` mount `<CookieConsent />` component.
3. CookieConsent reads `document.cookie` → check `kh_consent` cookie.
4. Cookie absent / expired → render banner bottom-right với:
   - Heading "Chính sách Cookie"
   - Body text "Chúng tôi dùng cookie để cải thiện trải nghiệm. Bạn có thể chọn loại cookie chấp nhận."
   - 3 category toggles:
     - Essential ✅ (disabled toggle, always ON)
     - Functional ❌ (default OFF)
     - Analytics ❌ (default OFF)
   - 3 action buttons:
     - "Chấp nhận tất cả" (set all categories = true)
     - "Chỉ cần thiết" (set functional + analytics = false)
     - "Tùy chỉnh" (focus toggle UI, button "Lưu lựa chọn" enable)
   - 2 links:
     - "Xem chính sách bảo mật" → `/legal/privacy`
     - "Xem chính sách cookie" → `/legal/cookies`
5. Trong khi banner hiển thị, analytics scripts (GA / Mixpanel) KHÔNG load (per BR-COOKIE-001).
6. User click action button → trigger UC-COOKIE-CONSENT-RECORD.

### FE behavior notes

- Banner KHÔNG block page render (positioned fixed bottom-right; page content vẫn interact).
- Banner KHÔNG có "X close" button (UX dark pattern — close ≠ consent). User MUST chọn action.
- Mobile responsive: banner stack vertical (max-width 100vw, padding-bottom adjust với device).
- Accessibility: trap focus inside banner khi keyboard navigate; ESC key open "Customize" view nhưng KHÔNG dismiss.
- I18n: vi-VN primary (per CLAUDE.md communication language); en-US optional Phase 2.

---

## UC-COOKIE-CONSENT-RECORD — User submit lựa chọn consent

**Actor:** Anonymous visitor đang xem banner (UC-COOKIE-BANNER-DISPLAY).
**Trigger:** Click "Chấp nhận tất cả" / "Chỉ cần thiết" / "Lưu lựa chọn" sau customize.
**Endpoint:** `POST /api/v1/consent/cookie`
**Rules:** BR-COOKIE-003 (persist 12mo), BR-COOKIE-004 (analytics gating runtime).

### Happy path

1. User submit → FE collect `{ essential: true, functional: bool, analytics: bool }`.
2. FE generate UUID v4 cho `cookieId` (anonymous identifier; rotated nếu user clear cookies).
3. FE call `POST /api/v1/consent/cookie` với body `{ cookieId, categories: {essential: true, functional, analytics}, userAgent, language }`.
4. BE validate categories (whitelist `essential,functional,analytics`), insert `cookie_consents` row với:
   - `cookie_id` = UUID
   - `user_id` = null (anonymous) hoặc JWT subject nếu authenticated request
   - `tenant_id` = null
   - `categories_accepted` = JSONB
   - `request_ip` (truncated to /24 cho IPv4, /48 cho IPv6 per PDPL minimization)
   - `user_agent` (truncated 200 chars)
   - `created_at` = now
   - `expires_at` = now + 12 months
5. BE return 201 với `{ cookieId, expiresAt }`.
6. FE set browser cookie `kh_consent=<cookieId>; Max-Age=31536000; SameSite=Lax; Path=/` (12 tháng).
7. FE update React state → banner fade out.
8. FE conditional analytics load:
   - `analytics=true` → call `window.loadAnalytics()` → inject `<script>` tags GA / Mixpanel với pre-configured IDs.
   - `analytics=false` → no-op (scripts không inject; future page loads honor cookie).
9. FE conditional functional cookie load:
   - `functional=true` → enable cross-session preference cache (theme, language).
   - `functional=false` → fallback localStorage only (cleared khi user clear browser data).

### Authenticated user case

If user logged in khi banner display (vd Owner mở banner via footer "Cookie preferences"):
- FE attach `Authorization: Bearer <accessToken>` header.
- BE set `cookie_consents.user_id` = JWT subject + `tenant_id` = JWT claim.
- Same cookie set browser-side.
- Future logins from new device → check DB by `user_id`; if active consent exists → skip banner (already informed).

### Error branches

| Step | Failure | HTTP | Error code | FE behavior |
|------|---------|:----:|------------|-------------|
| 4 | `categories` chứa key không trong whitelist | 400 | `INVALID_CATEGORY` | Console error; defensive — should not happen từ UI |
| 4 | `essential=false` (user tampered request) | 400 | `INVALID_ESSENTIAL_CONSENT` | Reject; UI force essential=true |
| 4 | `cookieId` đã tồn tại với active consent | 409 | `CONSENT_ALREADY_RECORDED` | Toast "Đã ghi nhận"; FE force re-read; banner close |
| (gw) | Rate limit (30/min/IP) | 429 | `RATE_LIMITED` | Toast "Thử lại sau"; banner stays |
| (net) | Network offline | (FE) | — | Toast "Mất mạng. Lựa chọn sẽ lưu cục bộ + sync sau"; FE store localStorage fallback + retry queue |

### FE behavior notes

- Submit button disabled khi pending network call (avoid double-submit).
- localStorage fallback: nếu network failed, FE store `localStorage.kh_consent_pending = {cookieId, categories}` + register `online` event handler để retry.
- After submit, banner auto-close 500ms animation.
- Subtle toast bottom-left "Lựa chọn cookie đã lưu" 3s.

---

## UC-COOKIE-WITHDRAW — User withdraw consent

**Actor:** Visitor đã có consent active (cookie `kh_consent` present).
**Trigger:** Click footer link "Cookie preferences" → modal hiện hoặc banner re-show.
**Endpoint:** `DELETE /api/v1/consent/cookie/{cookieId}` (anonymous) hoặc `PUT /api/v1/consent/cookie/{cookieId}` (update categories without revoke entire consent)
**Rules:** BR-COOKIE-005 (right to withdraw), BR-COOKIE-001 (consent gating analytics).

### Happy path (full withdraw — DELETE)

1. User click "Cookie preferences" footer link → modal hiện trạng thái consent hiện tại.
2. User click "Withdraw all" → confirm dialog "Bạn chắc chắn? Banner sẽ hiện lại + analytics tắt".
3. FE call `DELETE /api/v1/consent/cookie/{cookieId}` với cookie ID từ browser cookie.
4. BE soft-delete `cookie_consents.status = 'WITHDRAWN'` + `withdrawn_at = now` (preserve row cho audit trail per `pre-launch-owasp-rest-hardening-checklist.md` §2.8 A09).
5. BE return 204 No Content.
6. FE clear browser cookie `kh_consent` (set `Max-Age=0`).
7. FE call `window.unloadAnalytics()` (no-op cho already-loaded scripts in current session; future reloads won't load).
8. FE reload banner via state reset.
9. User must re-select categories (banner appears again).

### Happy path (partial update — PUT)

1. User click "Cookie preferences" → modal hiện current toggles.
2. User toggle off `analytics`, keep `functional` → submit.
3. FE call `PUT /api/v1/consent/cookie/{cookieId}` với new categories.
4. BE update existing row `categories_accepted` JSONB + `updated_at = now` (keep `expires_at` same).
5. BE return 200 với updated state.
6. FE update React state; analytics scripts not unloaded từ current session nhưng future reload không load.
7. Modal close, subtle toast.

### Error branches

| Step | Failure | HTTP | Error code | FE behavior |
|------|---------|:----:|------------|-------------|
| 3 | Cookie ID không tồn tại trong DB | 404 | `CONSENT_NOT_FOUND` | Toast "Không tìm thấy consent. Banner sẽ hiện lại"; clear cookie; reload banner |
| 3 | Cookie ID đã withdrawn rồi | 410 | `CONSENT_ALREADY_WITHDRAWN` | Toast "Đã thu hồi"; clear cookie; reload banner |
| (gw) | Rate limit | 429 | `RATE_LIMITED` | Toast retry |

### FE behavior notes

- Modal có table preview: hiện tại bạn đã accept categories nào, expires khi nào.
- "Withdraw all" button red style + confirm dialog (destructive action UX pattern).
- Sau withdraw, banner re-show ngay (UC-COOKIE-BANNER-DISPLAY entry point).

---

## UC-COOKIE-GRANULAR-CATEGORY-TOGGLE — User customize categories trước khi submit

**Actor:** Anonymous visitor đang xem banner.
**Trigger:** Click "Tùy chỉnh" trên banner.
**Rules:** BR-COOKIE-002 (granular toggles).

### Happy path

1. User click "Tùy chỉnh" trên banner.
2. Banner expand show 3 toggle switches:
   - Essential (disabled toggle, always ON, locked với tooltip "Cần thiết cho trang web hoạt động")
   - Functional (toggle, default OFF)
   - Analytics (toggle, default OFF)
3. Mỗi category có "Learn more" disclosure (chevron expand) hiện danh sách cookie cụ thể + purpose:
   - Essential: `JSESSIONID` (session), `XSRF-TOKEN` (CSRF), `kh_consent` (preference này)
   - Functional: `kh_theme` (dark/light), `kh_locale` (vi/en), `kh_last_visited`
   - Analytics: `_ga`, `_ga_*` (Google Analytics 4); `mp_*` (Mixpanel — defer Phase 2)
4. User toggle Functional ON, Analytics ON/OFF theo lựa chọn → button "Lưu lựa chọn" activate.
5. User click "Lưu lựa chọn" → trigger UC-COOKIE-CONSENT-RECORD với chosen categories.

### FE behavior notes

- Toggle a11y: `role="switch"`, `aria-checked` proper, keyboard `Space` toggles.
- Learn more disclosure: `<details>` element OR Radix UI Accordion.
- Mobile: stack vertical, full-width toggles cho tap target.
- Banner height adapt với content (max ~70vh trên mobile để không cover toàn screen).
