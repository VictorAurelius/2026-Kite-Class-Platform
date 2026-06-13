/**
 * RBAC role-management API — owner-shell (`/api/v1/roles`, GAP-1119 Bucket D).
 *
 * All endpoints are OWNER/ADMIN-only (class-level @PreAuthorize on the controller).
 * The base {@link apiClient} injects Bearer + `X-Tenant-Id`; tenant scope is enforced
 * server-side via the tenant filter. There is NO permission-edit endpoint by design.
 *
 * @author KiteClass Team
 * @since GAP-1119 (RBAC Bucket D)
 */

import { apiClient } from '@/lib/api-client';
import type { ApiResponse } from '@/types/api';
import type {
  RoleTemplate,
  UserRoleAssignment,
  AssignRoleRequest,
  SystemRoleName,
} from '@/types/role';

export const rolesApi = {
  /** List the 5 system role templates + their seed-state. */
  getTemplates: async (): Promise<RoleTemplate[]> => {
    const res = await apiClient.get<ApiResponse<RoleTemplate[]>>('/api/v1/roles/templates');
    return res.data.data ?? [];
  },

  /** Idempotently seed the 5 system role templates for the current tenant. */
  seedTemplates: async (): Promise<RoleTemplate[]> => {
    const res = await apiClient.post<ApiResponse<RoleTemplate[]>>('/api/v1/roles/seed');
    return res.data.data ?? [];
  },

  /** List tenant users with their currently-assigned role names. */
  listAssignments: async (): Promise<UserRoleAssignment[]> => {
    const res = await apiClient.get<ApiResponse<UserRoleAssignment[]>>(
      '/api/v1/roles/assignments',
    );
    return res.data.data ?? [];
  },

  /** Assign a user to a role template (idempotent). */
  assignRole: async (data: AssignRoleRequest): Promise<UserRoleAssignment> => {
    const res = await apiClient.post<ApiResponse<UserRoleAssignment>>(
      '/api/v1/roles/assignments',
      data,
    );
    return res.data.data!;
  },

  /** Revoke a role template from a user (idempotent). */
  revokeRole: async (userId: number, roleName: SystemRoleName): Promise<void> => {
    await apiClient.delete('/api/v1/roles/assignments', {
      params: { userId, roleName },
    });
  },
};
