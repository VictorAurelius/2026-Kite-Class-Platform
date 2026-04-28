/**
 * Student registration page — thin shell that lazy-loads the form body.
 *
 * GAP-236 Sub-PR B Agent A — code-splitting for `/register/student`.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */

'use client';

import nextDynamic from 'next/dynamic';
import { AuthLayout } from '@/components/layout';
import { Skeleton } from '@/components/ui/skeleton';

const StudentRegisterForm = nextDynamic(
  () =>
    import('@/components/auth/student-register-form').then((m) => ({
      default: m.StudentRegisterForm,
    })),
  {
    ssr: false,
    loading: () => (
      <div className="space-y-6">
        <Skeleton className="h-9 w-3/4 mx-auto" />
        <Skeleton className="h-5 w-2/3 mx-auto" />
        <div className="space-y-3">
          {Array.from({ length: 7 }).map((_, i) => (
            <Skeleton key={i} className="h-10 w-full" />
          ))}
        </div>
      </div>
    ),
  },
);

export default function StudentRegisterPage() {
  return (
    <AuthLayout>
      <StudentRegisterForm />
    </AuthLayout>
  );
}
