# GAP-1508: OTP error-code value drift docs ↔ code

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-06-21 (API-contract full audit refresh — phase-1-closeout-loop)
**Affects:** `documents/01-business/kitehub/signup-otp/` ↔ `OtpController` (mobile signup OTP, GAP-286)

## Problem

Mã lỗi (error code) trong 3-layer docs của OTP signup KHÔNG khớp giá trị controller thực sự trả về:

- `documents/01-business/kitehub/signup-otp/api-contract.md:32` ghi `400 | { "error": "INVALID_PHONE" }`
- `documents/01-business/kitehub/signup-otp/use-cases.md:25` ghi `400 INVALID_PHONE`
- Nhưng `OtpController.java:81` (`InvalidPhoneException` handler) set `body.setProperty("error", "OTP_INVALID_PHONE")` và `:92` set `"OTP_INVALID_PHONE"` / `"OTP_INVALID_PAYLOAD"`.

Hệ quả: consumer mobile app branch theo error code sẽ không match (`INVALID_PHONE` ≠ `OTP_INVALID_PHONE`); thêm code `OTP_INVALID_PAYLOAD` (validation lỗi field ≠ phone) hoàn toàn không được document.

OTP request/response schema + 429 rate-limit + RFC 7807 envelope còn lại đều khớp đúng — chỉ riêng error-code value bị drift.

## Root Cause

GAP-286 ship feature + 3-layer docs cùng PR (#2515) nhưng error-code literal trong doc viết tay (`INVALID_PHONE`) lệch so với hằng số controller (`OTP_INVALID_PHONE`) — không có cross-layer detector bắt error-code-value (chỉ có path/method drift detector).

## Proposed Fix

Đồng bộ error-code value: sửa docs (`api-contract.md` + `use-cases.md`) thành `OTP_INVALID_PHONE` + thêm row document `OTP_INVALID_PAYLOAD` cho lỗi validation non-phone field. (Hoặc đổi controller về `INVALID_PHONE` nếu muốn giữ tên ngắn — nhưng sửa doc rẻ hơn + đã có prefix convention `OTP_*`.)

## Acceptance Criteria

- [ ] `api-contract.md` + `use-cases.md` dùng đúng `OTP_INVALID_PHONE` khớp `OtpController.java:81,92`
- [ ] `OTP_INVALID_PAYLOAD` được document trong api-contract.md error table
- [ ] grep `INVALID_PHONE` trong signup-otp docs → 0 hit lệch code

## Related

- Discovered in: API-contract full audit `documents/04-quality/audits/api-contract/2026-06-21-api-contract-full-audit.md` (B3)
- Feature gap: GAP-286 (mobile OTP signup)
- Sibling detector gap: GAP-1509 (cross-layer drift detector không bắt error-code value)
