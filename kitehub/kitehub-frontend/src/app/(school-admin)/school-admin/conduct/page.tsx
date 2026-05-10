/**
 * Conduct/behaviour tracking — 5-step escalation ladder.
 *
 * Wave 50 Bucket A (GAP-271). Source: ui_kits/kitehub-admin/screens/conduct.html
 */

'use client';

import { useState } from 'react';
import { AlertTriangle, FileText } from 'lucide-react';
import { PageHeader, Pill, KpiCard } from '@/components/school-admin/school-admin-ui';
import {
  SCHOOL_PROFILE,
  CONDUCT_CASES,
  CONDUCT_LADDER,
  type ConductCase,
} from '@/components/school-admin/school-admin-mock-data';

const SEVERITY_PILL = {
  high: { kind: 'danger' as const, label: 'Nghiêm trọng' },
  medium: { kind: 'warning' as const, label: 'Trung bình' },
  low: { kind: 'info' as const, label: 'Nhẹ' },
};

const STATUS_PILL = {
  open: { kind: 'warning' as const, label: 'Mở' },
  in_progress: { kind: 'info' as const, label: 'Đang xử lý' },
  resolved: { kind: 'success' as const, label: 'Đã xử lý' },
};

function StepBadge({ step, currentStep }: { step: number; currentStep?: number }) {
  const isActive = currentStep === step;
  const isPast = currentStep !== undefined && step < currentStep;
  return (
    <div
      className={`flex h-7 w-7 shrink-0 items-center justify-center rounded-full text-xs font-bold ${
        isActive
          ? 'bg-red-500 text-white ring-2 ring-red-200 dark:ring-red-900'
          : isPast
            ? 'bg-emerald-500 text-white'
            : 'bg-muted text-muted-foreground'
      }`}
      aria-label={`Bước ${step}`}
    >
      {step}
    </div>
  );
}

export default function ConductPage() {
  const [filterStep, setFilterStep] = useState<'all' | number>('all');

  const filtered: ConductCase[] = filterStep === 'all' ? CONDUCT_CASES : CONDUCT_CASES.filter((c) => c.step === filterStep);
  const open = CONDUCT_CASES.filter((c) => c.status === 'open').length;
  const escalated = CONDUCT_CASES.filter((c) => c.step >= 4).length;

  return (
    <div>
      <PageHeader
        title="Theo dõi hạnh kiểm"
        subtitle={`${CONDUCT_CASES.length} ca đang theo dõi · 5 bước xử lý theo quy chế MoET`}
        breadcrumbs={[{ label: SCHOOL_PROFILE.name, href: '/school-admin/dashboard' }, { label: 'Học sinh' }, { label: 'Hạnh kiểm' }]}
        actions={
          <button type="button" className="inline-flex items-center gap-1.5 rounded-md bg-primary px-3 py-1.5 text-sm font-medium text-primary-foreground hover:bg-primary/90">
            <FileText className="h-4 w-4" aria-hidden="true" />
            Báo cáo HK
          </button>
        }
      />

      <div className="mb-4 grid grid-cols-2 gap-3 md:grid-cols-4">
        <KpiCard label="Tổng ca" value={CONDUCT_CASES.length.toString()} delta="Tuần này" deltaDirection="flat" sub="Đang theo dõi" />
        <KpiCard label="Đang mở" value={open.toString()} delta="Cần xử lý" deltaDirection="down" sub="Chờ phân công" />
        <KpiCard label="Leo thang ≥B4" value={escalated.toString()} delta="Cần HT trực tiếp" deltaDirection="down" sub="Bước 4–5" />
        <KpiCard label="Đã xử lý" value={CONDUCT_CASES.filter((c) => c.status === 'resolved').length.toString()} delta="Tháng này" deltaDirection="up" sub="Hoàn tất" />
      </div>

      <section className="mb-6 rounded-lg border bg-background p-4">
        <h2 className="mb-3 text-base font-semibold">Thang xử lý 5 bước (theo quy chế MoET)</h2>
        <div className="grid grid-cols-1 gap-3 md:grid-cols-5">
          {CONDUCT_LADDER.map((s) => (
            <div key={s.step} className="rounded-md border bg-muted/20 p-3">
              <div className="mb-2 flex items-center gap-2">
                <StepBadge step={s.step} />
                <span className="text-xs font-semibold uppercase">{s.label}</span>
              </div>
              <p className="text-xs text-muted-foreground">{s.description}</p>
              <button
                type="button"
                onClick={() => setFilterStep(filterStep === s.step ? 'all' : s.step)}
                className={`mt-2 text-xs ${filterStep === s.step ? 'font-semibold text-primary' : 'text-muted-foreground hover:text-foreground'}`}
              >
                {CONDUCT_CASES.filter((c) => c.step === s.step).length} ca · {filterStep === s.step ? 'Bỏ lọc' : 'Lọc'}
              </button>
            </div>
          ))}
        </div>
      </section>

      <div className="overflow-x-auto rounded-lg border bg-background">
        <div className="flex items-center justify-between border-b bg-muted/20 px-3 py-2 text-xs">
          <span className="text-muted-foreground">
            Hiển thị {filtered.length} / {CONDUCT_CASES.length} ca
            {filterStep !== 'all' && ` · Lọc: Bước ${filterStep}`}
          </span>
          {filterStep !== 'all' && (
            <button type="button" onClick={() => setFilterStep('all')} className="text-primary hover:underline">
              Bỏ lọc
            </button>
          )}
        </div>
        <table className="w-full text-sm" aria-label="Danh sách ca hạnh kiểm">
          <thead>
            <tr className="border-b bg-muted/40 text-xs uppercase text-muted-foreground">
              <th className="px-3 py-2 text-left">Bước</th>
              <th className="px-3 py-2 text-left">Học sinh / Lớp</th>
              <th className="px-3 py-2 text-left">Loại sự việc</th>
              <th className="px-3 py-2 text-left">Mức độ</th>
              <th className="px-3 py-2 text-left">Ngày sự việc</th>
              <th className="px-3 py-2 text-left">GVCN</th>
              <th className="px-3 py-2 text-left">Trạng thái</th>
            </tr>
          </thead>
          <tbody>
            {filtered.map((c) => (
              <tr key={c.id} className="border-b last:border-0 hover:bg-muted/30">
                <td className="px-3 py-2">
                  <div className="flex items-center gap-2">
                    <StepBadge step={c.step} currentStep={c.step} />
                    <span className="text-xs text-muted-foreground">{c.stepLabel}</span>
                  </div>
                </td>
                <td className="px-3 py-2">
                  <div className="font-medium">{c.studentName}</div>
                  <div className="font-mono text-xs text-muted-foreground">{c.classCode}</div>
                </td>
                <td className="px-3 py-2 capitalize">{c.category}</td>
                <td className="px-3 py-2">
                  <Pill kind={SEVERITY_PILL[c.severity].kind}>{SEVERITY_PILL[c.severity].label}</Pill>
                </td>
                <td className="px-3 py-2 font-mono text-xs">{c.incidentDate}</td>
                <td className="px-3 py-2 text-xs text-muted-foreground">{c.homeroom}</td>
                <td className="px-3 py-2">
                  <Pill kind={STATUS_PILL[c.status].kind}>{STATUS_PILL[c.status].label}</Pill>
                </td>
              </tr>
            ))}
            {filtered.length === 0 && (
              <tr>
                <td colSpan={7} className="px-3 py-12 text-center text-sm text-muted-foreground">
                  <AlertTriangle className="mx-auto mb-2 h-6 w-6" aria-hidden="true" />
                  Không có ca nào ở bước này.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
