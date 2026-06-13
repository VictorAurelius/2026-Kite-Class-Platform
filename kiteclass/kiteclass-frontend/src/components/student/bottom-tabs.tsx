/**
 * Mobile bottom-tab navigation for the student PWA shell.
 *
 * Wave 49 Bucket C (Track 2 Phase 4 — GAP-269). Five tabs per the
 * `kiteclass-student` HTML kit — distinct from the parent kit's 4-tab
 * layout. Each tab targets a 44px+ tap area (WCAG 2.5.5 Target Size AA).
 *
 * Accessibility:
 * - `<nav>` with `aria-label` so screen readers announce the landmark
 * - active tab uses `aria-current="page"` (per HTML spec for primary nav)
 * - icons are decorative (`aria-hidden`); the visible label carries meaning
 */
'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import {
  Bell,
  BookOpen,
  CalendarDays,
  GraduationCap,
  Home,
  User,
} from 'lucide-react';
import { cn } from '@/lib/utils';

interface Tab {
  href: string;
  label: string;
  Icon: React.ComponentType<{ className?: string; 'aria-hidden'?: boolean }>;
}

const TABS: readonly Tab[] = [
  { href: '/student/today', label: 'Hôm nay', Icon: Home },
  { href: '/student/learning', label: 'Học tập', Icon: BookOpen },
  { href: '/student/my-classes', label: 'Lớp học', Icon: CalendarDays },
  { href: '/student/grades', label: 'Điểm', Icon: GraduationCap },
  { href: '/student/notifications', label: 'Thông báo', Icon: Bell },
  { href: '/student/profile', label: 'Cá nhân', Icon: User },
] as const;

export function StudentBottomTabs() {
  const pathname = usePathname();

  return (
    <nav
      aria-label="Điều hướng chính"
      className="fixed inset-x-0 bottom-0 z-40 border-t border-border bg-background/95 pb-[env(safe-area-inset-bottom)] backdrop-blur supports-[backdrop-filter]:bg-background/80"
    >
      <ul className="mx-auto flex max-w-screen-sm">
        {TABS.map(({ href, label, Icon }) => {
          // Match exact path OR any sub-route (e.g. /student/my-classes/123)
          const active =
            pathname === href || pathname.startsWith(`${href}/`);
          return (
            <li key={href} className="flex-1">
              <Link
                href={href}
                aria-current={active ? 'page' : undefined}
                className={cn(
                  'flex min-h-[56px] flex-col items-center justify-center gap-1 px-2 py-2 text-xs font-medium transition-colors',
                  active
                    ? 'text-primary'
                    : 'text-muted-foreground hover:text-foreground',
                )}
              >
                <Icon className="h-5 w-5" aria-hidden />
                <span>{label}</span>
              </Link>
            </li>
          );
        })}
      </ul>
    </nav>
  );
}
