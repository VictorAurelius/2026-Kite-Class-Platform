/**
 * Error page for public routes.
 * Handles errors that occur in public pages.
 *
 * @author KiteClass Team
 * @since 3.4.0
 */

'use client';

import { useEffect } from 'react';
import { AlertCircle } from 'lucide-react';
import { Button } from '@/components/ui/button';

export default function Error({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error('Public route error:', error);
  }, [error]);

  return (
    <div className="flex flex-col items-center justify-center min-h-[60vh] p-8">
      <AlertCircle className="h-16 w-16 text-destructive mb-4" />
      <h2 className="text-2xl font-bold mb-2">Đã xảy ra lỗi</h2>
      <p className="text-muted-foreground mb-6 text-center max-w-md">
        Rất tiếc, đã có lỗi xảy ra khi tải trang này. Vui lòng thử lại sau.
      </p>
      <div className="flex gap-4">
        <Button onClick={() => reset()}>Thử lại</Button>
        <Button variant="outline" onClick={() => (window.location.href = '/')}>
          Về trang chủ
        </Button>
      </div>
      {process.env.NODE_ENV === 'development' && (
        <details className="mt-8 p-4 bg-muted rounded-lg max-w-2xl w-full">
          <summary className="cursor-pointer font-semibold">
            Chi tiết lỗi (development only)
          </summary>
          <pre className="mt-4 text-xs overflow-auto whitespace-pre-wrap">
            {error.toString()}
            {error.digest && `\n\nDigest: ${error.digest}`}
            {error.stack && `\n\nStack:\n${error.stack}`}
          </pre>
        </details>
      )}
    </div>
  );
}
