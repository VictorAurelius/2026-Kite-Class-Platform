# GAP-521: Admin action audit log

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (PDPL compliance + incident forensics)
**Domain:** Backend
**Found:** 2026-05-13 (Wave 71c per `pre-launch-auth-hardening-checklist.md` §2.7)

## Problem

Không có audit log cho PLATFORM_ADMIN actions (approve/reject beta requests, suspend instances, edit config). Incident response không reconstruct được "ai làm gì lúc nào".

## Proposed Fix

1. `admin_audit_log` table: id, admin_user_id, action, target_entity_type, target_entity_id, request_ip, user_agent, created_at, payload_json
2. `@AuditableAdminAction` annotation + AOP interceptor on admin controllers
3. Index on (admin_user_id, created_at)
4. Retention 7 năm per PDPL retention base; archive older monthly

## Acceptance Criteria

- [ ] Migration + entity + repository
- [ ] Interceptor catches approve/reject/suspend/edit actions
- [ ] Admin UI page `/admin/audit-log` for review
- [ ] Unit + IT tests

## Related

- Rule: `pre-launch-auth-hardening-checklist.md` §2.7
- PDPL retention compliance overlap
