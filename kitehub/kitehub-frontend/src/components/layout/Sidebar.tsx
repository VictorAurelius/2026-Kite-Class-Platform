'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { cn } from '@/lib/utils';
import { LayoutDashboard, CreditCard, Palette, Settings, Building2, TrendingUp, type LucideIcon } from 'lucide-react';

interface NavItem {
  href: string;
  label: string;
  icon: LucideIcon;
}

const customerNav: NavItem[] = [
  { href: '/dashboard', label: 'Tổng quan', icon: LayoutDashboard },
  { href: '/billing', label: 'Thanh toán', icon: CreditCard },
  { href: '/branding', label: 'AI Branding', icon: Palette },
  { href: '/settings', label: 'Cài đặt', icon: Settings },
];

const adminNav: NavItem[] = [
  { href: '/admin', label: 'Dashboard', icon: LayoutDashboard },
  { href: '/admin/instances', label: 'Instances', icon: Building2 },
  { href: '/admin/payments', label: 'Thanh toán', icon: CreditCard },
  { href: '/admin/revenue', label: 'Doanh thu', icon: TrendingUp },
];

export function Sidebar({ variant = 'customer' }: { variant?: 'customer' | 'admin' }) {
  const pathname = usePathname();
  const navItems = variant === 'admin' ? adminNav : customerNav;

  return (
    <aside className="w-64 border-r bg-muted/30 min-h-screen p-4">
      <div className="mb-8">
        <Link href="/" className="text-xl font-bold text-primary">
          KiteHub
        </Link>
        {variant === 'admin' && (
          <span className="ml-2 rounded bg-destructive/10 px-2 py-0.5 text-xs font-medium text-destructive">
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
