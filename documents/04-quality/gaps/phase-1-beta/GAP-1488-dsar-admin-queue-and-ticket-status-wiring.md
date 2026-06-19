# GAP-1488: DSAR DPO admin queue page + public ticket live-status BE wiring

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Mixed (Backend + Frontend)
**Found:** 2026-06-19 (PR #2508 BE→FE URL contract fix — GAP-1414 follow-up)
**Affects:** `kitehub-frontend` admin + `(public)/legal/data-rights/status` + `kitehub-subscription` DSAR

## Problem

GAP-1414 (PR #2508) để lộ 2 email link DSAR (PDPL Điều 14) trỏ tới FE route 404:
- `dpoQueueUrl = appBaseUrl + "/admin/dsar"` (email `dsar-acknowledgement-requester` — DPO queue nội bộ)
- `statusCheckUrl = appBaseUrl + "/legal/data-rights/status?id=<ticketUuid>"` (requester tra cứu trạng thái, reach non-user)

Fix tạm (PR #2508):
- `/admin/dsar` → map literal sang `/admin` (admin dashboard có sẵn; DPO tự điều hướng).
- `/legal/data-rights/status` → thêm trang public **stub** đọc `?id`, hiển thị mã ticket + hướng dẫn liên hệ `dpo@kitehub.me`.

**Còn thiếu:** (a) trang admin DSAR queue chuyên biệt cho DPO; (b) endpoint tra cứu trạng thái ticket trực tuyến theo `ticketUuid`.

## Proposed Fix

- FE: trang `(admin)/admin/dsar` — danh sách ticket DSAR + SLA + trạng thái; đổi `dpoQueueUrl` về `/admin/dsar`.
- BE: endpoint `GET /api/platform/dsar/{ticketUuid}/status` (public, an toàn — chỉ trả trạng thái + SLA, không lộ PII).
- FE: trang `(public)/legal/data-rights/status` gọi endpoint, hiển thị trạng thái live (đã có scaffold đọc `searchParams.id`).

## Acceptance Criteria

- [ ] `/admin/dsar` DPO queue page liệt kê ticket + status + SLA; `dpoQueueUrl` trỏ về `/admin/dsar`
- [ ] `GET .../dsar/{id}/status` trả trạng thái + SLA, không lộ PII
- [ ] Trang public status hiển thị trạng thái live theo ticket id

## Related

- Parent: GAP-1414 (email URL config base) + PR #2508
- Sibling: GAP-1487 (unsubscribe page BE wiring — cùng class email-link-stub)
- DSAR self-service form có sẵn: `(public)/legal/data-rights` (GAP-353c)
