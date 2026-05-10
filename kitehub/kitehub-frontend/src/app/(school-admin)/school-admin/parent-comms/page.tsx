/**
 * Parent communication monitor — escalation queue + SLA timer.
 *
 * Wave 50 Bucket A (GAP-271). Source: ui_kits/kitehub-admin/screens/parent-comms.html
 */

'use client';

import { Send, AlertTriangle } from 'lucide-react';
import { PageHeader, Pill, SlaIndicator, KpiCard } from '@/components/school-admin/school-admin-ui';
import { SCHOOL_PROFILE, PARENT_COMMS } from '@/components/school-admin/school-admin-mock-data';

const STATUS_PILL = {
  pending: { kind: 'warning' as const, label: 'Chờ xử lý' },
  in_progress: { kind: 'info' as const, label: 'Đang xử lý' },
  escalated: { kind: 'danger' as const, label: 'Leo thang' },
  resolved: { kind: 'success' as const, label: 'Đã xử lý' },
};

const CHANNEL_PILL = {
  'Zalo OA': 'info',
  Email: 'neutral',
  Hotline: 'danger',
  'In-app': 'success',
} as const;

export default function ParentCommsPage() {
  const escalated = PARENT_COMMS.filter((p) => p.status === 'escalated').length;
  const overdue = PARENT_COMMS.filter((p) => p.slaHoursLeft < 0).length;
  const pending = PARENT_COMMS.filter((p) => p.status === 'pending').length;
  const avgSla = (PARENT_COMMS.reduce((s, p) => s + Math.abs(p.slaHoursLeft), 0) / PARENT_COMMS.length).toFixed(1);

  return (
    <div>
      <PageHeader
        title="Hộp thư phụ huynh"
        subtitle="Hàng đợi xử lý + SLA giám sát · Mục tiêu phản hồi <4h trong giờ hành chính"
        breadcrumbs={[{ label: SCHOOL_PROFILE.name, href: '/school-admin/dashboard' }, { label: 'Nhân sự' }, { label: 'Phụ huynh' }]}
        actions={
          <button type="button" className="inline-flex items-center gap-1.5 rounded-md bg-primary px-3 py-1.5 text-sm font-medium text-primary-foreground hover:bg-primary/90">
            <Send className="h-4 w-4" aria-hidden="true" />
            Soạn thông báo
          </button>
        }
      />

      <div className="mb-4 grid grid-cols-2 gap-3 md:grid-cols-4">
        <KpiCard label="Tổng yêu cầu" value={PARENT_COMMS.length.toString()} delta="Tuần này" deltaDirection="flat" sub="Mọi kênh" />
        <KpiCard label="Chờ xử lý" value={pending.toString()} delta="Cần phản hồi" deltaDirection="up" sub="Trong giờ" />
        <KpiCard label="Trễ SLA" value={overdue.toString()} delta="Vượt 4h" deltaDirection="down" sub={`Cần xử lý ngay`} />
        <KpiCard label="Leo thang" value={escalated.toString()} delta="Tới hiệu trưởng" deltaDirection="down" sub={`SLA TB ${avgSla}h`} />
      </div>

      {escalated > 0 && (
        <div className="mb-4 flex items-start gap-3 rounded-lg border border-red-300 bg-red-50 p-3 dark:border-red-700/40 dark:bg-red-950/20">
          <AlertTriangle className="h-5 w-5 shrink-0 text-red-600" aria-hidden="true" />
          <div className="text-sm">
            <div className="font-semibold text-red-900 dark:text-red-200">Có {escalated} ca đã leo thang</div>
            <p className="mt-0.5 text-xs text-red-800 dark:text-red-300">
              Các ca đã chuyển từ GVCN/Tổ trưởng lên hiệu trưởng. Cần phản hồi trong 24h theo quy chế trường.
            </p>
          </div>
        </div>
      )}

      <div className="overflow-x-auto rounded-lg border bg-background">
        <table className="w-full text-sm" aria-label="Yêu cầu phụ huynh">
          <thead>
            <tr className="border-b bg-muted/40 text-xs uppercase text-muted-foreground">
              <th className="px-3 py-2 text-left">Phụ huynh</th>
              <th className="px-3 py-2 text-left">Học sinh / Lớp</th>
              <th className="px-3 py-2 text-left">Nội dung</th>
              <th className="px-3 py-2 text-left">Kênh</th>
              <th className="px-3 py-2 text-left">Nhận lúc</th>
              <th className="px-3 py-2 text-left">SLA</th>
              <th className="px-3 py-2 text-left">Trạng thái</th>
              <th className="px-3 py-2 text-left">Phụ trách</th>
            </tr>
          </thead>
          <tbody>
            {PARENT_COMMS.map((p) => (
              <tr key={p.id} className={`border-b last:border-0 hover:bg-muted/30 ${p.status === 'escalated' ? 'bg-red-50/40 dark:bg-red-950/10' : ''}`}>
                <td className="px-3 py-2 font-medium">{p.parentName}</td>
                <td className="px-3 py-2">
                  <div>{p.studentName}</div>
                  <div className="font-mono text-xs text-muted-foreground">{p.classCode}</div>
                </td>
                <td className="px-3 py-2 max-w-md">{p.topic}</td>
                <td className="px-3 py-2">
                  <Pill kind={CHANNEL_PILL[p.channel]}>{p.channel}</Pill>
                </td>
                <td className="px-3 py-2 font-mono text-xs">{p.receivedAt}</td>
                <td className="px-3 py-2">
                  <SlaIndicator hoursLeft={p.slaHoursLeft} />
                </td>
                <td className="px-3 py-2">
                  <Pill kind={STATUS_PILL[p.status].kind}>{STATUS_PILL[p.status].label}</Pill>
                </td>
                <td className="px-3 py-2 text-xs text-muted-foreground">{p.assignedTo}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
