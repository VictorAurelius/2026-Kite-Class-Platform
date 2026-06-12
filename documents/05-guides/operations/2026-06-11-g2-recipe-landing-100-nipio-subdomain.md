---
audience: dev
date: 2026-06-11
flow: landing-100 — per-tenant landing qua subdomain (G2★ production-accurate)
gate: G2★ (absorbs G3-functional, per flow-verification-campaign.md §1 2026-06-11)
fe: kiteclass-frontend :3000 (KC per kitehub-kiteclass-boundary.md §2)
access-mode: nip.io subdomain Host thật — CẤM ?tenant= (per g1-browser-walk-before-flip.md §3.1)
---

# G2★ Recipe — Landing per-tenant qua subdomain nip.io (wave landing-100)

## 1. Mục tiêu

Bạn (human) walk landing page per-tenant qua **đúng access-mode production**: browser → subdomain Host → middleware FE resolve tenant → gateway :9000 → kiteclass-core → render landing riêng từng tenant (data + màu + template khác nhau). Đây là phần còn thiếu của GAP-1077 (AC cuối) + gate G2★ wave landing-100; G1 trước đây chỉ probe `?tenant=` — chưa chứng minh Host-resolution.

**Tiêu chí quan sát then chốt (chống "green-but-wrong"):** mỗi tenant phải ĐÚNG MÀU + ĐÚNG NỘI DUNG của nó. Thấy trang **CAM "cô Khánh"** ở subdomain khác sky-education = resolve fail thầm lặng (fallback), KHÔNG phải pass.

## 2. Setup (5 phút)

1. Stack đầy đủ đang chạy (12 container healthy): `bash kitehub/scripts/status.sh` — cần `kite-gateway`, `kiteclass-core`, `kiteclass-frontend`, `kitehub-subscription` đều Up healthy. Nếu thiếu: `bash kitehub/scripts/up.sh` (profile **full** — bắt buộc, `kc-only` thiếu subscription → resolve fail).
2. **kiteclass-frontend image phải build từ code có middleware** (≥ 2026-06-11): `docker images --format '{{.Repository}} {{.CreatedAt}}' | grep kiteclass-frontend` — nếu cũ hơn merge PR walk này: `bash kitehub/scripts/rebuild.sh kiteclass-frontend`.
3. Pre-walk curl check (Bước 0 — 30 giây):
   ```bash
   curl -s http://localhost:9000/api/v1/public/tenants/by-subdomain/co-ha-toan
   # Kỳ vọng: {"id":"a1100000-...","subdomain":"co-ha-toan","name":"Co Ha Toan","status":"ACTIVE"}
   ```
   404 ở đây = seeder demo-trio chưa chạy → restart `kitehub-subscription` (DemoTrioInstanceSeeder @Profile dev) rồi thử lại.
4. Không cần sửa /etc/hosts — nip.io là wildcard DNS công cộng trỏ về 127.0.0.1 (cần internet).

## 3. Các bước walk (browser thật — Chrome/Edge, mở DevTools Console + Network)

### Bước 1 — Tenant cô Hà (personal, xanh dương)

- **Action:** mở `http://co-ha-toan.127.0.0.1.nip.io:3000/`
- **Expected:**
  - Hero "Lấy lại căn bản môn Toán cùng cô Hà" + tagline Toán tiểu học
  - **Hero background = ảnh AI-scene cô giáo** (không phải gradient trơn — gradient = asset 404, xem GAP-1203 troubleshooting)
  - Màu chủ đạo **XANH DƯƠNG #2563EB** (nút CTA, nav hover, link)
  - Template PERSONAL + các section nội dung (Vì sao chọn / 3 bước bắt đầu / trust strip) hiển thị NỘI DUNG CỦA CÔ HÀ (audience phụ huynh/học viên) — KHÔNG còn câu platform như "Vận hành trung tâm" / "Hệ thống LMS" (fix GAP-1205). Scroll xuống để section reveal (animation).
  - Tab title + OG: "Lấy lại căn bản môn Toán cùng cô Hà"
  - Console: không error đỏ (CSP warning logo MinIO đã fix GAP-1198)
- **Verify thêm:** Network tab — KHÔNG có request nào kèm `?tenant=`; document request Host = `co-ha-toan.127.0.0.1.nip.io`.

### Bước 2 — Tenant thầy Nhì (personal, xanh lá)

- **Action:** mở `http://thay-nhi-hoa.127.0.0.1.nip.io:3000/`
- **Expected:** hero "Hóa học THCS — học là hiểu cùng thầy Nhì" + màu **XANH LÁ #16A34A**. Khác hẳn Bước 1 (chứng minh per-tenant binding).

### Bước 3 — Tenant cô Khánh (organization)

- **Action:** mở `http://sky-education.127.0.0.1.nip.io:3000/`
- **Expected:** hero "Mất gốc tiếng Anh? Đã có cô Khánh" + màu **CAM #E8590C** + template ORGANIZATION (nhiều section: teachers/pricing/testimonials...).

### Bước 4 — Empty-state / anti-fabrication (Bucket A)

- **Action:** mở `http://g2test-an-8.127.0.0.1.nip.io:3000/` (tenant ACTIVE không có landing data)
- **Expected:** trang render generic "Trung tâm giáo dục" fallback — **KHÔNG có data bịa** (không danh sách giáo viên fake, không SĐT placeholder `1900 xxxx`, section trống thì ẨN).

### Bước 5 — Mobile hero + theme contrast (Bucket C/D)

- **Action:** DevTools → toggle device toolbar (iPhone 14) → reload Bước 1 + 2
- **Expected:** hero AI-scene bg + HTML text overlay đọc được trên mobile; chữ trên nút CTA đạt contrast (không chữ trắng trên nền sáng — contrast guard WCAG); **không FOUC** (không nháy màu default rồi mới đổi sang màu tenant khi reload — ThemeSync SSR-inline).

### Bước 6 — Sad path: tenant SUSPENDED (fix GAP-1199 trong PR này)

- **Action:** mở `http://sky-edu-test.127.0.0.1.nip.io:3000/`
- **Expected:** redirect 1 lần → trang `/suspended` render thông báo thân thiện. **KHÔNG ERR_TOO_MANY_REDIRECTS** (bug GAP-1199 đã fix — nếu vẫn loop = image chưa rebuild sau merge).

### Bước 7 — Sad path: subdomain không tồn tại (fix GAP-1200 trong PR này)

- **Action:** mở `http://khong-ton-tai.127.0.0.1.nip.io:3000/`
- **Expected:** trang "Không tìm thấy trung tâm" (chrome KiteClass generic, gợi ý kiểm tra địa chỉ) — **KHÔNG** render landing của tenant khác (trước fix: hiện trang cô Khánh fallback).

## 4. Sad path checks tổng

| # | Check | PASS khi |
|---|---|---|
| S1 | SUSPENDED (Bước 6) | 1 redirect → trang suspended, không loop |
| S2 | Unknown subdomain (Bước 7) | Trang "Không tìm thấy trung tâm" — không render tenant fallback |
| S3 | BE down (optional): `docker stop kitehub-subscription` rồi reload Bước 1 | Trang vẫn render (degrade, không crash); `docker start kitehub-subscription` sau test |

## 5. Báo kết quả (4 outcome)

1. ✅ **PASS toàn bộ** — báo "landing-100 G2★ PASS" → flip campaign row + GAP-1077 DONE + wave closure.
2. ⚠️ **PASS có ghi chú cosmetic** — liệt kê (screenshot) → file gap P3 defer, vẫn flip PASS.
3. ❌ **FAIL bước N** — chụp screenshot + Console + Network tab của bước fail, báo "FAIL Bước N: <symptom>" → Claude fix + re-walk.
4. 🚫 **BLOCKED setup** — stack không lên / seeder fail → báo output lệnh Bước 0.

## 6. Troubleshooting

| Triệu chứng | Nguyên nhân khả dĩ | Fix |
|---|---|---|
| Mọi subdomain đều ra trang CAM cô Khánh | Middleware không inject (image cũ / subscription down / seeder chưa chạy) | Check Setup 2-3; rebuild FE; restart subscription |
| ERR_TOO_MANY_REDIRECTS ở Bước 6 | Image chưa có fix GAP-1199 | `bash kitehub/scripts/rebuild.sh kiteclass-frontend` |
| nip.io không resolve | Mạng chặn DNS public / offline | Thêm `/etc/hosts`: `127.0.0.1 co-ha-toan.127.0.0.1.nip.io` (từng host) |
| Trang trắng + console CORS | Gateway CORS thiếu origin nip.io | Báo Claude — check gateway CORS config |

## 7. G3-infra preview (sau G2★)

G2★ PASS → flow `🟢 THÔNG (local)`. Phần còn lại **G3-infra** (AWS-gated GAP-612): wildcard cert `*.kitehub.me` + real DNS + TLS + ALB routing — checkpoint riêng khi AWS stack restore, không block local-verifiable layer.
