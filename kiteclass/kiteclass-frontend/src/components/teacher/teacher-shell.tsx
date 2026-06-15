/**
 * TeacherShell — top navigation + auth-bound shell for the (teacher) route group.
 *
 * Mirrors the kiteclass-teacher HTML prototype header (Round 2). Renders a
 * sticky top nav with the four primary teacher destinations: Điểm danh,
 * Vào điểm, Lịch dạy, Báo cáo + a settings link, plus the signed-in teacher
 * identity badge.
 *
 * @since Wave 49 Bucket B (GAP-268)
 */

'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { Bell, ClipboardCheck, BookOpenCheck, CalendarDays, BarChart3, Settings, ClipboardList, LogOut } from 'lucide-react';
import { cn } from '@/lib/utils';
import { SkipToContent } from '@/components/a11y/skip-to-content';
import { useAuth } from '@/hooks/useAuth';

type NavItem = {
  href: string;
  label: string;
  icon: React.ComponentType<{ className?: string }>;
  match: (pathname: string) => boolean;
};

const NAV_ITEMS: NavItem[] = [
  {
    href: '/teacher/attendance',
    label: 'Điểm danh',
    icon: ClipboardCheck,
    match: (p) => p.startsWith('/teacher/attendance'),
  },
  {
    href: '/teacher/grades',
    label: 'Vào điểm',
    icon: BookOpenCheck,
    match: (p) => p.startsWith('/teacher/grades'),
  },
  {
    href: '/teacher/assignments',
    label: 'Bài tập',
    icon: ClipboardList,
    match: (p) => p.startsWith('/teacher/assignments'),
  },
  {
    href: '/teacher/schedule',
    label: 'Lịch dạy',
    icon: CalendarDays,
    match: (p) => p.startsWith('/teacher/schedule'),
  },
  {
    href: '/teacher/reports',
    label: 'Báo cáo',
    icon: BarChart3,
    match: (p) => p.startsWith('/teacher/reports'),
  },
];

export interface TeacherShellProps {
  children: React.ReactNode;
  teacherName?: string;
  teacherSubtitle?: string;
  teacherInitials?: string;
}

export function TeacherShell({
  children,
  teacherName = 'Cô Trần Thu Hà',
  teacherSubtitle = 'GVCN Lớp 10A2',
  teacherInitials = 'TH',
}: TeacherShellProps) {
  const pathname = usePathname() ?? '/teacher';
  const { logout } = useAuth();

  return (
    <div className="min-h-screen bg-background text-foreground">
      {/* Skip to main content (accessibility — WCAG 2.4.1) */}
      <SkipToContent />
      <header className="sticky top-0 z-30 border-b border-border bg-card">
        <div className="mx-auto flex h-14 max-w-7xl items-center justify-between px-4 lg:px-6">
          <div className="flex items-center gap-6">
            <Link
              href="/teacher/dashboard"
              className="flex items-center gap-2 text-lg font-bold"
            >
              <span className="text-primary">KiteClass</span>
              <span className="rounded-full border border-border px-2 py-0.5 text-xs font-medium text-muted-foreground">
                Giáo viên
              </span>
            </Link>
            <nav
              className="hidden items-center gap-1 text-sm md:flex"
              aria-label="Điều hướng chính giáo viên"
            >
              {NAV_ITEMS.map((item) => {
                const active = item.match(pathname);
                const Icon = item.icon;
                return (
                  <Link
                    key={item.href}
                    href={item.href}
                    className={cn(
                      'inline-flex items-center gap-1.5 rounded-md px-3 py-2 font-medium transition-colors',
                      active
                        ? 'bg-muted text-foreground'
                        : 'text-muted-foreground hover:bg-muted hover:text-foreground',
                    )}
                    aria-current={active ? 'page' : undefined}
                  >
                    <Icon className="h-4 w-4" aria-hidden="true" />
                    {item.label}
                  </Link>
                );
              })}
            </nav>
          </div>
          <div className="flex items-center gap-3">
            <Link
              href="/teacher/settings"
              className="rounded-md p-2 text-muted-foreground hover:bg-muted hover:text-foreground"
              aria-label="Cài đặt"
            >
              <Settings className="h-5 w-5" />
            </Link>
            <button
              type="button"
              className="relative rounded-md p-2 text-muted-foreground hover:bg-muted hover:text-foreground"
              aria-label="Thông báo (3 thông báo mới)"
            >
              <Bell className="h-5 w-5" />
              <span
                className="absolute right-1 top-1 h-2 w-2 rounded-full bg-red-500"
                aria-hidden="true"
              />
            </button>
            <div className="flex items-center gap-2 text-sm">
              <div
                className="flex h-8 w-8 items-center justify-center rounded-full bg-primary/15 font-semibold text-primary"
                aria-hidden="true"
              >
                {teacherInitials}
              </div>
              <div className="hidden leading-tight sm:block">
                <div className="font-medium">{teacherName}</div>
                <div className="text-xs text-muted-foreground">{teacherSubtitle}</div>
              </div>
            </div>
            <button
              type="button"
              onClick={() => logout()}
              className="rounded-md p-2 text-muted-foreground hover:bg-muted hover:text-red-600"
              aria-label="Đăng xuất"
            >
              <LogOut className="h-5 w-5" />
            </button>
          </div>
        </div>
        {/* Mobile nav: secondary row */}
        <nav
          className="flex items-center gap-1 overflow-x-auto border-t border-border px-4 py-2 text-sm md:hidden"
          aria-label="Điều hướng chính giáo viên (mobile)"
        >
          {NAV_ITEMS.map((item) => {
            const active = item.match(pathname);
            const Icon = item.icon;
            return (
              <Link
                key={item.href}
                href={item.href}
                className={cn(
                  'inline-flex shrink-0 items-center gap-1 rounded-md px-3 py-2 font-medium',
                  active
                    ? 'bg-muted text-foreground'
                    : 'text-muted-foreground',
                )}
                aria-current={active ? 'page' : undefined}
              >
                <Icon className="h-4 w-4" />
                {item.label}
              </Link>
            );
          })}
        </nav>
      </header>
      <main id="main-content" role="main" className="mx-auto max-w-7xl px-4 py-6 lg:px-6">{children}</main>
    </div>
  );
}
