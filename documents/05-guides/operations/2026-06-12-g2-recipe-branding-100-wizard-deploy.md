---
title: G2★ Recipe — Wave branding-100: AI Branding wizard → deploy → landing đổi THẬT (KH-6 + KC-10)
created: 2026-06-12
audience: dev
flows: [KH-6, KC-10]
wave: branding-100
status: ready-for-human-walk
---

# G2★ Recipe — AI Branding wizard end-to-end (TEMPLATE + FULL_AI)

> **Mục tiêu:** bạn tự walk bằng browser thật, xác nhận chuỗi
> `wizard 5 bước → generate → preview WYSIWYG → approve (quality gate) → deploy → SSE → landing tenant đổi THẬT`.
> G1 agent walk PASS 2026-06-12 (PR #2371 fix 5 bug); đây là gate G2★ trước khi flip wave complete.

## 0. Trạng thái stack + setup (đã sẵn, chỉ verify)

| Thứ | Trạng thái | Verify nhanh |
|---|---|---|
| Stack local | 6 services rebuilt 2026-06-12 (gateway/branding/subscription/core/2 FE) | `docker ps` — tất cả `(healthy)` |
| Persona | `owner@skyedu.vn` / `SkyEdu@2026` — tenant Sky Education, **PREMIUM ACTIVE** | đã set: subscription PREMIUM + `instances.status=ACTIVE` |
| OpenAI key | key thật từ Secrets Manager đã trong `kitehub/.env` (`sk-proj…`, 164 ký tự) | **BE-only check:** `docker exec kitehub-branding sh -c 'echo ${OPENAI_API_KEY:0:7}'` → `sk-proj` |
| ⚠️ OpenAI billing | **GAP-1240 PENDING** — billing hard limit; nhánh FULL_AI ảnh thật chỉ chạy SAU khi bạn nạp credit (~$10 đề xuất) | platform.openai.com → Billing |
| Landing backup | Walk sẽ ĐỔI landing Sky (màu + logo). Restore nếu cần: `/tmp/branding-walk/landing_pages_backup.csv` | — |

## 1. Nhánh A — TEMPLATE (mọi tier, không tốn OpenAI)

> **Flow đã update theo kit v3 (PR #2376/#2378 — 2026-06-12):** stepper 5 bước
> `Bắt đầu / Phong cách / Hình ảnh / Tạo & Duyệt / Triển khai` — KHÔNG còn bước
> "Mẫu thiết kế" riêng; **Triển khai là bước 5 trong stepper**; Bước 2 có mục
> **nhập thông tin trung tâm** (thu gọn) sẽ tự lên landing sau deploy.

| # | Hành động (browser) | Kỳ vọng |
|---|---|---|
| A1 | Mở `http://localhost:3001/login` → đăng nhập `owner@skyedu.vn` / `SkyEdu@2026` | Vào `/dashboard` KH |
| A2 | Sidebar → **AI Branding** | Stepper 5 bước: Bắt đầu / Phong cách / Hình ảnh / **Tạo & Duyệt** / **Triển khai** |
| A3 | Bước 1: tên `Sky Education`, slug `sky-education` (slug CHÍNH MÌNH không báo trùng — GAP-1239), card **"Mẫu dựng sẵn"** (badge Khuyến nghị) → Tiếp tục | Không còn chọn loại hình trung tâm (gỡ per kit); card "AI vẽ toàn bộ" hiện quota PREMIUM |
| A4 | Bước 2: chọn đối tượng + phong cách; mở mục **"Thông tin hiển thị trên trang"** → nhập địa chỉ + SĐT (+ học phí nếu muốn, format `1.500.000đ`) → Tiếp tục | Mục thông tin thu gọn mặc định; các trường optional |
| A5 | Bước 3 Hình ảnh: bỏ qua (hoặc thử upload logo) → Tiếp tục | Optional |
| A6 | Bước 4 **Tạo & Duyệt**: preview WYSIWYG render ngay (slug mới chưa tồn tại cũng render draft — fix #2375); đổi **biến thể A/B/C**; xem **panel Điểm chất lượng**; bật **4 toggle phê duyệt** | Footer "Bước 4 / 5 · 4/4 phần được áp dụng" → nút **"Triển khai & lên sóng"** sáng |
| A7 | Bấm **Triển khai & lên sóng** | Stepper nhảy **bước 5 Triển khai** (SSE progress trong stepper) → "TRIỂN KHAI THÀNH CÔNG" + link `http://localhost:3000/?tenant=sky-education` |
| A8 | Mở `http://sky-education.127.0.0.1.nip.io:3000/` (subdomain thật — KHÔNG dùng `?tenant=` làm bằng chứng) | Landing đổi **màu theme** + hiển thị **SĐT/địa chỉ vừa nhập ở A4** (footer/contact section) |
| A9 | Verify DB (BE-only): `docker exec kite-postgres psql -U kitehub -d kiteclass_shared -c "SELECT branding_version, contact_phone, address FROM landing_pages WHERE instance_id='e8ff87e1-69fc-4842-a263-7385c68b4ffb';"` | `branding_version` +1; phone/address khớp A4 |

**Sad path A (bắt buộc 1 trong 2):**
- A-sad-1: Bước 1 nhập slug của tenant KHÁC (vd `co-ha-toan`) → phải báo **trùng** + suggestions.
- A-sad-2: Bước 4 KHÔNG bật đủ 4 toggle → nút "Triển khai & lên sóng" phải **disabled**.

## 2. Nhánh B — FULL_AI (PREMIUM, tốn ~$0.25/ảnh — SAU khi nạp billing GAP-1240)

| # | Hành động | Kỳ vọng |
|---|---|---|
| B1 | Wizard mới: Bước 1 chọn mode **"AI cao cấp"** | Card không khóa (PREMIUM + quota còn) |
| B2 | Bước 2 như A4; Bước 3 thử **upload chân dung** (portrait — chỉ hiện ở FULL_AI) | `wizard-portrait-drop` hiện; upload OK |
| B3 | Sau Bước 3 → vào **Bước 4 Tạo & Duyệt** (không còn bước Mẫu — cả 2 mode cùng stepper 5 bước) | — |
| B4 | Bước 5: bấm **"Tạo bằng AI cao cấp (tốn 1 lượt)"** → đợi 15–60s | Toast "Đã tạo banner bằng AI cao cấp — đã trừ 1 lượt"; banner trong preview = **ảnh AI thật** (URL MinIO `full-ai-banner-*.png`, KHÔNG phải placehold.co) |
| B5 | Approve 4 toggle → Triển khai → mở landing nip.io | Landing đổi theme + banner AI |
| B-sad | (Khi billing CHƯA nạp) bấm Tạo AI | Toast **GENERATION_FAILED dùng bản Mẫu** + **KHÔNG trừ lượt** (fix GAP-1218 — verify quota label không giảm) |

## 3. Gộp walk 2 gap chờ human (cùng phiên)

| Gap | Walk | Kỳ vọng |
|---|---|---|
| GAP-1229 favicon | Sau deploy, mở landing nip.io → xem tab icon | Favicon theo branding tenant (resolve từ settings) |
| GAP-1211 upload banner file | KC `:3000` login owner → Cài đặt → Landing → banner → **chọn file** (multipart) | Upload OK, 415 khi file sai loại |

## 4. Báo kết quả (4-outcome)

| Kết quả | Bạn báo | Tôi sẽ làm |
|---|---|---|
| ✅ PASS hết A (+B nếu đã nạp billing) | "G2 branding pass" | Flip wave branding-100 complete + reconciliation + campaign KH-6/KC-10 |
| ⚠️ PASS A, B chưa walk (billing) | "G2 pass A, B chờ billing" | Flip A-scope; GAP-1240 giữ PENDING, B re-walk sau nạp |
| ❌ Bug mới | Mô tả bước + screenshot | Catalog-then-batch fix → re-walk |
| 🛑 Blocker không đi tiếp được | Báo bước kẹt | Tôi debug live |

## 5. Troubleshooting nhanh

| Triệu chứng | Nguyên nhân thường | Fix |
|---|---|---|
| Landing "Trang tạm ngưng" | `instances.status` SUSPENDED (trial hết hạn — scheduler) | `UPDATE instances SET status='ACTIVE' WHERE subdomain='sky-education';` |
| FULL_AI card khóa dù PREMIUM | FE đọc tier từ **subscriptions** (không phải JWT) | Verify row subscriptions ACTIVE PREMIUM cho instance |
| Deploy xong landing không đổi | Consumer drop (đã fix wire-format #2371) — nếu tái diễn xem `docker logs kiteclass-core \| grep branding` | Báo tôi — regression |
| Preview iframe trống | CSP frame-ancestors (đã fix #2367) | `curl -sI localhost:3000/preview \| grep -i frame-ancestors` phải có `:3001` |

**G3-infra (residual, AWS-gated GAP-612):** TLS + wildcard cert + ALB routing — không chặn flip local per campaign §1 G2★ gate.
