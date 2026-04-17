/**
 * Error boundary component for handling runtime errors.
 * Provides fallback UI when errors occur in child components.
 *
 * @author KiteClass Team
 * @since 3.4.0
 */

'use client';

import { Component, ReactNode } from 'react';
import { AlertCircle } from 'lucide-react';
import { Button } from '@/components/ui/button';

interface ErrorBoundaryProps {
  children: ReactNode;
  fallback?: ReactNode;
}

interface ErrorBoundaryState {
  hasError: boolean;
  error?: Error;
}

export class ErrorBoundary extends Component<
  ErrorBoundaryProps,
  ErrorBoundaryState
> {
  constructor(props: ErrorBoundaryProps) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    console.error('ErrorBoundary caught an error:', error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      if (this.props.fallback) {
        return this.props.fallback;
      }

      return (
        <div className="flex flex-col items-center justify-center min-h-[400px] p-8">
          <AlertCircle className="h-16 w-16 text-destructive mb-4" />
          <h2 className="text-2xl font-bold mb-2">Đã xảy ra lỗi</h2>
          <p className="text-muted-foreground mb-6 text-center max-w-md">
            Rất tiếc, đã có lỗi xảy ra khi tải trang này. Vui lòng thử lại sau.
          </p>
          <Button
            onClick={() => {
              this.setState({ hasError: false, error: undefined });
              window.location.reload();
            }}
          >
            Tải lại trang
          </Button>
          {process.env.NODE_ENV === 'development' && this.state.error && (
            <details className="mt-8 p-4 bg-muted rounded-lg max-w-2xl w-full">
              <summary className="cursor-pointer font-semibold">
                Chi tiết lỗi (development only)
              </summary>
              <pre className="mt-4 text-xs overflow-auto">
                {this.state.error.toString()}
                {'\n\n'}
                {this.state.error.stack}
              </pre>
            </details>
          )}
        </div>
      );
    }

    return this.props.children;
  }
}
