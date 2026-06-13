# GAP-1264: Retention clock determinism (suspended_at) + paid post-suspend retention + thống nhất messaging

**Status:** 🟢 DONE 2026-06-14
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-06-13 (Wave kitehub-biz-100 outside-in audit)
**Affects:** `kitehub-subscription` DataRetentionService + owner-facing retention messaging

## Problem

Failure-mode audit (FM-7) + persona audit (F5) + benchmark audit (F10): `DataRetentionService:70,127` dùng `updatedAt` làm mốc `suspendedAt` → bất kỳ update nào cũng reset đồng hồ retention (rủi ro PDPL — dữ liệu giữ/xóa sai mốc). Rule TR-05 retention 7 ngày chỉ áp cho TRIAL; PAID sub không có retention window xác định. Messaging cho owner cũng không nhất quán (không hiển thị ngày cụ thể).

## Proposed Fix

Thêm cột `suspended_at` riêng (deterministic, set 1 lần khi suspend). Định nghĩa retention window cho PAID post-suspend. Thống nhất messaging hiển thị ngày xóa dữ liệu cụ thể cho owner.

## Acceptance Criteria

- [x] Cột `suspended_at` riêng, không bị update khác reset
- [x] PAID sub có retention window xác định sau suspend
- [x] Owner messaging hiển thị ngày cụ thể dữ liệu sẽ bị xóa

## Related

- Audit: `documents/04-quality/audits/persona-review/2026-06-13-pre-wave-kitehub-biz-100-failure-mode.md` (FM-7)
- Audit: `documents/04-quality/audits/persona-review/2026-06-13-pre-wave-kitehub-biz-100-persona.md` (F5)
- Audit: `documents/04-quality/audits/persona-review/2026-06-13-pre-wave-kitehub-biz-100-benchmark.md` (F10)
- Sister: GAP-1256 (tier desync retention impact), GAP-1260 (involuntary churn)

## Closure — wave-kitehub-biz-100 (2026-06-14)

🟢 DONE — engineering-complete + G3 production-parity walk verified. G3 walk #7 — suspended_at stamp qua Instance.setStatus() override (retention clock determinism).

- G3 walk: `documents/04-quality/audits/persona-review/2026-06-13-g3-walk-kitehub-biz-100.md` (8 PASS / 1 PASS-with-P1 closed via GAP-1273 / 1 win-back async by-design).
- Tests: 963 backend (kitehub-subscription) + 906 frontend green.
- Consolidated into PR branch `wave/kitehub-biz-100`.
