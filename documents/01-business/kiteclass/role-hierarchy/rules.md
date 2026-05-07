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

## Five-attribute review per `business-logic-review.md`

Per-rule attributes (Source / Rationale / Reviewer / Compliance check / Review cadence) backfilled at file-level placeholder per Phase 1 of GAP-433. Per-rule granularity tracked via GAP-156 Phase 2 stakeholder sign-offs.

- **Source:** Existing rules in this file derive from a mix of: feature gaps cited inline (where present), ADRs, persona reviews, and informed-gut estimates from Wave 1-30 work. Rules without inline citation default to `informed gut` per `business-logic-review.md` §2.1 and inherit quarterly re-review obligation below.
- **Rationale:** Rule values reflect product judgment + (where applicable) competitor benchmarks + VN regulatory minimums. Detailed per-rule rationale to be backfilled during GAP-156 Phase 2 stakeholder review; until then, treat values as `informed gut` subject to next quarterly review.
- **Reviewer:** @nguyenvankiet (acting Product Owner, solo-dev, 2026-05-08). Formal stakeholder + legal counsel sign-off queued via GAP-156. Solo-dev exemption per `business-logic-review.md` §2.3 — the Reviewer line documents which hat is being worn AND obligation is attached for team-growth or pre-launch trigger.
- **Compliance check:** **Considered** — Luật An ninh mạng 2018 Art 26 (RBAC baseline); OWASP A01.
- **Review cadence:** Quarterly (default per `business-logic-review.md` §2.5). **Next review:** 2026-08-08. Event triggers: Role added/removed, RBAC audit.

## Log
- 2026-04-14 — Initial rules (GAP-058, ADR-003)
