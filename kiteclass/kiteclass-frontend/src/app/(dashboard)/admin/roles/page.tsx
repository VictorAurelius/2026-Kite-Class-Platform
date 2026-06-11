/**
 * Role-assignment — Bucket D placeholder.
 *
 * Wave RBAC-Shell 1 Bucket B (GAP-1119): the OWNER shell links here from the
 * sidebar ("Phân quyền") + the dashboard governance card. The ACTUAL assign
 * user→role UI is Bucket D (deferred) — this page is a scaffold so the owner nav
 * resolves (no 404 bounce) and previews the 5 seeded role templates read-only.
 *
 * Sits under `(dashboard)/admin/*`, inheriting the OWNER/ADMIN RoleGuard from
 * `(dashboard)/admin/layout.tsx` (Bucket A) — STAFF / TEACHER / PARENT / STUDENT
 * are bounced to their own role-home.
 *
 * @author KiteClass Team
 * @since Wave RBAC-Shell 1 Bucket B (GAP-1119)
 */
'use client';

import { Construction, ShieldCheck } from 'lucide-react';
import { DashboardLayout } from '@/components/layout';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';

/**
 * The 5 fixed-curated role templates seeded by `RoleSeederService` (beta scope —
 * owner assigns user→role, no per-role permission editing per GAP-1119 decision 1).
 */
const SEEDED_ROLES: ReadonlyArray<{ key: string; label: string; desc: string }> = [
  { key: 'OWNER', label: 'Chủ trung tâm', desc: 'Toàn quyền quản lý trung tâm' },
  { key: 'STAFF', label: 'Nhân viên', desc: 'Tuyển sinh, điểm danh, hóa đơn học phí' },
  { key: 'TEACHER', label: 'Giáo viên', desc: 'Lớp dạy, chấm điểm, điểm danh' },
  { key: 'PARENT', label: 'Phụ huynh', desc: 'Theo dõi con (chỉ xem)' },
  { key: 'STUDENT', label: 'Học sinh', desc: 'Học tập (chờ kích hoạt KC-9)' },
];

export default function RoleAssignmentPage() {
  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div className="flex items-center gap-3">
          <ShieldCheck className="h-7 w-7 text-primary" />
          <div>
            <h1 className="text-3xl font-bold">Phân quyền</h1>
            <p className="text-muted-foreground">
              Gán vai trò cho người dùng trong trung tâm
            </p>
          </div>
        </div>

        {/* Bucket D deferral banner */}
        <div
          role="status"
          className="flex items-start gap-3 rounded-lg border border-amber-300 bg-amber-50 p-4 text-amber-900 dark:border-amber-700 dark:bg-amber-950/40 dark:text-amber-200"
        >
          <Construction className="mt-0.5 h-5 w-5 shrink-0" />
          <div className="text-sm">
            <p className="font-medium">Màn gán vai trò đang được phát triển (Bucket D).</p>
            <p>
              Bản beta dùng 5 mẫu vai trò cố định bên dưới — chủ trung tâm chỉ gán
              người dùng vào vai trò, chưa chỉnh sửa quyền theo từng vai trò.
            </p>
          </div>
        </div>

        {/* Read-only preview of the 5 seeded role templates */}
        <Card>
          <CardHeader>
            <CardTitle>Mẫu vai trò</CardTitle>
            <CardDescription>5 vai trò mặc định của trung tâm (chỉ xem)</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              {SEEDED_ROLES.map((r) => (
                <div
                  key={r.key}
                  className="flex items-center justify-between rounded-lg border p-3"
                >
                  <div>
                    <p className="font-medium">{r.label}</p>
                    <p className="text-sm text-muted-foreground">{r.desc}</p>
                  </div>
                  <Badge variant="secondary">{r.key}</Badge>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      </div>
    </DashboardLayout>
  );
}
