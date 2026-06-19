---
title: Repo Discoverability & Star Strategy
status: active
created: 2026-06-19
owner: "@nguyenvankiet"
audience: dev
---

# Chiến lược tăng độ phủ + kéo star cho repo

> **Reality-check thẳng thắn:** GitHub Trending xếp theo **tốc độ tăng sao trong ngày/tuần**, đến từ
> người thật star. Với 1 repo đồ án solo, lên Trending là khó và phần lớn **ngoài tầm kiểm soát**.
> Cái kiểm soát được 100% là **discoverability** (được tìm thấy) + **first-impression** (gây ấn tượng
> để được star). Doc này tách rõ 2 phần: việc đã làm (nền tảng) và việc cần làm (kéo traffic thật).

---

## 1. Nền tảng discoverability — ĐÃ LÀM (PR repo-discoverability 2026-06-19)

| Hạng mục | Trạng thái | Ghi chú |
|---|---|---|
| GitHub About — description | ✅ | Mô tả 1 câu súc tích về 2 sản phẩm |
| GitHub Topics (18) | ✅ | `spring-boot` `nextjs` `multi-tenant` `saas` `edtech` `vietnam` … → xuất hiện trong topic pages + search |
| Homepage URL | ✅ | GitHub Pages design preview |
| LICENSE (MIT) | ✅ | Repo có license → tăng tín nhiệm + khả năng được dùng lại |
| README marketing overhaul | ✅ | Badges tĩnh + mục "Why interesting" + Mermaid architecture diagram |
| Community health (CONTRIBUTING / SECURITY / CODE_OF_CONDUCT) | ✅ | GitHub "Community Standards" đầy đủ |
| Social preview image (OG) | ⏳ upload thủ công | File `assets/og-preview.png` đã tạo — **cần upload qua Settings UI** (xem §2) |

---

## 2. Việc còn lại CẦN bạn làm thủ công (UI-only, không có command path)

### 2.1 Upload Social Preview image
GitHub không có API/CLI cho ảnh social preview — phải qua UI:
1. Vào **Settings** của repo → kéo xuống mục **Social preview**.
2. Bấm **Edit** → **Upload an image** → chọn `assets/og-preview.png`.
3. Lưu. Từ giờ khi share link repo lên Twitter/X, LinkedIn, Reddit, Discord… sẽ hiện ảnh đẹp 1280×640.

> Khi cần đổi ảnh: sửa `assets/og-preview.html` rồi render lại bằng Playwright (xem commit gốc),
> hoặc nhờ Claude render lại.

### 2.2 (Tùy chọn) Pin repo trên profile + đặt avatar/profile cho chuyên nghiệp.

---

## 3. Kéo star thật — các kênh (theo thứ tự ROI)

Star phải đến từ người thật. Dưới đây là kênh thực tế, xếp theo hiệu quả cho 1 project EdTech/SaaS VN:

| # | Kênh | Cách làm | Lưu ý |
|---|---|---|---|
| 1 | **Bài viết kỹ thuật (dev.to / Medium / Viblo)** | Viết 1 bài "How I built a multi-tenant EdTech SaaS that provisions itself" — kèm Mermaid diagram + ảnh OG. Link repo ở cuối. | ROI cao nhất, lâu dài (SEO). Viblo phủ cộng đồng dev VN tốt. |
| 2 | **Reddit** `r/SideProject`, `r/selfhosted`, `r/SaaS` | Post "I built X, feedback welcome" — khiêm tốn, hỏi feedback thay vì xin star. | Đọc rule từng sub; tránh spam. |
| 3 | **Hacker News — Show HN** | Tiêu đề `Show HN: Kite — self-provisioning multi-tenant school platform`. | Chỉ 1 lần; chọn giờ US morning. Cần repo + demo chỉn chu trước. |
| 4 | **LinkedIn** | Post cá nhân kể câu chuyện đồ án + ảnh OG + link. Tag #SpringBoot #NextJS #EdTech. | Phù hợp định vị tuyển dụng/portfolio. |
| 5 | **awesome-* lists** | PR thêm repo vào `awesome-selfhosted`, `awesome-multi-tenant`, các awesome EdTech (nếu đạt tiêu chí). | Yêu cầu repo trưởng thành (README + license + demo). |
| 6 | **Cộng đồng VN** (Facebook group dev, Discord J2TEAM, …) | Chia sẻ như portfolio đồ án tốt nghiệp. | Audience thân thiện cho project VN-first. |
| 7 | **Demo sống** | Deploy 1 demo công khai (khi Phase 4 deploy xong) hoặc giữ live design preview. Người ta star cái họ *thấy chạy được*. | Live design preview hiện tại đã là điểm cộng. |

---

## 4. Điều kiện "đáng để post" (làm TRƯỚC khi đẩy traffic)

Đừng đẩy traffic khi repo chưa sẵn — first impression chỉ có 1 lần:

- [x] README có hook rõ trong 5 giây đầu (✅ mục "Why interesting" + badges + diagram)
- [x] LICENSE rõ ràng (✅ MIT)
- [x] About + topics (✅)
- [ ] Social preview uploaded (§2.1)
- [ ] Có ít nhất 1 ảnh/GIF demo UI thật (hiện dùng Mermaid diagram + live preview; bổ sung screenshot dashboard khi FE chạy ổn)
- [ ] (Lý tưởng) 1 demo deploy công khai hoặc video ngắn

---

## 5. Đo lường

- **GitHub Insights → Traffic**: theo dõi Views/Clones/Referrers sau mỗi lần post → biết kênh nào hiệu quả.
- **Stars over time**: dùng star-history.com để xem đường cong.
- Lặp lại kênh ROI cao, bỏ kênh im lặng.

---

## 6. Không nên làm

- ❌ Mua star / trao đổi star / bot → vi phạm ToS GitHub, rủi ro ban + mất uy tín.
- ❌ Spam cùng 1 link nhiều group/sub liên tục.
- ❌ Tiêu đề giật gân không đúng năng lực sản phẩm → backfire ở HN/Reddit.
