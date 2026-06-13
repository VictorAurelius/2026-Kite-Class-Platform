/**
 * React Query hooks for owner-shell RBAC role management (GAP-1119 Bucket D).
 *
 * @author KiteClass Team
 * @since GAP-1119 (RBAC Bucket D)
 */

'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AxiosError } from 'axios';
import { rolesApi } from '@/lib/api/roles';
import { toast } from '@/hooks/use-toast';
import type { AssignRoleRequest, SystemRoleName } from '@/types/role';

const ROLES_KEY = 'roles';

function onErr(fallback: string) {
  return (error: AxiosError<{ message?: string; code?: string }>) => {
    const message = error.response?.data?.message || error.message || fallback;
    toast({ title: 'Lỗi', description: message, variant: 'destructive' });
  };
}

export function useRoleTemplates() {
  return useQuery({
    queryKey: [ROLES_KEY, 'templates'],
    queryFn: () => rolesApi.getTemplates(),
  });
}

export function useRoleAssignments() {
  return useQuery({
    queryKey: [ROLES_KEY, 'assignments'],
    queryFn: () => rolesApi.listAssignments(),
  });
}

export function useSeedRoleTemplates() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => rolesApi.seedTemplates(),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: [ROLES_KEY] });
      toast({ title: 'Thành công', description: 'Đã khởi tạo 5 mẫu vai trò cho trung tâm' });
    },
    onError: onErr('Không thể khởi tạo mẫu vai trò'),
  });
}

export function useAssignRole() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: AssignRoleRequest) => rolesApi.assignRole(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: [ROLES_KEY, 'assignments'] });
      toast({ title: 'Thành công', description: 'Đã gán vai trò cho người dùng' });
    },
    onError: onErr('Không thể gán vai trò'),
  });
}

export function useRevokeRole() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (vars: { userId: number; roleName: SystemRoleName }) =>
      rolesApi.revokeRole(vars.userId, vars.roleName),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: [ROLES_KEY, 'assignments'] });
      toast({ title: 'Thành công', description: 'Đã thu hồi vai trò' });
    },
    onError: onErr('Không thể thu hồi vai trò'),
  });
}
