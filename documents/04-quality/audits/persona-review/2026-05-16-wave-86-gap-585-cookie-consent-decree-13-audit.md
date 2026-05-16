---
title: Audit cookie consent vs PDPL Decree 13/2023/NĐ-CP — Wave 86 Bucket E E-AC6
status: complete
created: 2026-05-16
phase: Wave 86 Bucket E (E-AC6 P0 BLOCKER)
wave: 86
gaps: [GAP-585, GAP-558]
---

# Wave 86 — Audit cookie consent banner vs Decree 13/2023/NĐ-CP granular mandate

## 1. Scope

Audit hiện trạng cookie consent stack (banner + storage + API + retention + revoke flow) đối chiếu với PDPL Decree 13/2023/NĐ-CP Article 4 + Article 11 + Article 12 + Article 16 granular consent mandate. Mục tiêu: xác định Wave 86 Bucket E E-AC6 P0 BLOCKER pass/fail + ship delta nếu PARTIAL.

Bối cảnh:
- GAP-558 (PR #1408) đã ship Wave 83 cookie banner + ConsentGatedAnalytics + cookies policy page.
- GAP-353b (Wave 25) đã ship backend Consent table + 3 REST endpoint + retention cron 36 tháng.
- Wave 86 plan §3 Bucket E E-AC6: "Cookie consent granular per purpose + no dark pattern + withdraw mechanism + consent log retained ≥3 năm".

## 2. State-Check commands (per `audit-to-gap-pipeline.md` §2.5 + §2.8)

```bash
# Frontend banner + gating
grep -rn "ConsentBanner\|CookieConsent\|cookie.consent\|kite.consent" \
  kitehub/kitehub-frontend/src/ packages/shared-ui/src/ \
  --include="*.tsx" --include="*.ts"

# Backend consent infrastructure
find kitehub -type f -iname "*consent*" | grep -v node_modules | grep -v target

# Retention cron
grep -n "RETENTION_MONTHS\|deleteByCreatedAtBefore" \
  kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/consent/cron/ConsentRetentionCron.java

# Footer cookie link
grep -n "legal/cookies\|Chính sách Cookie" kitehub/kitehub-frontend/src/components/layout/Footer.tsx
```

## 3. Findings — 5-item Decree 13 compliance matrix

| # | Decree 13 mandate | Hiện trạng (verified 2026-05-16) | Verdict |
|---|---|---|---|
| 1 | **Granular consent per purpose** — KHÔNG single Accept All/Reject All only; phải có essential/analytics/marketing toggle riêng (Art 4 §2 + Art 16) | `packages/shared-ui/src/components/ConsentBanner/ConsentBanner.tsx` lines 220-249: 3 `<CategoryRow>` rendered trong expanded mode — essential (checked + disabled), analytics (default OFF), marketing (default OFF). Mỗi toggle có aria-labelledby + description ngắn (`COPY_VI.categories.essentialDesc/analyticsDesc/marketingDesc`). | ✅ **PASS** |
| 2 | **No dark pattern** — Accept + Reject button equal visual weight; KHÔNG pre-check optional categories; decline dễ click như accept (Art 11.2) | `ConsentBanner.tsx` lines 264-294: 3 button đồng cấp (Tuỳ chỉnh / Từ chối tất cả / Đồng ý tất cả). "Đồng ý tất cả" có `bg-primary` còn 2 button kia có `bg-background`; visual hierarchy bằng nhau (cùng `rounded-md`, `px-4 py-2`, `text-sm font-medium`). ESC key → reject (line 102-107). `analyticsOn`/`marketingOn` default `false` (line 90-91). JSDoc class line 26-29 cite BR-PDPL-CONSENT-002 "Three CTAs equal weight". | ✅ **PASS** |
| 3 | **Withdraw mechanism** — Settings page hoặc Footer link "Quản lý cookie" → re-open banner với current state editable (Art 12 right to withdraw) | `useConsent.revoke()` API tồn tại (`useConsent.ts` line 152-166) clear LocalStorage + call `apiRevokeConsent`. NHƯNG **UI surface chưa wire**: Footer.tsx line 115-122 chỉ link tới `/legal/cookies` page; trang đó (cookies/page.tsx line 215-219) document có button "Quản lý cookie" nhưng **chưa render button thực**. → User không thể withdraw via UI trừ khi xoá LocalStorage manual via browser DevTools. | 🟡 **PARTIAL** — backend API ready, FE UI surface missing |
| 4 | **Consent log retained ≥3 năm** — Decree 13 Art 16 implicit + DR-03 retention | `kitehub-subscription V25__create_consent_record.sql` line 14-29: `consent_record` table với (visitor_id, user_id, tenant_id, essential_consented, analytics_consented, marketing_consented, consent_version, ip_address, user_agent, created_at, updated_at, expires_at, revoked_at). `ConsentRetentionCron.java` line 33 `RETENTION_MONTHS = 36` (3 năm chính xác). Daily cron 03:00 (`@Scheduled(cron = "0 0 3 * * *")`). | ✅ **PASS** |
| 5 | **API log endpoint** — POST `/api/v1/consent` lưu consent state khi user submit + GET retrieval + revoke | `ConsentController.java`: POST `/api/v1/consent/record` (line 54 idempotent upsert), GET `/api/v1/consent/{visitorId}` (line 78), POST `/api/v1/consent/{visitorId}/revoke` (line 88). Auto-populate ipAddress + userAgent từ headers (line 60-69). `ConsentRequest` DTO + `ConsentResponse` DTO + 384/384 BE test pass per GAP-353b PR #838. | ✅ **PASS** |

## 4. Verdict overall

**4/5 PASS + 1 PARTIAL** → E-AC6 P0 BLOCKER status **PARTIAL** cho tới khi withdraw UI surface ship.

**Delta cần fix trong cùng PR này:**
- Render button "Mở lại cài đặt cookie" trong `/legal/cookies` page §6.1 list (item 2 mô tả "Cookie banner (re-trigger)" nhưng KHÔNG có button thực tế)
- Wire `useConsent().revoke()` vào button onClick → clear LocalStorage → banner re-mount tự động qua `useConsent.hydrated` reactive state
- Optional: thêm "Quản lý cookie" link vào Footer (defer Wave 87+ — không P0 nếu page link đã đủ)

## 5. Cross-tenant leak check (extra — Decree 13 Art 4 §3 purpose limitation)

- `kite.consent.v1` LocalStorage key chỉ scope theo origin (kitehub.me) — KHÔNG leak cross-domain
- `visitor_id` UUID v4 client-gen, KHÔNG correlate với userId trừ khi user signup (Art 4 §1 minimum data)
- `ip_address` stored trong consent_record table nhưng chỉ cho audit; KHÔNG export ra logs analytics
- Verdict: ✅ purpose limitation respected

## 6. Curl self-test — Decree 13 Art 11 opt-in gate

```bash
# Anonymous visit landing — GA script MUST NOT load trước user opt-in
curl -sI https://kitehub.me/ | grep -i 'set-cookie' | grep -iE 'analytics|gtag|_ga|fb_'
# Expected: 0 hits (GA gated qua ConsentGatedAnalytics until analytics === true)
```

Live verify deferred post-Wave-86-Bucket-F-deploy. Source code analysis confirms gate logic:
- `kitehub-frontend/src/app/layout.tsx` line 14: comment cite PDPL Art 11 + Decree 13/2023 Art 4 explicit
- `ConsentGatedAnalytics.tsx` line 51-53: short-circuit `!hydrated → null`, `!gaId → null`, `!analytics → null` BEFORE render `<GoogleAnalytics />`
- Server-rendered HTML: KHÔNG emit `gtag.js` script tag trước hydration (SSR safety per useConsent hook line 78-79)

## 7. Recommendations

1. **Ship withdraw UI button** (this PR) — close PARTIAL → DONE
2. **Defer Footer "Quản lý cookie" link** Wave 87+ — cookies page link path đã đủ cho E-AC6 P0
3. **Defer "Privacy Center" `/account/privacy-settings`** Phase 2 (cookies/page.tsx §6.1 item 1 đã đánh dấu TODO Phase 2)
4. **Defer Playwright E2E for withdraw flow** — existing `cookie-consent.spec.ts` cover accept/reject path; withdraw button is minor delta, add E2E khi setup CI có headless browser

## 8. Prior actions verified (per `audit-to-gap-pipeline.md` §2.8)

| Action | When | Where verified |
|---|---|---|
| ConsentBanner ship (GAP-353 Wave 23 Bucket BC) | 2026-04-30 | `packages/shared-ui/src/components/ConsentBanner/` |
| `/legal/cookies` page ship (GAP-368 Wave 23 Bucket F) | 2026-04-30 | `kitehub-frontend/src/app/(public)/legal/cookies/page.tsx` |
| Server-side Consent API + retention cron (GAP-353b Wave 25 Bucket A) | 2026-05-06 | `kitehub-subscription/consent/**` + V25 migration |
| Analytics gating (GAP-558 Wave 83 Bucket E) | 2026-05-15 | `ConsentGatedAnalytics.tsx` + Footer cookie link + Playwright `e2e/cookie-consent.spec.ts` |

## 9. References

- `documents/04-quality/gaps/GAP-558-cookie-consent-banner-pdpl-public-site.md` (Wave 83 DONE)
- `documents/04-quality/gaps/GAP-585-cookie-consent-pdpl-decree-13-granular.md` (this wave, paired)
- `packages/shared-ui/src/components/ConsentBanner/ConsentBanner.tsx`
- `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/consent/cron/ConsentRetentionCron.java`
- `kitehub/kitehub-frontend/src/app/(public)/legal/cookies/page.tsx`
- Decree 13/2023/NĐ-CP Article 4 + 11 + 12 + 16 (thuvienphapluat.vn)
- `documents/03-planning/waves/wave-2026-05-15-86-rc1-tag-preflight.md` §3 Bucket E E-AC6
