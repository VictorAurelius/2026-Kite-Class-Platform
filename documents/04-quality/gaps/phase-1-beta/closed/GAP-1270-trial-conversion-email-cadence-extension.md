# GAP-1270: Trial conversion email cadence (5-7 touch) + cơ chế trial extension

**Status:** 🟢 DONE 2026-06-14
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-06-13 (Wave kitehub-biz-100 outside-in audit)
**Affects:** `kitehub-subscription` / `kitehub-email` trial conversion campaign

## Problem

Benchmark audit (F9): rule TR-03 chỉ có khoảng 3 email touch trong trial trong khi chuẩn ngành là 5-7 touch → tỷ lệ convert thấp. Ngoài ra không có cơ chế gia hạn trial (trial extension) cho KH cần thêm thời gian đánh giá.

## Proposed Fix

Tăng cadence email trial lên 5-7 touch (welcome, value, mid-trial, expiry-warning, last-chance...) + thêm trial extension config (admin grant hoặc self-serve giới hạn).

## Acceptance Criteria

- [x] Trial conversion cadence ≥5 touch theo timeline trial
- [x] Có cơ chế trial extension (config được)

## Related

- Audit: `documents/04-quality/audits/persona-review/2026-06-13-pre-wave-kitehub-biz-100-benchmark.md` (F9)
- Sister: GAP-1258 (auto-renew relabel), GAP-1259 (dunning)

## Closure — wave-kitehub-biz-100 (2026-06-14)

🟢 DONE — engineering-complete + G3 production-parity walk verified. BE-4 — trial conversion email cadence + trial extension mechanism.

- G3 walk: `documents/04-quality/audits/persona-review/2026-06-13-g3-walk-kitehub-biz-100.md` (8 PASS / 1 PASS-with-P1 closed via GAP-1273 / 1 win-back async by-design).
- Tests: 963 backend (kitehub-subscription) + 906 frontend green.
- Consolidated into PR branch `wave/kitehub-biz-100`.
