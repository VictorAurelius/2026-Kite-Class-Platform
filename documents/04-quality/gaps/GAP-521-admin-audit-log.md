# GAP-521: Admin action audit log

**Status:** 🟡 PARTIAL (70% — Wave 72a Bucket B PR #1287)
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

- [x] Migration + entity + repository (`V36__create_admin_audit_log.sql` + `AdminAuditLog` + `AdminAuditLogRepository` — Wave 72a Bucket B PR #1287)
- [x] Interceptor catches approve/reject actions on BetaAccessController (`@Auditable` + `AdminAuditAspect`); IP + UA + redacted JSON payload persisted
- [ ] Other admin controllers annotated (suspend instance, edit config, AdminEmailController, etc.) — follow-up GAP
- [ ] Admin UI page `/admin/audit-log` for review (FE work — deferred to follow-up GAP)
- [x] Unit tests (`AdminAuditAspectTest` — 3 cases: success / sensitive-redaction / failure-propagation)

## Related

- Rule: `pre-launch-auth-hardening-checklist.md` §2.7
- PDPL retention compliance overlap
- PR: #1287 (Wave 72a Bucket B)

## Log

- **2026-05-14** Wave 72a Bucket B PR #1287 ships BE foundation: V36 migration + entity + repository + `@Auditable` annotation + `AdminAuditAspect` (Spring AOP `@Around` — captures SecurityContext principal, ServletRequest IP+UA, redacts password/token/secret/jwt sub-fields in JSON payload). Applied to `BetaAccessController.approve` + `.reject`. Status → 🟡 PARTIAL pending other admin controller annotations + FE review UI.
