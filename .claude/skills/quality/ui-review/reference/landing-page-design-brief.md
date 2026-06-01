---
title: Landing Page Design Brief — KiteClass (generative direction)
audience: mixed
status: active
created: 2026-06-01
applies_to: bản redesign landing KiteClass (production Next.js `(public)/page.tsx`)
pairs_with: landing-page-review-checklist.md (brief = direction "làm gì + vì sao"; checklist = thước đo "tốt hay chưa")
goal: balanced (conversion beta-signup + trust + feature comprehension)
---

# Landing Page Design Brief — KiteClass

**Đây là input *generative* (hướng đi sáng tạo) cho design-Claude.** Cặp với `landing-page-review-checklist.md` (input *evaluative*). Quy trình: design-Claude đọc CẢ HAI → thiết kế theo brief → tự self-score bằng checklist → iterate đến ≥85/100 + pass 4 hard-gate trước khi trả.

> ⚠️ **Đừng "teaching to the test".** Brief này cho hướng + cá tính; checklist chặn sàn chất lượng. Mục tiêu là bản landing *vừa khác biệt vừa pass gate*, không phải bản generic đủ điểm.

---

## 1. Bối cảnh + mục tiêu

- **Sản phẩm:** KiteClass — nền tảng quản lý trung tâm/trường học đa-tenant (học sinh · khóa học · lớp · điểm danh · điểm số · học phí). Thị trường: giáo dục VN (trung tâm Anh ngữ, dạy thêm, trường tư).
- **Giai đoạn:** Phase 1 BETA (soft launch). Landing phục vụ **mời beta**.
- **Mục tiêu balanced:** (1) drive đăng ký beta, (2) xây niềm tin với chủ trung tâm VN, (3) giúp họ hiểu KiteClass làm được gì — cân bằng, không hi sinh mảng nào.
- **Success metric:** tỉ lệ khách truy cập → submit form beta; thời gian hiểu value-prop < 5s; pass checklist ≥85.

## 2. Personas (xếp theo ưu tiên mua)

### 🥇 P2 — Chủ trung tâm (primary buyer, quyết định mua)
- Chủ trung tâm Anh ngữ / dạy thêm / trường tư nhỏ, 35–50 tuổi, quản lý 50–300 học sinh.
- **Hiện trạng:** Excel + Zalo + sổ giấy. Điểm danh thủ công, tính học phí dễ sai, phụ huynh nhắn Zalo hỏi điểm/điểm danh liên tục, khó theo dõi giáo viên dạy lớp nào.
- **Đau cốt lõi:** mất thời gian vận hành, sai sót học phí (mất tiền/mất uy tín), thiếu chuyên nghiệp trước phụ huynh.
- **Điều thuyết phục họ:** "tiết kiệm X giờ/tuần", "không còn tính nhầm học phí", "phụ huynh tự xem điểm danh/điểm — bớt nhắn tin", chuyên nghiệp hóa hình ảnh trung tâm. **Không** thuyết phục bằng jargon kỹ thuật.

### 🥈 P1 — Giáo viên tự do / dạy thêm (secondary)
- Ít học sinh, tự quản. Đau: theo dõi điểm danh + thu học phí từng buổi.
- **Thuyết phục:** đơn giản, dùng ngay, **miễn phí trong beta** (giảm rào cản).

### 🥉 P3 — Quản lý/nhân viên vận hành (influencer)
- Người dùng hằng ngày. Quan tâm: dễ dùng, ít thao tác.

### 👤 Phụ huynh (không mua, nhưng là *trust signal*)
- Muốn minh bạch điểm danh/điểm/học phí của con → nhắc được "phụ huynh theo dõi minh bạch" làm tăng sức hút với P2.

## 3. Value proposition + messaging

- **Một câu lõi (định hướng, design-Claude viết lại cho hay):** *"Quản lý trung tâm trọn vẹn trên một nền tảng — điểm danh, học phí, điểm số, phụ huynh, tất cả tự động."*
- **Hero headline:** nói lợi ích cho chủ trung tâm VN trong < 8 từ; tránh "SaaS/platform/giải pháp số hóa" sáo rỗng.
- **Subhead:** cụ thể hóa (vd "Thay Excel + Zalo + sổ giấy bằng một chỗ duy nhất").
- **Tone:** tin cậy-chuyên nghiệp NHƯNG ấm-gần gũi VN (không lạnh-corporate kiểu enterprise US). Xưng hô lịch sự ("Anh/Chị"), tiếng Việt tự nhiên.

## 4. Positioning (khác biệt)

- **vs cách làm thủ công (Excel/Zalo/sổ giấy):** đây là đối thủ thật sự — nhấn "thay thế mớ công cụ rời rạc bằng một nền tảng".
- **vs phần mềm quản lý trung tâm khác:** nhấn đa-tenant + tự động hóa điểm danh→học phí + cổng phụ huynh minh bạch + thuần Việt. **Không** bịa so sánh/đối thủ cụ thể nếu chưa có dữ liệu thật (hard-gate no-overclaim).
- **Trung thực beta:** "đang trong giai đoạn beta" là điểm tin cậy (mời đồng hành sớm), không giấu.

## 5. Information Architecture (đề xuất sections — design-Claude điều chỉnh được)

1. **Hero** — headline + subhead + primary CTA "Đăng ký Beta" + visual sản phẩm (mockup dashboard/điểm danh). Beta badge.
2. **Problem → Solution** — 3 nỗi đau chủ trung tâm (điểm danh thủ công / học phí sai / phụ huynh hỏi liên tục) → KiteClass giải quyết.
3. **Tính năng cốt lõi** — 4–6 feature card benefit-framed: Điểm danh · Học phí & hóa đơn · Điểm số · Lớp & khóa học · Cổng phụ huynh · Quản lý giáo viên. Mỗi card: icon + lợi ích (không chỉ tên).
4. **Cách hoạt động** — 3 bước đơn giản (Tạo lớp → Mời giáo viên/phụ huynh → Quản lý tự động).
5. **Trust** — social proof (đối tác beta trung thực / số liệu thật nếu có) + an toàn dữ liệu (PDPL/Nghị định 13) + minh bạch đơn vị phát hành.
6. **CTA cuối** — lặp lại "Đăng ký Beta" + lời mời đồng hành beta.
7. **Footer** — liên hệ, Điều khoản, Chính sách bảo mật, beta disclaimer.

## 6. Brand & mood

- **Cảm giác:** đáng tin + chuyên nghiệp + ấm áp gần gũi (giáo dục VN). Tránh: lạnh-tech, mè nheo-trẻ con, hoặc quá enterprise.
- **Màu/typography:** dùng design system KiteClass hiện hành (đừng tự chế palette mới); nếu chưa rõ token → hỏi/đọc `documents/02-architecture/design-system/`. Tương phản đạt WCAG AA (hard-gate).
- **Hình ảnh:** mockup sản phẩm thật / minh họa bối cảnh trung tâm VN; **không** stock ảnh nước ngoài lệch văn hóa, **không** placeholder.

## 7. Content outline (định hướng copy — tiếng Việt)

- Toàn bộ tiếng Việt tự nhiên (per `vn-localization-audit-checklist.md`). Số tiền `1.500.000đ`, ngày `dd/MM/yyyy`.
- Sample data hiển thị (nếu có): tên VN (`Trần Thị Hương`, `Lớp 5A1`) — **không** `John Doe`/`Class A1`.
- CTA copy: rõ + ít rào cản ("Đăng ký Beta miễn phí" thay vì "Get Started").
- **Không** Lorem Ipsum; **không** overclaim tính năng ngoài Phase 1 scope (điểm danh/học phí/điểm/lớp/khóa/phụ huynh/giáo viên có thật; AI/báo cáo nâng cao... chỉ nhắc nếu đã ship).

## 8. Constraints (bắt buộc — sẽ bị checklist chấm)

Thiết kế *hướng tới* các gate sau (chi tiết: `landing-page-review-checklist.md`):
- 🔴 **Beta disclaimer** hiển thị (GAP-539) · 🔴 **Contrast WCAG AA** · 🔴 **Cookie consent Nghị định 13** (GAP-585) · 🔴 **No-overclaim**.
- Mobile-first (đa số chủ trung tâm xem trên điện thoại), touch ≥44px.
- Performance: dùng `next/image` (avif/webp), `next/font`; giữ first-load JS `/` < 150KB; LCP < 2.5s, CLS < 0.1.
- SEO: title/meta tiếng Việt + OG/Twitter (share Zalo đẹp) + JsonLd + `lang="vi"`.

## 9. Out-of-scope / Don'ts
- Đừng tự chế brand palette/logo mới — dùng design system.
- Đừng bịa testimonial/đối thủ/số liệu.
- Đừng promise tính năng chưa ship.
- Đừng dịch máy cứng — copy phải tự nhiên (chủ trung tâm VN đọc thấy "người Việt viết").
- Đừng nhồi feature-dump; ưu tiên lợi ích + 1 primary CTA dominant.

## 10. Cách dùng cặp brief + checklist (TDD loop cho design)
1. Đọc brief (mục 1–9) → nắm direction.
2. Thiết kế draft theo IA mục 5 + tone mục 3/6.
3. Tự self-score bằng `landing-page-review-checklist.md` /100.
4. Iterate phần < pass + đảm bảo 4 hard-gate.
5. Trả bản đạt ≥85 + ghi self-score evidence.
