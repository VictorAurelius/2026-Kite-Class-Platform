/**
 * DeployingStep — Step 6 deploy-in-progress state for AI Branding Wizard v2.
 *
 * Wave 32 Bucket D — Direction C 6-step refactor.
 *
 * Renders during the GENERATING / INITIALIZING transition window. Two parts:
 *   1. SSE-driven log feed — shows live deploy progress messages
 *   2. Inline G9 LifecycleInline — visually surfaces state transitions
 *
 * SSE backend endpoint NOT YET IMPLEMENTED (state-checked 2026-05-07; no
 * `EventSource` consumers in `kitehub-frontend/src/lib/api/`). The component
 * accepts a `logs` prop so the parent owns transport. When the backend ships
 * `GET /branding/jobs/:id/stream` (text/event-stream), the parent will wire
 * `EventSource` and pass new lines as they arrive.
 *
 * // TODO(GAP-272d): wire SSE log streaming to real
 * //   `GET /branding/jobs/:id/stream` endpoint when backend ships. Component
 * //   accepts `logs` prop today so transport swap is a parent-only change.
 */

'use client';

import { useEffect, useRef } from 'react';
import { Card } from '@/components/ui/card';
import { Info, Loader2, AlertTriangle, RotateCw, ArrowLeft } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { LifecycleInline } from './LifecycleInline';
import type { LifecycleInlineProps } from './LifecycleInline';

export interface DeployingLogEntry {
  /** ISO timestamp string. */
  timestamp: string;
  /** Log line text — already localized by the backend. */
  message: string;
  /** Severity — colors the row icon. */
  level: 'info' | 'success' | 'pending' | 'error';
}

export interface DeployingStepProps {
  /** Log lines so far. New entries auto-scroll into view. */
  logs: readonly DeployingLogEntry[];
  /** Estimated progress percentage 0..100. */
  progressPercent?: number;
  /** Active instance ID — forwarded to LifecycleInline. */
  instanceId: string | undefined;
  /** Optional state override forwarded to LifecycleInline (for tests). */
  lifecycleStateOverride?: LifecycleInlineProps['stateOverride'];
  /** Optional retry handler when FAILED state appears. */
  onRetry?: () => void;
  /**
   * FAILED recovery (GAP-1216). When set, the component renders a terminal
   * error panel with retry + back actions INSTEAD of the in-progress copy —
   * the deploy stream emitted an `error` event (GENERATION_FAILED / deploy
   * failure / STREAM_DISCONNECTED). Keeps the user out of a dead-end.
   */
  errorMessage?: string;
  /** Machine error code surfaced for support (e.g. GENERATION_FAILED). */
  errorCode?: string;
  /** Whether the surfaced error is retryable (drives retry CTA visibility). */
  errorRetryable?: boolean;
  /** Back-to-preview handler (FAILED panel secondary action). */
  onBack?: () => void;
}

const LEVEL_PREFIX: Record<DeployingLogEntry['level'], string> = {
  info: 'ℹ',
  success: '✓',
  pending: '⟳',
  error: '✗',
};

const LEVEL_CLASS: Record<DeployingLogEntry['level'], string> = {
  info: 'text-blue-600',
  success: 'text-emerald-600',
  pending: 'text-blue-600',
  error: 'text-red-600',
};

export function DeployingStep(props: DeployingStepProps) {
  const {
    logs,
    progressPercent,
    instanceId,
    lifecycleStateOverride,
    onRetry,
    errorMessage,
    errorCode,
    errorRetryable = true,
    onBack,
  } = props;
  const logEndRef = useRef<HTMLDivElement | null>(null);

  // Auto-scroll log feed to latest line.
  useEffect(() => {
    logEndRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' });
  }, [logs.length]);

  // GAP-1216 — FAILED recovery panel: deploy stream emitted an error. Surface a
  // clear message + retry/back instead of leaving the user stuck on a spinner.
  if (errorMessage) {
    return (
      <div className="max-w-2xl mx-auto space-y-4" data-testid="deploying-step-failed">
        <Card className="p-6 text-center space-y-4 border-destructive/40">
          <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-destructive/10 text-destructive">
            <AlertTriangle className="h-7 w-7" aria-hidden="true" />
          </div>
          <div>
            <h2 className="text-xl font-bold text-foreground">Triển khai chưa thành công</h2>
            <p className="mt-2 text-sm text-muted-foreground" data-testid="deploying-step-error-message">
              {errorMessage}
            </p>
            {errorCode && (
              <p className="mt-1 font-mono text-xs text-muted-foreground">
                Mã lỗi: {errorCode}
              </p>
            )}
          </div>
          <div className="flex flex-col sm:flex-row items-center justify-center gap-2">
            {errorRetryable && onRetry && (
              <Button onClick={onRetry} data-testid="deploying-step-retry">
                <RotateCw className="mr-2 h-4 w-4" aria-hidden="true" />
                Thử lại
              </Button>
            )}
            {onBack && (
              <Button variant="outline" onClick={onBack} data-testid="deploying-step-back">
                <ArrowLeft className="mr-2 h-4 w-4" aria-hidden="true" />
                Quay lại chỉnh sửa
              </Button>
            )}
          </div>
        </Card>
        <Card className="p-3" data-testid="deploying-step-log">
          <div className="text-xs space-y-1 font-mono max-h-40 overflow-y-auto" role="log">
            {logs.map((entry, idx) => (
              <div
                key={`${entry.timestamp}-${idx}`}
                className={LEVEL_CLASS[entry.level]}
                data-testid={`deploying-step-log-line-${idx}`}
              >
                {LEVEL_PREFIX[entry.level]} {entry.timestamp.slice(11, 19)} — {entry.message}
              </div>
            ))}
          </div>
        </Card>
      </div>
    );
  }

  return (
    <div className="max-w-2xl mx-auto space-y-4" data-testid="deploying-step">
      <div>
        <p className="text-sm font-semibold text-primary uppercase tracking-wide mb-1 flex items-center gap-2">
          <Loader2 className="w-3 h-3 animate-spin" />
          Đang xử lý · không cần đóng tab
        </p>
        <h2 className="text-2xl font-bold text-foreground mb-1">Đang triển khai trang web…</h2>
        <p className="text-muted-foreground text-sm">
          Ước tính ~45 giây. Hệ thống đang đẩy theme + assets lên CDN, cấu hình DNS, và khởi tạo database. Tab này sẽ tự cập nhật.
        </p>
      </div>

      <LifecycleInline
        instanceId={instanceId}
        stateOverride={lifecycleStateOverride}
        onRetry={onRetry}
      />

      {progressPercent !== undefined && (
        <Card className="p-4">
          <div
            className="h-2 rounded-full overflow-hidden bg-muted/30"
            aria-label="Tiến trình triển khai"
            role="progressbar"
            aria-valuenow={progressPercent}
            aria-valuemin={0}
            aria-valuemax={100}
            data-testid="deploying-step-progress"
          >
            <div
              className="h-full bg-primary transition-all"
              style={{ width: `${Math.max(0, Math.min(100, progressPercent))}%` }}
            />
          </div>
          <p className="text-xs text-muted-foreground mt-2">
            {progressPercent}% — đang đẩy assets lên CDN…
          </p>
        </Card>
      )}

      <Card className="p-3" data-testid="deploying-step-log">
        <div
          className="text-xs space-y-1 font-mono max-h-40 overflow-y-auto"
          role="log"
          aria-live="polite"
          aria-label="Log triển khai"
        >
          {logs.length === 0 ? (
            <p className="text-muted-foreground italic">Đang chờ log…</p>
          ) : (
            logs.map((entry, idx) => (
              <div
                key={`${entry.timestamp}-${idx}`}
                className={LEVEL_CLASS[entry.level]}
                data-testid={`deploying-step-log-line-${idx}`}
              >
                {LEVEL_PREFIX[entry.level]} {entry.timestamp.slice(11, 19)} — {entry.message}
              </div>
            ))
          )}
          <div ref={logEndRef} />
        </div>
      </Card>

      <Card className="p-4 flex items-start gap-3 text-sm bg-blue-50/50 border-blue-200">
        <Info className="w-5 h-5 mt-0.5 shrink-0 text-blue-600" />
        <div>
          <p className="font-semibold">Hệ thống đang chạy quality gate cuối</p>
          <p className="text-muted-foreground">
            Sau khi DEPLOYED, một lần kiểm tra cuối sẽ chạy. Nếu fail, instance sẽ về FAILED — bạn sẽ được thông báo qua email.
          </p>
        </div>
      </Card>
    </div>
  );
}
