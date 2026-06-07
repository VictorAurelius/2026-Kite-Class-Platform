# GAP-1029: Admin audit completeness — suspend/activate không ghi audit + table drift admin_audit_log vs admin_audit_logs

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-06 (KH-9 admin console G1 walk)
**Affects:** `AdminController` suspend/activate + `admin_audit_log`/`admin_audit_logs` schema (kitehub-admin/subscription)

## Problem

KH-9 G1 walk catalog 2 audit-integrity issue (OWASP A09 logging):

1. **Suspend/activate KHÔNG ghi audit row (FM-2):** `AdminController.suspendInstance` + `activateInstance` không có `@Auditable` (beta-request approve/reject + impersonation thì CÓ). Walk evidence: suspend instance → 200 + DB status SUSPENDED, nhưng `admin_audit_log` không có row mới. Admin mutation nghiêm trọng (suspend tenant) không vào audit trail → vi phạm A09 + PDPL accountability.

2. **Table drift admin_audit_log (singular) vs admin_audit_logs (plural) (FM-1):** DB có CẢ HAI table. `AdminAuditLog` entity map `@Table(name="admin_audit_log")` (singular, V36). V50 immutability/RLS hardening target `admin_audit_logs` (plural). → immutability (no-UPDATE/no-DELETE trigger + RLS) áp lên table KHÔNG được code dùng → audit table thực tế (singular) có thể KHÔNG tamper-proof. PDPL Art 11 immutability có thể là no-op.

## Root Cause

(1) suspend/activate thiếu `@Auditable` annotation. (2) Migration V36 (singular) + V50 (plural) tạo 2 table, entity dùng singular nhưng hardening trên plural — drift chưa reconcile.

## Proposed Fix

1. Add `@Auditable(action="INSTANCE_SUSPEND"/"INSTANCE_ACTIVATE")` cho suspend/activate (+ sweep các admin mutation khác thiếu audit).
2. Reconcile table: xác định table canonical (singular admin_audit_log code dùng) → apply V50 immutability/RLS lên ĐÚNG table đó; drop/migrate table thừa. Verify trigger no-UPDATE/no-DELETE active trên table code thực dùng.

## Acceptance Criteria

- [ ] Suspend/activate → admin_audit_log row created (action + admin_user_id + target)
- [ ] Cross-flow sweep: mọi admin mutation @Auditable
- [ ] Immutability (no UPDATE/DELETE) active trên table code dùng (verify trigger)
- [ ] Single canonical audit table (no drift)

## Related

- Discovered in: KH-9 G1 walk — `documents/04-quality/audits/persona-review/2026-06-06-pre-walk-kh9-admin-console.md` (FM-1 + FM-2)
- Related: GAP-1028 (audit-log read 500); pre-launch-owasp-rest-hardening-checklist §2.8 A09

## Log

- **2026-06-07** (Wave g2-blockers-1 Bucket A, inline — investigation, NOT yet fixed): Để OPEN sau khi GAP-1028 (sister) shipped. Investigation findings cho next session (tránh re-discover):
  - **Part 1 (@Auditable suspend/activate):** suspend/activate ở `kitehub-admin/.../controller/AdminController.java` (suspendInstance line 140, activateInstance line 163). `@Auditable` annotation + `AdminAuditAspect` ở `kitehub-subscription/.../audit/`. kitehub-admin CÓ depend kitehub-subscription (pom.xml line 27) → annotation importable. **RISK chưa verify:** `AdminAuditAspect` (@Aspect) có được component-scan + active trong Spring context của kitehub-admin app không — nếu admin `@SpringBootApplication` chỉ scan `com.kitehub.admin`, aspect ở `com.kitehub.subscription.audit` KHÔNG fire → `@Auditable` thành no-op silent. PHẢI verify aspect active (hoặc add scan / re-declare aspect bean) TRƯỚC khi tin @Auditable ghi row. IT cross-module bắt buộc.
  - **Part 2 (table drift):** V36 `admin_audit_log` (singular, entity `@Table(name="admin_audit_log")` dùng) + V54 enrichment singular. V50 `admin_audit_logs` (plural) = RLS/immutability target. Cần: xác nhận table nào code thực dùng (singular per entity) → apply immutability (no-UPDATE/no-DELETE trigger + RLS FORCE) lên ĐÚNG singular table qua migration mới; drop/migrate plural thừa. Security-sensitive (PDPL Art 11) — fresh-context care.
  - **Status: 🔵 OPEN** — deferred từ Bucket A vì cross-module aspect risk + PDPL-immutability migration cần verify cẩn thận (không rush vào high-context). GAP-1028 sister đã ship riêng.
