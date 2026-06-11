/**
 * Role-aware dashboard navigation config.
 *
 * Wave RBAC-Shell 1 Bucket B (GAP-1119): the `(dashboard)` shell is shared by
 * OWNER / ADMIN / STAFF — but each persona sees a DIFFERENT nav surface per the
 * GAP-1119 role table:
 *
 *   - OWNER / ADMIN — full school-management surface (course/class + teachers +
 *     students + billing học phí + payroll + branding + analytics + role-assign).
 *   - STAFF — operational subset only (enrollment + attendance + invoice).
 *
 * TEACHER / PARENT / STUDENT use their OWN shells (TeacherShell / ParentShell /
 * StudentMobileShell) and never render this sidebar, so they are intentionally
 * absent from every item's allow-list.
 *
 * This is a DISPLAY filter — actual route access is enforced by `RoleGuard`
 * (Bucket A) at the leaf layouts, not here.
 *
 * @author KiteClass Team
 */

import {
  Home,
  Users,
  GraduationCap,
  BookOpen,
  Calendar,
  ClipboardCheck,
  DollarSign,
  BarChart,
  Settings,
  Wallet,
  Palette,
  ShieldCheck,
  type LucideIcon,
} from 'lucide-react';
import { UserType } from '@/types/auth';
import type { KcRole } from '@/lib/auth/roles';

export interface DashboardNavItem {
  title: string;
  href: string;
  icon: LucideIcon;
  /** Normalized roles permitted to SEE this item in the sidebar. */
  roles: readonly KcRole[];
  badge?: string;
}

/** Operational items shared by owner + staff. */
const OWNER_ADMIN_STAFF: readonly KcRole[] = [
  UserType.OWNER,
  UserType.ADMIN,
  UserType.STAFF,
];
/** Owner-governance items (teachers, payroll, branding, analytics, role-assign). */
const OWNER_ADMIN: readonly KcRole[] = [UserType.OWNER, UserType.ADMIN];

/**
 * Canonical dashboard nav. Ordered so the STAFF-visible (shared) items form the
 * top contiguous block; owner-only governance items follow.
 */
export const DASHBOARD_NAV: readonly DashboardNavItem[] = [
  { title: 'Tổng quan', href: '/dashboard', icon: Home, roles: OWNER_ADMIN_STAFF },
  { title: 'Học viên', href: '/students', icon: Users, roles: OWNER_ADMIN_STAFF },
  { title: 'Lớp học', href: '/classes', icon: Calendar, roles: OWNER_ADMIN_STAFF },
  { title: 'Điểm danh', href: '/attendance', icon: ClipboardCheck, roles: OWNER_ADMIN_STAFF },
  { title: 'Học phí', href: '/billing', icon: DollarSign, roles: OWNER_ADMIN_STAFF },
  { title: 'Giáo viên', href: '/teachers', icon: GraduationCap, roles: OWNER_ADMIN },
  { title: 'Khóa học', href: '/courses', icon: BookOpen, roles: OWNER_ADMIN },
  { title: 'Báo cáo', href: '/reports', icon: BarChart, roles: OWNER_ADMIN },
  { title: 'Bảng lương', href: '/admin/payroll', icon: Wallet, roles: OWNER_ADMIN },
  { title: 'Thương hiệu', href: '/branding', icon: Palette, roles: OWNER_ADMIN },
  // Role-assignment UI itself is Bucket D — this links to the placeholder page.
  { title: 'Phân quyền', href: '/admin/roles', icon: ShieldCheck, roles: OWNER_ADMIN },
  { title: 'Cài đặt', href: '/settings', icon: Settings, roles: OWNER_ADMIN_STAFF },
];

/**
 * Filter the dashboard nav for a given normalized role.
 *
 * `null` only occurs during the brief Zustand hydration window — default to
 * OWNER so the sidebar never flashes empty (owner/staff/admin are the only
 * DashboardLayout consumers; route access is guarded separately).
 */
export function navItemsForRole(
  role: KcRole | null | undefined,
): readonly DashboardNavItem[] {
  const effective = role ?? UserType.OWNER;
  return DASHBOARD_NAV.filter((item) => item.roles.includes(effective));
}
