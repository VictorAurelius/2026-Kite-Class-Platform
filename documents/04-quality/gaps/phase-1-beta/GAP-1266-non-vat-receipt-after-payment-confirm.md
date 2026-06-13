# GAP-1266: Biên nhận non-VAT sau khi admin confirm payment

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-06-13 (Wave kitehub-biz-100 outside-in audit)
**Affects:** `kitehub-subscription` confirmPayment + `kitehub-email` receipt template

## Problem

Benchmark audit (F6): owner chuyển khoản và được admin confirm nhưng KHÔNG nhận biên nhận nào → thiếu bằng chứng giao dịch, giảm tin tưởng. VAT e-invoice đầy đủ là Phase 1.5+ (GAP-185 / GAP-634); gap này chỉ là biên nhận đơn giản non-VAT.

## Proposed Fix

Sinh biên nhận non-VAT (PDF hoặc email) ngay sau `confirmPayment`: gồm số tiền, gói, chu kỳ, ngày, mã giao dịch.

## Acceptance Criteria

- [ ] Sau confirmPayment → sinh biên nhận non-VAT (PDF/email)
- [ ] Biên nhận có amount, tier, billing cycle, ngày, mã GD

## Related

- Audit: `documents/04-quality/audits/persona-review/2026-06-13-pre-wave-kitehub-biz-100-benchmark.md` (F6)
- Related (defer Phase 1.5+): GAP-185, GAP-634 (VAT e-invoice MISA)
- Sister: GAP-1257 (payment confirm flow), GAP-1267 (billing portal receipt download)
