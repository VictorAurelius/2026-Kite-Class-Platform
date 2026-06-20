---
audience: mixed
---

# SMS / Zalo ZNS Provider Evaluation — Phase 2 live notification

**Status:** 🟡 EVALUATION (Phase 1 = mock; Phase 2 = pick + integrate live)
**Created:** 2026-06-21
**Scope:** So sánh nhà cung cấp cho kênh notification chủ động (Zalo ZNS + SMS) — input cho quyết định Phase 2 GAP-063 live integration.
**Related:** `documents/04-quality/gaps/phase-1-beta/GAP-063-zalo-oa-sms-notification-infra.md` · `documents/00-brd/compliance-checklist.md` (L1 PDPL · L6 Cybersecurity localization · L3 Consumer Protection) · `.claude/rules/business-logic-review.md`

---

## 1. Bối cảnh + nguyên tắc

Thị trường VN: **Zalo là kênh push chủ đạo** (>70% người dùng), SMS là fallback khi user chưa follow OA hoặc ZNS lỗi. Phase 1 BETA ship adapter **mock-mode** (`ZaloOAMockService` per GAP-063 Phase 1) qua `NotificationChannel` abstraction — slot sẵn sàng cho Phase 2 cắm provider thật.

Phase 2 cần chọn provider thoả 3 nghĩa vụ tuân thủ (per `compliance-checklist.md`):
- **L1 PDPL (NĐ13/2023):** số điện thoại = PII → provider phải có DPA + xử lý dữ liệu hợp pháp.
- **L6 An ninh mạng (NĐ53/2022):** ưu tiên provider lưu/xử lý trong VN (data localization).
- **L3 Bảo vệ NTD:** nội dung ZNS/SMS không spam, có opt-out, đúng template duyệt.

> TBD (Phase 2 — cần product + legal quyết): chốt provider + ký DPA + duyệt template ZNS với Zalo.

---

## 2. Kênh + provider ứng viên

Kiến trúc 2 lớp: **(a) Zalo ZNS** (kênh chính) + **(b) SMS fallback** (khi user không có Zalo). 3 ứng viên đại diện cho 3 mô hình. (GAP-063 §Bucket-C gợi ý ban đầu Twilio VN / VNStack / FPT SMS — đều là SMS-only; eval này reframe sang **ZNS-primary** vì Zalo là kênh chủ đạo VN. `eSMS.vn` đại diện class "VN SMS aggregator" — **FPT SMS / VNStack** là lựa chọn thay thế cùng class.)

| # | Provider | Mô hình | Kênh phủ |
|---|---|---|---|
| **P1** | **Zalo ZNS trực tiếp** (qua Zalo Official Account) | First-party, không qua trung gian | ZNS (template) — KHÔNG có SMS |
| **P2** | **eSMS.vn** (aggregator VN — đại diện class) | Trung gian VN: SMS brandname + ZNS reseller | SMS brandname + ZNS + Zalo |
| **P3** | **Twilio / Stringee** (programmable) | Twilio = global; Stringee = VN local | SMS (Twilio global, Stringee VN) — không ZNS native |

---

## 3. Ma trận so sánh

| Tiêu chí | P1 Zalo ZNS trực tiếp | P2 eSMS.vn | P3 Twilio / Stringee |
|---|---|---|---|
| **ZNS native** | ✅ trực tiếp (chất lượng cao nhất) | ✅ qua reseller | ❌ (Twilio) / ❌ (Stringee) |
| **SMS fallback** | ❌ không có | ✅ SMS brandname VN | ✅ (Twilio global / Stringee VN) |
| **Phủ thị trường VN** | ✅ Zalo-only | ✅ SMS + Zalo full | ⚠️ Twilio đắt + cần brandname riêng / Stringee tốt VN |
| **Giá (ước tính)** | ZNS ~200-400đ/tin (rẻ nhất) | SMS ~300-800đ + ZNS markup | Twilio SMS ~$0.04+/tin (đắt) / Stringee ~tương đương eSMS |
| **Data residency (L6)** | ✅ VN (Zalo/VNG) | ✅ VN | ⚠️ Twilio = ngoài VN (risk localization) / ✅ Stringee VN |
| **DPA / PDPL (L1)** | ✅ Zalo OA terms | ⚠️ cần ký DPA với eSMS | ⚠️ Twilio GDPR-DPA (không PDPL-specific) / Stringee VN |
| **Template approval** | cần Zalo duyệt ZNS template (~2-5 ngày) | eSMS hỗ trợ submit hộ | SMS brandname cần đăng ký (~1-2 tuần) |
| **Onboarding effort** | OA Business verify (~2-3 ngày) | 1 hợp đồng phủ cả SMS+ZNS | 2 vendor riêng (phức tạp) |
| **API quality** | REST OA API (ổn, docs VN) | REST (docs VN, có SDK) | Twilio xuất sắc / Stringee khá |
| **Fallback chain** | cần tự build SMS riêng | ✅ 1 provider lo cả 2 kênh | cần tự build ZNS riêng |

---

## 4. Khuyến nghị (sơ bộ — chốt ở Phase 2)

**Phương án đề xuất: Zalo ZNS trực tiếp (P1) làm kênh chính + eSMS.vn (P2) làm SMS fallback + ZNS backup.**

Lý do:
- ZNS trực tiếp = rẻ nhất + chất lượng cao nhất cho kênh chủ đạo (Zalo).
- eSMS.vn = 1 hợp đồng phủ SMS brandname (fallback khi user không có Zalo) + ZNS dự phòng, data tại VN (thoả L6), docs/SDK tiếng Việt.
- Tránh Twilio cho production VN: đắt + data residency risk (L6) + cần brandname đăng ký riêng. Stringee là lựa chọn thay thế eSMS nếu cần (cùng VN-local profile).

**Fallback chain (Phase 2):** Zalo ZNS → (nếu user chưa follow OA / ZNS fail) → SMS brandname qua eSMS → (nếu fail) → email (kênh đã có).

> TBD (Phase 2): xác minh giá thực tế qua báo giá vendor · ký DPA eSMS + Zalo OA terms · duyệt ≥3 ZNS template (mã OTP, nhắc lịch, hóa đơn) · per-tenant cost tracking · alert khi delivery-rate < ngưỡng.

---

## 5. Liên hệ Phase 1 (đã ship)

| Hạng mục Phase 1 | Trạng thái |
|---|---|
| `NotificationChannel` abstraction | ✅ (email shipped) |
| `ZaloOAClient` interface + mock impl | ✅ GAP-063 Phase 1 (mock-mode default) |
| Config skeleton (`kitehub.notification.zalo.*`) | ✅ placeholder keys |
| Mock IT test | ✅ |
| Provider eval (doc này) | ✅ |
| **Live integration (provider thật)** | ⛔ Phase 2 — cần vendor account + DPA + template approval (REAL-USER-ACTION) |

---

## 6. Log

- **2026-06-21 (v1.0):** Created — GAP-063 Phase-1 deliverable "3-provider SMS/ZNS evaluation". 3 ứng viên (Zalo ZNS trực tiếp / eSMS.vn / Twilio-Stringee) × 10 tiêu chí; khuyến nghị sơ bộ Zalo ZNS + eSMS fallback. Live pick + DPA + template approval = Phase 2 (vendor-gated). Tác giả: @nguyenvankiet (acting architect, solo-dev).
