'use client';

/**
 * DeployingStep — Wave 32 Bucket D (GAP-272)
 *
 * Step 6 "Deploying" sub-state:
 *   - SSE log streaming (mock EventSource if endpoint absent)
 *   - G9 InstanceLifecycleStatus inline via LifecycleInline
 *   - Simulates log lines appearing every 500ms in dev/mock mode
 *
 * TODO: Replace mock EventSource with real SSE endpoint:
 *   GET /api/v1/branding/instances/{instanceId}/deploy-stream
 *   Content-Type: text/event-stream
 */

import React, { useEffect, useRef, useState, useCallback } from 'react';
import { Loader2, Terminal } from 'lucide-react';
import type { WizardLifecycleState } from './LifecycleInline';
import { LifecycleInline } from './LifecycleInline';

const MOCK_LOG_LINES = [
  '[deploy] Khởi tạo triển khai…',
  '[deploy] Tải assets lên MinIO: logo.png ✓',
  '[deploy] Tải assets lên MinIO: banner.png ✓',
  '[deploy] Áp dụng CSS variables vào theme…',
  '[deploy] Kiểm tra WCAG contrast: 4.7:1 ✓',
  '[deploy] Kiểm tra Asset 404: 0 link gãy ✓',
  '[deploy] Chạy Visual Regression Diff: 8% ✓',
  '[deploy] Chạy Quality Gate scoring…',
  '[deploy] Điểm chất lượng: 95/100 ✓',
  '[deploy] Publish sự kiện branding.deployed tới RabbitMQ…',
  '[deploy] Cập nhật trạng thái instance → DEPLOYED',
  '[deploy] Hoàn thành triển khai ✓',
];

const MOCK_LOG_INTERVAL_MS = 500;

export interface DeployingStepProps {
  instanceId: string;
  instanceName?: string;
  /** Lifecycle state to show inline. Updates as deployment progresses. */
  lifecycleStatus: WizardLifecycleState;
  /** Live URL shown once DEPLOYED */
  liveUrl?: string;
  /** If true, use mock EventSource (dev/no endpoint). Default: true. */
  useMockSse?: boolean;
  /** Called when user clicks Retry in FAILED lifecycle state. */
  onRetry?: () => void;
}

export function DeployingStep({
  instanceId,
  instanceName,
  lifecycleStatus,
  liveUrl,
  useMockSse = true,
  onRetry,
}: DeployingStepProps) {
  const [logLines, setLogLines] = useState<string[]>([]);
  const [isStreamComplete, setIsStreamComplete] = useState(false);
  const logContainerRef = useRef<HTMLDivElement>(null);
  const mockIndexRef = useRef(0);

  // Auto-scroll log container to bottom
  useEffect(() => {
    const el = logContainerRef.current;
    if (el) {
      el.scrollTop = el.scrollHeight;
    }
  }, [logLines]);

  // Mock SSE: simulate log lines appearing every 500ms
  const startMockStream = useCallback(() => {
    const interval = setInterval(() => {
      const idx = mockIndexRef.current;
      if (idx >= MOCK_LOG_LINES.length) {
        clearInterval(interval);
        setIsStreamComplete(true);
        return;
      }
      const line = MOCK_LOG_LINES[idx];
      if (line !== undefined) {
        setLogLines((prev) => [...prev, line]);
      }
      mockIndexRef.current = idx + 1;
    }, MOCK_LOG_INTERVAL_MS);

    return () => clearInterval(interval);
  }, []);

  // Real SSE: connect to backend deploy-stream endpoint
  const startRealSse = useCallback(() => {
    // TODO: replace URL with real SSE endpoint when available
    const sseUrl = `/api/v1/branding/instances/${instanceId}/deploy-stream`;
    const source = new EventSource(sseUrl);

    source.onmessage = (event: MessageEvent<string>) => {
      setLogLines((prev) => [...prev, event.data]);
    };

    source.addEventListener('complete', () => {
      source.close();
      setIsStreamComplete(true);
    });

    source.onerror = () => {
      source.close();
      setIsStreamComplete(true);
    };

    return () => source.close();
  }, [instanceId]);

  useEffect(() => {
    const cleanup = useMockSse ? startMockStream() : startRealSse();
    return cleanup;
  }, [useMockSse, startMockStream, startRealSse]);

  const isDone = lifecycleStatus === 'DEPLOYED' || lifecycleStatus === 'FAILED';

  return (
    <div className="space-y-5">
      {/* Header */}
      <div className="flex items-center gap-3">
        {!isDone && (
          <Loader2 className="w-6 h-6 text-primary animate-spin shrink-0" aria-hidden />
        )}
        <div>
          <h2 className="text-xl font-bold">
            {lifecycleStatus === 'DEPLOYED'
              ? 'Triển khai hoàn thành!'
              : lifecycleStatus === 'FAILED'
                ? 'Triển khai thất bại'
                : 'Đang triển khai trang web…'}
          </h2>
          <p className="text-sm text-muted-foreground">
            {lifecycleStatus === 'DEPLOYED'
              ? 'Trang web KiteClass của bạn đã sẵn sàng.'
              : lifecycleStatus === 'FAILED'
                ? 'Có lỗi xảy ra trong quá trình triển khai.'
                : 'Quá trình này thường mất 1–3 phút. Vui lòng đợi.'}
          </p>
        </div>
      </div>

      {/* SSE Log viewer */}
      <div className="rounded-xl border bg-slate-950 overflow-hidden">
        <div className="flex items-center gap-2 px-4 py-2 border-b border-white/10 bg-slate-900">
          <Terminal className="w-4 h-4 text-slate-400" aria-hidden />
          <span className="text-xs text-slate-400 font-mono">Deploy log</span>
          {!isStreamComplete && (
            <span className="ml-auto flex items-center gap-1.5 text-xs text-emerald-400">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" aria-hidden />
              Live
            </span>
          )}
          {isStreamComplete && (
            <span className="ml-auto text-xs text-slate-500">Hoàn thành</span>
          )}
        </div>
        <div
          ref={logContainerRef}
          className="h-48 overflow-y-auto p-4 font-mono text-xs text-slate-300 space-y-1 scroll-smooth"
          role="log"
          aria-label="Deploy log stream"
          aria-live="polite"
        >
          {logLines.length === 0 && (
            <p className="text-slate-500">Đang kết nối…</p>
          )}
          {logLines.map((line, i) => (
            <p key={i} className="leading-relaxed">
              {line}
            </p>
          ))}
          {!isStreamComplete && logLines.length > 0 && (
            <p className="text-slate-500 animate-pulse">_</p>
          )}
        </div>
      </div>

      {/* G9 InstanceLifecycleStatus inline */}
      <LifecycleInline
        status={lifecycleStatus}
        instanceId={instanceId}
        instanceName={instanceName}
        liveUrl={liveUrl}
        onRetry={onRetry}
      />
    </div>
  );
}
