# GAP-1364: FE route bundle size UNCHECKED — cần fresh production build verify

**Status:** 🟢 DONE
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

- [x] Build output route-size table cả 2 app được capture
- [x] Mọi route ≤250KB gzipped HOẶC finding cụ thể cho route vượt
- [x] First Load JS shared ≤200KB xác nhận

## Resolution (2026-06-15 — audit-fixG-quality wave)

**DONE (measurement).** Chạy production build cả 2 app FOREGROUND: `pnpm --filter kitehub-frontend build` + `pnpm --filter kiteclass-frontend build` (Next.js 15.5.18) → cả 2 `BUILD EXIT 0` (KH 68 route, KC 98 route). Số liệu đầy đủ + bảng route lưu tại `documents/04-quality/audits/performance/2026-06-15-fe-bundle-measurement.md`.

Kết quả (Next.js báo RAW; gzip JS ≈ 30-35% raw):
- **Shared First Load JS**: KH 103 kB raw / KC 103 kB raw → ✅ PASS 3.2 (≤200KB, kể cả raw).
- **Max route**: KH `/billing` 212 kB raw (~72 kB gz); KC `/settings` 276 kB raw (~94 kB gz) → cả hai < 250KB gzipped → ✅ PASS 3.1.
- **Không route nào vượt 250KB gzipped.** Sub-check 3.1/3.2 chuyển ❓ UNCHECKED → ✅ PASS bằng số liệu thật.
- **Watch item** (không chặn): KC `/settings` 276 kB raw là route nặng nhất (page-level 33.9 kB); dưới ngưỡng gzipped, theo dõi ở performance audit kế — chưa cần action.

## Related

- Discovered in: 2026-06-14 performance audit (F-008)
- GAP-354 (phase-2) — per-kit bundle budget (scope khác: production-port kit)
