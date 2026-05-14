# GAP-558: Cookie consent banner thiếu trên public site (PDPL Art 11 + Decree 13/2023 Art 4)

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Frontend
**Detected:** 2026-05-14
**Related Audits:** `documents/04-quality/audits/persona-review/2026-05-14-pre-wave-79-outside-in.md` (Persona 1 N1-P0)
**Related Gaps:** GAP-385 (in-form consent, đã DONE — chỉ cover form consent, không cover cookie banner)

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

- [ ] `<CookieConsentBanner />` component rendered trên public routes (landing, pricing, beta-status, legal pages)
- [ ] Analytics scripts (GA + Vercel) KHÔNG fire trước user opt-in (verify Network tab DevTools)
- [ ] Cookie persistence `kitehub_cookie_consent` cookie set với category granularity
- [ ] `/legal/cookies` page exists với cookie list + revoke instruction
- [ ] Footer link "Cookie policy" added
- [ ] PDPL Art 11 + Decree 13/2023 Art 4 cross-check trong code review checklist
- [ ] Persona test: anonymous visit landing → banner shows → click "Chỉ cookie cần thiết" → no GA cookies set

## Related

- GAP-385 (in-form PDPL consent — DONE; complementary scope)
- GAP-201 (tenant off-boarding PDPL runbook — DONE; data deletion scope)
- Rule: `.claude/rules/business-logic-review.md` (compliance)
- Rule: `.claude/rules/output-review-mandate.md` §3 row "Customer-facing legal docs"
- Phase 1.5 PDPL audit deadline 2026-07-01

## Log

- 2026-05-14 — Filed via Wave 79 outside-in audit (Persona 3 Anonymous Prospect N1-P0). State-check confirmed no existing cookie banner component. Compliance gap NGOÀI inside-out queue 13 items. Priority P0 vì PDPL deadline ~7 tuần + legal/financial risk.
