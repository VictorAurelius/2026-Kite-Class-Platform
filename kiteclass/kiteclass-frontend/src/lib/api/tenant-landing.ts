/**
 * Server-side tenant landing-payload helpers (GAP-274 phase-2).
 *
 * Resolves the current tenant the same way the public landing page + layout do
 * (x-tenant-id header injected by the host→tenant middleware, then
 * NEXT_PUBLIC_TENANT_ID, then a hardcoded default), and fetches the tenant's
 * landing payload once for the public per-tenant pages (about / contact / detail
 * / layout theme). Returns `null` on an unresolved subdomain or a backend error
 * so callers render anti-fabrication empty states instead of fake placeholders.
 *
 * @author KiteClass Team
 */

import { headers } from 'next/headers';
import { publicApi } from '@/lib/api/public';

const DEFAULT_TENANT_ID = '11111111-1111-1111-1111-111111111111';

/** Tenant landing payload (loosely typed — fields per GET /tenants/{id}/landing). */
export type TenantLanding = Record<string, unknown>;

/**
 * Resolve the active tenant id from the request, or `null` when the middleware
 * flagged an unknown subdomain (so callers avoid leaking a different center's data).
 */
export async function resolveTenantId(): Promise<string | null> {
  const hdrs = await headers();
  if (hdrs.get('x-tenant-not-found')) return null;
  return (
    hdrs.get('x-tenant-id') ??
    process.env.NEXT_PUBLIC_TENANT_ID ??
    DEFAULT_TENANT_ID
  );
}

/**
 * Fetch the current tenant's landing payload. Returns `null` on unknown subdomain
 * or backend failure (degraded → callers hide tenant-specific surfaces).
 */
export async function getTenantLanding(): Promise<TenantLanding | null> {
  const tenantId = await resolveTenantId();
  if (!tenantId) return null;
  try {
    return (await publicApi.getLandingPage(tenantId)) as TenantLanding;
  } catch {
    return null;
  }
}

/** Trim a string field from the landing payload, returning `null` when empty. */
export function landingStr(ld: TenantLanding | null, key: string): string | null {
  if (!ld) return null;
  const v = ld[key];
  return typeof v === 'string' && v.trim() ? v.trim() : null;
}

/** Read a JSONB array field as a typed array, returning `[]` when missing/empty. */
export function landingArray<T = Record<string, unknown>>(
  ld: TenantLanding | null,
  key: string
): T[] {
  if (!ld) return [];
  const v = ld[key];
  return Array.isArray(v) && v.length > 0 ? (v as T[]) : [];
}
