/**
 * Root-level error boundary (Next.js App Router convention).
 *
 * Catches runtime errors thrown inside any route's React tree (server or
 * client) that bubble past nested error.tsx boundaries.
 *
 * Behaviour:
 * - Vietnamese-first copy with Shadcn Button + CSS variables (dark-mode aware).
 * - "Thử lại" calls reset() to retry the failing render.
 * - Fallback link returns to home.
 * - Sentry-ready hook: useEffect logs error so an APM client can subclass.
 * - Stack trace surface is gated behind NODE_ENV === 'development'.
 *
 * @author KiteHub Team
 * @since GAP-136
 */

'use client';

import { useEffect } from 'react';
import Link from 'next/link';
import { AlertCircle } from 'lucide-react';
import { Button } from '@/components/ui/button';

interface ErrorProps {
  error: Error & { digest?: string };
  reset: () => void;
}

export default function ErrorBoundary({ error, reset }: ErrorProps) {
  useEffect(() => {
    // Sentry-ready hook — replace with Sentry.captureException(error) when wired.
    console.error('KiteHub route error:', error);
  }, [error]);

  return (
    <main className="flex min-h-[70vh] flex-col items-center justify-center bg-background px-4 py-16 text-foreground">
      <div className="flex flex-col items-center text-center">
        <AlertCircle
          className="mb-6 h-16 w-16 text-destructive"
          aria-hidden="true"
        />
        <p className="mb-2 text-sm font-medium uppercase tracking-wider text-destructive">
          Lỗi 500
        </p>
        <h1 className="mb-3 text-3xl font-bold tracking-tight sm:text-4xl">
          Đã xảy ra lỗi
        </h1>
        <p className="mb-8 max-w-md text-base text-muted-foreground">
          Rất tiếc, đã có lỗi xảy ra khi tải trang này. Vui lòng thử lại sau ít
          phút hoặc quay về trang chủ KiteHub.
        </p>
        <div className="flex flex-col gap-3 sm:flex-row">
          <Button onClick={() => reset()} size="lg">
            Thử lại
          </Button>
          <Button asChild size="lg" variant="outline">
            <Link href="/">Về trang chủ</Link>
          </Button>
        </div>
        {process.env.NODE_ENV === 'development' && (
          <details className="mt-10 w-full max-w-2xl rounded-lg border border-border bg-muted p-4 text-left">
            <summary className="cursor-pointer text-sm font-semibold text-foreground">
              Chi tiết lỗi (development only)
            </summary>
            <pre className="mt-3 whitespace-pre-wrap break-words text-xs text-muted-foreground">
              {error.message}
              {error.digest ? `\n\nDigest: ${error.digest}` : ''}
              {error.stack ? `\n\nStack:\n${error.stack}` : ''}
            </pre>
          </details>
        )}
      </div>
    </main>
  );
}
