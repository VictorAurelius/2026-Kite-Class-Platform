/**
 * Dashboard home (route-group index) — KC pro v2 foundation showcase.
 *
 * Wave 30 Bucket A: refactored to apply kiteclass-pro-v2 design tokens via
 * the new dashboard-foundation primitives. This page serves both as the
 * landing page for the (dashboard) route group AND as the integration smoke
 * test for ThemeProvider + CommandPalette + KPICard + Sparkline.
 *
 * Source spec: kiteclass-pro-v2/screens/dashboard-default.html.
 */

'use client';

import {
  BookOpen,
  CalendarCheck,
  CreditCard,
  GraduationCap,
  TrendingUp,
  Users,
} from 'lucide-react';
import { KPICard, type KPIData } from '@/_shared/dashboard-foundation';

const KPI_ROW: KPIData[] = [
  {
    label: 'Học viên',
    value: '428',
    delta: 6.4,
    sparkline: [380, 392, 401, 410, 415, 420, 428],
    tone: 'positive',
    icon: <Users className="h-4 w-4" />,
  },
  {
    label: 'Lớp đang chạy',
    value: '24',
    delta: 0,
    sparkline: [22, 22, 23, 24, 24, 24, 24],
    tone: 'neutral',
    icon: <BookOpen className="h-4 w-4" />,
  },
  {
    label: 'Giáo viên',
    value: '18',
    delta: 5.9,
    sparkline: [16, 17, 17, 17, 18, 18, 18],
    tone: 'positive',
    icon: <GraduationCap className="h-4 w-4" />,
  },
  {
    label: 'Điểm danh hôm nay',
    value: '92%',
    delta: 1.8,
    sparkline: [88, 90, 89, 91, 92, 93, 92],
    tone: 'positive',
    icon: <CalendarCheck className="h-4 w-4" />,
  },
  {
    label: 'Doanh thu tuần',
    value: '₫42M',
    delta: 8.2,
    sparkline: [32, 34, 36, 35, 38, 40, 42],
    tone: 'positive',
    icon: <CreditCard className="h-4 w-4" />,
  },
  {
    label: 'Tỷ lệ giữ chân',
    value: '94.6%',
    delta: -0.4,
    sparkline: [95, 95, 95.2, 94.8, 94.7, 94.6, 94.6],
    tone: 'warning',
    icon: <TrendingUp className="h-4 w-4" />,
  },
];

export default function DashboardHomePage() {
  return (
    <div className="space-y-6 p-6">
      <header className="flex items-end justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Tổng quan</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Hôm nay có 12 buổi học · 3 lớp đang chờ điểm danh · doanh thu tuần
            tăng 8.2%.
          </p>
        </div>
      </header>

      <section
        aria-label="Chỉ số trung tâm"
        className="grid gap-4 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6"
      >
        {KPI_ROW.map((kpi) => (
          <KPICard key={kpi.label} {...kpi} />
        ))}
      </section>

      <section
        aria-label="Hoạt động gần đây"
        className="rounded-xl border border-border bg-card p-4"
      >
        <h2 className="text-sm font-semibold text-foreground">Hoạt động gần đây</h2>
        <p className="mt-2 text-sm text-muted-foreground">
          Bucket B sẽ thêm danh sách hoạt động chi tiết — placeholder để
          foundation render đầy đủ.
        </p>
      </section>
    </div>
  );
}
