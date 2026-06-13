# GAP-1257: Pending-payment 'đang chờ xác nhận' — màn trạng thái + SLA + thông báo cho owner (admin-confirm vô hình)

**Status:** 🟢 DONE 2026-06-14
**Priority:** 🟠 P1
**Domain:** Mixed
**Found:** 2026-06-13 (Wave kitehub-biz-100 outside-in audit)
**Affects:** `kitehub-frontend` billing + `kitehub-subscription` payment confirm flow

## Problem

Persona audit (F1 + F7) + benchmark audit (F1): owner chuyển khoản VietQR xong nhưng KHÔNG có màn trạng thái 'Đang chờ xác nhận', không có SLA cam kết thời gian confirm, và không có thông báo khi admin đã confirm. Toàn bộ bước admin-confirm là vô hình với owner → owner hoang mang không biết tiền đã tới chưa, đã được duyệt chưa.

## Proposed Fix

Thêm màn 'Đang chờ xác nhận' hiển thị sau khi owner báo đã CK + SLA (vd 'xác nhận trong 24h làm việc') + gửi thông báo (email/Zalo/in-app) khi admin confirm payment.

## Acceptance Criteria

- [x] Sau khi owner submit báo CK → FE hiển thị trạng thái 'Đang chờ xác nhận' + SLA
- [x] Khi admin confirm → owner nhận thông báo (kênh khả dụng)
- [x] Trạng thái pending hiển thị nhất quán trong billing portal

## Related

- Audit: `documents/04-quality/audits/persona-review/2026-06-13-pre-wave-kitehub-biz-100-persona.md` (F1, F7)
- Audit: `documents/04-quality/audits/persona-review/2026-06-13-pre-wave-kitehub-biz-100-benchmark.md` (F1)
- Sister: GAP-1259 (pending TTL + dunning), GAP-1265 (notification fallback), GAP-1266 (receipt)

## Closure — wave-kitehub-biz-100 (2026-06-14)

🟢 DONE — engineering-complete + G3 production-parity walk verified. BE-4 — GET pending-payment-status endpoint + SLA + owner notify.

- G3 walk: `documents/04-quality/audits/persona-review/2026-06-13-g3-walk-kitehub-biz-100.md` (8 PASS / 1 PASS-with-P1 closed via GAP-1273 / 1 win-back async by-design).
- Tests: 963 backend (kitehub-subscription) + 906 frontend green.
- Consolidated into PR branch `wave/kitehub-biz-100`.
