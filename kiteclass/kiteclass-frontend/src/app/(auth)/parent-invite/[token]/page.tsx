/**
 * Parent invitation redemption page — thin shell that lazy-loads the
 * form body. The wizard form (with bespoke validation) lives in
 * `@/components/auth/parent-invite-form` and ships in its own chunk.
 *
 * GAP-236 Sub-PR B Agent A — code-splitting for `/parent-invite/[token]`.
 *
 * @author KiteClass Team
 * @since 3.14.0 (Wave 2 — GAP-052a)
 */

'use client';

import nextDynamic from 'next/dynamic';
import { AuthLayout } from '@/components/layout';
import { Skeleton } from '@/components/ui/skeleton';

const ParentInviteForm = nextDynamic(
  () =>
    import('@/components/auth/parent-invite-form').then((m) => ({
      default: m.ParentInviteForm,
    })),
  {
    ssr: false,
    loading: () => (
      <div className="space-y-6">
        <Skeleton className="h-8 w-3/4 mx-auto" />
        <Skeleton className="h-4 w-full" />
        <div className="space-y-3">
          {Array.from({ length: 5 }).map((_, i) => (
            <Skeleton key={i} className="h-10 w-full" />
          ))}
        </div>
      </div>
    ),
  },
);

export default function ParentInviteRedeemPage() {
  return (
    <AuthLayout>
      <ParentInviteForm />
    </AuthLayout>
  );
}
