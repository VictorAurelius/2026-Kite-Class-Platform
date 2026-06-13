/**
 * RBAC role-management types — owner-shell role assignment (GAP-1119 Bucket D).
 *
 * Mirrors kiteclass-core `RoleController` (`/api/v1/roles`). Fixed-curated RBAC:
 * owner assigns users to one of the 5 seeded templates; NO permission-edit (Phase 3).
 *
 * @author KiteClass Team
 * @since GAP-1119 (RBAC Bucket D)
 */

/** The 5 fixed-curated system role template names. */
export type SystemRoleName = 'OWNER' | 'STAFF' | 'TEACHER' | 'PARENT' | 'STUDENT';

/** A system role template + its seed-state in the current tenant. */
export interface RoleTemplate {
  name: SystemRoleName;
  level: number;
  description: string;
  roleId: number | null;
  seeded: boolean;
}

/** A tenant user (by numeric reference id) + the role names assigned to them. */
export interface UserRoleAssignment {
  userId: number;
  roles: string[];
}

/** Request to assign a user to a role template (by name — server resolves lazily). */
export interface AssignRoleRequest {
  userId: number;
  roleName: SystemRoleName;
}
