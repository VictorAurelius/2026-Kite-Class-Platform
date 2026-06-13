# GAP-1263: Phân biệt fraud-block vs voluntary-cancel; re-signup/reactivate + win-back outreach

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Mixed
**Found:** 2026-06-13 (Wave kitehub-biz-100 outside-in audit)
**Affects:** `kitehub-subscription` off-boarding (OFF-15 tombstone, TR-07 re-trial-block) + signup

## Problem

Persona audit (F4) + benchmark audit (F7): cơ chế tombstone (rule OFF-15) + re-trial-block (rule TR-07) chặn cả khách hàng quay lại chính đáng (đã hủy tự nguyện, nay muốn dùng lại) lẫn fraud. Không phân biệt fraud-block vs voluntary-cancel → mất cơ hội win-back KH cũ.

## Proposed Fix

Phân biệt fraud vs voluntary cancel; cho phép reactivate bằng identifier cũ cho voluntary-cancel + thêm win-back offer. Giữ block chỉ cho fraud thật.

## Acceptance Criteria

- [ ] Hệ thống phân loại off-boarding reason: fraud vs voluntary
- [ ] Voluntary-cancel cho phép re-signup/reactivate bằng identifier cũ
- [ ] Có win-back offer cho KH quay lại

## Related

- Audit: `documents/04-quality/audits/persona-review/2026-06-13-pre-wave-kitehub-biz-100-persona.md` (F4)
- Audit: `documents/04-quality/audits/persona-review/2026-06-13-pre-wave-kitehub-biz-100-benchmark.md` (F7)
- Sister: GAP-1268 (cancel wizard), GAP-1260 (involuntary churn)
