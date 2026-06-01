---
title: Landing Page Review Checklist — KiteClass (Production Next.js)
audience: mixed
status: active
created: 2026-06-01
applies_to: kiteclass/kiteclass-frontend/src/app/(public)/page.tsx + bất kỳ marketing/landing route production
weighting: balanced (conversion + trust + feature + chất lượng cân bằng)
related: [quality/ui-review/SKILL.md, quality/ui-review-prototype/SKILL.md, quality/marketing-legal-review, .claude/rules/vn-localization-audit-checklist.md]
---

# Landing Page Review Checklist — KiteClass

Tiêu chuẩn review cho bản **redesign landing KiteClass** (production Next.js `(public)/page.tsx`). Layer lên `ui-review` /128 (chất lượng UI per-screen) + thêm các chiều **đặc thù landing** mà rubric /128 không bắt: value-prop, conversion funnel, trust, SEO, Core Web Vitals.

**Companion (generative):** `landing-page-design-brief.md` — input *hướng đi* cho design-Claude (persona, value-prop, IA, brand, content). Brief = "làm gì + vì sao"; checklist này = "tốt hay chưa". Đưa CẢ HAI cho design-Claude → thiết kế theo brief → tự self-score bằng checklist → iterate ≥85.

**Khi nào dùng:** sau khi redesign landing (trước merge PR) + định kỳ pre-launch. Per `output-review-mandate.md` §3 — landing là output customer-facing, BẮT BUỘC có review standard + process + evidence trước khi ship.

**Mục tiêu (Balanced):** cân bằng conversion (drive beta signup) + trust (uy tín với trung tâm GD VN) + feature comprehension + chất lượng kỹ thuật. 10 chiều × 10 điểm = **/100**.

---

## Cách chấm + ngưỡng pass

| Tổng điểm | Verdict | Hành động |
|---|---|---|
| **≥ 85/100** | ✅ SHIP | Merge; ghi evidence vào PR |
| **70–84** | ⚠️ SHIP-WITH-FIXES | File gap cho mục < pass; merge nếu không có chiều nào < 6 |
| **< 70** | ❌ ITERATE | Redesign vòng nữa trước khi merge |
| **Bất kỳ chiều nào < 6/10** | ❌ BLOCK | Phải fix chiều đó dù tổng ≥ 85 (không bù trừ chiều yếu chí mạng) |

**Hard-gate (fail → block bất kể điểm):** beta disclaimer (D3), WCAG AA contrast (D7), cookie consent Decree 13 (D10), không overclaim/sai sự thật (D10).

---

## Quy trình review (production Next.js)

1. **Build + serve:** `cd kiteclass/kiteclass-frontend && pnpm build && pnpm start` (đo bản production, KHÔNG dev).
2. **Capture:** screenshot desktop (1440) + mobile (390) per `ui-review` SKILL §1. Lưu vào `documents/04-quality/audits/ui/` (KHÔNG /tmp per `feedback_no_tmp_review_artifacts.md`).
3. **Đo định lượng:**
   - Lighthouse (mobile preset) → LCP / CLS / INP / Performance / SEO / a11y scores.
   - `axe` DevTools hoặc `@axe-core/cli` → đếm critical/serious violations.
   - View-source / DevTools → SEO meta, OG, JsonLd, lang.
   - `pnpm build` summary → first-load JS của route `/`.
4. **Chấm 10 chiều** bên dưới, mỗi chiều ghi điểm + evidence (số đo / screenshot / dòng code).
5. **Output:** bảng điểm /100 + before/after (nếu có bản cũ) + danh sách finding → gap per `audit-to-gap-pipeline.md`.

---

## 10 chiều chấm điểm

### D1 — Value Proposition & Messaging (/10)
- [ ] Hero headline trả lời "KiteClass là gì + cho ai" trong < 5 giây (trung tâm/trường học VN)
- [ ] Subheadline nêu lợi ích chính (quản lý học sinh/lớp/điểm danh/học phí 1 chỗ), không jargon kỹ thuật
- [ ] Above-the-fold không cần scroll vẫn hiểu được giá trị + có CTA
- [ ] Thông điệp benefit-framed ("tiết kiệm thời gian điểm danh") không feature-dump thuần
- **Pass:** người lạ xem 5s nói được KiteClass làm gì cho ai. **Đo:** 5-second test (hỏi 1 người ngoài).

### D2 — Conversion & CTA (/10)
- [ ] CTA chính ("Đăng ký Beta" / "Dùng thử") nổi bật, tương phản cao, 1 hành động ưu tiên rõ ràng
- [ ] CTA lặp lại hợp lý (hero + cuối trang) không gây nhiễu
- [ ] Friction thấp: form signup tối thiểu trường; nêu rõ "miễn phí beta / không cần thẻ"
- [ ] Đường dẫn rõ tới beta-signup (`/request-beta-access` hoặc route hiện hành) — không dead-end
- [ ] Secondary CTA (xem tính năng / liên hệ) không cạnh tranh thị giác với primary
- **Pass:** 1 primary CTA dominant + funnel signup ≤ 2 bước. **Đo:** click-through từng CTA → đến đúng đích.

### D3 — Trust & Social Proof (/10) 🔴 *beta disclaimer = hard-gate*
- [ ] **Beta disclaimer hiển thị** (per GAP-539 — "v1 pending counsel review" / "đang trong giai đoạn beta") — **HARD GATE**
- [ ] Social proof: testimonial / logo trường / số liệu (nếu chưa có thật → KHÔNG fabricate; dùng "đối tác beta" trung thực)
- [ ] Tín hiệu an toàn dữ liệu: nhắc PDPL / bảo mật / tuân thủ Nghị định 13
- [ ] Có thông tin liên hệ + about (đơn vị phát hành) — minh bạch, tăng uy tín
- [ ] Link Điều khoản / Chính sách bảo mật ở footer
- **Pass:** beta disclaimer present + ≥2 trust element thật. **Đo:** grep `(public)/page.tsx` + footer.

### D4 — Feature Comprehension (/10)
- [ ] Liệt kê tính năng cốt lõi (học sinh / lớp / điểm danh / điểm số / học phí) rõ ràng, có icon/visual
- [ ] Mỗi tính năng nêu lợi ích (vì sao quan trọng) không chỉ tên
- [ ] Phù hợp persona mua (chủ trung tâm, quản lý) — ngôn ngữ nghiệp vụ GD VN
- [ ] Không over-promise tính năng chưa có (Phase 1 scope) — trung thực
- **Pass:** người mua hiểu KiteClass giải quyết bài toán gì. **Đo:** đối chiếu với feature thật Phase 1.

### D5 — Visual & UI Quality (/10) → ties `ui-review` /128
- [ ] Hierarchy thị giác rõ (heading scale, whitespace, nhóm nội dung)
- [ ] Brand consistency: màu/logo/typography khớp design system KiteClass
- [ ] Spacing/alignment nhất quán; không lệch grid
- [ ] Hình ảnh/illustration chất lượng, không vỡ/placeholder
- [ ] Điểm `ui-review` /128 của landing ≥ 100 (A-) cho riêng screen này
- **Pass:** ui-review screen-score ≥ 100/128. **Đo:** chạy `ui-review` SKILL trên landing.

### D6 — Responsive & Mobile-first (/10) *VN users đa phần mobile*
- [ ] Mobile (390px) layout không vỡ, không horizontal scroll
- [ ] Touch target ≥ 44×44px (CTA, nav, form)
- [ ] Font-size mobile đọc được (body ≥ 16px tránh zoom)
- [ ] Hero + CTA vẫn above-the-fold trên mobile
- [ ] Test ≥ 3 breakpoint (390 / 768 / 1440)
- **Pass:** không vỡ + CTA reachable mọi breakpoint. **Đo:** DevTools responsive + screenshot mobile.

### D7 — Accessibility WCAG AA (/10) 🔴 *contrast = hard-gate*
- [ ] **Contrast text/nền ≥ 4.5:1** (≥ 3:1 cho text lớn) — **HARD GATE**
- [ ] Keyboard navigable (Tab qua mọi CTA/link/form) + focus state nhìn thấy
- [ ] Mọi `<img>` có `alt` ý nghĩa; icon-only button có `aria-label`
- [ ] Semantic landmarks (`<header>/<main>/<footer>`, 1 `<h1>`, heading tuần tự)
- [ ] axe: 0 critical + 0 serious violation
- **Pass:** axe 0 critical/serious + contrast pass. **Đo:** `axe` DevTools + Lighthouse a11y ≥ 95.

### D8 — Performance / Core Web Vitals (/10)
- [ ] **LCP < 2.5s** (mobile, throttled — VN 3G/4G phổ biến)
- [ ] **CLS < 0.1** (không layout shift; ảnh/hero có kích thước cố định)
- [ ] **INP < 200ms**
- [ ] First-load JS route `/` **< 150 KB** (marketing cap — per build summary hiện hành landing ~110KB)
- [ ] Ảnh dùng `next/image` (avif/webp), lazy-load below-fold; font tối ưu (next/font)
- **Pass:** LCP<2.5 + CLS<0.1 + JS<150KB. **Đo:** Lighthouse mobile + `pnpm build` summary.

### D9 — SEO & Shareability (/10)
- [ ] `<title>` + meta description tiếng Việt, mô tả đúng + có keyword (phần mềm quản lý trung tâm/trường)
- [ ] Open Graph + Twitter card (title/description/image) → share Zalo/FB hiển thị đẹp
- [ ] JsonLd structured data (Organization / SoftwareApplication)
- [ ] `<html lang="vi">`, canonical URL, có trong sitemap + robots cho phép index
- [ ] 1 `<h1>` chứa value prop; heading có cấu trúc
- **Pass:** OG present + lang=vi + title/desc đúng. **Đo:** view-source + Lighthouse SEO ≥ 95 + OG debugger.

### D10 — VN Localization & Content (/10) 🔴 *Decree 13 + no-overclaim = hard-gate*
- [ ] Toàn bộ copy tiếng Việt tự nhiên (per `vn-localization-audit-checklist.md` §2) — không English narrative
- [ ] Số/tiền định dạng VN: `1.500.000đ`, ngày `dd/MM/yyyy` (per checklist §1)
- [ ] Sample data (nếu hiển thị) là tên VN (`Trần Thị Hương`, `Lớp 5A1`) không `John Doe` (§3)
- [ ] Văn hóa VN: nhắc Zalo/điện thoại hợp lý (§4); không áp US convention
- [ ] **Không Lorem Ipsum / placeholder**; grammar + tone chuẩn
- [ ] **Cookie consent Nghị định 13** present nếu set cookie (per GAP-585) — **HARD GATE**
- [ ] **Không overclaim** (không khẳng định tính năng/đối tác chưa có) — **HARD GATE**
- **Pass:** copy VN sạch + Decree 13 consent + 0 overclaim. **Đo:** đọc toàn trang + grep English/Lorem.

---

## Bảng tổng điểm (template)

```
Landing KiteClass review — <date> — <commit/PR>
D1 Value prop        : __/10   evidence: ...
D2 Conversion/CTA    : __/10
D3 Trust (disclaimer): __/10   [hard-gate: disclaimer ✓/✗]
D4 Feature           : __/10
D5 Visual (ui /128)  : __/10   (screen ___/128)
D6 Responsive        : __/10
D7 A11y (contrast)   : __/10   [hard-gate: contrast ✓/✗; axe crit=__]
D8 Performance       : __/10   (LCP __s, CLS __, JS __KB)
D9 SEO               : __/10   (Lighthouse SEO __)
D10 VN/Content       : __/10   [hard-gate: Decree13 ✓/✗, overclaim ✓/✗]
─────────────────────────────
TOTAL                : __/100  → SHIP / FIX / ITERATE
Hard-gates           : disclaimer / contrast / Decree13 / no-overclaim — ALL ✓?
```

## Evidence (per `output-review-mandate.md` §3)
Lưu vào `documents/04-quality/audits/ui/<YYYY-MM-DD>-kiteclass-landing-review.md`: bảng điểm + screenshot desktop/mobile + Lighthouse JSON/score + axe output + danh sách finding → gap. Before/after nếu có bản cũ.

## Governance hook
Khi checklist này ổn định: thêm 1 dòng vào `output-review-mandate.md` §3 matrix ("Landing page (customer-facing)" → standard = file này) per `incident-to-rule-pipeline.md` Stage 3. Hiện tại = reference doc dưới `ui-review` skill, dùng được ngay.
