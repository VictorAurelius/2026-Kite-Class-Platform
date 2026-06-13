/**
 * Owner-shell RBAC role-assignment (GAP-1119 Bucket D — the real assign UI).
 *
 * Replaces the Bucket B placeholder. Wires the kiteclass-core `/api/v1/roles`
 * endpoints so an owner can:
 *   (a) list the 5 seeded role templates + seed them if missing,
 *   (b) list tenant users with their current role(s),
 *   (c) assign a user → one of the 5 templates,
 *   (d) revoke a role from a user.
 *
 * Fixed-curated per GAP-1119 decision 1: NO permission-edit UI (deferred Phase 3).
 * Sits under `(dashboard)/admin/*`, inheriting the OWNER/ADMIN RoleGuard from
 * `(dashboard)/admin/layout.tsx`.
 *
 * Note (BE constraint): KiteClass has no central user table, so the assignment
 * roster is keyed by `user_roles.user_id` (numeric reference id). New users are
 * assigned by entering that id directly — there is no tenant-wide user directory
 * endpoint to populate a dropdown.
 *
 * @author KiteClass Team
 * @since GAP-1119 (RBAC Bucket D)
 */
'use client';

import { useMemo, useState } from 'react';
import { ShieldCheck, UserPlus, Trash2, Loader2 } from 'lucide-react';
import { DashboardLayout } from '@/components/layout';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import {
  useRoleTemplates,
  useRoleAssignments,
  useSeedRoleTemplates,
  useAssignRole,
  useRevokeRole,
} from '@/hooks/use-roles';
import type { SystemRoleName } from '@/types/role';

/** Vietnamese labels for the 5 fixed-curated templates. */
const ROLE_LABELS: Record<SystemRoleName, string> = {
  OWNER: 'Chủ trung tâm',
  STAFF: 'Nhân viên',
  TEACHER: 'Giáo viên',
  PARENT: 'Phụ huynh',
  STUDENT: 'Học sinh',
};

const ROLE_ORDER: SystemRoleName[] = ['OWNER', 'STAFF', 'TEACHER', 'PARENT', 'STUDENT'];

export default function RoleAssignmentPage() {
  const templatesQuery = useRoleTemplates();
  const assignmentsQuery = useRoleAssignments();
  const seedMutation = useSeedRoleTemplates();
  const assignMutation = useAssignRole();
  const revokeMutation = useRevokeRole();

  const [userIdInput, setUserIdInput] = useState('');
  const [roleInput, setRoleInput] = useState<SystemRoleName>('TEACHER');
  const [formError, setFormError] = useState<string | null>(null);
  const [revokeTarget, setRevokeTarget] = useState<{ userId: number; roleName: SystemRoleName } | null>(
    null,
  );

  const templates = useMemo(() => templatesQuery.data ?? [], [templatesQuery.data]);
  const assignments = assignmentsQuery.data ?? [];
  const anyUnseeded = useMemo(() => templates.some((t) => !t.seeded), [templates]);

  const handleAssign = () => {
    setFormError(null);
    const userId = Number(userIdInput.trim());
    if (!userIdInput.trim() || Number.isNaN(userId) || userId <= 0) {
      setFormError('Vui lòng nhập ID người dùng hợp lệ (số dương).');
      return;
    }
    assignMutation.mutate(
      { userId, roleName: roleInput },
      { onSuccess: () => setUserIdInput('') },
    );
  };

  const confirmRevoke = () => {
    if (revokeTarget) {
      revokeMutation.mutate(revokeTarget, { onSuccess: () => setRevokeTarget(null) });
    }
  };

  return (
    <DashboardLayout>
      <div className="space-y-6">
        {/* Header */}
        <div className="flex items-center gap-3">
          <ShieldCheck className="h-7 w-7 text-primary" />
          <div>
            <h1 className="text-3xl font-bold">Phân quyền</h1>
            <p className="text-muted-foreground">
              Gán vai trò cho người dùng trong trung tâm (5 mẫu vai trò cố định)
            </p>
          </div>
        </div>

        {/* Role templates */}
        <Card>
          <CardHeader>
            <div className="flex items-start justify-between gap-3">
              <div>
                <CardTitle>Mẫu vai trò</CardTitle>
                <CardDescription>
                  5 vai trò mặc định của trung tâm. Bản beta chỉ gán người dùng vào
                  vai trò, chưa chỉnh sửa quyền theo từng vai trò.
                </CardDescription>
              </div>
              {anyUnseeded && (
                <Button
                  onClick={() => seedMutation.mutate()}
                  disabled={seedMutation.isPending}
                  size="sm"
                >
                  {seedMutation.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                  Khởi tạo mẫu vai trò
                </Button>
              )}
            </div>
          </CardHeader>
          <CardContent>
            {templatesQuery.isLoading ? (
              <div className="flex justify-center py-6">
                <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />
              </div>
            ) : (
              <div className="space-y-3">
                {templates.map((t) => (
                  <div
                    key={t.name}
                    className="flex items-center justify-between rounded-lg border p-3"
                  >
                    <div>
                      <p className="font-medium">{ROLE_LABELS[t.name] ?? t.name}</p>
                      <p className="text-sm text-muted-foreground">{t.description}</p>
                    </div>
                    <div className="flex items-center gap-2">
                      <Badge variant="secondary">{t.name}</Badge>
                      <Badge variant={t.seeded ? 'default' : 'outline'}>
                        {t.seeded ? 'Đã khởi tạo' : 'Chưa khởi tạo'}
                      </Badge>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>

        {/* Assign a user → role */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <UserPlus className="h-5 w-5" /> Gán vai trò cho người dùng
            </CardTitle>
            <CardDescription>
              Nhập ID người dùng và chọn vai trò. Vai trò được tạo tự động nếu chưa khởi tạo.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="flex flex-col gap-4 sm:flex-row sm:items-end">
              <div className="flex-1 space-y-1.5">
                <Label htmlFor="role-user-id">ID người dùng</Label>
                <Input
                  id="role-user-id"
                  inputMode="numeric"
                  placeholder="VD: 1024"
                  value={userIdInput}
                  onChange={(e) => setUserIdInput(e.target.value)}
                />
              </div>
              <div className="flex-1 space-y-1.5">
                <Label htmlFor="role-name">Vai trò</Label>
                <select
                  id="role-name"
                  className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                  value={roleInput}
                  onChange={(e) => setRoleInput(e.target.value as SystemRoleName)}
                >
                  {ROLE_ORDER.map((r) => (
                    <option key={r} value={r}>
                      {ROLE_LABELS[r]} ({r})
                    </option>
                  ))}
                </select>
              </div>
              <Button onClick={handleAssign} disabled={assignMutation.isPending}>
                {assignMutation.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                Gán vai trò
              </Button>
            </div>
            {formError && <p className="mt-2 text-sm text-destructive">{formError}</p>}
          </CardContent>
        </Card>

        {/* Current assignments */}
        <Card>
          <CardHeader>
            <CardTitle>Người dùng &amp; vai trò</CardTitle>
            <CardDescription>
              Danh sách người dùng đã được gán vai trò trong trung tâm.
            </CardDescription>
          </CardHeader>
          <CardContent>
            {assignmentsQuery.isLoading ? (
              <div className="flex justify-center py-6">
                <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />
              </div>
            ) : assignments.length === 0 ? (
              <p className="py-6 text-center text-sm text-muted-foreground">
                Chưa có người dùng nào được gán vai trò.
              </p>
            ) : (
              <div className="space-y-3">
                {assignments.map((a) => (
                  <div
                    key={a.userId}
                    className="flex flex-wrap items-center justify-between gap-3 rounded-lg border p-3"
                  >
                    <div className="flex items-center gap-2">
                      <span className="font-medium">Người dùng #{a.userId}</span>
                    </div>
                    <div className="flex flex-wrap items-center gap-2">
                      {a.roles.length === 0 ? (
                        <span className="text-sm text-muted-foreground">(chưa có vai trò)</span>
                      ) : (
                        a.roles.map((r) => (
                          <span
                            key={r}
                            className="inline-flex items-center gap-1 rounded-full border bg-muted px-2.5 py-1 text-xs font-medium"
                          >
                            {ROLE_LABELS[r as SystemRoleName] ?? r}
                            <button
                              type="button"
                              aria-label={`Thu hồi vai trò ${r} khỏi người dùng ${a.userId}`}
                              className="text-muted-foreground hover:text-destructive"
                              onClick={() =>
                                setRevokeTarget({ userId: a.userId, roleName: r as SystemRoleName })
                              }
                            >
                              <Trash2 className="h-3.5 w-3.5" />
                            </button>
                          </span>
                        ))
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      <ConfirmDialog
        open={!!revokeTarget}
        onOpenChange={(open) => !open && setRevokeTarget(null)}
        onConfirm={confirmRevoke}
        title="Thu hồi vai trò"
        description={
          revokeTarget
            ? `Thu hồi vai trò ${ROLE_LABELS[revokeTarget.roleName] ?? revokeTarget.roleName} khỏi người dùng #${revokeTarget.userId}?`
            : ''
        }
        confirmText="Thu hồi"
        variant="destructive"
      />
    </DashboardLayout>
  );
}
