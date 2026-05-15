---
persona: p2-center-owner
topic: branding
last-updated: 2026-05-15
version: v0.9.0-beta
effort_minutes: 4
---

# Tuỳ chỉnh logo + màu thương hiệu — Hướng dẫn

> 📅 Cập nhật lần cuối: **2026-05-15** · Phiên bản KiteHub: **v0.9.0-beta** · Đọc khoảng **4 phút**

## TL;DR

Sky Education có logo riêng + màu xanh navy. Chị Hằng upload logo + chọn màu trong `/settings/branding` để app + email + hoá đơn hiển thị đúng thương hiệu.

- 🎨 **Tier yêu cầu:** PRO trở lên (BETA_FREE_6M đã bao gồm)
- 🖼️ **Logo:** PNG/SVG ≤500KB, kích thước ≥256×256px
- 🎨 **Màu chủ đạo:** 1 màu primary + 1 màu accent (hex code)
- 🤖 **AI tạo logo:** Có với tier PREMIUM (tự gen từ tên trung tâm)

---

## 1. Mở trang Branding

<!-- Screenshot placeholder pending B+C merge: capture branding-step-1.png — 1440×900 vi-VN — show /settings/branding với 3 section (Logo upload / Color picker / Email signature) + preview live trên dashboard + mũi tên đỏ chỉ vào nút Upload logo -->

URL: `/settings/branding`

3 section chính:
1. **Logo** — upload file
2. **Màu chủ đạo** — color picker primary + accent
3. **Email signature** — header/footer cho email gửi phụ huynh

---

## 2. Upload logo

### 2.1 Yêu cầu file

| Field | Yêu cầu |
|---|---|
| Định dạng | PNG hoặc SVG (khuyến nghị SVG cho scaling) |
| Kích thước | ≥256×256px (cho hi-DPI) |
| Dung lượng | ≤500KB |
| Background | Trong suốt (alpha channel) hoặc trắng |
| Aspect ratio | Vuông (1:1) hoặc gần vuông |

### 2.2 Quy trình upload

<!-- Screenshot placeholder pending B+C merge: capture branding-step-2.png — 1440×900 vi-VN — show upload modal "Kéo thả file logo vào đây" + drag area + preview thumbnail "logo-sky.png 250KB 512×512" + nút Lưu mũi tên đỏ -->

Click nút **Upload logo** → modal kéo thả:

1. Kéo file `logo-sky.png` vào vùng dotted hoặc click **Chọn file**
2. Preview thumbnail xuất hiện
3. Validation tự động: kích thước/dung lượng/format
4. Click **Lưu**

Hệ thống:
1. Upload tới MinIO/S3 bucket `kitehub-branding-{tenant_id}/logo.png`
2. Generate 3 size variants: 64×64 (favicon), 256×256 (header), 512×512 (email)
3. Cập nhật `tenants.branding.logo_url` → cache invalidate
4. Trong ~5 giây, dashboard + email template tự động dùng logo mới

### 2.3 AI tạo logo (PREMIUM)

<!-- Screenshot placeholder pending B+C merge: capture branding-step-3.png — 1440×900 vi-VN — show AI logo generator "Nhập tên + ngành" form (Sky Education / Anh ngữ) + 4 logo preview AI gen + nút Chọn mũi tên đỏ -->

Nếu chưa có logo, click **Tạo logo bằng AI** (PREMIUM only):

1. Nhập tên trung tâm: `Sky Education`
2. Chọn ngành: `Anh ngữ trẻ em` / `Toán năng khiếu` / `STEM`
3. Chọn phong cách: `Modern` / `Classic` / `Playful` / `Professional`
4. Click **Tạo** → AI gen 4 logo variants trong ~30 giây
5. Chọn 1 → save làm logo chính

Quota AI: 10 lần regen/tháng cho PREMIUM, 30 lần cho ENTERPRISE.

---

## 3. Chọn màu chủ đạo

<!-- Screenshot placeholder pending B+C merge: capture branding-step-4.png — 1440×900 vi-VN — show color picker với Primary "#1e40af" navy + Accent "#fbbf24" vàng + live preview mini dashboard với màu mới -->

2 màu:
- **Primary:** Màu chính của brand (button, link, header) — `#1e40af` navy
- **Accent:** Màu phụ (highlight, badge) — `#fbbf24` vàng

3 cách chọn:
1. **Color picker:** Click ô màu → palette mở ra
2. **Hex input:** Gõ trực tiếp `#1e40af`
3. **Preset palette:** 10 preset đã được test contrast WCAG AA (xanh, đỏ, cam, tím, ...)

Click **Save** → cập nhật CSS custom properties → toàn app + email + hoá đơn dùng màu mới.

### 3.1 WCAG AA contrast check

Hệ thống auto-verify contrast ratio Primary vs background trắng:
- ✅ ≥4.5:1 → OK
- ⚠️ 3.0-4.5:1 → warning "Màu này có thể khó đọc cho người khiếm thị"
- ❌ <3.0:1 → reject, đề nghị màu đậm hơn

---

## 4. Email signature

URL: `/settings/branding/email`

Tuỳ chỉnh header + footer email gửi phụ huynh:

```
[Header: Logo Sky Education + tên trung tâm]

{nội dung email tự động hoặc gõ tay}

[Footer:
Trung tâm Anh ngữ Sky Education
123 Lê Lợi, Q.1, TP.HCM
SĐT: 0901-234-567
Website: sky-edu.vn
]
```

Variables hỗ trợ:
- `{tenant.name}` — tên trung tâm
- `{tenant.address}` — địa chỉ
- `{tenant.phone}` — SĐT
- `{tenant.email}` — email contact
- `{parent.name}` — tên phụ huynh
- `{student.name}` — tên học sinh

---

## 5. Preview tổng thể

<!-- Screenshot placeholder pending B+C merge: capture branding-step-5.png — 1440×900 vi-VN — show preview tab với 3 thumbnail (Dashboard / Email / Invoice PDF) hiển thị branding mới + mũi tên đỏ chỉ vào nút Publish -->

Tab **Preview** hiển thị 3 thumbnail:
1. Dashboard view với logo + màu mới
2. Sample email gửi phụ huynh "Bé Hoa đã có mặt"
3. Sample invoice PDF với logo + màu

Click **Publish** → branding go-live cho toàn bộ tenant trong ~10 giây.

---

## 6. Troubleshooting

| Triệu chứng | Khả năng | Hành động |
|---|---|---|
| Upload báo "File quá lớn" | >500KB | Optimize bằng tinypng.com hoặc tăng compression |
| Preview không refresh sau save | Browser cache | F5 hoặc Ctrl+Shift+R hard refresh |
| Email gửi đi vẫn dùng logo cũ | Email queue chưa update | Wait ~5 phút cho cache TTL |
| AI gen logo fail "Quota exhausted" | Đã dùng hết 10 lần/tháng | Wait tháng sau hoặc upgrade ENTERPRISE |

---

## 7. Liên kết

- [Tổng quan Chủ trung tâm](index.md)
- [Bảng giá + Thanh toán](pricing-billing.md)
- [Cài đặt chung](settings.md)

---

## 🆘 Cần hỗ trợ?

- 📧 Email: [support@kitehub.me](mailto:support@kitehub.me)
- 📞 Hotline: 1900-xxxx (giờ hành chính)
- 🎨 Design help: Phase 1.5+ sẽ có gói thiết kế logo có phí
- 📊 Trạng thái beta: [/beta-status](/beta-status)
