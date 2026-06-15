/**
 * Owner-shell RBAC role overview (GAP-1119 Bucket D — gated for Phase 1 BETA).
 *
 * IMPORTANT (why this is read-only in Phase 1):
 * The `user_roles` assignment store this page used to write to is NOT consumed by
 * any authorization path. Real authz runs off the login role (`entity_type` →
 * gateway `X-User-Roles` header → Spring `ROLE_*` + `@PreAuthorize`). Roles are
 * therefore assigned automatically at account provisioning:
 *   - TEACHER / PARENT / STUDENT  → set when the account is provisioned in KiteClass
 *   - OWNER / STAFF               → managed in KiteHub (cross-product)
 *
 * The interactive assign/revoke UI (+ searchable user picker, #2441) wrote rows that
 * nothing read, which misled owners (assign → nothing happens, roster stays empty).
 * Per GAP-1119 decision 1 the rich permission layer is deferred to Phase 3, so for
 * the beta this page is a read-only overview + an explicit Phase-3 notice. The assign
 * UI is preserved in git history (#2441) and rewired when user_roles becomes the
 * authz source in Phase 3 (tracked by the RBAC-disconnect gap).
 *
 * @author KiteClass Team
 * @since GAP-1119 (RBAC Bucket D) — gated 2026-06-15
 */
'use client';

import { ShieldCheck, Info } from 'lucide-react';
import { DashboardLayout } from '@/components/layout';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import type { SystemRoleName } from '@/types/role';

/** Vietnamese labels + how each role is granted in Phase 1. */
const ROLE_INFO: { name: SystemRoleName; label: string; granted: string }[] = [
  { name: 'OWNER', label: 'Chủ trung tâm', granted: 'Tài khoản KiteHub (đăng ký trung tâm)' },
  { name: 'STAFF', label: 'Nhân viên', granted: 'Lời mời nhân viên qua KiteHub' },
  { name: 'TEACHER', label: 'Giáo viên', granted: 'Khi tạo giáo viên + cấp mật khẩu' },
  { name: 'PARENT', label: 'Phụ huynh', granted: 'Khi nhận lời mời phụ huynh' },
  { name: 'STUDENT', label: 'Học sinh', granted: 'Khi ghi danh học sinh' },
];

export default function RoleOverviewPage() {
  return (
    <DashboardLayout>
      <div className="space-y-6">
        {/* Header */}
        <div className="flex items-center gap-3">
          <ShieldCheck className="h-7 w-7 text-primary" />
          <div>
            <h1 className="text-3xl font-bold">Phân quyền</h1>
            <p className="text-muted-foreground">Tổng quan 5 vai trò của trung tâm</p>
          </div>
        </div>

        {/* Phase-3 notice — why there is no manual assign UI yet */}
        <Card className="border-primary/30 bg-primary/5">
          <CardContent className="flex gap-3 py-4">
            <Info className="mt-0.5 h-5 w-5 shrink-0 text-primary" />
            <div className="space-y-1 text-sm">
              <p className="font-medium">
                Vai trò được gán tự động khi tạo tài khoản — không cần gán thủ công.
              </p>
              <p className="text-muted-foreground">
                Mỗi người dùng nhận vai trò ngay khi tài khoản được tạo (xem cột “Cách gán”
                bên dưới). Tùy chỉnh quyền chi tiết theo từng vai trò sẽ có ở <strong>Phase 3</strong>.
              </p>
            </div>
          </CardContent>
        </Card>

        {/* Read-only role overview */}
        <Card>
          <CardHeader>
            <CardTitle>Vai trò trong trung tâm</CardTitle>
            <CardDescription>
              5 vai trò cố định. Bản beta chưa cho chỉnh quyền theo từng vai trò.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              {ROLE_INFO.map((r) => (
                <div
                  key={r.name}
                  className="flex flex-wrap items-center justify-between gap-3 rounded-lg border p-3"
                >
                  <div>
                    <p className="font-medium">{r.label}</p>
                    <p className="text-sm text-muted-foreground">Cách gán: {r.granted}</p>
                  </div>
                  <Badge variant="secondary">{r.name}</Badge>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      </div>
    </DashboardLayout>
  );
}
