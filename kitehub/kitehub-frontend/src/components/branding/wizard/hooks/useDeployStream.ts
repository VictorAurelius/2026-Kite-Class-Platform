/**
 * useDeployStream — Wave 34 SSE deploy progress stream (GAP-272e).
 *
 * Schema: GET `/api/v1/branding/jobs/{jobId}/deploy-stream`
 * (text/event-stream) per `api-contract.md` Wave 34.
 *
 * Event names per contract: log / progress / state-change / complete /
 * error / heartbeat. Per `ai-branding-guidelines.md` §3.3 heavy AI tasks
 * are async — this hook is the only acceptable consumer for live
 * progress in the wizard (no polling fallback in v1).
 *
 * Replaces Wave 32 inline log-fixture in `DeployingStep` callers. The
 * hook unwraps each SSE event into `{ name, data }` so callers can
 * branch by event name. Heartbeats are filtered out before reaching
 * the consumer (kept as a no-op).
 */

import { useEffect, useRef, useState } from 'react';
import { endpoints } from '@/lib/api/endpoints';
import { getAccessToken } from '@/lib/auth/jwt-storage';
import type {
  DeployStreamEvent,
  DeployStreamEventName,
} from './types';

const EVENT_NAMES: readonly DeployStreamEventName[] = [
  'log',
  'progress',
  'state-change',
  'complete',
  'error',
  'heartbeat',
] as const;

export interface UseDeployStreamOptions {
  /** When false the hook does NOT open an EventSource (default true). */
  enabled?: boolean;
}

export interface UseDeployStreamResult {
  events: DeployStreamEvent[];
  /** Latest non-heartbeat event, useful for branching. */
  latestEvent: DeployStreamEvent | undefined;
  /** Stream open state — false once `complete` or `error` arrives. */
  isStreaming: boolean;
}

export function useDeployStream(
  jobId: string | undefined,
  options: UseDeployStreamOptions = {}
): UseDeployStreamResult {
  const { enabled = true } = options;
  const [events, setEvents] = useState<DeployStreamEvent[]>([]);
  const [isStreaming, setIsStreaming] = useState(false);
  // GAP-1105: a terminal event (server `complete`/`error`) closes the stream
  // intentionally. EventSource then fires its native `error` on the underlying
  // socket close — that is NOT a real failure, so suppress the spurious
  // STREAM_DISCONNECTED once we've already seen a terminal event. The mock
  // provision completes in ~4s, so this completion-race is the common path.
  const completedRef = useRef(false);

  useEffect(() => {
    if (!enabled || !jobId) return;
    completedRef.current = false;
    if (typeof window === 'undefined' || typeof window.EventSource === 'undefined') {
      // SSR or jsdom without EventSource polyfill — bail silently.
      return;
    }

    // GAP-1021 pt2: browser EventSource cannot set the Authorization header, so
    // the JWT is passed as a short-lived `?token=` query param. The gateway
    // (JwtAuthenticationGatewayFilter) accepts token-in-query when no Bearer
    // header is present and injects the X-User-* headers downstream.
    const baseUrl = endpoints.brandingV1.jobDeployStream(jobId);
    const token = getAccessToken();
    const url = token
      ? `${baseUrl}?token=${encodeURIComponent(token)}`
      : baseUrl;
    const source = new window.EventSource(url, { withCredentials: true });
    setIsStreaming(true);

    const handlers: Array<{ name: DeployStreamEventName; fn: (e: MessageEvent) => void }> = [];

    for (const name of EVENT_NAMES) {
      const fn = (e: MessageEvent) => {
        let data: unknown = null;
        try {
          data = e.data ? JSON.parse(e.data) : null;
        } catch {
          data = e.data;
        }
        if (name === 'heartbeat') {
          return; // keepalive — drop
        }
        const event: DeployStreamEvent = { name, data };
        setEvents((prev) => [...prev, event]);
        if (name === 'complete' || name === 'error') {
          completedRef.current = true;
          setIsStreaming(false);
          source.close();
        }
      };
      source.addEventListener(name, fn as EventListener);
      handlers.push({ name, fn });
    }

    const onError = () => {
      // GAP-1105: a native EventSource error right after a terminal event is the
      // post-complete socket close, not a real disconnect — swallow it silently.
      if (completedRef.current) {
        setIsStreaming(false);
        source.close();
        return;
      }
      // Genuine network drop — close and surface as an `error` event.
      setIsStreaming(false);
      setEvents((prev) => [
        ...prev,
        { name: 'error', data: { errorCode: 'STREAM_DISCONNECTED', retryable: true } },
      ]);
      source.close();
    };
    source.addEventListener('error', onError);

    return () => {
      for (const { name, fn } of handlers) {
        source.removeEventListener(name, fn as EventListener);
      }
      source.removeEventListener('error', onError);
      source.close();
      setIsStreaming(false);
    };
  }, [enabled, jobId]);

  const latestEvent = events.length > 0 ? events[events.length - 1] : undefined;

  return { events, latestEvent, isStreaming };
}
