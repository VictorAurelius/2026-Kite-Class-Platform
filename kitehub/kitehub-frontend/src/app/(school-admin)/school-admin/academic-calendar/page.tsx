/**
 * Academic calendar — semester, terms, holidays, exam weeks.
 *
 * Wave 50 Bucket A (GAP-271). Source: ui_kits/kitehub-admin/screens/academic-calendar.html
 */

'use client';

import { PageHeader, MoetStamp, Pill } from '@/components/school-admin/school-admin-ui';
import { SCHOOL_PROFILE, CALENDAR_EVENTS } from '@/components/school-admin/school-admin-mock-data';

const TYPE_PILL = {
  holiday: { kind: 'success' as const, label: 'Nghỉ' },
  exam: { kind: 'warning' as const, label: 'Kiểm tra' },
  meeting: { kind: 'info' as const, label: 'Họp' },
  moet: { kind: 'danger' as const, label: 'MoET' },
  event: { kind: 'neutral' as const, label: 'Sự kiện' },
};

export default function AcademicCalendarPage() {
  return (
    <div>
      <PageHeader
        title="Lịch học vụ"
        subtitle={`Năm học ${SCHOOL_PROFILE.academicYear} · ${SCHOOL_PROFILE.semester} · Tuần ${SCHOOL_PROFILE.weekNumber}/${SCHOOL_PROFILE.totalWeeks}`}
        breadcrumbs={[{ label: SCHOOL_PROFILE.name, href: '/school-admin/dashboard' }, { label: 'Tổng quan' }, { label: 'Lịch học vụ' }]}
      />

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
        <div className="rounded-lg border bg-background p-4 lg:col-span-2">
          <div className="mb-3 flex items-center justify-between">
            <h2 className="text-base font-semibold">Sự kiện sắp tới</h2>
            <span className="text-xs text-muted-foreground">{CALENDAR_EVENTS.length} sự kiện</span>
          </div>
          <ul className="divide-y">
            {CALENDAR_EVENTS.map((e) => (
              <li key={e.title} className="flex gap-4 py-3">
                <div className="w-20 shrink-0 font-mono text-sm font-semibold">{e.date}</div>
                <div className="flex-1">
                  <div className="flex items-center gap-2">
                    <span className="font-medium">{e.title}</span>
                    <Pill kind={TYPE_PILL[e.type].kind}>{TYPE_PILL[e.type].label}</Pill>
                  </div>
                  {e.description && <div className="mt-0.5 text-xs text-muted-foreground">{e.description}</div>}
                  {e.type === 'moet' && (
                    <div className="mt-1.5">
                      <MoetStamp />
                    </div>
                  )}
                </div>
              </li>
            ))}
          </ul>
        </div>

        <div className="space-y-4">
          <div className="rounded-lg border bg-background p-4">
            <div className="mb-2 text-xs font-semibold uppercase tracking-wider text-muted-foreground">Cấu trúc năm học</div>
            <ul className="space-y-2 text-sm">
              <li className="flex justify-between">
                <span>Học kỳ 1</span>
                <span className="font-mono text-xs text-muted-foreground">15/08 – 15/01</span>
              </li>
              <li className="flex justify-between">
                <span>Học kỳ 2</span>
                <span className="font-mono text-xs text-muted-foreground">20/01 – 30/05</span>
              </li>
              <li className="flex justify-between">
                <span>Tuần kiểm tra giữa kỳ</span>
                <span className="font-mono text-xs text-muted-foreground">Tuần 8 + 26</span>
              </li>
              <li className="flex justify-between">
                <span>Tuần kiểm tra cuối kỳ</span>
                <span className="font-mono text-xs text-muted-foreground">Tuần 17 + 35</span>
              </li>
            </ul>
          </div>

          <div className="rounded-lg border bg-amber-50 p-4 dark:bg-amber-950/20">
            <MoetStamp size="md" />
            <div className="mt-2 text-sm font-semibold">Tuân thủ Sở GD&ĐT</div>
            <p className="mt-1 text-xs text-muted-foreground">
              Lịch học vụ tuân thủ Quyết định khung kế hoạch thời gian năm học của Sở GD&ĐT TP.HCM. Mọi điều chỉnh cần xin ý kiến phòng GD trước khi áp dụng.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
