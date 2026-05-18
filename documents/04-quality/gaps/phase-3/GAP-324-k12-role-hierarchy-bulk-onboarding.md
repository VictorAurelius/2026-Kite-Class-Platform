# GAP-324: K-12 Multi-tenant Role Hierarchy Bulk-Onboarding

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Backend
**Detected:** 2026-05-04 (Wave 17 Bucket D)
**Related:** P5-k12-school.md AC-ONBOARD-001; existing GAP-058

## Current State (verified 2026-05-04)

```bash
grep -rl "Role\b" kiteclass/kiteclass-core/src/main/java --include="*.java" | head -5
```
Existing roles: flat admin/teacher/student. K-12 needs: Hiệu trưởng + 2 Phó HT (CM + CSVCH) + Tổ trưởng (multiple) + GVCN + GV bộ môn + Văn thư + Kế toán + Y tế + Bảo vệ + Lao công + Thư viện. **Missing.**

## Problem

Without K-12 hierarchy + bulk onboarding, AC-ONBOARD-001 (8h SLA for 65 staff) FAIL. Cannot model real K-12 org chart. RBAC scope (Phó CM sees lịch dạy, Tổ trưởng sees giáo án bộ môn) impossible.

## Proposed Fix

1. **K-12 role enum:** Extend `Role` with `PRINCIPAL`, `VICE_PRINCIPAL_CURRICULUM`, `VICE_PRINCIPAL_FACILITIES`, `DEPARTMENT_HEAD`, `HOMEROOM_TEACHER (GVCN)`, `SUBJECT_TEACHER`, `RECORDS_CLERK`, `ACCOUNTANT`, `SCHOOL_NURSE`, `SECURITY_GUARD`, `LIBRARIAN`
2. **Role hierarchy:** `RoleHierarchy` mapping for RBAC scope inheritance
3. **Bulk import xlsx:** with role + department assignment + auto credential dispatch (email + SMS + Zalo)
4. **8h SLA:** queue-based dispatch + status dashboard for HR

## Acceptance Criteria

- [ ] K-12 role enum migration shipped
- [ ] RBAC scope per role tested (Phó CM sees scope, Tổ trưởng sees department)
- [ ] Bulk xlsx import 65 staff completes ≤8 working hours
- [ ] Credentials auto-dispatched all channels
- [ ] Test: import 65-row xlsx → 65 accounts + role assignment + role-scope verified
- [ ] Documentation 3-layer
- [ ] business-logic-review.md 5-attribute

## Related

- **Consolidates K-12 scope of:** GAP-058
- **Blocks:** GAP-322 (vetting needs role), GAP-329 (substitute), GAP-333 (signoff chain)
- **Wave plan:** Bucket D Stage 3

## Log

- **2026-05-04** — Filed Wave 17 Bucket D. State-check: flat roles only; no hierarchy.
