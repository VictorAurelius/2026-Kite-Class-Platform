/**
 * Beta Status API client (Wave 78 GAP-539).
 *
 * Schema source-of-truth:
 * `documents/01-business/kitehub/beta-status/api-contract.md`.
 *
 * Public endpoint — no auth required.
 *
 * @since Wave 78 Bucket B
 */

import axios from 'axios';

export interface BetaStatusKnownIssue {
  title: string;
  severity: 'MINOR' | 'MAJOR' | 'CRITICAL';
  since: string;
}

export interface BetaStatusResponse {
  version: string;
  lastUpdatedAt: string;
  contentMarkdown: string;
  currentStatus: 'OPERATIONAL' | 'DEGRADED' | 'PARTIAL_OUTAGE' | 'MAJOR_OUTAGE' | 'MAINTENANCE';
  knownIssues: BetaStatusKnownIssue[];
}

const ENDPOINT = '/api/v1/beta-status';

/**
 * Resolve the gateway base URL. GAP-1444: `getBetaStatus` runs in a Server
 * Component, where `localhost:9000` points at the Next.js container itself, not
 * the gateway — SSR fetch 404s → page falls back forever to the stale changelog.
 * Server-side must use the Docker-network DNS name (`INTERNAL_API_URL`); the
 * browser uses the host-mapped `NEXT_PUBLIC_API_URL`. Mirrors the kiteclass
 * server/browser split in `kiteclass-frontend/src/lib/api/public.ts`.
 */
function resolveBaseUrl(): string {
  const isServer = typeof window === 'undefined';
  return isServer
    ? process.env.INTERNAL_API_URL || 'http://kite-gateway:9000'
    : process.env.NEXT_PUBLIC_API_URL || 'http://localhost:9000';
}

export async function getBetaStatus(): Promise<BetaStatusResponse> {
  const { data } = await axios.get<BetaStatusResponse>(`${resolveBaseUrl()}${ENDPOINT}`, {
    timeout: 10000,
  });
  return data;
}
