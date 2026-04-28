/**
 * Reset password page — thin shell that lazy-loads the form body.
 *
 * Suspense boundary stays here because `useSearchParams()` (used by
 * the lazy form) requires one for static export safety.
 *
 * GAP-236 Sub-PR B Agent A — code-splitting for `/reset-password`.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */

'use client';

export const dynamic = 'force-dynamic';

import { Suspense } from 'react';
import nextDynamic from 'next/dynamic';
import { AuthLayout } from '@/components/layout';
import { LoadingSpinner } from '@/components/common';
import { Skeleton } from '@/components/ui/skeleton';

const ResetPasswordForm = nextDynamic(
  () =>
    import('@/components/auth/reset-password-form').then((m) => ({
      default: m.ResetPasswordForm,
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

export default function ResetPasswordPage() {
  return (
    <AuthLayout>
      <Suspense
        fallback={
          <div className="flex items-center justify-center">
            <LoadingSpinner />
          </div>
        }
      >
        <ResetPasswordForm />
      </Suspense>
    </AuthLayout>
  );
}
