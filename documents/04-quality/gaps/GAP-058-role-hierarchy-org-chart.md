# GAP-058: Role Hierarchy + Organizational Chart

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend / Security
**Detected:** 2026-04-14 (persona review)
**Persona blocked:** P3 Medium Center, P4 Chain, P5 School

## Problem

Hiện tại role flat: OWNER, ADMIN, TEACHER, STUDENT. Không đủ cho org phức tạp:
- **School:** Principal → VP Academic / VP Admin → Dept Heads → Teachers
- **Chain:** CEO → Regional Dir → Branch Manager → Staff
- **Center:** Director → Academic Dir / Finance Dir → Teachers

Không có role hierarchy → không map được permission đúng đắn.

## Proposed Fix

### Hierarchical roles

```java
@Entity
public class Role {
  String name;
  Role parent;  // hierarchy
  Set<Permission> permissions;
  Integer level;  // 1=CEO, 2=VP, 3=Dept Head, ...
}

Pre-defined role templates:
- PLATFORM_ADMIN (KiteHub)
- TENANT_OWNER (top)
- PRINCIPAL / DIRECTOR
- VICE_PRINCIPAL / DEPT_DIRECTOR
- DEPT_HEAD / TEAM_LEAD
- HOMEROOM_TEACHER (GVCN)
- SUBJECT_TEACHER
- ACCOUNTANT
- RECEPTIONIST
- STUDENT
- PARENT
```

### Permission model

Granular permissions:
- VIEW_ALL_CLASSES / VIEW_OWN_CLASSES
- EDIT_GRADES_OWN / EDIT_GRADES_ALL
- APPROVE_PAYROLL
- MANAGE_USERS
- VIEW_FINANCIALS
- etc.

Role = bundle of permissions. User có nhiều roles (e.g., vừa là Teacher vừa là Dept Head).

### Org Chart UI

Visual tree:
```
Hiệu trưởng (Principal)
├── P.Hiệu trưởng phụ trách chuyên môn
│   ├── Tổ trưởng Toán
│   │   ├── GV Toán 1
│   │   └── GV Toán 2
│   └── Tổ trưởng Văn
│       └── GV Văn
└── P.Hiệu trưởng phụ trách hành chính
    ├── Thủ quỹ
    └── Văn thư
```

### Approval workflows

Per org level, define approval chains:
- Leave request: Teacher → Dept Head → VP → Principal
- Payroll: Accountant → Finance Dir → Principal → Tenant Owner

## Acceptance Criteria

- [ ] Hierarchical Role entity
- [ ] Permission granularity (50+ permissions)
- [ ] Pre-populated role templates
- [ ] User-role many-to-many
- [ ] Org chart visualization UI
- [ ] Approval workflow engine
- [ ] Migration: existing flat roles → hierarchical

## Dependencies

- Affects all authorization throughout platform
- GAP-041 (security) — role enforcement

## Log
- 2026-04-14 — Persona review
