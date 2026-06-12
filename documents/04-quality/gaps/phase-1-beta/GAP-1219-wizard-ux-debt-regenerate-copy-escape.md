# GAP-1219: Wizard UX debt — regenerate no-op mid-wizard + logo over-promise copy + thiếu escape-ramp Welcome

**Status:** 🟡 PARTIAL (90% — code+tests shipped, chờ G1 walk wave branding-100)
**Priority:** 🟡 P2
**Domain:** Frontend
**Found:** 2026-06-11 (branding-100 persona audit F3/F4/F10)
**Affects:** KH wizard (RegenerateCounter, LogoStep copy, WelcomeStep)

## Problem

(1) Nút "Tạo lại" hiển thị/trừ counter cả khi CHƯA generate gì (no-op vô nghĩa); (2) copy bước Logo hứa "AI tự tạo logo" vượt khả năng hiện tại; (3) Welcome không có escape-ramp "dùng mặc định, làm sau" ngay từ đầu (GAP-287 chỉ defaults từ Logo trở đi).

## Proposed Fix

Gom 3 fix nhỏ UX vào bucket E/F wave branding-100 (cùng đợt reorder output-first).

## Acceptance Criteria

- [ ] Regenerate chỉ active sau lần generate đầu
- [ ] Copy logo khớp năng lực thật
- [ ] Welcome có lối "bỏ qua — dùng mặc định"

## Related

- Persona F3/F4/F10; benchmark khuyến nghị #4/#6

## Log

- **2026-06-12 (PARTIAL 90% — Bucket F branding-100):** 3 fix shipped: (a) `RegenerateCounter` prop `hasGenerated` — button disabled + status "Khả dụng sau khi tạo bản đầu tiên" khi chưa có job (Step6 truyền `jobId && instanceId`); (b) LogoStep + WelcomeStep copy hết over-promise — "AI tự tạo logo" → "logo chữ lồng (monogram) từ tên + màu thương hiệu"; (c) WelcomeStep escape-ramp "Dùng gợi ý an toàn — thiết lập sau" (defaults audience theo orgType + tone professional + GO_TO_STEP 6). Tests: 11 test mới PASS (RegenerateCounter gating 3 + WelcomeStep escape-ramp 3 + suite). Residual: G1 browser walk.
