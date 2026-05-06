/**
 * dashboard-commands — KH-specific command palette dataset.
 *
 * Centralized so Bucket B/C/D consumers can extend without re-instantiating
 * the palette. Sections mirror the kitehub-pro v2 sidebar groups
 * ("Tổng quan", "Tài chính", "Cài đặt").
 *
 * Source spec: kitehub-pro-v2/screens/dashboard-default.html (sidebar nav).
 */

import {
  LayoutDashboard,
  Building2,
  Sparkles,
  Server,
  Receipt,
  CreditCard,
  TrendingUp,
  Users,
  KeyRound,
  Settings,
  HelpCircle,
} from 'lucide-react';
import type { DashboardCommand } from './types';

export const KH_DASHBOARD_COMMANDS: DashboardCommand[] = [
  // Tổng quan
  {
    id: 'nav.dashboard',
    label: 'Bảng điều khiển',
    section: 'Tổng quan',
    href: '/dashboard',
    icon: LayoutDashboard,
    keywords: ['dashboard', 'home', 'overview', 'tổng quan'],
  },
  {
    id: 'nav.instances',
    label: 'Phiên bản trung tâm',
    section: 'Tổng quan',
    href: '/instances',
    icon: Server,
    keywords: ['instances', 'tenants', 'centers', 'trung tâm'],
  },
  {
    id: 'nav.branding',
    label: 'Thương hiệu AI',
    section: 'Tổng quan',
    href: '/branding',
    icon: Sparkles,
    keywords: ['ai', 'branding', 'logo', 'thương hiệu'],
  },
  {
    id: 'action.create-instance',
    label: 'Tạo trung tâm mới',
    section: 'Tổng quan',
    href: '/register',
    icon: Building2,
    hint: 'C',
    keywords: ['create', 'new', 'instance', 'tạo'],
  },
  // Tài chính
  {
    id: 'nav.billing',
    label: 'Hóa đơn',
    section: 'Tài chính',
    href: '/billing',
    icon: Receipt,
    keywords: ['invoices', 'billing', 'hóa đơn'],
  },
  {
    id: 'nav.payment',
    label: 'Thanh toán',
    section: 'Tài chính',
    href: '/billing/payment',
    icon: CreditCard,
    keywords: ['payment', 'thanh toán', 'pay'],
  },
  {
    id: 'nav.upgrade',
    label: 'Nâng cấp gói',
    section: 'Tài chính',
    href: '/billing/upgrade',
    icon: TrendingUp,
    keywords: ['upgrade', 'plan', 'tier', 'nâng cấp'],
  },
  // Cài đặt
  {
    id: 'nav.users',
    label: 'Người dùng',
    section: 'Cài đặt',
    href: '/settings/users',
    icon: Users,
    keywords: ['users', 'team', 'người dùng'],
  },
  {
    id: 'nav.security',
    label: 'Bảo mật',
    section: 'Cài đặt',
    href: '/settings/security',
    icon: KeyRound,
    keywords: ['security', 'password', 'bảo mật'],
  },
  {
    id: 'nav.settings',
    label: 'Cấu hình',
    section: 'Cài đặt',
    href: '/settings',
    icon: Settings,
    keywords: ['settings', 'config', 'cấu hình'],
  },
  // Help
  {
    id: 'help.support',
    label: 'Hỗ trợ',
    section: 'Hỗ trợ',
    href: '/support',
    icon: HelpCircle,
    keywords: ['help', 'support', 'hỗ trợ', 'contact'],
  },
];
