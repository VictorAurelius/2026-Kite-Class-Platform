# GAP-1218: FULL_AI trừ quota + toast "đã tạo AI cao cấp" nhưng render TEMPLATE y hệt — consumer trust

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Mixed
**Found:** 2026-06-11 (branding-100 failure-mode audit #5)
**Affects:** FULL_AI path (GAP-1147 action + GAP-1135 wire)

## Problem

Chọn FULL_AI: quota bị trừ + toast thành công nhưng output là template thường (image-gen chưa wire per GAP-1135) → user trả giá trị/quota cho thứ không nhận được — rủi ro Luật Quảng cáo/consumer trust (khác scope GAP-1147 vốn chỉ là action thiếu).

## Proposed Fix

Tới khi GAP-1135 wire xong: FULL_AI disabled với badge "sắp ra mắt" (không trừ quota); sau wire: output phải khác biệt thật + label đúng. Bucket F wave branding-100.

## Acceptance Criteria

- [ ] Không trừ quota khi không có output AI thật
- [ ] Toast/label phản ánh đúng cái được tạo

## Related

- Failure-mode #5; GAP-1135/1147; vn-localization §2 tone trung thực
