# Role Hierarchy — Business Rules

**Domain:** role-hierarchy
**Source:** GAP-058, ADR-003

## Rules

### Permission
| ID | Rule |
|----|------|
| BR-PERM-001 | Name globally unique per tenant |
| BR-PERM-002 | System permissions cannot be deleted by tenant |
| BR-PERM-003 | Category grouping (STUDENT/GRADE/PAYROLL/etc.) |

### Role
| ID | Rule |
|----|------|
| BR-ROLE-001 | Name unique per tenant |
| BR-ROLE-002 | System roles pre-seeded + not deletable |
| BR-ROLE-003 | Level 1-10 (1=top, 10=lowest) |
| BR-ROLE-004 | Can have 1 parent (tree structure) |
| BR-ROLE-005 | Can bundle multiple Permissions |

### UserRole
| ID | Rule |
|----|------|
| BR-UR-001 | Unique (userId, roleId) — 1 assignment per pair |
| BR-UR-002 | assignedAt recorded for audit |
| BR-UR-003 | User có thể có nhiều roles; effective permissions = union |

## Pre-defined Role Templates (seeded by RoleSeederService)

```
Level 1: TENANT_OWNER
Level 2: PRINCIPAL / DIRECTOR / CEO
Level 3: VICE_PRINCIPAL / VP_ACADEMIC / VP_ADMIN
Level 4: DEPT_HEAD / BRANCH_MANAGER
Level 5: HOMEROOM_TEACHER (GVCN) / SUBJECT_TEACHER
Level 6: ACCOUNTANT / RECEPTIONIST
Level 7: STUDENT
Level 8: PARENT
```

## Permission Categories (seeded)

- STUDENT: VIEW_ALL, VIEW_OWN, CREATE, EDIT, DELETE
- TEACHER: VIEW_ALL, ASSIGN_CLASS, MANAGE
- CLASS: VIEW_ALL, CREATE, EDIT_SCHEDULE
- GRADE: EDIT_OWN, EDIT_ALL, FINALIZE
- PAYROLL: VIEW, APPROVE
- USER: MANAGE, ASSIGN_ROLE
- BRANDING: EDIT, REGENERATE
- FINANCIAL: VIEW_REPORTS, APPROVE_INVOICES

## Log
- 2026-04-14 — Initial rules (GAP-058, ADR-003)
