---
title: Kế hoạch slide bảo vệ 20 trang — KiteHub Platform
status: draft
created: 2026-06-13
updated: 2026-06-13
audience: dev
related: [GAP-653, thesis-info.md, defense-deck.html]
---

# Kế hoạch slide bảo vệ — Bản gọn 20 trang

**Mục tiêu:** thiết kế deck **20 slide** cô đọng từ `defense-deck.html` (40 slide hiện có) cho buổi bảo vệ UTC. 20 slide × ~50–60s/slide ≈ **18–20 phút nói**, chừa đủ cho live demo (~10–15 phút) + Q&A (~15–20 phút) trong khung 40–60 phút UTC.

**Quan hệ với deck 40 slide:** đây là bản **rút gọn**, KHÔNG thay thế. Deck 40 slide giữ làm bản đầy đủ/dự phòng (nếu hội đồng muốn đi sâu). Bản 20 slide = bản trình bày chính, nội dung kéo lại từ deck 40 (gộp 2–3 slide chi tiết thành 1 slide thông điệp).

**Đề tài:** "XÂY DỰNG HỆ THỐNG SAAS CUNG CẤP DỊCH VỤ ĐÀO TẠO" — Nguyễn Văn Kiệt (221230890, CNTT1-K63, UTC GTVT). Defense window 15/08→15/10/2026.

---

## 1. Nguyên tắc cô đọng 40 → 20

| Nguyên tắc | Áp dụng |
|---|---|
| **1 slide = 1 thông điệp** | Mỗi slide trả lời đúng 1 câu hỏi hội đồng |
| **Gộp slide chi tiết → slide tổng** | VD 4 slide AI (tổng quan + 4-phương-pháp + pipeline + khác-biệt) → 1 slide "AI Branding" + để chi tiết cho Q&A |
| **Ưu tiên ĐÓNG GÓP của đề tài** | Multi-tenant RLS + AI Branding + hướng chất lượng = 3 điểm nhấn, mỗi cái 1 slide riêng |
| **Số liệu/sơ đồ > chữ** | Mỗi slide ≤ 5 gạch đầu dòng; ưu tiên sơ đồ Mermaid + số KPI |
| **Slide dự phòng cho Q&A** | Slide chi tiết cắt ra → mục "Phụ lục" cuối deck (không tính trong 20) để bật khi bị hỏi sâu |

---

## 2. Khung 20 slide (từng slide)

Cột "Ánh xạ 40-deck" = slide nguồn trong `defense-deck.html` để kéo nội dung/Mermaid.

| # | Tiêu đề slide | Chương | Ánh xạ 40-deck | Thông điệp chính (1 câu) | Hình ảnh | Nói |
|:-:|---|:--:|---|---|---|:--:|
| 1 | **Bìa** — Tên đề tài + SV + GVHD + UTC + logo | — | s1 | "Hệ thống SaaS đào tạo, native multi-tenant + AI" | Logo UTC + banner | 30s |
| 2 | **Nội dung trình bày** (chương trình 5 phần) | — | mới | "Lộ trình: vấn đề → thiết kế → cài đặt → kết quả → kết luận" | thanh 5 bước | 20s |
| 3 | **Bối cảnh & vấn đề** | Ch1 | s3+s4 (gộp) | "Trung tâm/trường nhỏ VN thiếu nền tảng quản lý + thương hiệu số giá rẻ" | 3 quan sát thực tế | 60s |
| 4 | **Mục tiêu & phạm vi** | Ch1 | s5+s6 (gộp) | "4 nhóm mục tiêu; phạm vi Phase 1 BETA P1+P2" | 4 nhóm + khung phạm vi | 60s |
| 5 | **Khảo sát hệ thống tương tự + khác biệt** | Ch1 | s10+s11 (gộp) | "5 đối thủ (BeeClass/MISA/Mona/EasyEdu/DotB) — ta khác ở native multi-tenant + AI branding" | Bảng so sánh rút gọn | 70s |
| 6 | **Điểm nhấn 1 — Kỹ thuật AI Branding** | Ch1 | s7+s8+s9 (gộp 3→1) | "Pipeline tạo bộ nhận diện thương hiệu tự động (text→image, quality gate ≥70)" | Mermaid pipeline | 80s |
| 7 | **Tuân thủ pháp luật VN** | Ch1 | s13+s14 (gộp) | "PDPL 2023 + An ninh mạng 2018 + TT78 → ánh xạ điều luật ↔ tính năng" | Bảng ánh xạ 3 dòng | 60s |
| 8 | **Kiến trúc tổng thể — C4 Level 1** | Ch2 | s15 | "KiteHub (lifecycle/billing) + KiteClass (nghiệp vụ trường) chia sẻ hạ tầng" | Mermaid C4 L1 | 70s |
| 9 | **Điểm nhấn 2 — Multi-tenant + cô lập** | Ch2 | s16+s19 (gộp) | "Pool model 1 DB, cô lập bằng PostgreSQL RLS + tenant resolution" | Mermaid isolation | 70s |
| 10 | **PostgreSQL RLS — cài đặt** | Ch2/Ch3 | s17 | "RLS policy + `app.current_tenant_id` GUC chặn rò chéo tenant ở tầng DB" | Code/policy snippet | 70s |
| 11 | **Defense-in-depth 5 lớp** | Ch2 | s18 | "Bảo mật nhiều lớp: gateway → JWT → role guard → RLS → audit log" | Mermaid 5-layer | 60s |
| 12 | **Tech stack + phân rã service** | Ch2 | s20+s21 (gộp) | "Spring Boot modular monolith + microservices, Next.js, Postgres/Redis/RabbitMQ/MinIO" | Sơ đồ stack | 50s |
| 13 | **Điểm nhấn 3 — Hiện thực tiêu biểu** | Ch3 | s22 | "Code tiêu biểu: JWT auth filter + Tenant RLS interceptor + Outbox dispatcher" | 1 code snippet | 70s |
| 14 | **Triển khai — AWS Singapore Free Tier** | Ch4 | s23+s24 (gộp) | "Triển khai thực tế trên AWS ap-southeast-1, tối ưu chi phí $0 Free Tier" | Mermaid deploy topo | 60s |
| 15 | **CI/CD + Observability** | Ch4 | s25+s26 (gộp) | "GitHub Actions OIDC (không long-lived key) + observability 3 lớp" | Sơ đồ pipeline | 50s |
| 16 | **Phương pháp hướng chất lượng** | Ch1/Ch6 | mới (từ Ch1 §1.6) | "4 trụ cột: audit /100 + gap pipeline + wave + meta-rule governance" | 4 trụ cột | 50s |
| 17 | **Kết quả & KPI** | Ch4/Ch6 | s27+s28+s29 (gộp 3→1) | "Quỹ đạo điểm chất lượng + test coverage + phân tích chi phí" | Biểu đồ KPI + chi phí | 80s |
| 18 | **DEMO** (slide cầu nối → demo trực tiếp) | — | mới | "Demo trực tiếp: khách ẩn danh → onboarding → wizard tạo tenant → chứng minh multi-tenant" | ảnh chụp mở đầu | 30s + demo |
| 19 | **Hạn chế & hướng phát triển** | Ch7 | cuối deck 40 | "Thừa nhận hạn chế Phase 1 + lộ trình Phase 2/3 (EKS, payment gateway, K-12)" | 2 cột hạn chế/tương lai | 70s |
| 20 | **Kết luận + Lời cảm ơn** | Ch7 | cuối deck 40 | "Đóng góp chính + cảm ơn GVHD/hội đồng + sẵn sàng Q&A" | đóng góp 3 gạch | 40s |

**Tổng nói:** ~18.5 phút (chưa tính demo ở slide 18).

---

## 3. Phụ lục (slide dự phòng — không tính 20, bật khi Q&A)

Cắt các slide chi tiết của deck 40 ra mục phụ lục để trả lời khi bị hỏi sâu:
- A1: So sánh chi tiết 4 phương pháp text-to-image (deck s8)
- A2: PDPL 2023 ánh xạ đầy đủ điều luật (deck s14)
- A3: Sơ đồ tuần tự auth flow đầy đủ (deck s20)
- A4: Quỹ đạo KPI chi tiết theo wave (deck s28)
- A5: Phân tích chi phí chi tiết theo dịch vụ AWS (deck s29)
- A6: Phân khúc thị trường mục tiêu (deck s12)

→ ánh xạ sẵn theo 4 archetype trong `defense-qa-response-sheet.md` (Architecture / NFR-DB-DevOps / Business-Compliance / Process-Methodology).

---

## 4. Kế hoạch dựng slide

**Cách dựng (đề xuất):** tạo file mới `defense-deck-20slide.html` (reveal.js) **tái dùng** CSS/theme + Mermaid block từ `defense-deck.html` — KHÔNG viết lại từ đầu. Mỗi slide kéo nội dung từ cột "Ánh xạ 40-deck".

| Bước | Việc | Kết quả |
|:-:|---|---|
| 1 | Duyệt khung 20 slide này (bạn chốt) | chốt kế hoạch |
| 2 | Tách CSS/theme + Mermaid init từ deck 40 → file mới | khung sườn `defense-deck-20slide.html` |
| 3 | Đổ nội dung 20 slide (gộp theo ánh xạ) + ghi chú thuyết trình mỗi slide | bản nháp deck |
| 4 | Render Mermaid → kiểm 6 sơ đồ hiển thị đúng (theo memory: Mermaid dạng sơ đồ, không phải văn bản thuần) | đã kiểm sơ đồ |
| 5 | Bổ sung phụ lục A1–A6 | dự phòng sẵn sàng |
| 6 | Tự rà soát theo `thesis-content-standard.md` (giọng văn học thuật, không biệt ngữ Wave/GAP/BETA, không nhắc Claude) | sạch |
| 7 | Dry-run đo thời gian theo `practice-schedule.md` | thời lượng ≤ 20 phút |

**Chuẩn áp dụng khi dựng:**
- `thesis-content-standard.md` — giọng văn học thuật, không biệt ngữ repo (Wave/Phase/GAP), không nhắc Claude/AI-assistant.
- Sơ đồ dùng Mermaid (theo `diagram-format-selection.md`), không ASCII/văn bản thuần.
- Tài nguyên: chân dung/banner đã có ở `08-thesis/portrait/` + `banners/`.

---

## 5. Quyết định cần bạn chốt

1. **Số liệu KPI thật (slide 17):** thesis còn 2 đồng hồ chờ (GAP-648 NFR ≥30 ngày + GAP-649 beta reviews). Slide 17 dùng số thật khi có, hay dùng placeholder + nhãn "đo tại thời điểm bảo vệ"?
2. **Demo (slide 18):** demo trực tiếp hay video dự phòng? (`backup-demo.mp4` theo README).
3. **Thứ tự AI vs Kiến trúc:** hiện đặt AI (s6) trước Kiến trúc (s8). Có muốn đảo Kiến trúc trước để mạch "hệ thống → AI tính năng" không?
4. **Dựng ngay hay chỉ lập kế hoạch:** bạn nói "lên kế hoạch trước" — sau khi chốt kế hoạch này, tôi dựng `defense-deck-20slide.html` luôn hay chờ?

---

## 6. Log

- **2026-06-13:** Kế hoạch tạo inline (song song wave-kitehub-biz-100 outside-in audit). Cô đọng 40→20 slide bám 4 chương thesis + ánh xạ từ `defense-deck.html`. Chờ user chốt §5.
