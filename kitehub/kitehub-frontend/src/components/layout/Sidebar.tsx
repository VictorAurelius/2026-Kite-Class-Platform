'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { cn } from '@/lib/utils';
import { LayoutDashboard, CreditCard, Palette, Settings, Building2, TrendingUp, ClipboardList, type LucideIcon } from 'lucide-react';
import { KiteLogo } from '@/components/brand/KiteLogo';

interface NavItem {
  href: string;
  label: string;
  icon: LucideIcon;
  testId?: string;
}

const customerNav: NavItem[] = [
  { href: '/dashboard', label: 'Tổng quan', icon: LayoutDashboard },
  { href: '/billing', label: 'Thanh toán', icon: CreditCard },
  { href: '/branding', label: 'AI Branding', icon: Palette },
  { href: '/settings', label: 'Cài đặt', icon: Settings },
];

// GAP-519: admin sidebar nav — 4 testid'd links so PLATFORM_ADMIN can navigate
// to beta-requests / instances / payments / revenue from /admin home.
const adminNav: NavItem[] = [
  { href: '/admin/beta-requests', label: 'Beta Requests', icon: ClipboardList, testId: 'admin-nav-beta-requests' },
  { href: '/admin/instances', label: 'Instances', icon: Building2, testId: 'admin-nav-instances' },
  { href: '/admin/payments', label: 'Payments', icon: CreditCard, testId: 'admin-nav-payments' },
  { href: '/admin/revenue', label: 'Revenue', icon: TrendingUp, testId: 'admin-nav-revenue' },
];

export function Sidebar({ variant = 'customer' }: { variant?: 'customer' | 'admin' }) {
  const pathname = usePathname();
  const navItems = variant === 'admin' ? adminNav : customerNav;

  return (
    <aside className="w-64 border-r bg-muted/30 dark:bg-muted/50 min-h-screen p-4">
      <div className="mb-8">
        <Link href="/" className="block">
          <KiteLogo size="sm" />
          <p className="mt-0.5 text-[10px] italic text-muted-foreground">Quản lý giáo dục thông minh</p>
        </Link>
        {variant === 'admin' && (
          <span className="mt-2 inline-block rounded bg-destructive/10 px-2 py-0.5 text-xs font-medium text-destructive">
            Admin
          </span>
        )}
      </div>
      <nav className="space-y-1">
        {navItems.map((item) => {
          const Icon = item.icon;
          const isActive = pathname === item.href || (item.href !== '/' && pathname.startsWith(item.href));
          return (
            <Link
              key={item.href}
              href={item.href}
              data-testid={item.testId}
              className={cn(
                'flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm transition-all',
                isActive
                  ? 'bg-primary/10 text-primary font-medium shadow-sm'
                  : 'text-muted-foreground hover:bg-muted hover:text-foreground'
              )}
            >
              <Icon className="h-4 w-4 shrink-0" />
              {item.label}
            </Link>
          );
        })}
      </nav>
    </aside>
  );
}
