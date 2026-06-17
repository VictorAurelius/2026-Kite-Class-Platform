/**
 * Annual fees panel — class-by-class fee collection status.
 *
 * Wave 50 Bucket A (GAP-271). Source: ui_kits/kitehub-admin/screens/fees.html
 * Uses G10 PaymentStatusTimeline (showcase example).
 */

'use client';

import { useState } from 'react';
import { Send, Download, ExternalLink } from 'lucide-react';
import { PaymentStatusTimeline } from '@kite/shared-ui';
import { PageHeader, KpiCard, Pill } from '@/components/school-admin/school-admin-ui';
import { SCHOOL_PROFILE, FEES_BY_CLASS } from '@/components/school-admin/school-admin-mock-data';

function formatVnd(n: number) {
  return n.toLocaleString('vi-VN') + 'đ';
}

export default function FeesPage() {
  const [showSampleTimeline, setShowSampleTimeline] = useState(false);

  const totalCollected = FEES_BY_CLASS.reduce((s, r) => s + r.collectedAmount, 0);
  const totalExpected = FEES_BY_CLASS.reduce((s, r) => s + r.expectedAmount, 0);
  const totalPaid = FEES_BY_CLASS.reduce((s, r) => s + r.paidCount, 0);
  const totalOverdue = FEES_BY_CLASS.reduce((s, r) => s + r.overdueCount, 0);
  const collectionRate = ((totalCollected / totalExpected) * 100).toFixed(1);

  return (
    <div>
      <PageHeader
        title="Học phí năm học"
        subtitle={`${SCHOOL_PROFILE.semester} năm học ${SCHOOL_PROFILE.academicYear} · Học phí 4.500.000đ/HS/HK`}
        breadcrumbs={[{ label: SCHOOL_PROFILE.name, href: '/school-admin/dashboard' }, { label: 'Tài chính' }, { label: 'Học phí năm' }]}
        actions={
          <>
            <button type="button" className="inline-flex items-center gap-1.5 rounded-md border bg-background px-3 py-1.5 text-sm font-medium hover:bg-muted">
              <Download className="h-4 w-4" aria-hidden="true" />
              Xuất Excel
            </button>
            <button type="button" className="inline-flex items-center gap-1.5 rounded-md bg-primary px-3 py-1.5 text-sm font-medium text-primary-foreground hover:bg-primary/90">
              <Send className="h-4 w-4" aria-hidden="true" />
              Gửi nhắc Zalo OA
            </button>
          </>
        }
      />

      <div className="mb-4 grid grid-cols-2 gap-3 md:grid-cols-4">
        <KpiCard label="Tỷ lệ thu" value={`${collectionRate}%`} delta="Mục tiêu 95%" deltaDirection="flat" sub={`${formatVnd(totalCollected)} / ${formatVnd(totalExpected)}`} />
        <KpiCard label="Đã thu" value={totalPaid.toString()} delta="HS đã đóng" deltaDirection="up" sub={formatVnd(totalCollected)} />
        <KpiCard label="Trễ hạn" value={totalOverdue.toString()} delta={`${(totalOverdue * 4500000).toLocaleString('vi-VN')}đ`} deltaDirection="down" sub="Cần nhắc thu" />
        <KpiCard label="Số lớp" value={FEES_BY_CLASS.length.toString()} delta={`${FEES_BY_CLASS.filter((r) => r.overdueCount === 0).length} lớp đã đủ`} deltaDirection="up" sub="Hoàn thành" />
      </div>

      <div className="overflow-x-auto rounded-lg border bg-background">
        <table className="w-full text-sm" aria-label="Học phí theo lớp">
          <thead>
            <tr className="border-b bg-muted/40 text-xs uppercase text-muted-foreground">
              <th className="px-3 py-2 text-left">Lớp</th>
              <th className="px-3 py-2 text-left">Sĩ số</th>
              <th className="px-3 py-2 text-left">Đã đóng</th>
              <th className="px-3 py-2 text-left">Chờ</th>
              <th className="px-3 py-2 text-left">Trễ</th>
              <th className="px-3 py-2 text-left">% Thu</th>
              <th className="px-3 py-2 text-left">Số tiền đã thu</th>
              <th className="px-3 py-2 text-right">Hành động</th>
            </tr>
          </thead>
          <tbody>
            {FEES_BY_CLASS.map((r) => {
              const pct = Math.round((r.paidCount / r.totalStudents) * 100);
              return (
                <tr key={r.classCode} className="border-b last:border-0 hover:bg-muted/30">
                  <td className="px-3 py-2 font-mono font-semibold">{r.classCode}</td>
                  <td className="px-3 py-2 tabular-nums">{r.totalStudents}</td>
                  <td className="px-3 py-2 tabular-nums">{r.paidCount}</td>
                  <td className="px-3 py-2 tabular-nums">{r.pendingCount}</td>
                  <td className="px-3 py-2 tabular-nums">
                    {r.overdueCount > 0 ? <Pill kind="danger">{r.overdueCount}</Pill> : <span className="text-muted-foreground">0</span>}
                  </td>
                  <td className="px-3 py-2">
                    <div className="flex items-center gap-2">
                      <div className="h-1.5 w-20 rounded-full bg-muted">
                        <div className={`h-full rounded-full ${pct >= 95 ? 'bg-emerald-500' : pct >= 75 ? 'bg-amber-500' : 'bg-red-500'}`} style={{ width: `${pct}%` }} />
                      </div>
                      <span className="text-xs tabular-nums">{pct}%</span>
                    </div>
                  </td>
                  <td className="px-3 py-2 font-mono text-xs">{formatVnd(r.collectedAmount)}</td>
                  <td className="px-3 py-2 text-right">
                    <button type="button" className="text-xs text-primary hover:underline">
                      Soạn nhắc
                    </button>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      <div className="mt-6 rounded-lg border bg-background p-4">
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-base font-semibold">Mẫu hành trình thanh toán (G10)</h2>
          <button
            type="button"
            onClick={() => setShowSampleTimeline(!showSampleTimeline)}
            className="text-xs text-primary hover:underline"
          >
            {showSampleTimeline ? 'Đóng' : 'Xem mẫu'}
            <ExternalLink className="ml-1 inline h-3 w-3" aria-hidden="true" />
          </button>
        </div>
        {showSampleTimeline && (
          <PaymentStatusTimeline
            invoiceNumber="KH-2026-10-0042"
            state="partial-paid"
            totalAmount={4500000}
            embedded
            events={[
              {
                step: 'CREATED',
                at: new Date('2026-04-15T08:00:00+07:00'),
                note: 'Hoá đơn HK1 năm học 2026–2027',
                actor: 'Hệ thống tự động',
              },
              {
                step: 'PAYMENT_PENDING',
                at: new Date('2026-04-16T09:30:00+07:00'),
                note: 'Đã gửi nhắc Zalo OA',
                actor: 'Cô Trần Thị Lan (Hiệu trưởng)',
              },
              {
                step: 'PAYMENT_RECEIVED',
                at: new Date('2026-04-22T14:15:00+07:00'),
                note: 'Chuyển khoản qua VietQR',
                actor: 'Phụ huynh Phạm Văn Hùng',
                amount: 2500000,
              },
            ]}
          />
        )}
      </div>
    </div>
  );
}
