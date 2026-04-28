/**
 * Invoice detail bottom panels — items, adjustments, payment history.
 *
 * Extracted from `/billing/[id]/page.tsx` so the panels (and the
 * `PaymentStatusBadge` + currency/date formatting they pull) can be code-split
 * via `dynamic-invoice-detail-panels.tsx`. The summary cards above the fold
 * remain part of the page bundle.
 *
 * GAP-236 Sub-PR B (Wave) — code-split admin + attendance + billing routes.
 *
 * @author KiteClass Team
 */

'use client';

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { PaymentStatusBadge } from '@/components/billing/payment-status-badge';
import { formatCurrency, formatDate } from '@/lib/utils';
import type { Invoice } from '@/types/invoice';
import type { Payment } from '@/types/payment';

export interface InvoiceDetailPanelsProps {
  invoice: Invoice;
  payments?: Payment[];
}

export function InvoiceDetailPanels({ invoice, payments }: InvoiceDetailPanelsProps) {
  return (
    <>
      {/* Invoice Items */}
      <Card>
        <CardHeader>
          <CardTitle>Chi tiết</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-2">
            {invoice.items.map((item) => (
              <div key={item.id} className="flex justify-between">
                <span>
                  {item.description} (x{item.quantity})
                </span>
                <span>{formatCurrency(item.amount)}</span>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>

      {/* Adjustments */}
      {invoice.adjustments && invoice.adjustments.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>Điều chỉnh</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-2">
              {invoice.adjustments.map((adjustment) => (
                <div key={adjustment.id} className="flex justify-between border-b pb-2">
                  <div>
                    <p className="font-medium">{adjustment.description}</p>
                    <p className="text-sm text-muted-foreground">{adjustment.type}</p>
                  </div>
                  <span>{formatCurrency(adjustment.amount)}</span>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}

      {/* Payment History */}
      {payments && payments.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>Lịch sử thanh toán</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-2">
              {payments.map((payment) => (
                <div key={payment.id} className="flex justify-between border-b pb-2">
                  <div>
                    <p className="font-medium">{payment.paymentNumber}</p>
                    <p className="text-sm text-muted-foreground">
                      {formatDate(payment.initiatedAt)}
                    </p>
                  </div>
                  <div className="text-right">
                    <p className="font-medium">{formatCurrency(payment.amount)}</p>
                    <PaymentStatusBadge status={payment.paymentStatus} />
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}
    </>
  );
}
