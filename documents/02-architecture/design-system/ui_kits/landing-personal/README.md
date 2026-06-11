# UI Kit — landing-personal (KiteClass per-tenant landing · GIÁO VIÊN ĐỘC LẬP)

Kit design **chính thức** cho trang landing per-tenant của **giáo viên độc lập** (persona **P1 Solo Teacher**). Trước đây template production PERSONAL chỉ là engineering-defined (không có design spec) → kit này là design source of truth, hợp nhất spec **carousel đa-banner (GAP-826)** vào hero **2 cột copy-trái + banner-phải đóng khung (GAP-1210)**.

Mở `index.html` để xem landing hoàn chỉnh. Demo tenant: **Lớp Toán cô Nguyễn Thị Hà** (xanh dương `#2563EB`, dữ liệu thật từ demo-trio).

**Phân biệt với các kit landing khác:**
- `marketing-site/` = landing của CHÍNH NỀN TẢNG KiteClass (mời chủ trung tâm P2 vào Beta).
- `kitehub-story-v2/` = marketing storytelling cho P2 chủ trung tâm.
- **`landing-personal/` (kit này)** = landing PER-TENANT do 1 GV độc lập sở hữu — audience là **phụ huynh + học viên**, voice là **cá nhân GV** (không phải trung tâm).

## Persona & audience
- **Chủ tenant:** P1 Solo Teacher (gia sư tự do, 5–50 học viên, 0 nhân viên) — `dossier/01-personas.md` §P1.
- **Audience đọc trang:** phụ huynh đang tìm lớp cho con + học viên.
- **Voice (GAP-1208):** xưng "tôi/cô", giọng cá nhân ấm áp — section dùng nhãn GV độc lập: **"Về giáo viên"** (không "Giới thiệu trung tâm"), **"Giáo viên đồng hành"** (không "Đội ngũ giáo viên"), **"Học phí"** (không "Bảng giá"), **"Phụ huynh & học viên nói gì"** (không "Đánh giá").

## Screens / sections — khớp 1:1 PERSONAL_TEMPLATE production
Thứ tự bám `kiteclass/kiteclass-frontend/src/lib/template/configs.ts` `PERSONAL_TEMPLATE`:

| # | Section | Nhãn (PERSONAL) | Trong kit |
|---|---------|-----------------|-----------|
| 0 | hero | — | Copy trái + **carousel banner phải đóng khung** |
| 1 | stats | Chỉ số nổi bật | 4 chỉ số (12 năm · 200+ HV · 6–8/lớp · 98%) |
| 2 | problemSolution | Vấn đề & Giải pháp | 3 pain → solution (mất gốc / không nắm tiến độ / học phí mập mờ) |
| 3 | about | **Về giáo viên** | Profile cô Hà (giọng "tôi") |
| 4 | teachers | **Giáo viên đồng hành** | Profile đơn (1 GV — đặc thù GV độc lập) |
| 5 | howItWorks | Cách hoạt động | 3 bước (đăng ký → kiểm tra → học) |
| 6 | timeline | Lộ trình học tập | 3 giai đoạn theo tuần |
| 7 | certificates | **Chương trình giảng dạy** | 2 khóa Toán lớp 4 (800k) / lớp 5 (900k) |
| 8 | trustStrip | Tin cậy & minh bạch | 4 trust badges |
| 9 | pricing | **Học phí** | 2 gói (lớp 5 featured) |
| 10 | testimonials | **Phụ huynh & học viên nói gì** | 3 lời chứng phụ huynh |
| 11 | faq | Câu hỏi thường gặp | 4 Q&A |
| — | finalCTA + footer | — | CTA học thử + liên hệ Zalo |

## Hero carousel đa-banner (GAP-826) — spec
- **Layout (GAP-1210):** hero 2 cột — copy + CTA bên TRÁI; cột PHẢI là **khung banner** (rounded card `border-radius:20px`, ring trắng, glow CTA) chứa carousel; banner không bao giờ nằm dưới chữ.
- **States:**
  - `default` — auto-rotate 5s, vòng lặp `(active+1)%n`.
  - `paused` — `mouseenter` dừng timer, `mouseleave` chạy lại.
  - `single` — JS phát hiện `n<=1` → `.carousel.single` ẩn dots + arrows, không auto-rotate (degrade tĩnh).
  - `reduced-motion` — `prefers-reduced-motion:reduce` → không auto-rotate (chỉ điều khiển tay), transition tắt.
- **Controls:** dots (tablist, active = pill), prev/next arrows (ẩn trên mobile ≤860px), keyboard ← → khi focus trong carousel.
- **Wire nguồn:** pattern `.hero-slide/.hero-dots/.hero-arrow` từ `marketing-site/landing.css` + `carousel-demo.html` (GAP-826 prototype), tái hiện self-contained bằng vanilla JS (không phụ thuộc React/CDN).
- **Slides demo:** 3 banner cô Hà (Khai giảng lớp 5 / Cam kết tiến bộ / Học thử miễn phí) — placeholder gradient theme + emoji chủ đề (kit self-contained, không cần asset ngoài). Production = `heroImages[]` per-tenant.

## Theme switcher (GAP-274) — runtime
3 GV demo, đổi màu **toàn trang thật** (set class `.theme-*` trên `<body>` + cập nhật brand identity): **Cô Hà** xanh dương · **Thầy Nhì** xanh lá · **Cô Khánh** cam. Per `design-source-implementation-parity.md` §3.2 — affordance click có hiệu ứng runtime thật (không inert). Parity với `marketing-site` ThemeSwitcher.

## VN localization
- 100% tiếng Việt; VND `800.000đ`/`900.000đ`; SĐT `0912 345 678`.
- VN sample data: `Nguyễn Thị Hà`, `Trần Thị Hồng`, `Nguyễn Văn An`, `bé Minh lớp 5` — không `John Doe`/`$`.
- VN culture: Zalo là kênh báo cáo + liên hệ chính; "gia sư"/"học thử"/"phụ huynh"; ĐH Sư phạm Hà Nội.

## WCAG AA self-measurement
Đo trong comment đầu `index.html`. Cặp text-bearing chính ≥4.5:1 (hero trắng/gradient 6.1–8.6:1; CTA trắng/orange-700 4.8:1; body 14:1; muted 4.7:1). CTA dùng `--cta-strong` (orange-700 `#C2410C`) cho nút có chữ trắng — `--cta` (`#F97316`) chỉ dùng cho fill trang trí. Skip-link + semantic landmarks + `aria-*` trên carousel.

## Responsive
- ≤860px: hero stack (banner lên trên), arrows ẩn (chỉ dots + swipe-equivalent qua dots).
- ≤390px: brand 1 dòng (ẩn tagline phụ), hero h1 30px, container padding 16px.

## Self-score — 113/128 (8 dimension × /16)

| Dim | Điểm | Ghi chú |
|-----|:----:|---------|
| D1 Visual hierarchy | 15 | Hero 2 cột rõ, eyebrow→h1→sub→CTA→trust thang bậc tốt |
| D2 Persona fit (P1) | 15 | Voice cá nhân GV, profile đơn, audience phụ huynh xuyên suốt |
| D3 Carousel / interaction | 14 | 4 states + keyboard + degrade tĩnh; chưa có swipe-touch gesture |
| D4 Theme system (GAP-274) | 14 | Runtime swap 3 GV thật; production cần token đầy đủ hơn |
| D5 Content / VN localization | 15 | VND + Zalo + sample data thật, 0 jargon, voice nhất quán |
| D6 Responsive | 14 | 3 breakpoint; mobile arrows ẩn (dots còn) — chưa có touch-swipe |
| D7 Accessibility (WCAG AA) | 14 | Contrast đo + landmarks + aria; carousel chưa có aria-live announce |
| D8 Section completeness vs template | 12 | Khớp 12 section PERSONAL; programs/timeline ở mức demo tĩnh |
| **Tổng** | **113/128** | ≥105 target ✓ (floor mỗi dim ≥95/128 quy đổi ✓) |

Hard gates: ✓ VN-only data · ✓ WCAG AA đo · ✓ persona khai báo (HTML comment) · ✓ no hardcoded hex ngoài token block · ✓ carousel degrade tĩnh.

## 4-layer design coverage (per `.claude/rules/design-layer-coverage.md` §2.2)
| Layer | Artifact pointer |
|-------|------------------|
| **要件定義** (requirements / persona / use-case) | `dossier/01-personas.md` §P1 Solo Teacher · `dossier/05-business-flows.md` (prospect → tenant landing) |
| **基本設計** (basic / screen design) | **kit này** `index.html` + section map ở trên (khớp PERSONAL_TEMPLATE) |
| **詳細設計** (detail / state / ADR) | Carousel states §"Hero carousel" (default/paused/single/reduced-motion) · GAP-826 / GAP-1210 / GAP-274 / GAP-1208 |
| **コンポーネント設計** (component design) | Carousel + ThemeSwitcher (parity `marketing-site/`) · primitives pattern `_shared/colors_and_type.css` theme vars · `components/G11-theme` |

## Cross-link
- GAP-826 (hero carousel đa-banner — production wire) · GAP-1210 (hero copy-trái banner-phải) · GAP-1208 (voice GV độc lập) · GAP-274 (per-tenant theme).
- Production mapping: `kiteclass/kiteclass-frontend/src/components/sections/HeroSection.tsx` + `src/lib/template/configs.ts` `PERSONAL_TEMPLATE` + `(public)/page.tsx`.
- Carousel nguồn: `marketing-site/carousel-demo.html` + `landing.css` `.hero-slide`.
- Dossier: `dossier/01-personas.md` · `dossier/02-vietnamese-ux-musts.md` · `dossier/06-quality-bar.md`.

> Design artifact (self-contained HTML+CSS+vanilla JS — không phụ thuộc `_shared/` hay CDN). Production port = Next.js + `next/image` + `next/font`, re-run Lighthouse/axe trước merge per review checklist.
