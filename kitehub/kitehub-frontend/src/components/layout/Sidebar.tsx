'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { cn } from '@/lib/utils';

interface NavItem {
  href: string;
  label: string;
  icon: string;
}

const customerNav: NavItem[] = [
  { href: '/dashboard', label: 'Tổng quan', icon: '📊' },
  { href: '/billing', label: 'Thanh toán', icon: '💳' },
  { href: '/branding', label: 'AI Branding', icon: '🎨' },
  { href: '/settings', label: 'Cài đặt', icon: '⚙️' },
];

const adminNav: NavItem[] = [
  { href: '/admin', label: 'Dashboard', icon: '📊' },
  { href: '/admin/instances', label: 'Instances', icon: '🏢' },
  { href: '/admin/payments', label: 'Thanh toán', icon: '💳' },
  { href: '/admin/revenue', label: 'Doanh thu', icon: '📈' },
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
        {navItems.map((item) => (
          <Link
            key={item.href}
            href={item.href}
            className={cn(
              'flex items-center gap-3 rounded-md px-3 py-2 text-sm transition-colors',
              pathname === item.href
                ? 'bg-primary/10 text-primary font-medium'
                : 'text-muted-foreground hover:bg-muted hover:text-foreground'
            )}
          >
            <span>{item.icon}</span>
            {item.label}
          </Link>
        ))}
      </nav>
    </aside>
  );
}
