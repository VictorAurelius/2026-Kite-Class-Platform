# ADR-003: Hierarchical Role-Based Access Control

**Status:** ACCEPTED
**Date:** 2026-04-14
**Deciders:** Tech Lead + Security Lead
**Related Gap:** GAP-058

## Context

Current roles flat: OWNER, ADMIN, TEACHER, STUDENT, PARENT.

Insufficient cho:
- **School:** Principal → VP → Dept Head → Teacher → GVCN
- **Chain:** CEO → Regional Dir → Branch Mgr → Staff
- **Center:** Director → Academic/Finance Dir → Teacher

Flat roles → cannot express:
- Approval chains (teacher → dept head → principal)
- Department-scoped access
- GVCN vs subject teacher distinction
- Granular permissions (view-only vs edit)

## Decision

**Replace flat role enum với hierarchical Role entity + granular Permission set.**

```
Role
├── name
├── parent: Role (tree)
├── level: 1=top, 10=lowest
└── permissions: Set<Permission>

Permission (granular, 50+ types)
├── VIEW_ALL_CLASSES vs VIEW_OWN_CLASSES
├── EDIT_GRADES_OWN vs EDIT_GRADES_ALL
├── APPROVE_PAYROLL
├── MANAGE_USERS
└── ...

User has Set<Role> (composable)
```

Pre-populated templates:
- PLATFORM_ADMIN, TENANT_OWNER
- PRINCIPAL, VICE_PRINCIPAL, DEPT_HEAD
- HOMEROOM_TEACHER (GVCN), SUBJECT_TEACHER
- ACCOUNTANT, RECEPTIONIST
- STUDENT, PARENT

Composite Pattern: org chart = role tree visualization.

## Consequences

### Positive
- ✅ Supports all tenant personas (P1-P5+)
- ✅ Approval workflows expressible
- ✅ Fine-grained security
- ✅ Org chart UI for admins

### Negative
- ❌ Permission check complexity (cache needed)
- ❌ Migration from flat roles (data)
- ❌ UI complexity for role management

## Alternatives Considered

### Alternative A: Keep flat, add sub-role field
`Teacher.type = HOMEROOM | SUBJECT`

Pros: minimal change
Cons: breaks with each new persona requirement

**Rejected:** not scalable

### Alternative B: External IAM (Keycloak, Auth0)
Pros: Industry standard
Cons: Vendor lock, cost, latency

**Rejected:** in-house sufficient for SaaS tier

### Alternative C: ACL per resource
Pros: Maximum flexibility
Cons: Explosion of data, unmanageable

**Rejected:** RBAC with hierarchy is sweet spot

## Implementation Notes

Migration V30:
```sql
CREATE TABLE roles (
  id BIGSERIAL PRIMARY KEY,
  tenant_id UUID,
  name VARCHAR(50),
  parent_id BIGINT REFERENCES roles(id),
  level INT,
  is_system_role BOOLEAN DEFAULT FALSE,
  UNIQUE (tenant_id, name)
);

CREATE TABLE permissions (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(100) UNIQUE,
  description TEXT,
  category VARCHAR(50)
);

CREATE TABLE role_permissions (
  role_id BIGINT,
  permission_id BIGINT,
  PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE user_roles (
  user_id BIGINT,
  role_id BIGINT,
  assigned_at TIMESTAMP,
  PRIMARY KEY (user_id, role_id)
);

-- Migration: convert flat roles
-- OWNER → TENANT_OWNER role
-- ADMIN → TENANT_ADMIN role
-- TEACHER → SUBJECT_TEACHER role (default)
-- STUDENT → STUDENT role
```

Backward compat:
- @PreAuthorize("hasRole('OWNER')") works via role name matching
- Gradual migration to permission-based checks

## References

- GAP-058
- Design pattern: Composite (role tree) + Strategy (permission check)
- OWASP RBAC guidance

## Log
- 2026-04-14 — Accepted
