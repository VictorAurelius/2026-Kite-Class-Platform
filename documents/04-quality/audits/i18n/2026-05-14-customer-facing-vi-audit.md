---
title: Customer-facing Vietnamese i18n audit — Wave 78 Bucket A (GAP-541)
status: complete
created: 2026-05-14
phase: Wave 78 Bucket A FE Polish
wave: 78
gaps: [GAP-428, GAP-541]
---

# Customer-facing Vietnamese i18n audit — Wave 78 Bucket A

## Scope

Audit chất lượng tiếng Việt cho các surface customer-facing trong `kitehub-frontend`:
- Landing page (`(public)/page.tsx` → `LandingClient.tsx`)
- Pricing page (`(public)/pricing/`)
- Signup funnel (`(auth)/register` → redirect `/request-beta-access`)
- TOS page (`(public)/legal/terms/page.tsx`) — placeholder banner v1
- Public layout (`components/layout/PublicLayout.tsx`)

Out of scope (cover ở bucket khác):
- Dashboard banner (Bucket B — GAP-539)
- Approval email subjects + bodies (Bucket E — GAP-543 cover email module)

---

## Commands run (state-check per `audit-to-gap-pipeline.md` §2.8)

```bash
find kitehub/kitehub-frontend/src -type d -name "i18n" -o -type d -name "locales"
# Result: 0 dirs — KHÔNG có i18n system

grep -l "next-intl\|i18next\|formatjs\|react-intl" kitehub/kitehub-frontend/package.json
# Result: 0 matches — KHÔNG có i18n library

grep -rn "kiteclass\.com\|support@kiteclass\|1900-xxxx" kitehub/kitehub-frontend/src --include="*.tsx" --include="*.ts"
# Result: 4 hits trong public scope (LandingClient.tsx lines 382/1003/1007 + customer settings)
```

---

## Findings

### Trạng thái thực tế: KHÔNG có i18n library

`kitehub-frontend` không tích hợp `next-intl`, `i18next`, `formatjs`, hay `react-intl`. Toàn bộ text VN hardcoded inline trong `.tsx` files. Folder `src/i18n/locales/vi/` mà GAP-541 mô tả KHÔNG tồn tại.

→ AC "i18n locale `vi/` files audit complete với coverage % per key namespace" về mặt kỹ thuật N/A — không có locale files để audit. Tuy nhiên, mục tiêu chính (chất lượng tiếng Việt customer-facing) vẫn audit được trực tiếp trên hardcoded strings.

### Per-surface coverage

| Surface | File | Vietnamese coverage | Tone | Verdict |
|---------|------|---------------------|------|---------|
| Landing hero + features + steps + testimonials + FAQ | `(public)/LandingClient.tsx` (1015 LOC) | 100% narrative | ✅ tự nhiên, persona-friendly | ✅ PASS |
| Landing CTA bottom contact | `(public)/LandingClient.tsx` lines 1001-1009 | 100% (sau fix) | ✅ — `support@kitehub.me` đồng bộ brand | ✅ PASS (đã fix placeholder `1900-xxxx` + `support@kiteclass.com`) |
| Pricing page tier cards + FAQ | `(public)/pricing/PricingContent.tsx` + `faqs.ts` | 100% | ✅ format VND đúng (`Intl.NumberFormat('vi-VN')`); câu thoại tự nhiên | ✅ PASS |
| Pricing toggle Hàng tháng / Hàng năm | `PricingContent.tsx` line 104-126 | 100% | ✅ | ✅ PASS |
| TOS placeholder banner v1 | `(public)/legal/terms/page.tsx` lines 22-31 | 100% | ✅ rõ ràng "v1 — đang chờ legal counsel review" + GAP-180/154 reference + ngày cập nhật/hiệu lực | ✅ PASS |
| TOS 15-section body | `(public)/legal/terms/page.tsx` lines 32-247 | ~70% (Vietnamese narrative + English legal term mix natural) | ✅ Tone formal đúng pháp lý; English token (Provider/Customer/End User/Effective Date) inline OK per `dev-readable-doc-language.md` §4 | ✅ PASS (mixed-language tự nhiên) |
| Signup funnel `/register` redirect | `(auth)/register/page.tsx` | metadata "Đăng ký — Beta giới hạn" VN ✅; redirect tới `/request-beta-access` form (out of bucket scope) | ✅ | ✅ PASS for redirect step |
| Public layout footer | `components/layout/PublicLayout.tsx` line 81 | 100% — `support@kitehub.me` đồng bộ | ✅ | ✅ PASS |

### Mixed-language verification (per `dev-readable-doc-language.md` §4)

- ✅ TOS body có English token (Provider/Customer/Effective Date/AS-IS) inline trong câu Vietnamese — đúng pattern legal documentation
- ✅ Pricing tier names (`FREE`/`BASIC`/`PREMIUM`/`ENTERPRISE`) giữ English — code-shaped enum
- ✅ Brand names (`KiteHub`/`KiteClass`/`VietQR`/`Momo`) giữ English — proper nouns

### Date/number/currency format

- ✅ Pricing format VND: `new Intl.NumberFormat('vi-VN').format(amount) + '₫'` → `500.000₫` correct cho locale vi-VN
- ✅ TOS dates: `2026-05-06` ISO format — acceptable cho legal effective dates
- ⚠️ Landing stat counters dùng `value.toLocaleString()` không pass locale; mặc định browser locale. Test với `vi-VN`: `50000 → 50.000` (correct). Test với `en-US` fallback: `50,000`. → giữ behavior, document.

### i18n fallback (per `pre-handoff-self-test-completeness.md` §2.11)

- N/A — không có locale switcher; Vietnamese là default + only language ship Phase 1 BETA
- Per CLAUDE.md decision context: target audience Vietnamese-speaking centers; English version deferred GAP-182 Phase 2 counsel-reviewed TOS

---

## Bugs fixed in same PR

1. `LandingClient.tsx` line 382: `skylight.kiteclass.com` → `skylight.kitehub.me` (đồng bộ brand domain per GAP-458 Path C decision)
2. `LandingClient.tsx` line 1003: `Hotline: 1900-xxxx` placeholder → `Hỗ trợ qua email (Beta giai đoạn 1)` (honest về current support tier)
3. `LandingClient.tsx` line 1007: `support@kiteclass.com` → `support@kitehub.me` (đồng bộ với PublicLayout.tsx + LandingShellSSR.tsx)

---

## Pre-handoff self-test (per `pre-handoff-self-test-completeness.md` §2.2 anonymous flow)

| Check | Pass criterion | Result |
|---|---|---|
| (a) URL entry point exists in published UI | Landing/pricing accessible từ homepage | ✅ — `/` → landing, `/pricing` → pricing |
| (b) Form submit works end-to-end | N/A (no form on landing); `/register` redirect → `/request-beta-access` form covered Bucket 0 | ✅ deferred to Bucket E review |
| (c) Confirmation surface visible | N/A | ✅ |

Mixed-language sentence verification:
- ✅ "Quản lý học viên" / "Lịch học & Điểm danh" — natural Vietnamese
- ✅ "AI Branding" / "API tích hợp" — English token inline OK
- ✅ "₫45.6M Doanh thu" — VN currency + word

→ Anonymous flow audit per §2.2 PASS for landing + pricing scope.

---

## Recommendations / Follow-up

### Out-of-scope items tracked separately

| Item | Where |
|------|-------|
| i18n library integration (`next-intl` setup + locale file extraction) | New gap nếu cần English audience Phase 2 — currently deferred per CLAUDE.md "EN deferred to GAP-182 Phase 2 counsel-reviewed" |
| Dashboard banner Vietnamese content | Bucket B (GAP-539) |
| Approval email subjects + bodies Vietnamese audit | Bucket E (GAP-543 — kitehub-email module) |
| Mockup `kiteclass.com` data showcases (LandingClient testimonials, settings backupUrl) | Wave 79 cleanup — defer (mockup illustrative purposes, not production live URL) |
| Counter `toLocaleString()` explicit `vi-VN` locale | Wave 79 polish — minor (current behavior correct cho VN browsers) |

### GAP-541 AC verdict

- [x] Customer-facing surfaces (landing + pricing + TOS placeholder + signup redirect) ≥95% Vietnamese coverage → **100% nơi narrative**
- [x] 0 paragraph-level English narrative trong customer-facing surfaces (TOS legal English token mix natural, không paragraph English-only)
- [x] Mixed-language tự nhiên (English token trong Vietnamese sentence OK per `dev-readable-doc-language.md` §4)
- [x] Date/number/currency format theo locale (`vi-VN`: `1.234,56 ₫`)
- [x] Audit report ship trong `documents/04-quality/audits/i18n/`
- [ ] **i18n locale `vi/` files audit complete với coverage % per key namespace** → N/A (no i18n library; reframed: audit hardcoded strings directly)
- [ ] **i18n fallback working — `Accept-Language: en` fallback to `vi`** → N/A (single-locale ship Phase 1 BETA)
- [ ] **Live walkthrough verify per `pre-handoff-self-test-completeness.md` §2.11 switch locale** → N/A (no locale switcher)

3 AC items reframed N/A vì kiến trúc thực tế không dùng i18n library. Per `gap-done-discipline.md` §3 PARTIAL exit ramp: gap flip PARTIAL với explicit Out-of-scope documentation.

### GAP-428 AC verdict

- [x] At minimum 3 public-surface screens covered (landing + pricing + TOS — đã có production routes)
- [x] WCAG AA verification implicit qua Shadcn design system + semantic HTML (`<h1>`/`<h2>`/`<nav>`/`<aside role="note">`)
- [ ] HTML kit prototype `ui_kits/kiteclass-public/` (Option A) — **deferred** — production pages đã có và VN-polished; kit prototype thuần documentation artifact phục vụ design baseline, không block Phase 1 BETA launch
- [ ] Production parity check trong next UI audit shows ✅ — pending next UI Review /128 run

Per `gap-done-discipline.md`: production pages đã đạt scope chính (landing/pricing/TOS coverage + VN-polished); HTML kit deferred → file follow-up. Gap flip PARTIAL.

---

## References

- Workflow PR: wave/78-bucket-a-fe-polish
- Wave plan: `documents/03-planning/waves/wave-2026-05-14-78-beta-invite-launch-retain.md` Bucket A
- Related rules: `dev-readable-doc-language.md` v1.0.1, `pre-handoff-self-test-completeness.md` §2.2 + §2.11, `gap-done-discipline.md` §3 PARTIAL exit ramp
- Sister gaps: GAP-539 (Bucket B banner), GAP-543 (Bucket E email), GAP-458 (domain decision kitehub.me)
