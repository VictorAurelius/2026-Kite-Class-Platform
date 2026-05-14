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

export async function getBetaStatus(): Promise<BetaStatusResponse> {
  const baseURL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:9000';
  const { data } = await axios.get<BetaStatusResponse>(`${baseURL}${ENDPOINT}`, {
    timeout: 10000,
  });
  return data;
}
