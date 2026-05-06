/**
 * Server-side consent API wrappers (GAP-353b Wave 25 Bucket A).
 *
 * Best-effort: every wrapper swallows network/server errors and returns
 * `null` (read) or `false` (write) so the LocalStorage primary path never
 * blocks on offline / API outages.
 *
 * Endpoints (per `documents/01-business/kitehub/marketing/api-contract.md`):
 *   POST /api/v1/consent/record           — idempotent upsert
 *   GET  /api/v1/consent/{visitorId}      — read latest record
 *   POST /api/v1/consent/{visitorId}/revoke
 */

import type { ConsentCategory } from './types';

export type ConsentApiPayload = {
  visitorId: string;
  analyticsConsented: boolean;
  marketingConsented: boolean;
  /** Always true server-side — sent for forward-compatibility. */
  essentialConsented?: boolean;
  consentVersion?: number;
  userId?: number | null;
  tenantId?: string | null;
};

export type ConsentApiRecord = {
  id: number;
  visitorId: string;
  analyticsConsented: boolean;
  marketingConsented: boolean;
  essentialConsented: boolean;
  consentVersion: number;
  createdAt: string;
  updatedAt: string;
  expiresAt: string;
  revokedAt: string | null;
  revoked: boolean;
  userId: number | null;
  tenantId: string | null;
};

/**
 * Resolve the API base URL. Reads `KITE_CONSENT_API_BASE` from
 * `globalThis.__KITE_CONFIG__` first (runtime injection),
 * then falls back to relative path so the gateway routes by host.
 */
function resolveBaseUrl(): string {
  const cfg = (globalThis as unknown as { __KITE_CONFIG__?: { KITE_CONSENT_API_BASE?: string } })
    .__KITE_CONFIG__;
  return cfg?.KITE_CONSENT_API_BASE ?? '';
}

async function safeFetch(input: string, init?: RequestInit): Promise<Response | null> {
  if (typeof fetch === 'undefined') return null;
  try {
    return await fetch(input, init);
  } catch {
    return null;
  }
}

/**
 * Upsert consent record on the server. Returns `true` on success, `false`
 * on any failure (offline, 4xx, 5xx). Never throws.
 */
export async function recordConsent(payload: ConsentApiPayload): Promise<boolean> {
  const res = await safeFetch(`${resolveBaseUrl()}/api/v1/consent/record`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      visitorId: payload.visitorId,
      analyticsConsented: payload.analyticsConsented,
      marketingConsented: payload.marketingConsented,
      essentialConsented: payload.essentialConsented ?? true,
      consentVersion: payload.consentVersion ?? 1,
      userId: payload.userId ?? null,
      tenantId: payload.tenantId ?? null,
    }),
    credentials: 'include',
  });
  return res !== null && res.ok;
}

/** Read latest record. Returns `null` if missing, offline, or any error. */
export async function getConsent(visitorId: string): Promise<ConsentApiRecord | null> {
  const res = await safeFetch(
    `${resolveBaseUrl()}/api/v1/consent/${encodeURIComponent(visitorId)}`,
    { method: 'GET', credentials: 'include' },
  );
  if (!res || !res.ok) return null;
  try {
    return (await res.json()) as ConsentApiRecord;
  } catch {
    return null;
  }
}

/** Revoke consent server-side. Returns `true` on success, `false` otherwise. */
export async function revokeConsent(visitorId: string): Promise<boolean> {
  const res = await safeFetch(
    `${resolveBaseUrl()}/api/v1/consent/${encodeURIComponent(visitorId)}/revoke`,
    { method: 'POST', credentials: 'include' },
  );
  return res !== null && res.ok;
}

/** Convenience: derive payload from a category map. */
export function buildPayload(
  visitorId: string,
  categories: Record<ConsentCategory, boolean>,
  extras: Pick<ConsentApiPayload, 'userId' | 'tenantId' | 'consentVersion'> = {},
): ConsentApiPayload {
  return {
    visitorId,
    analyticsConsented: categories.analytics === true,
    marketingConsented: categories.marketing === true,
    essentialConsented: true,
    consentVersion: extras.consentVersion ?? 1,
    userId: extras.userId ?? null,
    tenantId: extras.tenantId ?? null,
  };
}
