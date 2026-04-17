/**
 * Error alert component for displaying error messages.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */

'use client';

import Link from 'next/link';
import { AlertCircle, X, ArrowLeft } from 'lucide-react';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';

interface ErrorAlertProps {
  title?: string;
  message: string;
  onDismiss?: () => void;
  onRetry?: () => void;
  backHref?: string;
  backLabel?: string;
}

export function ErrorAlert({
  title = 'Lỗi',
  message,
  onDismiss,
  onRetry,
  backHref,
  backLabel = 'Quay lại',
}: ErrorAlertProps) {
  return (
    <div className="space-y-4">
      <Alert variant="destructive">
        <AlertCircle className="h-4 w-4" />
        <AlertTitle className="flex items-center justify-between">
          <span>{title}</span>
          {onDismiss && (
            <Button
              variant="ghost"
              size="icon"
              className="h-6 w-6"
              onClick={onDismiss}
            >
              <X className="h-4 w-4" />
              <span className="sr-only">Bỏ qua</span>
            </Button>
          )}
        </AlertTitle>
        <AlertDescription className="mt-2">
          <p>{message}</p>
          {onRetry && (
            <Button
              variant="outline"
              size="sm"
              onClick={onRetry}
              className="mt-3"
            >
              Thử lại
            </Button>
          )}
        </AlertDescription>
      </Alert>
      {backHref && (
        <Link href={backHref}>
          <Button variant="outline" size="sm">
            <ArrowLeft className="mr-2 h-4 w-4" />
            {backLabel}
          </Button>
        </Link>
      )}
    </div>
  );
}
