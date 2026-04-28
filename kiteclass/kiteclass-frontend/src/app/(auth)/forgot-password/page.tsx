/**
 * Forgot password page — thin shell that lazy-loads the form body.
 *
 * GAP-236 Sub-PR B Agent A — code-splitting for `/forgot-password`.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */

'use client';

export const dynamic = 'force-dynamic';

import nextDynamic from 'next/dynamic';
import { AuthLayout } from '@/components/layout';
import { Skeleton } from '@/components/ui/skeleton';

const ForgotPasswordForm = nextDynamic(
  () =>
    import('@/components/auth/forgot-password-form').then((m) => ({
      default: m.ForgotPasswordForm,
    })),
  {
    ssr: false,
    loading: () => (
      <div className="space-y-6">
        <Skeleton className="h-9 w-3/4 mx-auto" />
        <Skeleton className="h-5 w-2/3 mx-auto" />
        <Skeleton className="h-10 w-full" />
        <Skeleton className="h-10 w-full" />
      </div>
    ),
  },
);

export default function ForgotPasswordPage() {
  return (
    <AuthLayout>
      <ForgotPasswordForm />
    </AuthLayout>
  );
}
