/**
 * Student PWA — Học phí (payments balance + history).
 * Wave 49 Bucket C (GAP-269). Mirrors `kiteclass-student/screens/payments.html`.
 *
 * For older students (HS/cấp 3 + sinh viên) — younger students see this in
 * the parent app. Visible only when `viewerCanPay = true`; otherwise the
 * route shows an explainer + nudge to ask parent.
 */
'use client';

import Link from 'next/link';
import { CheckCircle2, ChevronRight, Clock, Receipt } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { StudentMobileShell } from '@/components/student/mobile-shell';

interface InvoiceRow {
  id: string;
  period: string;
  amount: string;
  status: 'paid' | 'due' | 'overdue';
  dueDate: string;
}

const INVOICES: readonly InvoiceRow[] = [
  { id: 'inv-04', period: 'Tháng 04 / 2026', amount: '2.500.000 ₫', status: 'due', dueDate: '20/04/2026' },
  { id: 'inv-03', period: 'Tháng 03 / 2026', amount: '2.500.000 ₫', status: 'paid', dueDate: '20/03/2026' },
  { id: 'inv-02', period: 'Tháng 02 / 2026', amount: '2.500.000 ₫', status: 'paid', dueDate: '20/02/2026' },
  { id: 'inv-01', period: 'Tháng 01 / 2026', amount: '2.500.000 ₫', status: 'paid', dueDate: '20/01/2026' },
];

const TIMELINE = [
  { id: 't1', label: 'Hoá đơn được tạo', at: '01/04/2026 · 08:00', done: true },
  { id: 't2', label: 'Đã gửi qua email', at: '01/04/2026 · 09:15', done: true },
  { id: 't3', label: 'Phụ huynh đã xem', at: '02/04/2026 · 14:00', done: true },
  { id: 't4', label: 'Chờ thanh toán', at: 'Hạn 20/04/2026', done: false },
];

export default function StudentPaymentsPage() {
  const due = INVOICES.find((i) => i.status === 'due');
  return (
    <StudentMobileShell title="Học phí" subtitle="Theo dõi hoá đơn và thanh toán">
      {due ? (
        <Card className="border-amber-500/40 bg-amber-50 dark:bg-amber-950/30">
          <CardContent className="p-4">
            <div className="text-xs uppercase tracking-wider text-muted-foreground">
              Cần thanh toán
            </div>
            <div className="mt-1 text-3xl font-bold text-amber-700 dark:text-amber-300">
              {due.amount}
            </div>
            <div className="mt-1 flex items-center gap-1 text-xs text-muted-foreground">
              <Clock className="h-3 w-3" aria-hidden /> Hạn {due.dueDate} · {due.period}
            </div>
            <Button asChild className="mt-3 w-full">
              <Link href={`/student/payments/${due.id}`}>
                Xem chi tiết & thanh toán
              </Link>
            </Button>
          </CardContent>
        </Card>
      ) : null}

      <Card className="mt-4">
        <CardHeader className="pb-2">
          <CardTitle className="text-sm">Tiến trình hoá đơn hiện tại</CardTitle>
        </CardHeader>
        <CardContent>
          <ol className="space-y-3">
            {TIMELINE.map((step) => (
              <li key={step.id} className="flex items-start gap-3">
                <span
                  aria-hidden
                  className={`mt-1 h-2 w-2 shrink-0 rounded-full ${step.done ? 'bg-emerald-600' : 'bg-muted-foreground/40'}`}
                />
                <div className="min-w-0">
                  <div className={`text-sm ${step.done ? 'font-medium' : 'text-muted-foreground'}`}>
                    {step.label}
                  </div>
                  <div className="text-xs text-muted-foreground">{step.at}</div>
                </div>
              </li>
            ))}
          </ol>
        </CardContent>
      </Card>

      <Card className="mt-4">
        <CardHeader className="pb-2">
          <CardTitle className="flex items-center gap-2 text-sm">
            <Receipt className="h-4 w-4" aria-hidden /> Lịch sử
          </CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          <ul>
            {INVOICES.map((inv, idx) => (
              <li
                key={inv.id}
                className={`flex items-center gap-3 px-4 py-3 ${idx > 0 ? 'border-t border-border' : ''}`}
              >
                <div className="min-w-0 flex-1">
                  <div className="text-sm font-medium">{inv.period}</div>
                  <div className="text-xs text-muted-foreground">{inv.amount}</div>
                </div>
                {inv.status === 'paid' ? (
                  <Badge variant="default" className="bg-emerald-600">
                    <CheckCircle2 className="mr-1 h-3 w-3" aria-hidden /> Đã trả
                  </Badge>
                ) : inv.status === 'overdue' ? (
                  <Badge variant="destructive">Quá hạn</Badge>
                ) : (
                  <Badge variant="outline">Chờ trả</Badge>
                )}
                <ChevronRight className="h-4 w-4 text-muted-foreground" aria-hidden />
              </li>
            ))}
          </ul>
        </CardContent>
      </Card>
    </StudentMobileShell>
  );
}
