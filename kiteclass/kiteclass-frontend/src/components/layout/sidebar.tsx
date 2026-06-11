/**
 * Sidebar navigation component for dashboard layout.
 * Desktop: fixed sidebar. Mobile: hidden (use MobileSidebar with Sheet).
 *
 * Wave RBAC-Shell 1 Bucket B (GAP-1119): the nav is now role-aware. OWNER / ADMIN
 * get the full school-management surface; STAFF gets the operational subset only
 * (enrollment + attendance + invoice). Items come from {@link navItemsForRole}.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */

'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { GraduationCap } from 'lucide-react';
import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { useAuthStore } from '@/stores/auth-store';
import { normalizeRole } from '@/lib/auth/roles';
import { navItemsForRole } from './dashboard-nav';

/** Shared nav content — used by both desktop Sidebar and mobile Sheet */
export function SidebarNav({ onNavigate }: { onNavigate?: () => void }) {
  const pathname = usePathname();
  // Role-aware nav: owner/staff/admin see different surfaces per GAP-1119.
  const rawRole = useAuthStore((s) => s.user?.userType);
  const navItems = navItemsForRole(normalizeRole(rawRole));

  return (
    <div className="flex h-full flex-col">
      {/* Logo */}
      <div className="flex h-16 items-center border-b px-6">
        <Link href="/dashboard" className="flex items-center gap-2" onClick={onNavigate}>
          <GraduationCap className="h-6 w-6 text-primary" />
          <span className="text-xl font-bold">KiteClass</span>
        </Link>
      </div>

      {/* Navigation */}
      <nav className="flex-1 space-y-1 overflow-y-auto px-3 py-4">
        {navItems.map((item) => {
          const isActive = pathname === item.href || pathname.startsWith(`${item.href}/`);
          const Icon = item.icon;

          return (
            <Link key={item.href} href={item.href} onClick={onNavigate}>
              <Button
                variant={isActive ? 'secondary' : 'ghost'}
                className={cn(
                  // min-h-[44px]: WCAG 2.5.5 touch target ≥44px for mobile drawer nav
                  'min-h-[44px] w-full justify-start gap-3',
                  isActive && 'bg-secondary font-medium'
                )}
              >
                <Icon className="h-4 w-4" />
                {item.title}
                {item.badge && (
                  <span className="ml-auto rounded-full bg-primary px-2 py-0.5 text-xs text-primary-foreground">
                    {item.badge}
                  </span>
                )}
              </Button>
            </Link>
          );
        })}
      </nav>

      {/* Footer */}
      <div className="border-t p-4">
        <p className="text-center text-xs text-muted-foreground">
          © 2026 KiteClass Platform
        </p>
      </div>
    </div>
  );
}

/** Desktop-only fixed sidebar (hidden on mobile) */
export function Sidebar() {
  return (
    <aside className="fixed left-0 top-0 z-40 hidden h-screen w-64 border-r bg-background md:flex md:flex-col">
      <SidebarNav />
    </aside>
  );
}
