---
audience: dev
---

# GAP-871 — KH admin audit-log dashboard FE page

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Frontend
**Found:** 2026-06-02 (GAP-774 BE wave — FE deferral follow-up)
**Affects:** D4 Xem nhật ký audit — admin xem đăng nhập + hành động nhạy cảm
**Phase:** phase-1-beta

## Problem

GAP-774 đã ship backend `AdminAuditLogController` (`GET /api/v1/admin/audit-logs` list paginated/filtered + `GET /api/v1/admin/audit-logs/{id}` detail — `hasRole('PLATFORM_ADMIN')`, filter action/adminUserId/from/to). NHƯNG chưa có FE page tiêu thụ. Admin (PLATFORM_ADMIN) cần page xem nhật ký audit.

Catalog hiện tại `kitehub-frontend/src/app/(admin)/admin/`: payments, instances, staff, beta-requests, revenue. KHÔNG có `admin/audit-logs`.

## Proposed Fix

`kitehub-frontend/src/app/(admin)/admin/audit-logs/page.tsx`:
- Table với cột: thời gian, hành động, người thực hiện, đối tượng, IP, trạng thái.
- Filter: khoảng ngày (from/to), loại hành động (action), admin.
- Pagination (page size 20, max 100 — khớp BE clamp).
- Nhãn tiếng Việt per `vn-localization-audit-checklist.md` §2; VN diacritic preserve trong action narrative.
- Menu item Admin-only (FE role guard) — khớp BE `hasRole('PLATFORM_ADMIN')`.
- Row detail (expand/modal) hiển thị `payloadJson` / `beforeState` / `afterState`.

## Acceptance Criteria

- [ ] `(admin)/admin/audit-logs/page.tsx` render table + filter + pagination
- [ ] 3 BETA_REQUEST_APPROVE events visible (seeded V62/V63 data)
- [ ] Nhãn tiếng Việt + VN diacritic preserve trong narrative
- [ ] FE role guard: menu/route PLATFORM_ADMIN-only
- [ ] **Live RST walk end-to-end** per `feature-ship-runtime-walk-mandate.md` trên full stack: admin login → /admin/audit-logs → see entries → filter works → non-admin bị chặn (deferred do isolated worktree không có stack)

## Related

- GAP-774 (BE AdminAuditLogController — PARTIAL, branch `feature/GAP-774-kh-admin-audit-log`)
- DB schema: V62/V63 admin_audit_log (3 BETA_REQUEST_APPROVE rows seeded)
- `vn-localization-audit-checklist.md` §2 (VN label)
- Sister pattern: GAP-775 (BE) → GAP-865 (FE) mirror
