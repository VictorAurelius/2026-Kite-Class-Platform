# GAP-1260: Involuntary-churn lifecycle spec — PAID sub hết grace → suspend path (rule + impl)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-13 (Wave kitehub-biz-100 outside-in audit)
**Affects:** `kitehub-subscription` PAID subscription lifecycle + grace handling

## Problem

Benchmark audit (F4): rule SUB-04 có grace period nhưng KHÔNG rule nào mô tả hành động ở CUỐI grace cho một subscription PAID (đã từng trả tiền, nay không gia hạn). Thiếu spec involuntary-churn → không rõ PAID sub hết grace thì suspend ra sao, retention bao lâu, dữ liệu thế nào. Mở rộng GAP-1016 / GAP-1017 (voluntary cancel) sang nhánh involuntary.

## Proposed Fix

Bổ sung rule mô tả involuntary-churn path: PAID sub hết grace → auto-suspend + retention window + lộ trình reactivate. Implement scheduler tương ứng.

## Acceptance Criteria

- [ ] Rule mô tả rõ PAID-sub-end-of-grace → suspend action + retention
- [ ] Scheduler auto-suspend PAID sub quá grace
- [ ] Phân biệt rõ involuntary (hết hạn) vs voluntary (chủ động hủy)

## Related

- Audit: `documents/04-quality/audits/persona-review/2026-06-13-pre-wave-kitehub-biz-100-benchmark.md` (F4)
- Parent: GAP-1016, GAP-1017 (voluntary cancel lifecycle)
- Sister: GAP-1259 (grace dunning), GAP-1264 (retention clock)
