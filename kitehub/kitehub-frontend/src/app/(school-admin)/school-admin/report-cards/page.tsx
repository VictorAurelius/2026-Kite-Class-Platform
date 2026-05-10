/**
 * Report cards — MoET-compliant report card generation per class.
 *
 * Wave 50 Bucket A (GAP-271). Source: ui_kits/kitehub-admin/screens/report-cards.html
 * Uses G3 GradebookEntryGrid + G10 PaymentStatusTimeline references via @kite/shared-ui.
 */

'use client';

import { useState } from 'react';
import Link from 'next/link';
import { FileText, Download, CheckCircle2, Circle } from 'lucide-react';
import { GradebookEntryGrid } from '@kite/shared-ui';
import { PageHeader, Pill, MoetStamp } from '@/components/school-admin/school-admin-ui';
import { SCHOOL_PROFILE, REPORT_CARD_STATUS } from '@/components/school-admin/school-admin-mock-data';

const SAMPLE_COLUMNS = [
  { id: 'TX1', label: "KT 15'", weight: 1 as const },
  { id: 'TX2', label: "KT 15' (2)", weight: 1 as const },
  { id: 'GK', label: 'Giữa kỳ', weight: 2 as const },
  { id: 'CK', label: 'Cuối kỳ', weight: 3 as const },
];

const SAMPLE_STUDENTS = [
  { studentCode: 'HS-001', fullName: 'Phạm Thị Mai', grades: { TX1: 8.5, TX2: 9, GK: 8, CK: 8.75 } },
  { studentCode: 'HS-002', fullName: 'Nguyễn Tuấn Khang', grades: { TX1: 7, TX2: 7.5, GK: 7.25, CK: 8 } },
  { studentCode: 'HS-003', fullName: 'Lê Thị Bích Ngọc', grades: { TX1: 9, TX2: 9.5, GK: 9.25, CK: 9.5 } },
];

export default function ReportCardsPage() {
  const [previewClass, setPreviewClass] = useState<string | null>(null);

  const completedCount = REPORT_CARD_STATUS.filter((r) => r.moetCompliant).length;
  const totalCount = REPORT_CARD_STATUS.length;

  return (
    <div>
      <PageHeader
        title="Học bạ MoET — Học kỳ 1"
        subtitle={`${completedCount}/${totalCount} lớp đã hoàn tất · Hạn nộp Sở GD: 15/05/2026`}
        breadcrumbs={[{ label: SCHOOL_PROFILE.name, href: '/school-admin/dashboard' }, { label: 'Học sinh' }, { label: 'Học bạ MoET' }]}
        actions={
          <>
            <button type="button" className="inline-flex items-center gap-1.5 rounded-md border bg-background px-3 py-1.5 text-sm font-medium hover:bg-muted">
              <Download className="h-4 w-4" aria-hidden="true" />
              Xuất tất cả PDF
            </button>
            <button type="button" className="inline-flex items-center gap-1.5 rounded-md bg-primary px-3 py-1.5 text-sm font-medium text-primary-foreground hover:bg-primary/90">
              <FileText className="h-4 w-4" aria-hidden="true" />
              Phê duyệt hàng loạt
            </button>
          </>
        }
      />

      <div className="mb-4 flex items-center gap-3 rounded-lg border-2 border-amber-600/30 bg-amber-50 p-4 dark:border-amber-400/30 dark:bg-amber-950/20">
        <MoetStamp size="md" />
        <div className="text-sm">
          <div className="font-semibold">Tuân thủ chuẩn học bạ MoET (Bộ GD&ĐT)</div>
          <p className="text-xs text-muted-foreground">
            Hệ thống xuất học bạ theo mẫu chính thức của Bộ GD&ĐT (Thông tư 22/2021/TT-BGDĐT). Mỗi học bạ cần GVCN nhập điểm + tổ trưởng duyệt + hiệu trưởng ký số trước khi nộp Sở.
          </p>
        </div>
      </div>

      <div className="overflow-x-auto rounded-lg border bg-background">
        <table className="w-full text-sm" aria-label="Trạng thái học bạ theo lớp">
          <thead>
            <tr className="border-b bg-muted/40 text-xs uppercase text-muted-foreground">
              <th className="px-3 py-2 text-left">Lớp</th>
              <th className="px-3 py-2 text-left">GVCN</th>
              <th className="px-3 py-2 text-left">Sĩ số</th>
              <th className="px-3 py-2 text-left">Tiến độ</th>
              <th className="px-3 py-2 text-left">GVCN duyệt</th>
              <th className="px-3 py-2 text-left">HT ký</th>
              <th className="px-3 py-2 text-left">MoET</th>
              <th className="px-3 py-2 text-right">Hành động</th>
            </tr>
          </thead>
          <tbody>
            {REPORT_CARD_STATUS.map((r) => {
              const pct = Math.round((r.enteredCount / r.studentCount) * 100);
              return (
                <tr key={r.classCode} className="border-b last:border-0 hover:bg-muted/30">
                  <td className="px-3 py-2 font-mono font-semibold">{r.classCode}</td>
                  <td className="px-3 py-2">{r.homeroom}</td>
                  <td className="px-3 py-2 tabular-nums">{r.studentCount}</td>
                  <td className="px-3 py-2">
                    <div className="flex items-center gap-2">
                      <div className="h-1.5 w-24 rounded-full bg-muted">
                        <div className={`h-full rounded-full ${pct === 100 ? 'bg-emerald-500' : 'bg-amber-500'}`} style={{ width: `${pct}%` }} />
                      </div>
                      <span className="text-xs tabular-nums text-muted-foreground">{pct}%</span>
                    </div>
                  </td>
                  <td className="px-3 py-2">
                    {r.approvedByHomeroom ? (
                      <CheckCircle2 className="h-4 w-4 text-emerald-600" aria-label="Đã duyệt" />
                    ) : (
                      <Circle className="h-4 w-4 text-muted-foreground" aria-label="Chưa duyệt" />
                    )}
                  </td>
                  <td className="px-3 py-2">
                    {r.signedByPrincipal ? (
                      <CheckCircle2 className="h-4 w-4 text-emerald-600" aria-label="Đã ký" />
                    ) : (
                      <Circle className="h-4 w-4 text-muted-foreground" aria-label="Chưa ký" />
                    )}
                  </td>
                  <td className="px-3 py-2">
                    {r.moetCompliant ? <Pill kind="success">Đạt MoET</Pill> : <Pill kind="warning">Chưa đạt</Pill>}
                  </td>
                  <td className="px-3 py-2 text-right">
                    <button
                      type="button"
                      onClick={() => setPreviewClass(previewClass === r.classCode ? null : r.classCode)}
                      className="text-xs text-primary hover:underline"
                    >
                      {previewClass === r.classCode ? 'Đóng' : 'Xem'}
                    </button>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      {previewClass && (
        <div className="mt-4 rounded-lg border bg-background p-4">
          <div className="mb-3 flex items-center justify-between">
            <h3 className="text-base font-semibold">
              Xem trước bảng điểm — Lớp <span className="font-mono">{previewClass}</span>
            </h3>
            <Link href="#" className="text-xs text-primary hover:underline">
              Mở trang điểm chi tiết →
            </Link>
          </div>
          <GradebookEntryGrid
            session={{
              className: `Lớp ${previewClass} — Toán`,
              term: 'Học kỳ I · 2026-2027',
              teacherName: 'Cô Phạm Thị Yến',
            }}
            columns={SAMPLE_COLUMNS}
            students={SAMPLE_STUDENTS}
            state="default"
            onCellChange={() => {
              // Read-only preview — no persistence
            }}
            onSave={() => {
              // Read-only preview
            }}
          />
        </div>
      )}
    </div>
  );
}
