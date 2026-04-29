# 02 — Vietnamese UX Musts

Vietnam-specific UX patterns the design system MUST honor. Round 1 bundle covered currency + greeting style but missed date format, payment gateways, identity formats, and Zalo integration.

**Use this when:** designing any form, list, or notification touching VN user data. Failing these is a quality blocker — not a "nice to have."

---

## 1. Locale primitives

| Item | Format | Example | Source |
|------|--------|---------|--------|
| Locale code | `vi-VN` (default), `en-US` (placeholder fallback) | — | `next/i18n` config |
| Currency | Dot thousands separator + lowercase `đ` (no space) | `199.000đ`, `1.500.000đ`, `25.000.000đ/năm` | repo CSS strings |
| Currency (alt formal) | `1,500,000 VND` | rare, for export to Excel | accounting context |
| Date — short | `dd/MM/yyyy` | `15/04/2026` | MoET standard |
| Date — long | `dd 'tháng' MM 'năm' yyyy` | `15 tháng 4 năm 2026` | official documents (report card, contract) |
| Date — relative | `'hôm nay'`, `'hôm qua'`, `'X ngày trước'`, `'cách đây X tháng'` | `2 ngày trước` | Vietnamese conventions |
| Time | 24-hour `HH:mm` | `14:30`, `08:00` | preferred over AM/PM in VN |
| Datetime | `dd/MM/yyyy HH:mm` | `15/04/2026 14:30` | combines both |
| Phone — input | 10 digits starting `0` (`09xx xxx xxx` / `08xx xxx xxx` / `03xx xxx xxx`) | `0901234567` | input + `09 0123 4567` display |
| Phone — display | Group 4-3-3 with spaces | `0901 234 567` | typography preference |
| Address | `Số nhà / Đường, Phường/Xã, Quận/Huyện, Tỉnh/Thành phố` | `123 Nguyễn Văn Cừ, P. Phước Long B, Q. 9, TP. HCM` | Postal standard |
| Name order | Surname first: `Nguyễn Văn An` (family + middle + given) | display + sort by surname | VN convention |
| Name truncation | Show last 2 words if too long: `Nguyễn Văn An → Văn An` | avoid truncating from start (loses identity) | repo uses `last 2 words` |

## 2. Identity documents

| Doc | Format | When required | UX |
|-----|--------|---------------|----|
| **CCCD** (Căn cước công dân — new ID, post-2021) | 12 digits | Adult registration, payment >5M VND | Mask middle: `0123-****-7890`. Validate Luhn-like checksum. |
| **CMND** (Chứng minh nhân dân — old ID, pre-2021) | 9 digits | Older users still use; accept both | Mask middle: `012-***-789`. |
| **MST** (Mã số thuế — tax code) | 10 or 13 digits | Invoice with tax | Validate format; show as `0123456789-001` if 13-digit branch |
| **GPKD** (Giấy phép kinh doanh — business license) | varies | Tenant onboarding | File upload only, no validation |

**Rule:** never log raw CCCD/CMND in plaintext (per `logs-format-standard.md` §2.4). UI must mask after submission.

## 3. Education-specific

| Item | Format | Notes |
|------|--------|-------|
| Grade scale | 0–10 (decimal allowed: 8.5, 9.25) | NEVER A-F. Letter grades not used in VN. |
| Pass threshold | Default 5.0 | Configurable per tenant |
| Honor classification | Xuất sắc (≥9.0) / Giỏi (≥8.0) / Khá (≥6.5) / Trung bình (≥5.0) / Yếu (<5.0) | Standard MoET scale |
| Class naming | `Lớp [grade-level][section]` | `Lớp 10A2`, `Lớp 1A1`, `Lớp Mầm 2` (kindergarten) |
| Academic year | `2025-2026` (cross-year) | Sept → June convention |
| Semester | `Học kỳ I` (Sept-Jan), `Học kỳ II` (Feb-June) | Roman numerals |
| Attendance code | `P` (Present, có mặt) / `V` (Vắng có phép, excused) / `M` (Vắng không phép, unexcused) / `L` (Late, đi trễ) / `S` (Sick, nghỉ ốm) | Show as colored chip |
| Report card | "Học bạ" — VN MoET official format with: hạnh kiểm (conduct), học lực (academic), môn học (subjects 0-10 each), kết quả (overall), nhận xét GVCN (homeroom comment) | GAP-055 P1 |
| Conduct rating | Tốt / Khá / Trung bình / Yếu | Required by MoET |
| Homeroom teacher | "Giáo viên chủ nhiệm" (GVCN) — concept distinct from subject teacher | GAP-056 |

## 4. Payment gateways

VN payment is **wallet-first**, not card-first. Design must reflect this.

| Gateway | Logo | Flow | UX requirement |
|---------|------|------|----------------|
| **VNPay** | Sky-blue gradient + "VNPay" wordmark | Redirect to vnpay.vn → bank choice → confirm | Show "Bạn sẽ được chuyển đến VNPay" notice before redirect |
| **MoMo** | Pink/magenta circle + "M" mark | App-deep-link OR QR scan | Show **QR code prominently**. MoMo users scan with their MoMo app. |
| **ZaloPay** | Blue square + "ZaloPay" wordmark | App-deep-link OR QR | Same as MoMo. |
| Bank transfer (chuyển khoản ngân hàng) | bank logo + account info | Manual: show account #, account name, amount, transfer code | "Đã chuyển khoản" button → backend reconciliation |
| Cash (tiền mặt) | wallet icon | At center, admin marks paid | Common at small centers, do NOT remove |

**Mandatory UX:**
- Payment method selector → 4-5 options visible (radio cards or grid)
- QR code: 200×200 minimum, with 4mm quiet zone
- Display amount before redirect: `Bạn sẽ thanh toán: **199.000đ**`
- Trust markers: `✓ Bảo mật bởi VNPay/MoMo  ✓ Không lưu thông tin thẻ`
- After payment: success page with invoice number + "Tải biên lai" (download receipt) button

## 5. Communication & notification

| Channel | Status | Use cases | UX requirement |
|---------|--------|-----------|----------------|
| **Zalo OA** (Zalo Official Account) | Primary push channel for parents | Grade update, attendance alert, fee reminder | Card layout: 320×100, brand logo, headline 1 line, body 2 lines, CTA button. Zalo deep-link supported. |
| **Email** | Secondary; transactional only | Invite token, password reset, invoice PDF | Standard responsive HTML email. VN-first, EN fallback. |
| **SMS** | Tertiary; OTP + critical only (cost) | Login OTP (6-digit), payment confirmation | 1 message limit (~70 chars), no link if possible (anti-phishing concern in VN) |
| **Web push** | Optional; supported on Chrome/Edge desktop + Chrome Android, Safari iOS 16.4+ | Same as Zalo OA but for app users | Fallback to Zalo if browser unsupported |
| **In-app toast** | UI only | Success/error feedback | `sonner` library (KH) or shadcn `toaster` (KC) |
| **In-app notification center** | Bell icon top-right | History of last 50 notifications | Unread dot, mark all read, click → relevant route |

**Anti-pattern:** assuming Web Push parity with mobile app. iOS Safari only supports it 16.4+ (March 2023) and requires "Add to Home Screen" first. Zalo OA reaches ~95% of VN parents — use as primary.

## 6. SMS OTP layout

OTP screens are common (login, payment, parent invite redemption). Standard layout:

```
[App logo, centered]

Nhập mã xác thực
Mã đã gửi đến số 0901 234 567

[6 separate digit boxes, large 56×64, monospace]

Mã sẽ hết hạn sau 04:32   [countdown]

Không nhận được mã? Gửi lại  [link, disabled until 0:00]

[Continue button — disabled until 6 digits entered]
```

Spec: 6-digit OTP, 5-minute expiry, resend after 60s cooldown, mask phone middle, show countdown.

## 7. Trust markers

Vietnamese SaaS users are wary. Lead with trust signals:

| Marker | Where | Copy |
|--------|-------|------|
| Free trial reassurance | CTA buttons | `✓ Không cần thẻ tín dụng   ✓ Hủy bất kỳ lúc nào   ✓ Hỗ trợ tiếng Việt` |
| Security claim — explain in plain words | Footer + onboarding | `Mã hóa AES-256 — an toàn như gửi tiền ngân hàng` (NOT raw "AES-256 encrypted") |
| Compliance | Settings + signup | `Tuân thủ Nghị định 13/2023/NĐ-CP về bảo vệ dữ liệu cá nhân (PDPL)` |
| Customer testimonials | Marketing | Real names + center names + city. Avoid stock photos. Vietnamese classroom photos preferred. |
| Support availability | Header CTA + footer | `Hỗ trợ 24/7 qua Zalo, Email, Hotline 1900-xxx` |

## 8. Address & phone validation regex

For form validation in design specs:

```
Phone (VN mobile): /^0(3|5|7|8|9)\d{8}$/
Phone (VN landline): /^0(2)\d{9}$/
CCCD (12 digits, post-2021): /^\d{12}$/
CMND (9 digits, pre-2021): /^\d{9}$/
MST (10 or 13 digits): /^\d{10}(-\d{3})?$/
```

## 9. Banned anti-patterns

| ❌ Don't | ✅ Do |
|---------|------|
| Format date as `MM/dd/yyyy` (US) | `dd/MM/yyyy` (VN) |
| Show currency as `$199` or `199 USD` | `199.000đ` (lowercase đ) |
| Display name as `Last, First` | `Last First` order, e.g. `Nguyễn Văn An` (no comma) |
| Use only Visa/Mastercard logos as payment options | VNPay + MoMo + ZaloPay first; cards optional |
| English-only error messages | Vietnamese-first; English only as fallback for unknown locale |
| Address `quý khách` / `quý vị` | `bạn` (informal you) |
| Capital-case headings (`Sign Up Now`) | Sentence case (`Đăng ký ngay`) |
| Stock photos of Western classrooms | Vietnamese classroom photography (when used) |
| Hide phone country code (assume +84) | Show `+84` prefix on display, accept both `0901...` and `+84901...` on input |

## 10. Reference documents

When in doubt, defer to:

- `documents/01-business/kiteclass/grade-assignment/rules.md` — grade scale + report card
- `documents/01-business/kiteclass/payment-invoice/rules.md` — payment gateway constraints
- `documents/01-business/kiteclass/attendance/rules.md` — attendance code definitions
- `documents/04-quality/gaps/GAP-055-*.md` — official report card spec
- VN MoET regulations — Thông tư 22/2021/TT-BGDĐT (assessment), Thông tư 32/2018/TT-BGDĐT (curriculum)
