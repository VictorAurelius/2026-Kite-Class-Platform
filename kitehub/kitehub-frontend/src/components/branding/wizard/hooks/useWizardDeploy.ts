'use client';

/**
 * useWizardDeploy — deploy controller lifted out of Step6Preview so that
 * "Triển khai" can be an EXPLICIT wizard step (5) in the kit v3 output-first
 * flow, instead of an internal sub-state of the preview step.
 *
 * Spec: documents/02-architecture/design-system/ui_kits/ai-branding-wizard-v2/v3/
 *   - generate-ready.html footer "Triển khai & lên sóng" (step 4 → step 5)
 *   - deploy-progress.html (step 5 SSE lifecycle) + deploy-failed.html (recovery)
 *   - done.html (terminal success after step 5)
 *
 * What it owns (lifted verbatim from the old Step6Preview deploy machine):
 *   - approve mutation (POST approve → 202) with GAP-1217 quality-gate 422 branch
 *   - SSE deploy stream → log entries (GAP-1021)
 *   - terminal complete → DONE screen + live landing URL (GAP-1108)
 *   - terminal error → FAILED recovery panel (GAP-1216)
 *
 * Step transitions: `start()` advances the wizard to step 5 (GO_TO_STEP) and
 * fires approve. On a 422 quality-gate rejection it returns the user to step 4
 * (the preview/review screen) so they can adjust + retry — never a dead-end.
 */

import { useEffect, useMemo, useRef, useState } from 'react';
import { toast } from 'sonner';
import { apiClient } from '@/lib/api/client';
import type { DeployingLogEntry } from '../DeployingStep';
import type { WizardAction, WizardState } from '../wizard-shared';
import { buildLandingFactsPayload } from '../facts-landing';
import {
  useApproveBrandingJob,
  useDeployStream,
  type DeployStreamEvent,
} from './index';

/**
 * Best-effort: PATCH the user-entered landing facts (GAP-1234) into the freshly
 * deployed tenant's KiteClass landing. Never throws — a failure shows a warning
 * toast and the deploy still counts as done (the user can fix the contact info
 * later in Settings → Landing). Skipped when there is no tenant id or nothing to
 * send.
 *
 * Endpoint: PUT /api/v1/tenants/{tenantId}/landing (kiteclass-core, via gateway;
 * apiClient attaches auth + X-Tenant-Id). Partial body — BE mapper IGNOREs nulls.
 */
export async function submitLandingFacts(state: WizardState): Promise<void> {
  const tenantId = state.instanceId;
  if (!tenantId) return;
  const payload = buildLandingFactsPayload(state.facts);
  if (!payload) return;
  try {
    await apiClient.put(`/api/v1/tenants/${tenantId}/landing`, payload);
  } catch {
    toast.warning('Lưu thông tin liên hệ chưa thành công — chỉnh lại trong Cài đặt landing.');
  }
}

/** Convert raw SSE deploy-stream events into renderable log rows. */
function eventsToLogEntries(events: readonly DeployStreamEvent[]): DeployingLogEntry[] {
  const out: DeployingLogEntry[] = [];
  for (const ev of events) {
    if (ev.name === 'heartbeat') continue;
    const data = (ev.data ?? {}) as {
      message?: string;
      timestamp?: string;
      ts?: string;
      level?: DeployingLogEntry['level'];
      percent?: number;
      toState?: string;
      errorCode?: string;
    };
    const timestamp = data.timestamp ?? data.ts ?? new Date().toISOString();
    let message = '';
    let level: DeployingLogEntry['level'] = 'info';
    switch (ev.name) {
      case 'log':
        message = data.message ?? '';
        level = data.level ?? 'info';
        break;
      case 'progress':
        message = `Tiến trình ${data.percent ?? 0}%`;
        level = 'pending';
        break;
      case 'state-change':
        message = `Trạng thái: ${data.toState ?? '?'}`;
        level = 'info';
        break;
      case 'complete':
        message = data.message ?? 'Triển khai hoàn tất';
        level = 'success';
        break;
      case 'error':
        message = data.message ?? `Lỗi triển khai (${data.errorCode ?? 'UNKNOWN'})`;
        level = 'error';
        break;
      default:
        continue;
    }
    if (message) out.push({ timestamp, message, level });
  }
  return out;
}

export interface WizardDeployController {
  /** Deploy in flight (step 5 active, SSE streaming). */
  isDeploying: boolean;
  /** Terminal success — orchestrator renders DoneStep. */
  deployDone: boolean;
  /** Live landing URL from the approve 202 / `complete` SSE event. */
  deployFrontendUrl: string | null;
  /** Terminal error — orchestrator renders DeployingStep FAILED recovery. */
  deployError: { message: string; code?: string; retryable: boolean } | null;
  /** Rendered SSE log rows. */
  deployLogs: DeployingLogEntry[];
  /** Trigger deploy from the step-4 footer (advances wizard to step 5). */
  start: () => void;
  /** Retry deploy after a FAILED state (stays on step 5). */
  retry: () => void;
  /** Cancel/return from the deploy step back to the review step (4). */
  back: () => void;
}

export function useWizardDeploy(opts: {
  wizardState: WizardState;
  dispatch: React.Dispatch<WizardAction>;
}): WizardDeployController {
  const { wizardState, dispatch } = opts;

  const { mutate: approveMutate } = useApproveBrandingJob();

  const [isDeploying, setIsDeploying] = useState(false);
  const [deployDone, setDeployDone] = useState(false);
  const [deployFrontendUrl, setDeployFrontendUrl] = useState<string | null>(null);
  const [deployError, setDeployError] = useState<
    { message: string; code?: string; retryable: boolean } | null
  >(null);
  const deployCompletedRef = useRef(false);

  const deployStream = useDeployStream(wizardState.jobId ?? undefined, {
    enabled: isDeploying && Boolean(wizardState.jobId),
  });

  const deployLogs = useMemo<DeployingLogEntry[]>(
    () => eventsToLogEntries(deployStream.events),
    [deployStream.events],
  );

  // Terminal events: complete → DONE screen (live landing URL, GAP-1108);
  // error → FAILED recovery panel (GAP-1216).
  useEffect(() => {
    if (!isDeploying || deployCompletedRef.current) return;
    const latest = deployStream.latestEvent;
    if (latest?.name === 'complete') {
      deployCompletedRef.current = true;
      const data = (latest.data ?? {}) as { frontendUrl?: string };
      if (data.frontendUrl) setDeployFrontendUrl(data.frontendUrl);
      toast.success('Triển khai thành công!');
      setDeployDone(true);
      // Best-effort: persist the optional landing facts (GAP-1234) after the
      // tenant is live. Fire-and-forget — never blocks the DONE screen.
      void submitLandingFacts(wizardState);
    } else if (latest?.name === 'error') {
      deployCompletedRef.current = true;
      const data = (latest.data ?? {}) as {
        message?: string;
        errorCode?: string;
        retryable?: boolean;
      };
      setDeployError({
        message:
          data.message ??
          'Quá trình triển khai gặp sự cố. Bạn có thể thử lại hoặc quay lại chỉnh sửa.',
        code: data.errorCode,
        retryable: data.retryable ?? true,
      });
    }
  }, [isDeploying, deployStream.latestEvent]);

  const runApprove = () => {
    if (!wizardState.jobId) return;
    deployCompletedRef.current = false;
    setDeployError(null);
    setDeployDone(false);
    approveMutate(
      {
        jobId: wizardState.jobId,
        slug: wizardState.slug || undefined,
        templateId: wizardState.templateId,
        approvedResources: wizardState.approvedResources,
      },
      {
        onSuccess: (res) => {
          if (res?.frontendUrl) setDeployFrontendUrl(res.frontendUrl);
        },
        onError: (err) => {
          const e = err as {
            response?: { status?: number; data?: { errorCode?: string; qualityScore?: number } };
          };
          const status = e?.response?.status;
          const code = e?.response?.data?.errorCode;
          // GAP-1217 — server-side quality gate rejected the approve (422).
          // Return the user to the preview (step 4) so they can edit + retry.
          if (status === 422 || code === 'QUALITY_GATE_FAILED') {
            const score = e?.response?.data?.qualityScore;
            toast.error(
              `Chưa đạt chuẩn chất lượng${
                score != null ? ` (điểm ${score}/100)` : ''
              } — hãy điều chỉnh rồi triển khai lại.`,
            );
            setIsDeploying(false);
            deployCompletedRef.current = true;
            dispatch({ type: 'GO_TO_STEP', step: 4 });
            return;
          }
          // Other approve failures → FAILED panel (retryable, stays on step 5).
          setDeployError({
            message: 'Không gửi được yêu cầu triển khai. Vui lòng thử lại.',
            code,
            retryable: true,
          });
          deployCompletedRef.current = true;
        },
      },
    );
    setIsDeploying(true);
  };

  const start = () => {
    if (!wizardState.jobId) return;
    // Advance to the explicit "Triển khai" step (5) then fire approve.
    dispatch({ type: 'GO_TO_STEP', step: 5 });
    runApprove();
  };

  const retry = () => runApprove();

  const back = () => {
    setIsDeploying(false);
    setDeployError(null);
    deployCompletedRef.current = false;
    dispatch({ type: 'GO_TO_STEP', step: 4 });
  };

  return {
    isDeploying,
    deployDone,
    deployFrontendUrl,
    deployError,
    deployLogs,
    start,
    retry,
    back,
  };
}
