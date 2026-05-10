'use client';

/**
 * School-admin shell — sidebar + header for K-12 School Principal scope.
 *
 * Wave 50 Bucket A (GAP-271): kh-admin K-12 Principal port.
 * Distinct from KiteHub PLATFORM admin (`(admin)/admin/**`) — different persona.
 *
 * Design ref: documents/02-architecture/design-system/ui_kits/kitehub-admin/screens/dashboard.html
 *
 * @since Wave 50 Bucket A
 */

import { type ReactNode } from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import {
  LayoutDashboard,
  CalendarDays,
  Grid3x3,
  UploadCloud,
  FileText,
  AlertTriangle,
  Users,
  MessageSquare,
  Receipt,
  Settings,
  Bell,
  Search,
} from 'lucide-react';
import { SCHOOL_PROFILE } from './school-admin-mock-data';

interface NavGroup {
  title: string;
  items: { href: string; label: string; icon: typeof LayoutDashboard; count?: number }[];
}

const NAV_GROUPS: NavGroup[] = [
  {
    title: 'Tổng quan',
    items: [
      { href: '/school-admin/dashboard', label: 'Bảng tổng quan', icon: LayoutDashboard },
      { href: '/school-admin/academic-calendar', label: 'Lịch học vụ', icon: CalendarDays },
      { href: '/school-admin/multi-class-roster', label: 'Bảng phân công', icon: Grid3x3, count: 25 },
    ],
  },
  {
    title: 'Học sinh',
    items: [
      { href: '/school-admin/bulk-import', label: 'Nhập danh sách', icon: UploadCloud, count: 1247 },
      { href: '/school-admin/report-cards', label: 'Học bạ MoET', icon: FileText },
      { href: '/school-admin/conduct', label: 'Hạnh kiểm', icon: AlertTriangle, count: 7 },
    ],
  },
  {
    title: 'Nhân sự',
    items: [
      { href: '/school-admin/teacher-management', label: 'Giáo viên', icon: Users, count: 62 },
      { href: '/school-admin/parent-comms', label: 'Phụ huynh', icon: MessageSquare, count: 12 },
    ],
  },
  {
    title: 'Tài chính',
    items: [{ href: '/school-admin/fees', label: 'Học phí năm', icon: Receipt }],
  },
  {
    title: 'Cấu hình',
    items: [{ href: '/school-admin/school-profile', label: 'Hồ sơ trường', icon: Settings }],
  },
];

function NavLink({
  href,
  label,
  icon: Icon,
  count,
  active,
}: {
  href: string;
  label: string;
  icon: typeof LayoutDashboard;
  count?: number;
  active: boolean;
}) {
  return (
    <Link
      href={href}
      className={`flex items-center gap-2.5 rounded-md px-2.5 py-2 text-sm transition-colors ${
        active
          ? 'bg-primary/10 font-medium text-primary'
          : 'text-muted-foreground hover:bg-muted hover:text-foreground'
      }`}
    >
      <Icon className="h-4 w-4 shrink-0" aria-hidden="true" />
      <span className="flex-1">{label}</span>
      {count !== undefined && (
        <span className="text-xs font-medium tabular-nums text-muted-foreground">
          {count.toLocaleString('vi-VN')}
        </span>
      )}
    </Link>
  );
}

export function SchoolAdminShell({
  children,
  notificationsCount = 7,
}: {
  children: ReactNode;
  notificationsCount?: number;
}) {
  const pathname = usePathname();
  return (
    <div className="flex min-h-screen bg-muted/20">
      <aside className="w-64 shrink-0 border-r bg-background p-4" aria-label="Điều hướng chính">
        <Link href="/school-admin/dashboard" className="mb-3 flex items-center gap-2" aria-label="KiteHub School Admin">
          <span className="text-lg font-bold text-primary">KiteHub</span>
          <span className="rounded bg-primary/10 px-1.5 py-0.5 text-[10px] font-medium uppercase tracking-wider text-primary">
            Trường học
          </span>
        </Link>

        <button
          type="button"
          className="mb-4 flex w-full items-center gap-2 rounded-md border bg-background px-2.5 py-1.5 text-xs text-muted-foreground hover:bg-muted"
          aria-haspopup="dialog"
          aria-keyshortcuts="Meta+K"
        >
          <Search className="h-3.5 w-3.5" aria-hidden="true" />
          <span className="flex-1 text-left">Tìm nhanh, lệnh…</span>
          <kbd className="rounded border bg-muted px-1 text-[10px]">⌘K</kbd>
        </button>

        <div className="mb-4 rounded-md border bg-muted/40 px-2.5 py-2 text-xs">
          <div className="text-[10px] font-medium uppercase tracking-wider text-muted-foreground">
            Năm học {SCHOOL_PROFILE.academicYear}
          </div>
          <div className="mt-0.5 text-sm font-semibold">{SCHOOL_PROFILE.name}</div>
          <div className="text-xs text-muted-foreground">
            {SCHOOL_PROFILE.semester} · Tuần {SCHOOL_PROFILE.weekNumber} / {SCHOOL_PROFILE.totalWeeks}
          </div>
        </div>

        <nav className="space-y-4">
          {NAV_GROUPS.map((group) => (
            <div key={group.title}>
              <div className="mb-1 px-2 text-[10px] font-semibold uppercase tracking-wider text-muted-foreground">
                {group.title}
              </div>
              <div className="space-y-0.5">
                {group.items.map((it) => (
                  <NavLink
                    key={it.href}
                    href={it.href}
                    label={it.label}
                    icon={it.icon}
                    count={it.count}
                    active={pathname === it.href || pathname.startsWith(it.href + '/')}
                  />
                ))}
              </div>
            </div>
          ))}
        </nav>

        <div className="mt-6 rounded-md border bg-background px-3 py-2.5">
          <div className="text-[10px] uppercase tracking-wider text-muted-foreground">Đã đăng nhập</div>
          <div className="mt-0.5 text-sm font-semibold">{SCHOOL_PROFILE.principalName}</div>
          <div className="text-xs text-muted-foreground">{SCHOOL_PROFILE.principalRole}</div>
        </div>
      </aside>

      <div className="flex flex-1 flex-col">
        <header className="flex h-14 items-center justify-between border-b bg-background px-6">
          <div className="text-sm text-muted-foreground">
            {SCHOOL_PROFILE.name} · {SCHOOL_PROFILE.district}
          </div>
          <div className="flex items-center gap-3">
            <button
              type="button"
              className="relative rounded-md p-2 text-muted-foreground hover:bg-muted"
              aria-label={`Thông báo (${notificationsCount} chưa đọc)`}
            >
              <Bell className="h-4 w-4" aria-hidden="true" />
              {notificationsCount > 0 && (
                <span className="absolute right-1 top-1 flex h-4 min-w-[16px] items-center justify-center rounded-full bg-destructive px-1 text-[10px] font-semibold text-destructive-foreground">
                  {notificationsCount}
                </span>
              )}
            </button>
            <span className="text-sm font-medium">{SCHOOL_PROFILE.principalName}</span>
          </div>
        </header>
        <main className="flex-1 p-6">{children}</main>
      </div>
    </div>
  );
}
