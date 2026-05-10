/**
 * Parent mobile-PWA shell — header + bottom tab nav + safe-area scaffolding.
 *
 * Wave 49 Bucket A (GAP-267 Track 2 port). Ports the `app-shell` + `app-header`
 * + `bottom-tabs` pattern from `ui_kits/kiteclass-parent/styles.css` to a
 * Tailwind-driven React shell. All screens under `(dashboard)/parent/*` wrap
 * their content with this component to share the bottom 4-tab navigation
 * (Trang chủ / Học bạ / Học phí / Cài đặt) and the persona-evening warm-blue
 * theme tokens.
 *
 * Mobile-first 320–414px primary viewport per `dossier/01-personas.md` row Pa.
 * Tap targets ≥44px (WCAG 2.5.5). Vietnamese-only copy per CLAUDE.md.
 *
 * @author KiteClass Team
 * @since 2.49.0 (Wave 49 — GAP-267)
 */

'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { Bell, GraduationCap, Home, Receipt, Settings } from 'lucide-react';
import { cn } from '@/lib/utils';

export interface ParentShellProps {
  /** Greeting / title shown in the header. */
  title: string;
  /** Optional sub-title (period, breadcrumb hint, …). */
  subtitle?: string;
  /** Notification badge count rendered on the bell icon (0 = hidden). */
  unreadCount?: number;
  /** Body content. */
  children: React.ReactNode;
}

interface TabDef {
  href: string;
  label: string;
  icon: React.ComponentType<{ className?: string; 'aria-hidden'?: boolean }>;
  /** Match this prefix to highlight the tab as active. */
  matches: (pathname: string) => boolean;
}

const TABS: ReadonlyArray<TabDef> = [
  {
    href: '/parent',
    label: 'Trang chủ',
    icon: Home,
    matches: (p) => p === '/parent' || p === '/parent/',
  },
  {
    href: '/parent/grades',
    label: 'Học bạ',
    icon: GraduationCap,
    matches: (p) => p.startsWith('/parent/grades') || p.startsWith('/parent/transcript'),
  },
  {
    href: '/parent/billing',
    label: 'Học phí',
    icon: Receipt,
    matches: (p) => p.startsWith('/parent/billing'),
  },
  {
    href: '/parent/settings',
    label: 'Cài đặt',
    icon: Settings,
    matches: (p) => p.startsWith('/parent/settings'),
  },
];

export function ParentShell({
  title,
  subtitle,
  unreadCount = 0,
  children,
}: ParentShellProps) {
  const pathname = usePathname() ?? '/parent';

  return (
    <div
      className="mx-auto flex min-h-[100dvh] max-w-[480px] flex-col bg-muted/40 pb-20"
      role="application"
      aria-label="Cổng phụ huynh"
      data-testid="parent-shell"
    >
      <header className="sticky top-0 z-20 flex items-start justify-between gap-3 border-b bg-card/95 px-4 py-4 backdrop-blur">
        <div className="min-w-0 flex-1">
          <h1 className="truncate text-xl font-bold tracking-tight">{title}</h1>
          {subtitle && (
            <p className="mt-0.5 text-xs font-medium text-muted-foreground">
              {subtitle}
            </p>
          )}
        </div>
        <Link
          href="/parent/settings#notifications"
          className="relative inline-flex h-11 w-11 items-center justify-center rounded-full text-foreground hover:bg-muted focus:outline-none focus-visible:ring-2 focus-visible:ring-ring"
          aria-label={
            unreadCount > 0
              ? `Thông báo (${unreadCount} chưa đọc)`
              : 'Thông báo'
          }
          data-testid="parent-shell-notifications"
        >
          <Bell className="h-5 w-5" aria-hidden />
          {unreadCount > 0 && (
            <span
              className="absolute right-2 top-2 inline-flex h-2.5 w-2.5 rounded-full border-2 border-card bg-destructive"
              aria-hidden
            />
          )}
        </Link>
      </header>

      <main className="flex-1 overflow-y-auto pb-4">{children}</main>

      <nav
        className="fixed inset-x-0 bottom-0 z-30 mx-auto flex max-w-[480px] items-stretch border-t bg-card/95 backdrop-blur"
        aria-label="Điều hướng chính"
        data-testid="parent-shell-tabs"
        style={{ paddingBottom: 'env(safe-area-inset-bottom)' }}
      >
        {TABS.map((tab) => {
          const Icon = tab.icon;
          const isActive = tab.matches(pathname);
          return (
            <Link
              key={tab.href}
              href={tab.href}
              className={cn(
                'flex min-h-11 flex-1 flex-col items-center justify-center gap-0.5 px-2 py-2 text-[11px] font-medium transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-ring',
                isActive
                  ? 'text-primary'
                  : 'text-muted-foreground hover:text-foreground',
              )}
              aria-current={isActive ? 'page' : undefined}
              data-testid={`parent-tab-${tab.href.replace(/\//g, '-')}`}
            >
              <Icon className="h-5 w-5" aria-hidden />
              <span>{tab.label}</span>
            </Link>
          );
        })}
      </nav>
    </div>
  );
}
