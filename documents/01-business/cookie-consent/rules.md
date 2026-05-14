# Cookie Consent — Business Rules

**Domain:** PDPL-compliant cookie consent banner + granular category opt-in (Wave 79 Bucket B — GAP-558)
**Last verified:** 2026-05-14 (Wave 79 Bucket 0 Foundation)
**Config prefix:** `kitehub.consent.cookie`

File này document business values cho cookie consent flow. Mỗi rule có 5 attributes theo `.claude/rules/business-logic-review.md` §2.

> **Wave 79 Bucket B context:** PDPL 2023 (Personal Data Protection Law) effective 2026-07-01 — Wave 79 ship consent banner trước deadline. In-house lightweight component (per Wave 79 plan §1 Brainstorm Q2 trade-off — defer vendor Cookiebot/Osano Phase 2).

---

## BR-COOKIE-001 — Consent required cho non-essential cookies trước khi load

- **Value:** Mọi non-essential cookie (analytics — GA / Mixpanel; functional — preference cache cross-session) MUST KHÔNG load cho tới khi user provide explicit consent qua banner. Essential cookies (session ID, CSRF token, auth cookies) load mặc định (out-of-scope cho consent).
- **Source:** PDPL 2023 Art 11 (explicit consent required cho processing personal data) + Nghị định 13/2023/NĐ-CP Art 4 (definition of consent must be voluntary, specific, informed, unambiguous).
- **Rationale:** Cookie analytics fire trước consent = vi phạm PDPL "opt-in by default required". Solution: deferred script loading (per Wave 79 plan §1 Brainstorm Q3 — `<Script strategy="afterInteractive">` Next.js + onConsent callback). Beta scale acceptable to lose few days analytics nếu cần. Functional cookies (vd theme preference) gated similar — degrade gracefully (theme localStorage fallback).
- **Reviewer:** @nguyenvankiet (acting Compliance scout + Product Owner, solo-dev, 2026-05-14). PDPL counsel review queued — see GAP-156.
- **Compliance check:** **Compliant** — PDPL 2023 Art 11 + Decree 13/2023 Art 4. NIST Privacy Framework Identify-P 6 (purpose) cross-reference.
- **Review cadence:** Quarterly + event-driven (PDPL implementing-decree update). **Next review:** 2026-08-14 OR within 30 ngày của bất kỳ PDPL decree publication.
- **Code reference:** Wave 79 Bucket B — `kitehub-frontend/src/components/CookieConsent.tsx` + analytics script loader.

## BR-COOKIE-002 — 3 granular categories: essential / functional / analytics

- **Value:** Banner provide 3 category toggles:
  - `essential` — always ON, không toggle được (per BR-COOKIE-001).
  - `functional` — theme preference, language preference, last-visited page. Default OFF; user opt-in.
  - `analytics` — Google Analytics 4, Mixpanel events, error tracking (Sentry — defer Phase 2 cũng N/A). Default OFF; user opt-in.
- **Source:** Cookiebot / Osano industry standard 3-tier categorization; matches GDPR + PDPL "specific" requirement (per category opt-in).
- **Rationale:** Single "Accept all" / "Reject all" buttons cho UX speed; granular toggle cho power users / privacy-conscious. 3 categories đủ Phase 1 BETA (no marketing/advertising cookies — KiteHub không advertise outbound). Phase 2+ có thể add `marketing` category nếu invest ad tracking.
- **Reviewer:** @nguyenvankiet (acting Compliance scout + Product Owner, solo-dev, 2026-05-14).
- **Compliance check:** **Compliant** — PDPL 2023 Art 11 (granular consent per purpose).
- **Review cadence:** Quarterly. **Next review:** 2026-08-14. Event triggers: thêm category (marketing) khi launch ads.
- **Code reference:** Wave 79 Bucket B — `kitehub-frontend/src/components/CookieConsent.tsx` toggle UI.

## BR-COOKIE-003 — Consent persist 12 tháng

- **Value:** User consent (accept/reject per category) lưu DB table `cookie_consents` (BE-side authoritative). Local cookie `kh_consent` mirror trạng thái (HTTPOnly: false, JS-readable, expires 12 tháng). Sau 12 tháng banner re-show cho refresh consent.
- **Source:** PDPL 2023 Art 11 (consent must be reaffirmed periodically); GDPR Art 7 (consent withdrawal anytime — implies refresh cadence).
- **Rationale:** 12 tháng đủ tránh banner fatigue (re-prompt thường = vi phạm UX) nhưng đủ tươi để treat as informed consent. Industry standard 6-13 tháng range (Cookiebot default 6mo; some legal jurisdictions require 12mo). 12 tháng matches DPDP Act 2023 (India) + DPA Singapore precedent.
- **Reviewer:** @nguyenvankiet (acting Compliance scout, solo-dev, 2026-05-14).
- **Compliance check:** **Compliant** — PDPL 2023 Art 11 (periodic consent).
- **Review cadence:** Annual. **Next review:** 2027-05-14. Event triggers: PDPL implementing-decree specifies different period.
- **Code reference:** Wave 79 Bucket B — `cookie_consents` table (`user_id` nullable cho anonymous, `cookie_id` UUID, `categories_accepted` JSONB, `created_at`, `expires_at = created_at + 12 months`).

## BR-COOKIE-004 — Analytics gated bằng consent state runtime

- **Value:** Analytics script (GA / Mixpanel) chỉ load AFTER user opt-in `analytics=true`. FE state management:
  - On banner submit (accept analytics) → call `window.loadAnalytics()` → inject `<script async src="https://www.googletagmanager.com/...">`
  - On withdraw consent → call `window.unloadAnalytics()` → no-op cho scripts đã load (browser session); subsequent reload won't load.
- **Source:** BR-COOKIE-001 (consent-first); GDPR + PDPL strict interpretation (no fire before consent).
- **Rationale:** Server-side approach impossible cho client analytics (browser fire). FE-only gate ensure correctness — accept tradeoff "lose data từ user accept analytics rồi tab close trong same session" (acceptable Phase 1 BETA scale; <5% data loss estimate).
- **Reviewer:** @nguyenvankiet (acting Compliance + Product Owner, solo-dev, 2026-05-14).
- **Compliance check:** **Compliant** — PDPL Art 11 + Decree 13/2023.
- **Review cadence:** Quarterly. **Next review:** 2026-08-14.
- **Code reference:** Wave 79 Bucket B — `kitehub-frontend/src/lib/analytics/consent-gated.ts` loader.

## BR-COOKIE-005 — Right to withdraw consent (UI button + endpoint)

- **Value:** User có thể withdraw consent anytime qua footer link "Cookie preferences" → modal hiện trạng thái + cho phép thay đổi từng category. Submit → call `DELETE /api/v1/consent/cookie/{cookieId}` (anonymous) hoặc PUT (authenticated). Server-side revoke + clear `kh_consent` cookie + reload banner.
- **Source:** PDPL 2023 Art 21 (right to withdraw consent) + Decree 13/2023.
- **Rationale:** "Right to withdraw" là core PDPL requirement. UI access cần discoverable (footer link mọi page, NOT chỉ banner). Modal pattern cho UX clarity (vs full settings page Phase 2).
- **Reviewer:** @nguyenvankiet (acting Compliance scout + Product Owner, solo-dev, 2026-05-14). PDPL counsel review queued — GAP-156.
- **Compliance check:** **Compliant** — PDPL Art 21 (withdrawal as easy as grant).
- **Review cadence:** Quarterly + event-driven (PDPL implementing decree). **Next review:** 2026-08-14.
- **Code reference:** Wave 79 Bucket B — `kitehub-frontend/src/components/CookieConsent.tsx` "Preferences" link + BE `ConsentController.withdraw()`.

---

## Config

| Key | Default | Purpose |
|-----|---------|---------|
| `kitehub.consent.cookie.expiry-months` | `12` | Consent persistence period (BR-COOKIE-003) |
| `kitehub.consent.cookie.categories` | `essential,functional,analytics` | Whitelist categories Phase 1 |
| `kitehub.consent.cookie.essential-always-on` | `true` | Cannot be disabled |
| `kitehub.consent.cookie.banner-position` | `bottom-right` | UI placement (mobile-friendly) |
| `kitehub.consent.cookie.privacy-policy-url` | `/legal/privacy` | Link trong banner |
| `kitehub.consent.cookie.cookie-policy-url` | `/legal/cookies` | Link trong banner |
| `kitehub.consent.cookie.public-rate-limit-per-min-per-ip` | `30` | Gateway rate limit (POST/PUT/DELETE) |

Config keys nằm `application.yml` của `kitehub-subscription` BE module (Bucket B) + Next.js env (FE).
