/**
 * School-admin dashboard — overview KPIs + pending tasks + recent enrollments.
 *
 * Wave 50 Bucket A (GAP-271). Source: ui_kits/kitehub-admin/screens/dashboard.html
 */

'use client';

import Link from 'next/link';
import { Bell, Upload, FileText, Database, CloudUpload, BellRing } from 'lucide-react';
import { PageHeader, KpiCard, Pill, MoetStamp } from '@/components/school-admin/school-admin-ui';
import {
  SCHOOL_PROFILE,
  DASHBOARD_KPIS,
  PENDING_TASKS,
  GRADE_BLOCKS,
  RECENT_ENROLLMENTS,
  CALENDAR_EVENTS,
  DATA_SYNC_STATUS,
} from '@/components/school-admin/school-admin-mock-data';

const URGENCY_BORDER: Record<string, string> = {
  urgent: 'border-l-4 border-l-red-500',
  high: 'border-l-4 border-l-amber-500',
  normal: 'border-l-4 border-l-blue-400',
};

const FEE_PILL = {
  paid: { kind: 'success' as const, label: 'Đã đóng' },
  pending: { kind: 'warning' as const, label: 'Chờ thanh toán' },
  overdue: { kind: 'danger' as const, label: 'Trễ hạn' },
};

const STUDENT_PILL = {
  active: { kind: 'success' as const, label: 'Đang học' },
  pending: { kind: 'info' as const, label: 'Chờ xác nhận' },
};

export default function DashboardPage() {
  const today = new Date().toLocaleDateString('vi-VN');
  return (
    <div>
      <PageHeader
        title={`Chào buổi sáng, ${SCHOOL_PROFILE.principalName.split(' ').pop() ?? SCHOOL_PROFILE.principalName} 👋`}
        subtitle={`Hôm nay ${today} · Tuần ${SCHOOL_PROFILE.weekNumber}/${SCHOOL_PROFILE.totalWeeks} ${SCHOOL_PROFILE.semester} · ${SCHOOL_PROFILE.totalStudents.toLocaleString('vi-VN')} học sinh đang học · ${SCHOOL_PROFILE.totalTeachers} giáo viên`}
        breadcrumbs={[{ label: SCHOOL_PROFILE.name, href: '/school-admin/dashboard' }, { label: `Tổng quan · ${SCHOOL_PROFILE.semester}` }]}
        actions={
          <>
            <button type="button" className="inline-flex items-center gap-1.5 rounded-md border bg-background px-3 py-1.5 text-sm font-medium hover:bg-muted" aria-label="Thông báo">
              <Bell className="h-4 w-4" aria-hidden="true" />
              <span className="rounded-full bg-destructive px-1.5 text-[10px] font-semibold text-destructive-foreground">7</span>
            </button>
            <Link href="/school-admin/bulk-import" className="inline-flex items-center gap-1.5 rounded-md border bg-background px-3 py-1.5 text-sm font-medium hover:bg-muted">
              <Upload className="h-4 w-4" aria-hidden="true" />
              Nhập danh sách
            </Link>
            <Link href="/school-admin/report-cards" className="inline-flex items-center gap-1.5 rounded-md bg-primary px-3 py-1.5 text-sm font-medium text-primary-foreground hover:bg-primary/90">
              <FileText className="h-4 w-4" aria-hidden="true" />
              Tạo học bạ HK1
            </Link>
          </>
        }
      />

      <section aria-label="Chỉ số chính của trường" className="mb-6">
        <div className="mb-2 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
          Chỉ số toàn trường · {SCHOOL_PROFILE.semester}
        </div>
        <div className="grid grid-cols-2 gap-3 md:grid-cols-3 xl:grid-cols-6">
          {DASHBOARD_KPIS.map((k) => (
            <KpiCard key={k.label} {...k} />
          ))}
        </div>
      </section>

      <section className="mb-6 grid grid-cols-1 gap-4 lg:grid-cols-3" aria-label="Việc cần làm và hoạt động">
        <div className="rounded-lg border bg-background p-4 lg:col-span-2">
          <div className="mb-3 flex items-center justify-between">
            <h2 className="text-base font-semibold">Việc cần xử lý</h2>
            <Link href="#" className="text-xs text-muted-foreground hover:text-foreground hover:underline">
              Xem tất cả
            </Link>
          </div>
          <ul className="space-y-2">
            {PENDING_TASKS.map((t) => (
              <li key={t.id} className={`flex items-center gap-3 rounded-md bg-muted/30 p-3 ${URGENCY_BORDER[t.urgency]}`}>
                <div className="flex h-9 w-9 items-center justify-center rounded-full bg-primary/10 text-xs font-semibold text-primary" aria-hidden="true">
                  {t.avatarText}
                </div>
                <div className="min-w-0 flex-1">
                  <div className="truncate text-sm font-medium">{t.title}</div>
                  <div className="truncate text-xs text-muted-foreground">{t.sub}</div>
                  <div className="mt-1.5 flex items-center gap-2">
                    <Pill kind={t.pillKind}>{t.pillLabel}</Pill>
                    <Pill kind={t.slaKind === 'bad' ? 'danger' : t.slaKind === 'warn' ? 'warning' : 'success'}>{t.slaLabel}</Pill>
                  </div>
                </div>
                <Link
                  href={t.ctaHref}
                  className="shrink-0 rounded-md border bg-background px-3 py-1 text-xs font-medium hover:bg-muted"
                >
                  {t.ctaLabel}
                </Link>
              </li>
            ))}
          </ul>
        </div>

        <div className="space-y-4">
          <div className="rounded-lg border bg-background p-4">
            <div className="mb-2 text-xs font-semibold uppercase tracking-wider text-muted-foreground">Khối lớp</div>
            <div className="space-y-2">
              {GRADE_BLOCKS.map((b) => (
                <div key={b.name}>
                  <div className="flex items-center justify-between text-sm">
                    <span>
                      {b.name} ({b.classCount} lớp)
                    </span>
                    <span className="font-mono text-xs text-muted-foreground">{b.studentCount} HS</span>
                  </div>
                  <div className="mt-1 h-1.5 w-full rounded-full bg-muted">
                    <div className="h-full rounded-full bg-primary" style={{ width: `${b.fillPct}%` }} />
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div className="rounded-lg border bg-background p-4">
            <div className="mb-2 text-xs font-semibold uppercase tracking-wider text-muted-foreground">Sự kiện sắp tới</div>
            <ul className="space-y-3 text-sm">
              {CALENDAR_EVENTS.slice(0, 3).map((e) => (
                <li key={e.title} className="flex gap-3">
                  <div className="w-14 shrink-0 font-mono text-xs text-muted-foreground">{e.date.slice(0, 5)}</div>
                  <div className="flex-1">
                    <div className="font-medium">{e.title}</div>
                    {e.description && <div className="text-xs text-muted-foreground">{e.description}</div>}
                    {e.type === 'moet' && (
                      <div className="mt-1">
                        <MoetStamp />
                      </div>
                    )}
                  </div>
                </li>
              ))}
            </ul>
          </div>

          <div className="rounded-lg border bg-background p-4">
            <div className="mb-2 text-xs font-semibold uppercase tracking-wider text-muted-foreground">Trạng thái dữ liệu</div>
            <ul className="space-y-2 text-sm">
              {DATA_SYNC_STATUS.map((s, i) => {
                const Icon = i === 0 ? Database : i === 1 ? CloudUpload : BellRing;
                return (
                  <li key={s.label} className="flex items-center justify-between">
                    <span className="flex items-center gap-1.5">
                      <Icon className="h-3.5 w-3.5 text-muted-foreground" aria-hidden="true" />
                      {s.label}
                    </span>
                    <Pill kind={s.status}>{s.detail}</Pill>
                  </li>
                );
              })}
            </ul>
          </div>
        </div>
      </section>

      <section aria-label="Học sinh mới đăng ký 7 ngày qua">
        <div className="mb-2 flex items-center justify-between">
          <h2 className="text-base font-semibold">Học sinh mới — 7 ngày qua</h2>
          <Link href="/school-admin/bulk-import" className="text-sm text-muted-foreground hover:text-foreground hover:underline">
            Đi tới Nhập danh sách →
          </Link>
        </div>
        <div className="overflow-x-auto rounded-lg border bg-background">
          <table className="w-full text-sm" aria-label="Học sinh mới đăng ký">
            <thead>
              <tr className="border-b bg-muted/40 text-xs uppercase text-muted-foreground">
                <th className="px-3 py-2 text-left">Ngày</th>
                <th className="px-3 py-2 text-left">Họ tên</th>
                <th className="px-3 py-2 text-left">Lớp</th>
                <th className="px-3 py-2 text-left">Phụ huynh</th>
                <th className="px-3 py-2 text-left">SĐT</th>
                <th className="px-3 py-2 text-left">Học phí</th>
                <th className="px-3 py-2 text-left">Trạng thái</th>
              </tr>
            </thead>
            <tbody>
              {RECENT_ENROLLMENTS.map((s) => (
                <tr key={s.fullName} className="border-b last:border-0 hover:bg-muted/30">
                  <td className="px-3 py-2 font-mono text-xs">{s.date}</td>
                  <td className="px-3 py-2">
                    <div className="flex items-center gap-2">
                      <span className="flex h-6 w-6 items-center justify-center rounded-full bg-primary/10 text-[10px] font-semibold text-primary">
                        {s.initials}
                      </span>
                      <span>{s.fullName}</span>
                    </div>
                  </td>
                  <td className="px-3 py-2 font-mono">{s.classCode}</td>
                  <td className="px-3 py-2">{s.parent}</td>
                  <td className="px-3 py-2 font-mono text-xs">{s.phone}</td>
                  <td className="px-3 py-2">
                    <Pill kind={FEE_PILL[s.feeStatus].kind}>{FEE_PILL[s.feeStatus].label}</Pill>
                  </td>
                  <td className="px-3 py-2">
                    <Pill kind={STUDENT_PILL[s.studentStatus].kind}>{STUDENT_PILL[s.studentStatus].label}</Pill>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="flex items-center justify-between border-t bg-muted/20 px-3 py-2 text-xs text-muted-foreground">
            <span>Hiển thị 1–{RECENT_ENROLLMENTS.length} của 23 học sinh mới</span>
            <span>Trang 1 / 5</span>
          </div>
        </div>
      </section>
    </div>
  );
}
