/**
 * Student-shell scaffold banner — marks the KC-9 (student auth) gate.
 *
 * Wave RBAC-Shell 1 Bucket B (GAP-1119): the student route group + mobile shell
 * are scaffolded, but the KC-native student login path is NOT yet functional
 * (KC-9 pending). This non-intrusive banner makes that explicit when any student
 * route renders, so the scaffold state is obvious to walkers / future devs.
 *
 * @author KiteClass Team
 * @since Wave RBAC-Shell 1 Bucket B (GAP-1119)
 */
'use client';

import { Construction } from 'lucide-react';

export function StudentAuthGatedBanner() {
  return (
    <div
      role="status"
      className="flex items-center gap-2 border-b border-amber-300 bg-amber-50 px-4 py-2 text-xs text-amber-900 dark:border-amber-700 dark:bg-amber-950/40 dark:text-amber-200"
    >
      <Construction className="h-4 w-4 shrink-0" />
      <span>
        Cổng học sinh đang ở giai đoạn dựng khung — đăng nhập học sinh sẽ khả dụng
        khi KC-9 hoàn tất.
      </span>
    </div>
  );
}
