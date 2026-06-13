# GAP-1270: Trial conversion email cadence (5-7 touch) + cơ chế trial extension

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-06-13 (Wave kitehub-biz-100 outside-in audit)
**Affects:** `kitehub-subscription` / `kitehub-email` trial conversion campaign

## Problem

Benchmark audit (F9): rule TR-03 chỉ có khoảng 3 email touch trong trial trong khi chuẩn ngành là 5-7 touch → tỷ lệ convert thấp. Ngoài ra không có cơ chế gia hạn trial (trial extension) cho KH cần thêm thời gian đánh giá.

## Proposed Fix

Tăng cadence email trial lên 5-7 touch (welcome, value, mid-trial, expiry-warning, last-chance...) + thêm trial extension config (admin grant hoặc self-serve giới hạn).

## Acceptance Criteria

- [ ] Trial conversion cadence ≥5 touch theo timeline trial
- [ ] Có cơ chế trial extension (config được)

## Related

- Audit: `documents/04-quality/audits/persona-review/2026-06-13-pre-wave-kitehub-biz-100-benchmark.md` (F9)
- Sister: GAP-1258 (auto-renew relabel), GAP-1259 (dunning)
