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
import apiClient from '@/lib/api/client';
import { endpoints } from '@/lib/api/endpoints';
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
      // SSR or jsdom without EventSource polyfill — bail silently (no mint call).
      return;
    }

    let aborted = false;
    let source: EventSource | null = null;
    const handlers: Array<{ name: DeployStreamEventName; fn: (e: MessageEvent) => void }> = [];
    let onError: ((e: Event) => void) | null = null;

    // GAP-1021 — open the stream with a freshly-minted, short-lived `?access_token=`
    // token (SseQueryTokenAuthFilter). Chosen over raw `?token=<JWT>` (the legacy
    // gateway path): the minted token is short, scoped to this job, and won't
    // expire mid-walk like a full JWT carried in the URL.
    const openStream = (accessToken: string) => {
      if (aborted) return;
      // GAP-1105: EventSource resolves a relative URL against window.location.origin
      // (the frontend), NOT the axios baseURL — so a relative path 404'd at Next.js
      // → STREAM_DISCONNECTED. Prepend the SAME gateway base apiClient uses so the
      // SSE actually reaches the gateway (:9000) + branding deploy-stream.
      const apiBase = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:9000';
      const baseUrl = `${apiBase}${endpoints.brandingV1.jobDeployStream(jobId)}`;
      const url = `${baseUrl}?access_token=${encodeURIComponent(accessToken)}`;
      const src = new window.EventSource(url, { withCredentials: true });
      source = src;
      setIsStreaming(true);

      for (const name of EVENT_NAMES) {
        const fn = (e: MessageEvent) => {
          // GAP-1105: the browser delivers its NATIVE EventSource connection error
          // to the listener registered for the server-sent `error` event too — but
          // with no `data`. Ignore it here: `onError` handles genuine disconnects,
          // and a real server `error` event always carries a JSON payload.
          if (name === 'error' && !e.data) return;
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
            src.close();
          }
        };
        src.addEventListener(name, fn as EventListener);
        handlers.push({ name, fn });
      }

      onError = () => {
        // GAP-1105: a native EventSource error right after a terminal event is the
        // post-complete socket close, not a real disconnect — swallow it silently.
        if (completedRef.current) {
          setIsStreaming(false);
          src.close();
          return;
        }
        // Genuine network drop — close and surface as an `error` event.
        setIsStreaming(false);
        setEvents((prev) => [
          ...prev,
          { name: 'error', data: { errorCode: 'STREAM_DISCONNECTED', retryable: true } },
        ]);
        src.close();
      };
      src.addEventListener('error', onError);
    };

    // Mint first (Bearer JWT auto-attached by apiClient interceptor), then open.
    apiClient
      .post<{ token: string; expiresInSeconds: number }>(
        endpoints.brandingV1.jobSseToken(jobId),
      )
      .then((res) => {
        if (!aborted && res.data?.token) openStream(res.data.token);
      })
      .catch(() => {
        if (aborted) return;
        setIsStreaming(false);
        setEvents((prev) => [
          ...prev,
          { name: 'error', data: { errorCode: 'SSE_TOKEN_MINT_FAILED', retryable: true } },
        ]);
      });

    return () => {
      aborted = true;
      if (source) {
        for (const { name, fn } of handlers) {
          source.removeEventListener(name, fn as EventListener);
        }
        if (onError) source.removeEventListener('error', onError);
        source.close();
      }
      setIsStreaming(false);
    };
  }, [enabled, jobId]);

  const latestEvent = events.length > 0 ? events[events.length - 1] : undefined;

  return { events, latestEvent, isStreaming };
}
