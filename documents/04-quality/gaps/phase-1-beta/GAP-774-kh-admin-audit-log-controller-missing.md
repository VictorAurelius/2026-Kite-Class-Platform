---
audience: dev
---

# GAP-774 — KH admin audit-log controller missing (Mảng D4 blocker)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-05-27 (Wave 106 RST Mảng D4 probe)
**Affects:** D4 Xem nhật ký audit — đăng nhập + hành động nhạy cảm
**Phase:** phase-1-beta

## Problem

Plan §3 D4 nói "V62/V63 đã ship" — verify:

```bash
docker exec kite-postgres psql -U kitehub -d kitehub -c \
  "SELECT action, COUNT(*) FROM admin_audit_log GROUP BY action;"
# Result: BETA_REQUEST_APPROVE | 3 — table populated ✅
```

DB schema OK. NHƯNG catalog probe `grep @RequestMapping kitehub-admin/**/*Controller.java`:

```
@RequestMapping("/api/platform/admin")
@RequestMapping("/api/v1/admin/instances")
@RequestMapping("/api/v1/admin/payments")
@RequestMapping("/api/v1/admin/revenue")
# KHÔNG có /api/v1/admin/audit-logs hoặc tương đương
```

Probe `GET /api/v1/admin/audit-logs` → 404.
FE catalog `find kitehub-frontend/src/app/(admin)/admin -type d`:
```
admin/payments, admin/instances, admin/staff, admin/beta-requests, admin/revenue
# KHÔNG có admin/audit-logs
```

→ D4 luồng: dữ liệu có, nhưng không có endpoint nào để Admin Mai xem được.

## Root Cause

Schema (V62/V63) shipped trước UI — implementation gap. Plan §3 D4 trust "ship V62/V63 = D4 ready" nhưng cần thêm Controller + FE page.

## Proposed Fix

1. `AdminAuditLogController` trong `kitehub-admin/src/main/java/.../controller/`:
   - `GET /api/v1/admin/audit-logs` — paginated + filter (date range, action type, admin_id)
   - Role-guard: PLATFORM_ADMIN only
   - Response DTO: id, action, performed_by, target_resource, timestamp, ip, user_agent, payload
2. FE page `kitehub-frontend/src/app/(admin)/admin/audit-logs/page.tsx`:
   - Table với filter + pagination
   - Vietnamese label per `vn-localization-audit-checklist.md` §2
3. Integration test: VN diacritic preserve trong action narrative (per `vn-localization-audit-checklist.md` v1.1.0 §5)

## Acceptance Criteria

- [ ] `AdminAuditLogController` ship với GET endpoint + role-guard
- [ ] FE page render với 3 BETA_REQUEST_APPROVE events visible
- [ ] D4 luồng walk PASS end-to-end (admin login → /admin/audit-logs → see entries)

## Related

- Wave 106 plan §3 D4
- DB schema: V62__admin_audit_log + V63__admin_audit_logs (verify migration files)
- Sister: GAP-770 (META audit Wave 92 closure scope-completeness — Wave 92 ship V62/V63 nhưng UI deferred → orphan item)
