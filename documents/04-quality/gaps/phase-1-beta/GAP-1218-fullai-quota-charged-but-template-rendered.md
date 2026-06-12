# GAP-1218: FULL_AI trừ quota + toast "đã tạo AI cao cấp" nhưng render TEMPLATE y hệt — consumer trust

**Status:** 🟡 PARTIAL (90% — code+tests shipped, chờ G1 walk wave branding-100)
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

## Log

- **2026-06-12 (PARTIAL 90% — Bucket F branding-100):** Code shipped: (BE) `BrandingJobV1Controller` guard `branding.full-ai.image-gen-enabled:false` — FULL_AI request khi image-gen chưa wire → `fallbackReason=NOT_AVAILABLE` + mode TEMPLATE + **KHÔNG gọi `recordFullAiUsage`** (test verify never()); Bucket E flips flag khi wire generator thật (GAP-1135). (FE) toast NOT_AVAILABLE nói thật "KHÔNG trừ lượt của bạn"; success toast chỉ claim trừ lượt khi mode=FULL_AI thật. Tests: BE 10/10 + FE suite 112 PASS. Residual: G1 browser walk wave branding-100.
