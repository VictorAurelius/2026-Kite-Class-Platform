# GAP-1257: Pending-payment 'đang chờ xác nhận' — màn trạng thái + SLA + thông báo cho owner (admin-confirm vô hình)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Mixed
**Found:** 2026-06-13 (Wave kitehub-biz-100 outside-in audit)
**Affects:** `kitehub-frontend` billing + `kitehub-subscription` payment confirm flow

## Problem

Persona audit (F1 + F7) + benchmark audit (F1): owner chuyển khoản VietQR xong nhưng KHÔNG có màn trạng thái 'Đang chờ xác nhận', không có SLA cam kết thời gian confirm, và không có thông báo khi admin đã confirm. Toàn bộ bước admin-confirm là vô hình với owner → owner hoang mang không biết tiền đã tới chưa, đã được duyệt chưa.

## Proposed Fix

Thêm màn 'Đang chờ xác nhận' hiển thị sau khi owner báo đã CK + SLA (vd 'xác nhận trong 24h làm việc') + gửi thông báo (email/Zalo/in-app) khi admin confirm payment.

## Acceptance Criteria

- [ ] Sau khi owner submit báo CK → FE hiển thị trạng thái 'Đang chờ xác nhận' + SLA
- [ ] Khi admin confirm → owner nhận thông báo (kênh khả dụng)
- [ ] Trạng thái pending hiển thị nhất quán trong billing portal

## Related

- Audit: `documents/04-quality/audits/persona-review/2026-06-13-pre-wave-kitehub-biz-100-persona.md` (F1, F7)
- Audit: `documents/04-quality/audits/persona-review/2026-06-13-pre-wave-kitehub-biz-100-benchmark.md` (F1)
- Sister: GAP-1259 (pending TTL + dunning), GAP-1265 (notification fallback), GAP-1266 (receipt)
