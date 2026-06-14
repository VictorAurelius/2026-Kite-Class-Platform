# GAP-1364: FE route bundle size UNCHECKED — cần fresh production build verify

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Frontend
**Found:** 2026-06-14 (Performance full audit post wave-p0-closeout-1, sub-check 3.1/3.2 ❓ UNCHECKED)
**Affects:** `kitehub/kitehub-frontend`, `kiteclass/kiteclass-frontend`

## Problem

Sub-check 3.1 (route bundle ≤250KB gzipped) + 3.2 (First Load JS shared ≤200KB) đánh dấu `❓ UNCHECKED` trong audit 2026-06-14 — production `pnpm build` KHÔNG chạy lượt audit (vượt light-probe budget). KHÔNG default PASS per rubric §4.5.

Static config tốt: `output: standalone` + `experimental.optimizePackageImports` + 17/26 `dynamic()` + raw `<img>` ≤5. Wave 85 baseline đo bundle trong-ngưỡng → KHÔNG có dấu hiệu regression, nhưng cần đo lại số liệu thực sau wave-p0-closeout-1 để xác nhận.

## Proposed Fix

Chạy `pnpm --filter kitehub-frontend build` + `pnpm --filter kiteclass-frontend build`, đọc route-size table + First Load JS shared. Flag route >250KB / shared >200KB. Lưu output vào audit performance.

## Acceptance Criteria

- [ ] Build output route-size table cả 2 app được capture
- [ ] Mọi route ≤250KB gzipped HOẶC finding cụ thể cho route vượt
- [ ] First Load JS shared ≤200KB xác nhận

## Related

- Discovered in: 2026-06-14 performance audit (F-008)
- GAP-354 (phase-2) — per-kit bundle budget (scope khác: production-port kit)
