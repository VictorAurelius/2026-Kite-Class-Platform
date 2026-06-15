/**
 * Login page — thin shell that lazy-loads the form body.
 *
 * Heavy deps (react-hook-form, zod, @hookform/resolvers) live in
 * `@/components/auth/login-form` and load on demand. The page itself
 * stays small so the route's First Load JS shrinks meaningfully.
 *
 * GAP-236 Sub-PR B Agent A — code-splitting for `/login`.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */

'use client';

export const dynamic = 'force-dynamic';

import nextDynamic from 'next/dynamic';
import { AuthLayout } from '@/components/layout';
import { Skeleton } from '@/components/ui/skeleton';

const LoginForm = nextDynamic(
  () => import('@/components/auth/login-form').then((m) => ({ default: m.LoginForm })),
  {
    // ssr: false — auth forms have no SEO value; skipping server render lets
    // the route shell ship a tiny skeleton until the form chunk hydrates.
    ssr: false,
    // GAP-1377: announce the loading state to assistive tech (WCAG 4.1.3 Status
    // Messages). role="status" + aria-busy + an sr-only label so a screen reader
    // reads "Đang tải biểu mẫu đăng nhập…" instead of silent skeleton boxes.
    loading: () => (
      <div className="space-y-6" role="status" aria-busy="true" aria-live="polite">
        <span className="sr-only">Đang tải biểu mẫu đăng nhập…</span>
        <div className="space-y-2 text-center" aria-hidden="true">
          <Skeleton className="h-9 w-3/4 mx-auto" />
          <Skeleton className="h-5 w-2/3 mx-auto" />
        </div>
        <div className="space-y-4" aria-hidden="true">
          <Skeleton className="h-10 w-full" />
          <Skeleton className="h-10 w-full" />
          <Skeleton className="h-10 w-full" />
        </div>
      </div>
    ),
  },
);

export default function LoginPage() {
  return (
    <AuthLayout>
      <LoginForm />
    </AuthLayout>
  );
}
