# GAP-1265: Notification channel fallback Zalo OA / in-app ngoài email

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Mixed
**Found:** 2026-06-13 (Wave kitehub-biz-100 outside-in audit)
**Affects:** `kitehub-email` / notification dispatch + owner-facing in-app banner

## Problem

Persona audit (F10): thông báo lifecycle (gia hạn, suspend, confirm payment) chỉ gửi qua email → bỏ lọt owner dùng Zalo là kênh chính (văn hóa VN) → owner không thấy thông báo → involuntary churn. Mở rộng GAP-063 (Zalo integration).

## Proposed Fix

Thêm fallback channel: Zalo OA và/hoặc in-app persistent banner cho các thông báo lifecycle quan trọng, ngoài email.

## Acceptance Criteria

- [ ] Thông báo lifecycle có ≥1 fallback channel ngoài email (Zalo OA hoặc in-app)
- [ ] In-app banner persistent cho thông báo critical (sắp suspend, chờ xác nhận)

## Related

- Audit: `documents/04-quality/audits/persona-review/2026-06-13-pre-wave-kitehub-biz-100-persona.md` (F10)
- Parent: GAP-063 (Zalo OA integration)
- Sister: GAP-1257 (pending status notify), GAP-1259 (dunning reminders)
