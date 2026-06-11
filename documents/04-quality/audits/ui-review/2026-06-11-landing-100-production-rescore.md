---
title: Re-score landing production per-tenant — Wave landing-100 closure gate (G3 parity)
audience: mixed
created: 2026-06-11
audit-type: ui-review
scope: kiteclass-frontend (public) per-tenant landing — demo-trio + secondary pages
method: ui-review /128 (5 dimension) → quy đổi /100
verdict: 81/100 (configured-tenant bar) · 72/100 (lowest tenant sky) — CHƯA đạt gate ≥90
baseline: ~64/100 (wave plan §1, tenant đã cấu hình)
target: ≥90/100
---

# Re-score landing production — Wave landing-100 (G3 parity gate)

## 1. Phương pháp (cite)

- **Thang chấm:** `quality/ui-review/SKILL.md` §4 — per-screen **/128**, 5 dimension: Technical /20, Design Heuristics /40 (Nielsen ×10, /4 mỗi), Visual /28, User Friendliness /20, WCAG /20. **Quy đổi /100** = `score/128*100`.
- **Lý do quy đổi:** baseline wave plan ghi `~64/100` (config tenant) ở thang /100 nhưng KHÔNG có artifact rubric gốc (`grep "64/100\|landing rubric"` chỉ trả về wave plan §1 — số ước lượng pre-wave, không có per-dimension breakdown). Để re-score apples-to-apples mà vẫn có rigor, chấm /128 (rubric chính thức của project) rồi quy /100. So-với-baseline phải hiểu baseline là số ước lượng thô.
- **Đối chiếu design source** per `design-source-implementation-parity.md` §3: `ui_kits/landing-personal/index.html` (kit 113/128 ≈ **88/100**) + `ui_kits/kiteclass-public/screens/*` (110-115/128).
- **Evidence:** Playwright headless (chromium-1217), full-page + scroll-loop (`page.evaluate` 400px steps → trigger ScrollReveal + crossfade carousel) → 7 screenshot: 3 tenant landing (co-ha-toan blue/free, thay-nhi-hoa green/paid, sky-education orange/walkthrough) + catalog/about/contact (co-ha-toan) + mobile 390px landing. DOM cross-check `curl` cho affordance ẩn (FloatingZalo / FAQ / Testimonials / stat labels).
- **Stack:** kiteclass-frontend `:3000` qua nip.io, kiteclass-core + kite-gateway + kite-postgres healthy (production-parity, bản mới nhất branch `wave/landing-100-g2-walk-2026-06-11`).

## 2. Per-dimension score (representative = co-ha-toan landing, tenant đã cấu hình đầy đủ)

| Dimension | /max | Điểm | Cite evidence |
|---|---|---|---|
| Technical | /20 | **16.5** | Theme per-tenant áp đúng (blue/green/orange) SSR-inline không FOUC (Bucket D); responsive mobile reflow tốt (hero HTML overlay, sections stack — screenshot mobile); anti-fabrication clean (no fake data); residual: cookie-consent z-index overlap section mid-page (screenshot landing/about) |
| Design Heuristics | /40 | **30** | Match-real-world VN edu copy 4; consistency cross-tenant/section 4; error-prevention form hints (contact `SĐT * — VD: 0912345678`) 4; recognition section rõ 3; **help/docs FAQ section render rỗng (no data) → 2**; còn lại ~3 |
| Visual Aesthetics | /28 | **23.5** | Palette gradient theme đẹp 4; typography VN crisp + hierarchy hero mạnh 4; spacing thoáng 4; polish residual: catalog class-card thiếu cover image (placeholder book icon) + FAQ/Testimonials section rỗng để lại whitespace 3; images hero AI-scene tốt 3 |
| User Friendliness | /20 | **17.5** | First impression hero mạnh 3.5; CTA "Học thử miễn phí" nổi bật + FloatingZalo (DOM confirm 10× "Zalo") 3.5; empty-state trung thực (sky sparse nhưng không bịa) 3; nav/loading/mobile ~3 |
| WCAG | /20 | **16** | Contrast guard WCAG AA clamp lightness (Bucket D) 3.5; form label đầy đủ (contact) 3; touch target lớn 3; headings có cấu trúc 3; keyboard/skip-link chưa verify 2.5 |
| **Tổng** | **/128** | **103.5** | → **≈ 81/100** |

## 3. Per-tenant / per-page verdict

| Trang / tenant | /128 | /100 | Ghi chú |
|---|---|---|---|
| **co-ha-toan** landing (blue, free, seeded) | 103.5 | **81** | Đại diện "config tenant" — apples-to-apples baseline 64 |
| **thay-nhi-hoa** landing (green, paid, richest) | ~105 | **82** | 3 pricing card thật (1.2/1.5/1.6M) → pricing section đầy đủ nhất |
| **sky-education** landing (orange, walkthrough §4.1) | ~92 | **72** | Sparse: thiếu teacher card/learning-path/pricing/stats (empty-state ẩn) — trung thực nhưng mỏng → **lowest-tenant bar** |
| co-ha-toan /catalog | ~98 | 77 | Filter chips + search + recommend block tốt; class-card thiếu imagery |
| co-ha-toan /about | ~104 | 81 | Stats CÓ label ("2 Lớp đang mở / 12 học sinh / 85% chuyển tiếp") + chứng chỉ + CTA học thử |
| co-ha-toan /contact | ~106 | 83 | Lead form + validation hint + Zalo CTA + PDPL consent (GAP-828/596) — trang mạnh nhất |
| mobile 390px landing | ~101 | 79 | Hero HTML overlay reflow tốt (xác nhận Bucket C — không bake text PNG); stats wrap hơi awkward |

**Verdict chính thức (gate G3):** **81/100** cho config-tenant bar (vs baseline ~64, vs target ≥90) · **72/100** lowest-tenant (sky). **CHƯA đạt ≥90.**

## 4. So với kit (parity) — delta classification

Kit `landing-personal` = 113/128 ≈ **88/100**. Production config-tenant 81/100 → **delta −7**. Phân loại nguồn:

| # | Delta vs kit | Điểm | Nguồn | Chi tiết |
|---|---|---|---|---|
| 1 | **FAQ + Testimonials section rỗng** | ~−4 | **DATA** (không phải code) | Component ĐÃ port + wire trong `TemplateRenderer.tsx` (`FaqSection`, `TestimonialsSection` đều có + reference) — nhưng KHÔNG có seed data → render empty/ẩn. Kit hardcode demo content nên luôn đầy. Đây là parity-by-design: anti-fabrication (GAP-958) cấm bịa, nhưng kit "đầy" vì có demo bịa. |
| 2 | **Visual polish residual** | ~−3 | **CODE** | (a) catalog class-card thiếu cover image (placeholder icon); (b) cookie-consent banner z-index/position overlap section mid-page trên trang dài (landing/about screenshot) |
| 3 | **sky-education sparse tenant** | −2 (lowest bar) | **DATA + empty-state design** | Tenant walkthrough §4.1 chưa seed lớp/GV/pricing → nhiều section ẩn → landing mỏng vs kit luôn-đầy |
| — | Theme switcher 4-option | 0 | **documented-drop** | Kit-preview affordance (`design-source-implementation-parity.md` §3 row 4) — production strip đúng, KHÔNG penalize |

**Kết luận parity:** code ở mức parity với kit (mọi section ported + wired); delta lớn nhất KHÔNG phải code-missing mà là **data-missing** (section ẩn khi rỗng) + polish nhỏ.

## 5. Path-to-90 (việc cần làm để đạt gate ≥90)

| # | Việc | +điểm ước tính | Effort | Gap cover? |
|---|---|---|---|---|
| 1 | Seed **FAQ + testimonials THẬT** cho demo-trio (Hà/Nhì — nội dung thật theo thesis, KHÔNG bịa) → lấp section rỗng | +3-4 | M | ❌ chưa có gap — cần file mới (extend Bucket G seed) hoặc gắn GAP-1083 (BE fields) |
| 2 | **Empty-section collapse** — khi FAQ/Testimonials rỗng, ẩn hoàn toàn wrapper (bỏ whitespace thừa) | +1-2 | S | 🟡 một phần GAP-958 (anti-fab) — cần verify residual empty-wrapper |
| 3 | **Catalog class-card cover image** (ảnh/gradient per lớp thay placeholder icon) | +1-2 | M | 🟡 GAP-810 (hero assets) — mở rộng sang catalog hoặc gap mới |
| 4 | **Cookie-consent z-index/position** — fixed bottom đúng, không overlap content | +1 | S | ❌ chưa có gap — cần file mới (P2 polish) |
| 5 | **sky-education** — seed tối thiểu (lớp + GV) HOẶC empty-state design giàu hơn để nâng lowest-tenant bar | +variable (lowest bar 72→~82) | M | 🟡 Bucket G scope (demo-trio) — Khánh §4.1 chưa seed đầy |

Tổng path-to-90: items 1-4 đưa config-tenant 81 → ~88-90; item 5 cần thiết để "**mọi** tenant ≥90" (đúng mục tiêu wave "đẹp 100% mọi tenant"). Nếu gate chấp nhận config-tenant bar → cần item 1+2+3+4 (~+6-9). Nếu gate là lowest-tenant → thêm item 5.

## 6. Ghi chú cho closure

- Gate G3 wave plan §5 = "rubric ≥90/100" → **hiện 81 (config) / 72 (lowest) — FAIL gate**. Wave re-open fix-pack 2026-06-11 (§8 Log) đúng hướng nhưng chưa đủ cho ≥90.
- Discovery khi re-score (per `discovery-to-gap-inline-filing.md` — agent KHÔNG tự file, báo coordinator): (a) FAQ/testimonials data-seed chưa có gap; (b) cookie z-index overlap chưa có gap; (c) catalog cover image. Coordinator quyết file.
- KHÔNG commit / KHÔNG sửa code (per nhiệm vụ audit-only).

## Coordinator correction (2026-06-11, post-audit)

- Finding "cookie-consent banner z-index overlap content mid-page": verified `ConsentBanner.tsx:180` = `fixed inset-x-0 bottom-0` — overlap chỉ xuất hiện trong **full-page screenshot stitching** (element fixed in tại scroll offset), browser thật banner nằm đáy viewport. KHÔNG phải bug → không trừ điểm → score config-tenant điều chỉnh ~82/100. Các delta còn lại giữ nguyên (GAP-1224/1225/1226 đang fix path-to-90).
