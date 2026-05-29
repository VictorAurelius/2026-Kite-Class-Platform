---
title: Claude Design System — setup form cho KiteClass landing (wave-thesis-4)
audience: dev
last-updated: 2026-05-30
---

# Claude Design System — Setup form KiteClass

Nội dung điền form "Set up your design system" trên claude.ai để Claude Design thiết kế lại
landing template KiteClass cho tất cả persona + nhiều banner. Tham chiếu khi tích hợp output.

Tham chiếu design: [mshoajunior.edu.vn](https://mshoajunior.edu.vn/) · [anhngumshoa.com](https://www.anhngumshoa.com/) · `banner-prompts-and-design-spec.md`.

---

## 1. Company name and blurb

> KiteClass — nền tảng quản lý giáo dục đa tenant (multi-tenant) cho giáo viên dạy thêm độc lập và trung tâm tại Việt Nam. Mỗi giáo viên/trung tâm có landing page công khai riêng (subdomain + theme + nội dung tự cấu hình), cùng hệ thống quản lý học viên, lớp học, điểm danh, học phí. Trang landing phục vụ nhiều nhóm đối tượng (persona): giáo viên độc lập, chủ trung tâm, quản lý, phụ huynh, học viên.

## 2. Link code on GitHub

`VictorAurelius/2026-Kite-Class-Platform` — repo mono-repo lớn (BE+FE). Hướng Claude focus FE qua field #3.

## 3. Link code from computer (khuyến nghị — chính xác hơn)

Folder: `kiteclass/kiteclass-frontend/src`. Trọng tâm:
- `components/sections/` — 15 section (Hero/Stats/Pricing/Testimonials/Timeline/FAQ/About/Certificates/CTA/Teachers/Contact/Enrollment/Features/TemplateRenderer)
- `app/(public)/page.tsx` + `layout.tsx` — landing + header/footer
- `app/globals.css` — theme tokens · `lib/template/configs.ts` — 2 template

## 4. Upload .fig

Không có (chưa dùng Figma).

## 5. Fonts, logos, assets

- Font: **Be Vietnam Pro** (Google Fonts, subsets latin+vietnamese)
- Banner mẫu: `public/demo-banners/{co-khanh-phapluat,co-ha-toan,thay-nhi-hoa}.png`
- Reference: screenshot mshoajunior + anhngumshoa

## 6. Notes (field quan trọng nhất)

```
HỆ THỐNG THIẾT KẾ KITECLASS LANDING

Mục tiêu: thiết kế lại landing template chuyên nghiệp như các trung tâm giáo dục hàng đầu VN
(tham chiếu: mshoajunior.edu.vn, anhngumshoa.com). Hiện template còn đơn giản, cần nâng cấp.

THEME (per-tenant, CSS RGB variables — đổi màu theo từng giáo viên):
- --theme-primary (mặc định #3B82F6 xanh dương)
- --theme-secondary (#8B5CF6)
- --theme-accent (#F59E0B vàng)
- --theme-cta (#F97316 CAM — nút kêu gọi hành động, tương phản nền)
- radius 8px (0.5rem), font Be Vietnam Pro (hỗ trợ dấu tiếng Việt)
Ví dụ theme thật: Cô Khánh navy+gold (#1E3A5F/#C9A227), Cô Hà blue (#2563EB), Thầy Nhì green (#16A34A).

2 TEMPLATE PER PERSONA:
- PERSONAL (giáo viên độc lập): hero, stats, "Về tôi", chương trình, lộ trình, bảng giá, đánh giá, FAQ, liên hệ.
  KHÔNG có "đội ngũ giáo viên"/gallery/tuyển sinh.
- ORGANIZATION (trung tâm): thêm đội ngũ GV, gallery, tin tức, tuyển sinh, phụ huynh.
Cần thiết kế template cho TẤT CẢ persona (P1 giáo viên độc lập, P2 chủ trung tâm, P3 quản lý,
P4 phụ huynh, học viên, khách ẩn danh).

YÊU CẦU TRỌNG TÂM — NHIỀU BANNER (không chỉ 1):
Thay vì 1 hero banner, dùng NHIỀU banner/visual xuyên suốt trang: hero carousel (xoay nhiều ảnh),
section dividers, banner urgency khai giảng, banner CTA giữa trang, banner cam kết, stat-counter band,
course-card banner. Mỗi banner = 3 lớp: (1) text slogan, (2) ảnh người/học sinh,
(3) icon chủ đề theo môn (⚖️ luật, ➗ toán, ⚗️ hóa, 📚 chung).

PHONG CÁCH:
- Hero gradient full-width, slogan UPPERCASE lớn (Montserrat/Be Vietnam Pro bold), CTA cam nổi bật.
- Counter động số to (năm KN / số HS / % đạt mục tiêu). KHÔNG bịa số quy mô — GV độc lập dùng số
  thật khiêm tốn, trung tâm dùng số thật lớn hơn.
- Card bo góc 8-12px, shadow nhẹ, ảnh GV tròn viền theme.
- Testimonial carousel (mobile scroll-snap), trust badge cam.
- 100% tiếng Việt, định dạng tiền VND (1.500.000đ), CTA "Học thử miễn phí".
- Mobile-first responsive.

GIỌNG ĐIỆU: chuyên nghiệp, tin cậy, ấm áp; tránh khoa trương/bịa số.
```

---

## Tích hợp output Claude Design (khi có)

Khi Claude Design trả component/CSS/design:
1. Map vào `kiteclass-frontend/src/components/sections/*` (giữ tên section + props data-driven).
2. Theme: dùng CSS vars `--theme-primary/secondary/accent/cta` (KHÔNG hardcode màu — per-tenant).
3. Giữ data-driven (slots + fallback) + `landing_pages` JSONB fields (teachers/programs/pricing/...).
4. Nhiều banner: nếu Claude Design đề xuất carousel/multi-banner → mở rộng `landing_pages` schema (thêm cột `banners JSONB` array) + `compose-teacher-banner.mjs` gen nhiều ảnh per tenant.
5. Build verify per `fe-build-local-verify.md` (`pnpm --filter kiteclass-frontend build`) trước commit.
6. Per-tenant template_type (`personal`/`organization`) đã có (V77) — Claude Design cần tôn trọng 2 template.

Cross-ref: `banner-prompts-and-design-spec.md` (banner AI prompts + design spec), memory `feedback_thesis_banner_html_compose` (banner 3 lớp).
