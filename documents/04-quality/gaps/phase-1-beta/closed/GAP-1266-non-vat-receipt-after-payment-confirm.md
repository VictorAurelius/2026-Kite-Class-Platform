# GAP-1266: Biên nhận non-VAT sau khi admin confirm payment

**Status:** 🟢 DONE 2026-06-14
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-06-13 (Wave kitehub-biz-100 outside-in audit)
**Affects:** `kitehub-subscription` confirmPayment + `kitehub-email` receipt template

## Problem

Benchmark audit (F6): owner chuyển khoản và được admin confirm nhưng KHÔNG nhận biên nhận nào → thiếu bằng chứng giao dịch, giảm tin tưởng. VAT e-invoice đầy đủ là Phase 1.5+ (GAP-185 / GAP-634); gap này chỉ là biên nhận đơn giản non-VAT.

## Proposed Fix

Sinh biên nhận non-VAT (PDF hoặc email) ngay sau `confirmPayment`: gồm số tiền, gói, chu kỳ, ngày, mã giao dịch.

## Acceptance Criteria

- [x] Sau confirmPayment → sinh biên nhận non-VAT (PDF/email)
- [x] Biên nhận có amount, tier, billing cycle, ngày, mã GD

## Related

- Audit: `documents/04-quality/audits/persona-review/2026-06-13-pre-wave-kitehub-biz-100-benchmark.md` (F6)
- Related (defer Phase 1.5+): GAP-185, GAP-634 (VAT e-invoice MISA)
- Sister: GAP-1257 (payment confirm flow), GAP-1267 (billing portal receipt download)

## Closure — wave-kitehub-biz-100 (2026-06-14)

🟢 DONE — engineering-complete + G3 production-parity walk verified. BE-4 — non-VAT receipt sau admin confirm payment.

- G3 walk: `documents/04-quality/audits/persona-review/2026-06-13-g3-walk-kitehub-biz-100.md` (8 PASS / 1 PASS-with-P1 closed via GAP-1273 / 1 win-back async by-design).
- Tests: 963 backend (kitehub-subscription) + 906 frontend green.
- Consolidated into PR branch `wave/kitehub-biz-100`.
