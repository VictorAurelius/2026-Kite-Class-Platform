/**
 * Lazy `PaymentForm` wrapper — keeps `react-hook-form` + `zod` +
 * `PaymentMethodSelector` out of the initial bundle for `/billing/[id]/pay`,
 * which renders summary cards before the form and only needs the form once
 * the invoice has loaded.
 *
 * GAP-236 Sub-PR B (Wave) — code-split admin + attendance + billing routes.
 *
 * @author KiteClass Team
 */

'use client';

import dynamic from 'next/dynamic';
import { Skeleton } from '@/components/ui/skeleton';
import type { PaymentFormProps } from './payment-form';

export type { PaymentFormData, PaymentFormProps } from './payment-form';

const LazyPaymentForm = dynamic(
  () =>
    import('./payment-form').then((m) => ({ default: m.PaymentForm })),
  {
    ssr: false,
    loading: () => (
      <div className="space-y-6">
        <div className="space-y-2">
          <Skeleton className="h-4 w-32" />
          <Skeleton className="h-10 w-full" />
        </div>
        <div className="space-y-2">
          <Skeleton className="h-4 w-40" />
          <Skeleton className="h-24 w-full" />
        </div>
        <Skeleton className="h-10 w-full" />
      </div>
    ),
  },
);

export function DynamicPaymentForm(props: PaymentFormProps) {
  return <LazyPaymentForm {...props} />;
}
