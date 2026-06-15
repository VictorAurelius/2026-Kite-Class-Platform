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
 * User picker (UX fix): KiteClass has no central user-directory endpoint, so the
 * picker composes one client-side from the teachers + students lists. Each option's
 * value is the entity reference id (teacher.id / student.id), which is exactly the
 * `X-User-Reference-Id` the RBAC layer keys `user_roles.user_id` on. The owner
 * searches by name/email instead of memorising numeric ids. The assignment roster
 * resolves those ids back to names via the same directory.
 *
 * Known BE limitations (tracked separately, not introduced here):
 *   - reference-id collision risk: teacher.id and student.id are independent
 *     sequences, so RBAC's untyped numeric user_id can be ambiguous. The picker
 *     shows the entity type to disambiguate visually.
 *   - OWNER/STAFF/PARENT have no list hook yet → not in the picker (teachers +
 *     students cover the Bucket D walk; extend when a directory endpoint lands).
 *
 * @author KiteClass Team
 * @since GAP-1119 (RBAC Bucket D)
 */
'use client';

import { useMemo, useState } from 'react';
import { ShieldCheck, UserPlus, Trash2, Loader2, Search, X } from 'lucide-react';
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
import { useTeachers } from '@/hooks/use-teachers';
import { useStudents } from '@/hooks/use-students';
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

/** A tenant user the owner can assign a role to (value = entity reference id). */
type DirectoryUser = { id: number; name: string; email: string; type: 'TEACHER' | 'STUDENT' };

const TYPE_LABEL: Record<DirectoryUser['type'], string> = {
  TEACHER: 'Giáo viên',
  STUDENT: 'Học sinh',
};

export default function RoleAssignmentPage() {
  const templatesQuery = useRoleTemplates();
  const assignmentsQuery = useRoleAssignments();
  const seedMutation = useSeedRoleTemplates();
  const assignMutation = useAssignRole();
  const revokeMutation = useRevokeRole();

  // Compose a tenant user directory from the two lists that DO have endpoints.
  const teachersQuery = useTeachers({ size: 200 });
  const studentsQuery = useStudents({ size: 200 });

  const [search, setSearch] = useState('');
  const [selectedUser, setSelectedUser] = useState<DirectoryUser | null>(null);
  const [roleInput, setRoleInput] = useState<SystemRoleName>('TEACHER');
  const [formError, setFormError] = useState<string | null>(null);
  const [revokeTarget, setRevokeTarget] = useState<{ userId: number; roleName: SystemRoleName } | null>(
    null,
  );

  const templates = useMemo(() => templatesQuery.data ?? [], [templatesQuery.data]);
  const assignments = assignmentsQuery.data ?? [];
  const anyUnseeded = useMemo(() => templates.some((t) => !t.seeded), [templates]);

  const directory = useMemo<DirectoryUser[]>(() => {
    const teachers = (teachersQuery.data?.content ?? []).map((t) => ({
      id: t.id,
      name: t.name,
      email: t.email,
      type: 'TEACHER' as const,
    }));
    const students = (studentsQuery.data?.content ?? []).map((s) => ({
      id: s.id,
      name: s.name,
      email: s.email,
      type: 'STUDENT' as const,
    }));
    return [...teachers, ...students];
  }, [teachersQuery.data, studentsQuery.data]);

  /** First directory entry per reference id (used to humanise the roster). */
  const directoryById = useMemo(() => {
    const map = new Map<number, DirectoryUser>();
    directory.forEach((u) => {
      if (!map.has(u.id)) map.set(u.id, u);
    });
    return map;
  }, [directory]);

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return [];
    return directory
      .filter((u) => u.name.toLowerCase().includes(q) || u.email.toLowerCase().includes(q))
      .slice(0, 8);
  }, [search, directory]);

  const handleAssign = () => {
    setFormError(null);
    if (!selectedUser) {
      setFormError('Vui lòng chọn người dùng từ danh sách.');
      return;
    }
    assignMutation.mutate(
      { userId: selectedUser.id, roleName: roleInput },
      {
        onSuccess: () => {
          setSelectedUser(null);
          setSearch('');
        },
      },
    );
  };

  const confirmRevoke = () => {
    if (revokeTarget) {
      revokeMutation.mutate(revokeTarget, { onSuccess: () => setRevokeTarget(null) });
    }
  };

  const directoryLoading = teachersQuery.isLoading || studentsQuery.isLoading;

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
              Tìm người dùng theo tên hoặc email rồi chọn vai trò. Vai trò được tạo tự động nếu chưa khởi tạo.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="flex flex-col gap-4 sm:flex-row sm:items-end">
              {/* User picker */}
              <div className="flex-1 space-y-1.5">
                <Label htmlFor="role-user-search">Người dùng</Label>
                {selectedUser ? (
                  <div className="flex h-10 items-center justify-between rounded-md border border-input bg-muted/40 px-3">
                    <span className="truncate text-sm">
                      <span className="font-medium">{selectedUser.name}</span>{' '}
                      <span className="text-muted-foreground">· {selectedUser.email}</span>{' '}
                      <Badge variant="secondary" className="ml-1 align-middle">
                        {TYPE_LABEL[selectedUser.type]}
                      </Badge>
                    </span>
                    <button
                      type="button"
                      aria-label="Bỏ chọn người dùng"
                      className="ml-2 text-muted-foreground hover:text-destructive"
                      onClick={() => setSelectedUser(null)}
                    >
                      <X className="h-4 w-4" />
                    </button>
                  </div>
                ) : (
                  <div className="relative">
                    <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                    <Input
                      id="role-user-search"
                      className="pl-9"
                      placeholder="Tìm theo tên hoặc email…"
                      value={search}
                      onChange={(e) => setSearch(e.target.value)}
                      autoComplete="off"
                    />
                    {search.trim() && (
                      <div className="absolute z-10 mt-1 max-h-64 w-full overflow-auto rounded-md border bg-popover p-1 shadow-md">
                        {directoryLoading ? (
                          <div className="flex justify-center py-3">
                            <Loader2 className="h-4 w-4 animate-spin text-muted-foreground" />
                          </div>
                        ) : filtered.length === 0 ? (
                          <p className="px-2 py-3 text-sm text-muted-foreground">
                            Không tìm thấy người dùng phù hợp.
                          </p>
                        ) : (
                          filtered.map((u) => (
                            <button
                              key={`${u.type}-${u.id}`}
                              type="button"
                              className="flex w-full flex-col rounded-sm px-2 py-1.5 text-left text-sm hover:bg-accent"
                              onClick={() => {
                                setSelectedUser(u);
                                setSearch('');
                                setFormError(null);
                              }}
                            >
                              <span className="font-medium">{u.name}</span>
                              <span className="text-xs text-muted-foreground">
                                {u.email} · {TYPE_LABEL[u.type]}
                              </span>
                            </button>
                          ))
                        )}
                      </div>
                    )}
                  </div>
                )}
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
                {assignments.map((a) => {
                  const known = directoryById.get(a.userId);
                  return (
                    <div
                      key={a.userId}
                      className="flex flex-wrap items-center justify-between gap-3 rounded-lg border p-3"
                    >
                      <div className="flex flex-col">
                        {known ? (
                          <>
                            <span className="font-medium">{known.name}</span>
                            <span className="text-xs text-muted-foreground">
                              {known.email} · {TYPE_LABEL[known.type]}
                            </span>
                          </>
                        ) : (
                          <span className="font-medium">Người dùng #{a.userId}</span>
                        )}
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
                                aria-label={`Thu hồi vai trò ${r} khỏi ${known?.name ?? `người dùng ${a.userId}`}`}
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
                  );
                })}
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
            ? `Thu hồi vai trò ${ROLE_LABELS[revokeTarget.roleName] ?? revokeTarget.roleName} khỏi ${directoryById.get(revokeTarget.userId)?.name ?? `người dùng #${revokeTarget.userId}`}?`
            : ''
        }
        confirmText="Thu hồi"
        variant="destructive"
      />
    </DashboardLayout>
  );
}
