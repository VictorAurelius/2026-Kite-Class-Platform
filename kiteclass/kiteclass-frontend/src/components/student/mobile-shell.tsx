/**
 * Mobile shell wrapper for the student PWA — sticky header + scroll body
 * + persistent bottom tabs.
 *
 * Wave 49 Bucket C (Track 2 Phase 4 — GAP-269). Pages compose by passing
 * a title (left) and optional `headerRight` (e.g. notification bell).
 *
 * The `pb-20` on the main element reserves space for {@link StudentBottomTabs}
 * (56px tab + safe-area-inset-bottom on notch devices).
 */
'use client';

import type { ReactNode } from 'react';
import { StudentBottomTabs } from './bottom-tabs';

interface StudentMobileShellProps {
  title: string;
  subtitle?: string;
  headerRight?: ReactNode;
  children: ReactNode;
}

export function StudentMobileShell({
  title,
  subtitle,
  headerRight,
  children,
}: StudentMobileShellProps) {
  return (
    <div className="mx-auto flex min-h-[100dvh] max-w-screen-sm flex-col bg-background">
      <header className="sticky top-0 z-30 flex items-center justify-between gap-2 border-b border-border bg-background/95 px-4 py-3 pt-[calc(env(safe-area-inset-top)+0.75rem)] backdrop-blur supports-[backdrop-filter]:bg-background/80">
        <div className="min-w-0">
          <h1 className="truncate text-base font-semibold leading-tight">
            {title}
          </h1>
          {subtitle ? (
            <p className="truncate text-xs text-muted-foreground">
              {subtitle}
            </p>
          ) : null}
        </div>
        {headerRight}
      </header>
      <main className="flex-1 overflow-x-hidden px-4 pb-24 pt-4">
        {children}
      </main>
      <StudentBottomTabs />
    </div>
  );
}
