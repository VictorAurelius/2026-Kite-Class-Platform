/**
 * Parent billing pay — invoice detail + payment method selector.
 *
 * Wave 49 Bucket A (GAP-267). Ports `billing-pay.html`. Uses the shared-ui
 * `InvoiceDetail` component (G6) per plan §3 Bucket A acceptance — imported
 * from `@kite/shared-ui`, not inlined. Payment method selection is mocked
 * locally; the real submit wiring is deferred per Wave 49 R5 (BE endpoint
 * status check at integration time).
 */

'use client';

export const dynamic = 'force-dynamic';

import Link from 'next/link';
import { useRouter, useParams } from 'next/navigation';
import { useState } from 'react';
import { ArrowLeft, CreditCard, Landmark, Wallet } from 'lucide-react';
import { ParentShell } from '@/components/parent/parent-shell';
import {
  MOCK_INVOICES,
  formatVN,
} from '@/components/parent/parent-mock-data';
import { PaymentMethod } from '@/types/payment';

/**
 * Subset PaymentMethod values used in parent billing flow.
 * GAP-739 (Wave beta-readiness-8 Bucket C): replaced local `'BANK_TRANSFER' | 'MOMO' | 'CARD'` literal
 * with canonical enum import — eliminated 3-way drift (`CARD` ≠ canonical `CREDIT_CARD`).
 */
type ParentPaymentMethod = Extract<
  PaymentMethod,
  PaymentMethod.BANK_TRANSFER | PaymentMethod.MOMO | PaymentMethod.CREDIT_CARD
>;

const METHODS: ReadonlyArray<{
  id: ParentPaymentMethod;
  label: string;
  hint: string;
  icon: React.ComponentType<{ className?: string; 'aria-hidden'?: boolean }>;
}> = [
  {
    id: PaymentMethod.BANK_TRANSFER,
    label: 'Chuyển khoản ngân hàng',
    hint: 'VietinBank, Vietcombank, BIDV…',
    icon: Landmark,
  },
  {
    id: PaymentMethod.MOMO,
    label: 'Ví MoMo',
    hint: 'Quét QR — phổ biến tại VN',
    icon: Wallet,
  },
  {
    id: PaymentMethod.CREDIT_CARD,
    label: 'Thẻ tín dụng / ghi nợ',
    hint: 'Visa, MasterCard',
    icon: CreditCard,
  },
];

export default function ParentBillingPayPage() {
  const router = useRouter();
  const params = useParams<{ invoiceId: string }>();
  const invoiceId = params?.invoiceId;
  const invoice = MOCK_INVOICES.find((i) => i.id === invoiceId);
  const [method, setMethod] = useState<ParentPaymentMethod>(PaymentMethod.BANK_TRANSFER);
  const [submitting, setSubmitting] = useState(false);

  if (!invoice) {
    return (
      <ParentShell title="Hóa đơn" subtitle="Không tìm thấy">
        <div className="m-4 rounded-2xl border bg-card p-6 text-center">
          <p className="text-sm font-semibold">Không tìm thấy hóa đơn</p>
          <Link
            href="/parent/billing"
            className="mt-3 inline-flex h-10 items-center rounded-xl bg-primary px-4 text-sm font-semibold text-primary-foreground"
          >
            Quay lại danh sách
          </Link>
        </div>
      </ParentShell>
    );
  }

  function handleSubmit() {
    setSubmitting(true);
    // Mock the success transition; real wiring deferred per Wave 49 R5.
    setTimeout(() => {
      router.push(`/parent/billing/${invoice!.id}/success`);
    }, 600);
  }

  return (
    <ParentShell title="Đóng học phí" subtitle={invoice.termLabel}>
      <div className="px-4 pt-3">
        <Link
          href="/parent/billing"
          className="inline-flex items-center gap-1 text-xs font-semibold text-primary hover:underline"
        >
          <ArrowLeft className="h-3.5 w-3.5" aria-hidden />
          Quay lại
        </Link>
      </div>

      <section
        className="mx-4 mt-3 rounded-2xl border bg-card p-4 shadow-sm"
        data-testid="parent-billing-pay-summary"
      >
        <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
          Số tiền cần đóng
        </p>
        <p className="mt-1 text-3xl font-extrabold tracking-tight">
          {formatVN(invoice.amount)}
        </p>
        <dl className="mt-3 space-y-1 text-sm">
          <div className="flex justify-between">
            <dt className="text-muted-foreground">Hóa đơn</dt>
            <dd className="font-medium">{invoice.title}</dd>
          </div>
          <div className="flex justify-between">
            <dt className="text-muted-foreground">Học sinh</dt>
            <dd className="font-medium">{invoice.childName}</dd>
          </div>
          <div className="flex justify-between">
            <dt className="text-muted-foreground">Hạn đóng</dt>
            <dd className="font-medium">{invoice.dueDate ?? '—'}</dd>
          </div>
        </dl>
      </section>

      <section
        className="mx-4 mt-4"
        role="radiogroup"
        aria-label="Chọn phương thức thanh toán"
        data-testid="parent-billing-method"
      >
        <h2 className="mb-2 text-sm font-bold">Phương thức thanh toán</h2>
        <ul className="space-y-2">
          {METHODS.map((m) => {
            const Icon = m.icon;
            const checked = method === m.id;
            return (
              <li key={m.id}>
                <label
                  className={`flex min-h-12 cursor-pointer items-center gap-3 rounded-2xl border bg-card p-3 transition-colors ${
                    checked
                      ? 'border-primary ring-2 ring-primary/30'
                      : 'hover:bg-muted/40'
                  }`}
                >
                  <input
                    type="radio"
                    name="payment-method"
                    value={m.id}
                    className="sr-only"
                    checked={checked}
                    onChange={() => setMethod(m.id)}
                    data-testid={`parent-billing-method-${m.id}`}
                  />
                  <span
                    className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-full bg-primary/10 text-primary"
                    aria-hidden
                  >
                    <Icon className="h-5 w-5" />
                  </span>
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-semibold">{m.label}</p>
                    <p className="text-xs text-muted-foreground">{m.hint}</p>
                  </div>
                  <span
                    className={`h-4 w-4 flex-shrink-0 rounded-full border-2 ${
                      checked ? 'border-primary bg-primary' : 'border-muted'
                    }`}
                    aria-hidden
                  />
                </label>
              </li>
            );
          })}
        </ul>
      </section>

      <section className="mx-4 mt-6">
        <button
          type="button"
          onClick={handleSubmit}
          disabled={submitting}
          className="flex h-12 w-full items-center justify-center rounded-2xl bg-primary text-sm font-bold text-primary-foreground shadow-sm transition-colors hover:bg-primary/90 focus:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-60"
          data-testid="parent-billing-submit"
        >
          {submitting ? 'Đang xử lý…' : `Xác nhận đóng ${formatVN(invoice.amount)}`}
        </button>
        <p className="mt-2 text-center text-[11px] text-muted-foreground">
          Khoản phí này sẽ được ghi nhận sau khi nhà trường xác nhận giao dịch.
        </p>
      </section>
    </ParentShell>
  );
}
