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

| # | Hành động (browser) | Kỳ vọng |
|---|---|---|
| A1 | Mở `http://localhost:3001/login` → đăng nhập `owner@skyedu.vn` / `SkyEdu@2026` | Vào `/dashboard` KH |
| A2 | Sidebar → **AI Branding** → vào wizard | Step indicator **5 bước**: Bắt đầu / Phong cách / Hình ảnh / Mẫu thiết kế / Xem & Tạo |
| A3 | Bước 1: tên `Sky Education`, slug `sky-education`, loại hình "Trung tâm nhỏ", mode **"Mẫu"** → Tiếp tục | ✅ Slug **CHÍNH MÌNH không báo trùng** (fix GAP-1239). Card "AI cao cấp" hiện **mở khóa + quota tháng** (PREMIUM) |
| A4 | Bước 2: chọn audience (vd "Trung tâm tiếng Anh") + tone (vd "Thân thiện") → Tiếp tục | 1 trang gộp — không còn 2 bước rời |
| A5 | Bước 3 Hình ảnh: bỏ qua (hoặc thử upload logo) → Tiếp tục | Optional, đi tiếp được không cần gì |
| A6 | Bước 4: chọn 1 template (vd T1) → Tiếp tục | Grid 6 template + "Xem toàn màn hình" |
| A7 | Bước 5: xem preview iframe (WYSIWYG = đúng render landing thật), thử đổi **biến thể màu A/B/C**, bật **4 toggle phê duyệt** (logo/màu/banner/hero) | Counter "4/4 tài nguyên đã phê duyệt" → nút **"Triển khai trang web"** sáng |
| A8 | Bấm **Triển khai trang web** | Màn Deploying + progress SSE → **"TRIỂN KHAI THÀNH CÔNG"** + link **"Mở trang web của bạn"** = `http://localhost:3000/?tenant=sky-education` (đúng slug mình, không phải -2) |
| A9 | **QUAN TRỌNG — landing đổi THẬT:** mở `http://sky-education.127.0.0.1.nip.io:3000/` (subdomain thật, KHÔNG dùng `?tenant=` làm bằng chứng) | Landing Sky render với **màu theme vừa deploy** (đổi so với trước walk) |
| A10 | Verify DB (BE-only): `docker exec kite-postgres psql -U kitehub -d kiteclass_shared -c "SELECT branding_version, primary_color FROM landing_pages WHERE instance_id='e8ff87e1-69fc-4842-a263-7385c68b4ffb';"` | `branding_version` tăng +1 so với trước bấm deploy; màu khớp variant đã chọn |

**Sad path A (bắt buộc 1 trong 2):**
- A-sad-1: Bước 1 nhập slug của tenant KHÁC (vd `co-ha-toan`) → phải báo **trùng** + suggestions.
- A-sad-2: Ở bước 5 KHÔNG bật đủ 4 toggle → nút Triển khai phải **disabled**.

## 2. Nhánh B — FULL_AI (PREMIUM, tốn ~$0.25/ảnh — SAU khi nạp billing GAP-1240)

| # | Hành động | Kỳ vọng |
|---|---|---|
| B1 | Wizard mới: Bước 1 chọn mode **"AI cao cấp"** | Card không khóa (PREMIUM + quota còn) |
| B2 | Bước 2 như A4; Bước 3 thử **upload chân dung** (portrait — chỉ hiện ở FULL_AI) | `wizard-portrait-drop` hiện; upload OK |
| B3 | Sau Bước 3 → **nhảy thẳng Bước 5** (bỏ qua Mẫu thiết kế) | Indicator ẩn bước Template |
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
