# GAP-1263: Phân biệt fraud-block vs voluntary-cancel; re-signup/reactivate + win-back outreach

**Status:** 🟢 DONE 2026-06-14
**Priority:** 🟠 P1
**Domain:** Mixed
**Found:** 2026-06-13 (Wave kitehub-biz-100 outside-in audit)
**Affects:** `kitehub-subscription` off-boarding (OFF-15 tombstone, TR-07 re-trial-block) + signup

## Problem

Persona audit (F4) + benchmark audit (F7): cơ chế tombstone (rule OFF-15) + re-trial-block (rule TR-07) chặn cả khách hàng quay lại chính đáng (đã hủy tự nguyện, nay muốn dùng lại) lẫn fraud. Không phân biệt fraud-block vs voluntary-cancel → mất cơ hội win-back KH cũ.

## Proposed Fix

Phân biệt fraud vs voluntary cancel; cho phép reactivate bằng identifier cũ cho voluntary-cancel + thêm win-back offer. Giữ block chỉ cho fraud thật.

## Acceptance Criteria

- [x] Hệ thống phân loại off-boarding reason: fraud vs voluntary
- [x] Voluntary-cancel cho phép re-signup/reactivate bằng identifier cũ
- [x] Có win-back offer cho KH quay lại

## Related

- Audit: `documents/04-quality/audits/persona-review/2026-06-13-pre-wave-kitehub-biz-100-persona.md` (F4)
- Audit: `documents/04-quality/audits/persona-review/2026-06-13-pre-wave-kitehub-biz-100-benchmark.md` (F7)
- Sister: GAP-1268 (cancel wizard), GAP-1260 (involuntary churn)

## Closure — wave-kitehub-biz-100 (2026-06-14)

🟢 DONE — engineering-complete + G3 production-parity walk verified. G3 walk #9 — reactivate → PAYMENT_REQUIRED (churnType VOLUNTARY); win-back seam wired + unit-tested 3/3 (OwnerNotificationDispatcher IN_APP+EMAIL).

- G3 walk: `documents/04-quality/audits/persona-review/2026-06-13-g3-walk-kitehub-biz-100.md` (8 PASS / 1 PASS-with-P1 closed via GAP-1273 / 1 win-back async by-design).
- Tests: 963 backend (kitehub-subscription) + 906 frontend green.
- Consolidated into PR branch `wave/kitehub-biz-100`.


## Out-of-scope (Phase 1.5+)

| Item | Tracking |
|---|---|
| Live-cron win-back side-effect verify | Phase 1.5 (scheduler-driven; walk #8 async by-design) |
