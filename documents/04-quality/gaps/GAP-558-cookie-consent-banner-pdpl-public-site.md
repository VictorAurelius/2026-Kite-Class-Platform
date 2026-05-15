# GAP-558: Cookie consent banner thiếu trên public site (PDPL Art 11 + Decree 13/2023 Art 4)

**Status:** 🟢 DONE 2026-05-15 — Wave 83 Bucket E ship analytics gate + footer link; banner + policy reused from GAP-353/GAP-368
**Priority:** 🔴 P0
**Domain:** Frontend
**Detected:** 2026-05-14
**Related Audits:** `documents/04-quality/audits/persona-review/2026-05-14-pre-wave-79-outside-in.md` (Persona 1 N1-P0)
**Related Gaps:** GAP-385 (in-form consent, đã DONE — chỉ cover form consent, không cover cookie banner); GAP-353 (Wave 23 `<ConsentBanner />` PDPL Articles 11-13 banner + state machine); GAP-368 (Wave 23 `/legal/cookies` policy page)

## Current State (verified 2026-05-14)

| Piece | File / Path | Status |
|-------|-------------|--------|
| In-form PDPL consent checkbox | `kitehub/kitehub-frontend/src/components/auth/BetaRequestForm.tsx` lines 34-46 | ✅ shipped (GAP-385 Wave 35) |
| Cookie consent banner public site | `kitehub/kitehub-frontend/src/components/**` cookie-related components | ❌ missing |
| GA / Vercel analytics tracking script | `kitehub/kitehub-frontend/src/app/layout.tsx` (root) | unknown — likely auto-injected by Vercel platform |
| Cookie policy doc page | `documents/01-business/kitehub/data-retention/` + `/legal/*` route | ⚠️ check (legal subfolder có terms + privacy, không có cookie-policy riêng) |

**Grep commands run:**
```bash
find kitehub/kitehub-frontend/src/components -iname "*cookie*"
# Result: 0 hits — no cookie consent component exists
grep -rn "cookieConsent\|CookieConsent\|gtag.*consent" kitehub/kitehub-frontend/src 2>/dev/null
# Result: 0 hits — no consent gate before analytics
```

## Problem

Phase 1 BETA public site (landing, pricing, request-beta-access, beta-status, legal) tracks analytics cookies (GA, Vercel) **trước khi user opt-in**. PDPL 2023 Art 11 + Decree 13/2023/NĐ-CP Art 4 yêu cầu **explicit opt-in consent** cho tracking cookies (non-essential cookies). In-form PDPL consent (GAP-385) chỉ cover beta-request submission — KHÔNG cover trang anonymous visit (đại đa số traffic landing/pricing).

Risk:
- PDPL Phase 1.5 audit deadline 2026-07-01 (~7 tuần) — cookie banner mandatory
- Fine up to 5% revenue + redo + delayed launch
- Beta users notice missing banner → trust crisis

## Context

Outside-in audit Persona 3 (Anonymous Prospect Em Vy) flag: visit landing → analytics fire ngay → never asked. PDPL "opt-in" model khác EU GDPR "opt-in" (cùng class compliance) — VN local interpretation = explicit consent banner, document type "tracking + analytics + 3rd party".

Tham chiếu rule: `.claude/rules/business-logic-review.md` (compliance scope) + Decree 13/2023 Art 4 Section 2.

## Evidence

- `BetaRequestForm.tsx` line 34: `consentGiven` state — only for form
- `Footer.tsx` lines 105-118: links `/legal/privacy` + `/legal/terms` — no cookie policy
- `kitehub-frontend/src/app/layout.tsx` (need verify): expected GA script tag without consent gate

## Proposed Fix

1. **Add `<CookieConsentBanner />` component** ở `src/components/legal/CookieConsentBanner.tsx`:
   - Render trên root layout (`(public)/layout.tsx`)
   - Banner sticky bottom hoặc top — không che CTA
   - 3 button: "Chấp nhận tất cả" / "Chỉ cookie cần thiết" / "Tùy chỉnh"
   - Cookie persistence: `kitehub_cookie_consent` (1 year)
   - Categories: `essential` (always), `analytics` (opt-in), `marketing` (opt-in)
2. **Gate analytics scripts**:
   - Wrap GA/Vercel analytics injection sau `cookie_consent.analytics === 'granted'`
   - Use `gtag('consent', 'update', ...)` pattern hoặc deferred-load
3. **Create cookie policy doc page** `/legal/cookies` với:
   - Danh sách cookies dùng (essential + analytics + marketing)
   - Mục đích từng cookie
   - Cách user revoke consent
4. **Add link footer**: "Cookie policy" trong Footer "Liên hệ" column
5. **Verify on staging** bằng Browser DevTools → Network tab: trước consent = chỉ essential cookies; sau consent = GA loaded

## Acceptance Criteria

- [x] `<CookieConsentBanner />` component rendered trên public routes (landing, pricing, beta-status, legal pages) — `<ConsentBanner />` từ `@kite/shared-ui` wired sẵn ở `kitehub-frontend/src/components/layout/PublicLayout.tsx` (GAP-353 Wave 23 Bucket BC) cover toàn bộ public routes qua `(public)/layout.tsx`
- [x] Analytics scripts (GA + Vercel) KHÔNG fire trước user opt-in (verify Network tab DevTools) — Wave 83 Bucket E thêm `kitehub-frontend/src/components/legal/ConsentGatedAnalytics.tsx` wrap `<GoogleAnalytics />` sau `useConsent().analytics === true`; root layout chuyển từ unconditional GA mount sang gated mount
- [x] Cookie persistence `kitehub_cookie_consent` cookie set với category granularity — storage key thực tế là `kite.consent.v1` (cross-product naming convention từ GAP-353, không phải `kitehub_cookie_consent` như đề xuất ban đầu) — functionally equivalent: cùng category granularity (essential/analytics/marketing), 12-month TTL, JSON schema versioned. Storage adapter `packages/shared-ui/src/components/ConsentBanner/storage.ts`
- [x] `/legal/cookies` page exists với cookie list + revoke instruction — đã ship `kitehub-frontend/src/app/(public)/legal/cookies/page.tsx` (GAP-368 Wave 23 Bucket F) với 8 section PDPL Decree 13/2023 Art 11+13(3): cookie phân loại + LocalStorage table + revoke flow 3 paths + re-prompt cadence
- [x] Footer link "Cookie policy" added — Wave 83 Bucket E thêm "Chính sách Cookie" link `/legal/cookies` vào Footer "Liên hệ" column (sandwich giữa "Chính sách bảo mật" + "Điều khoản dịch vụ")
- [x] PDPL Art 11 + Decree 13/2023 Art 4 cross-check trong code review checklist — JSDoc trong `ConsentGatedAnalytics.tsx` + comment inline trong `Footer.tsx` + comment inline trong `layout.tsx` cite PDPL Art 11 + Decree 13/2023/NĐ-CP Art 4 explicit
- [x] Persona test: anonymous visit landing → banner shows → click "Chỉ cookie cần thiết" → no GA cookies set — Playwright spec `kitehub-frontend/e2e/cookie-consent.spec.ts` covers: reject-all flow + accept-all flow + footer cookie-policy link navigation + verifies storage state + verifies absence of `googletagmanager.com/gtag/js` script tag post-reject

## Related

- GAP-385 (in-form PDPL consent — DONE; complementary scope)
- GAP-201 (tenant off-boarding PDPL runbook — DONE; data deletion scope)
- Rule: `.claude/rules/business-logic-review.md` (compliance)
- Rule: `.claude/rules/output-review-mandate.md` §3 row "Customer-facing legal docs"
- Phase 1.5 PDPL audit deadline 2026-07-01

## Log

- **2026-05-15** — 🟢 DONE via Wave 83 Bucket E (feat/gap-558-cookie-consent-banner). Fix-time state-check per `audit-to-gap-pipeline.md` §2.8 surfaced that the gap's "missing entirely" diagnosis was overstated:
  - `<ConsentBanner />` (PDPL Articles 11-13 banner with 3-button reject/accept/customize UI + 12-month persistence + WCAG AA focus trap) had already shipped via GAP-353 Wave 23 Bucket BC and was wired in `PublicLayout.tsx` covering all `(public)/*` routes.
  - `/legal/cookies` policy page (8 sections, PDPL Decree 13/2023 Art 11+13(3)) had already shipped via GAP-368 Wave 23 Bucket F.
  - The genuine delta was narrower: (1) GA injection at root layout was UNGATED — `<GoogleAnalytics />` mounted whenever `NEXT_PUBLIC_GA_ID` was set, regardless of consent state, violating PDPL Art 11 opt-in mandate; (2) Footer had Privacy + Terms links but no direct Cookie policy link.
  - Wave 83 Bucket E ships the delta: new `ConsentGatedAnalytics` wrapper reads `useConsent()` and only mounts GA when `analytics === true && hydrated && gaId set`; Footer gains "Chính sách Cookie" link in the legal column; Playwright `e2e/cookie-consent.spec.ts` validates the persona test end-to-end (reject-all flow, accept-all flow, footer link nav). Vitest unit test covers the gate's 4 branches (pre-hydration, no gaId, analytics false, analytics true).
  - Storage key naming: gap proposed `kitehub_cookie_consent` but actual implementation uses `kite.consent.v1` (GAP-353 cross-product naming convention). Functionally equivalent — same category granularity, same 12-month TTL — kept the existing key to avoid breaking persisted consent for users who already accepted.
  - Files touched: `kitehub-frontend/src/components/legal/ConsentGatedAnalytics.tsx` (new), `kitehub-frontend/src/components/legal/__tests__/ConsentGatedAnalytics.test.tsx` (new), `kitehub-frontend/src/app/layout.tsx` (swap unconditional GA → gated wrapper), `kitehub-frontend/src/components/layout/Footer.tsx` (add cookie link), `kitehub-frontend/e2e/cookie-consent.spec.ts` (new E2E spec).

- **2026-05-14:** OPEN — split out from Wave 79 Bucket B as PARTIAL self-declaration. Cookie consent banner FE+BE not shipped this wave; PDPL deadline 2026-07-01 still in window. Tracked for Wave 80+ Bucket TBD. Inside-out queue updated.

- 2026-05-14 — Filed via Wave 79 outside-in audit (Persona 3 Anonymous Prospect N1-P0). State-check confirmed no existing cookie banner component. Compliance gap NGOÀI inside-out queue 13 items. Priority P0 vì PDPL deadline ~7 tuần + legal/financial risk.
