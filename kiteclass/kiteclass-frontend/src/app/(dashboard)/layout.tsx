/**
 * Dashboard Layout with Auth Protection + KC pro v2 foundation primitives.
 *
 * Wave 30 Bucket A integration:
 *   - Mounts <CommandPalette> once for the route group (⌘K available
 *     everywhere under (dashboard)).
 *   - useDashboardTheme is consumed by child pages; provider already mounted
 *     at root layout via NextThemesProvider.
 *
 * @author KiteClass Team
 * @since 3.9.0
 */

'use client';

import { useEffect, useMemo, useState } from 'react';
import { useRouter } from 'next/navigation';
import {
  BookOpen,
  CalendarCheck,
  CreditCard,
  LayoutDashboard,
  Settings,
  Users,
} from 'lucide-react';
import { useAuthStore } from '@/stores/auth-store';
import { LoadingSpinner } from '@/components/common/loading-spinner';
import {
  CommandPalette,
  useCommandPalette,
  type DashboardCommand,
} from '@/_shared/dashboard-foundation';
import { BrandingThemeApplier } from '@/components/theme/BrandingThemeApplier';

const DEFAULT_COMMANDS: DashboardCommand[] = [
  {
    id: 'nav-home',
    label: 'Tổng quan',
    section: 'Điều hướng',
    href: '/',
    icon: LayoutDashboard,
    keywords: ['dashboard', 'home', 'overview'],
  },
  {
    id: 'nav-classes',
    label: 'Lớp học',
    section: 'Điều hướng',
    href: '/classes',
    icon: BookOpen,
    keywords: ['classes', 'lop'],
  },
  {
    id: 'nav-students',
    label: 'Học viên',
    section: 'Điều hướng',
    href: '/students',
    icon: Users,
    keywords: ['students', 'hoc vien'],
  },
  {
    id: 'nav-attendance',
    label: 'Điểm danh',
    section: 'Điều hướng',
    href: '/attendance',
    icon: CalendarCheck,
    keywords: ['attendance', 'diem danh'],
  },
  {
    id: 'nav-billing',
    label: 'Học phí',
    section: 'Tài chính',
    href: '/billing',
    icon: CreditCard,
    keywords: ['billing', 'invoice', 'hoc phi'],
  },
  {
    id: 'nav-settings',
    label: 'Cài đặt',
    section: 'Khác',
    href: '/settings',
    icon: Settings,
    keywords: ['settings', 'cai dat'],
  },
];

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const router = useRouter();
  const [isHydrated, setIsHydrated] = useState(false);
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const accessToken = useAuthStore((state) => state.accessToken);
  const palette = useCommandPalette();

  // Memoize so the palette doesn't re-rank on every render.
  const commands = useMemo<DashboardCommand[]>(() => DEFAULT_COMMANDS, []);

  // Wait for Zustand hydration from localStorage
  useEffect(() => {
    setIsHydrated(true);
  }, []);

  useEffect(() => {
    // Only check auth AFTER hydration completes
    if (!isHydrated) return;

    // Check if user is authenticated
    if (!isAuthenticated || !accessToken) {
      // Redirect to login page
      router.push('/login');
    }
  }, [isHydrated, isAuthenticated, accessToken, router]);

  // Show loading while hydrating or checking auth
  if (!isHydrated || !isAuthenticated || !accessToken) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  return (
    <>
      {/* Apply persisted tenant branding colours to the dashboard (GAP-807). */}
      <BrandingThemeApplier />
      {children}
      <CommandPalette
        open={palette.open}
        onOpenChange={palette.setOpen}
        commands={commands}
      />
    </>
  );
}
