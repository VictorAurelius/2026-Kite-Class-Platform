/**
 * Tenant resolution client — calls Public Tenant Resolve BE endpoint
 * (Wave tenant-domain-1 Bucket C, GAP-811).
 *
 * Contract: `documents/01-business/kitehub/marketing/api-contract.md` §9.1
 * Endpoint: `GET /api/v1/public/tenants/by-subdomain/{slug}`
 *
 * Bucket B (GAP-813) ships the backing controller. Until then, FE tests
 * consume MSW handlers in `src/test/msw/handlers/tenant.ts` per
 * `.claude/rules/contract-first-for-cross-layer.md`.
 *
 * Semantics:
 * - 200 OK + ACTIVE → returns `TenantResolveResult`
 * - 404 NOT_FOUND   → returns `null` (cached as negative entry)
 * - 410 GONE        → throws `TenantSuspendedError` (caller redirects to /suspended)
 * - Network / 5xx   → throws `TenantResolveNetworkError` (caller falls through gracefully)
 *
 * 5-min cache layered via `tenantCache.ts` per api-contract.md §9.2.
 *
 * @author KiteHub Team
 * @since Wave tenant-domain-1 Bucket C
 */

import { getCached, setCached } from './tenantCache';

export type TenantStatus = 'ACTIVE' | 'SUSPENDED' | 'ARCHIVED' | 'DELETED';

export interface TenantResolveResult {
  /** Tenant UUID (stable across renames). */
  id: string;
  /** Echo input slug (lowercase). */
  subdomain: string;
  /** Organization display name (Vietnamese OK). */
  name: string;
  /** When the endpoint returns 200, status is always `ACTIVE`. */
  status: TenantStatus;
}

/**
 * Slug regex per api-contract.md §9.1.1:
 * lowercase-kebab, length 1-50, no leading/trailing hyphen, single char allowed.
 */
const SLUG_RE = /^[a-z0-9]([a-z0-9-]{0,48}[a-z0-9])?$/;

/**
 * Thrown when BE returns 410 GONE — tenant exists but is SUSPENDED / ARCHIVED / DELETED.
 *
 * Caller (middleware) should redirect to a friendly status page rather than
 * render the marketing landing.
 */
export class TenantSuspendedError extends Error {
  constructor(
    public readonly slug: string,
    public readonly status: Exclude<TenantStatus, 'ACTIVE'>,
    public readonly code: string,
  ) {
    super(`Tenant '${slug}' is ${status.toLowerCase()} (code: ${code})`);
    this.name = 'TenantSuspendedError';
  }
}

/**
 * Thrown when BE is unreachable (network failure, 5xx, timeout).
 *
 * Caller should degrade gracefully (pass-through without tenant injection) per
 * GAP-811 §AC "BE down / slug không tồn tại → landing degrade về fallback
 * branding KHÔNG crash (graceful)".
 */
export class TenantResolveNetworkError extends Error {
  constructor(
    public readonly slug: string,
    public readonly cause: unknown,
  ) {
    super(`Failed to resolve tenant '${slug}' due to network / upstream error`);
    this.name = 'TenantResolveNetworkError';
  }
}

/**
 * Resolve a subdomain slug to a tenant.
 *
 * Cache strategy:
 * - Hit (value present) → return immediately, 0 network calls
 * - Hit (cached `null`) → return `null`, 0 network calls (negative cache)
 * - Miss → fetch BE, cache result on 200/404
 *
 * Errors are NOT cached so transient network issues recover on next request.
 */
export async function resolveTenant(
  slug: string,
  options?: { signal?: AbortSignal; baseUrl?: string },
): Promise<TenantResolveResult | null> {
  // Validate slug locally — saves a BE round-trip for obviously malformed input.
  if (!slug || !SLUG_RE.test(slug)) {
    return null;
  }

  const cached = getCached(slug);
  if (cached !== undefined) {
    return cached;
  }

  const baseUrl =
    options?.baseUrl ??
    // Middleware runs server-side inside the Next container; prefer the
    // internal cluster URL over the public one to avoid an unnecessary hop.
    process.env.INTERNAL_API_URL ??
    process.env.NEXT_PUBLIC_API_URL ??
    'http://localhost:9000';

  const url = `${baseUrl.replace(/\/$/, '')}/api/v1/public/tenants/by-subdomain/${encodeURIComponent(slug)}`;

  let response: Response;
  try {
    response = await fetch(url, {
      method: 'GET',
      headers: { Accept: 'application/json' },
      // Middleware should not be blocked by a slow upstream — keep timeout tight.
      signal: options?.signal,
    });
  } catch (cause) {
    throw new TenantResolveNetworkError(slug, cause);
  }

  if (response.status === 200) {
    const body = (await response.json()) as TenantResolveResult;
    setCached(slug, body);
    return body;
  }

  if (response.status === 404) {
    setCached(slug, null);
    return null;
  }

  if (response.status === 410) {
    let code = 'TENANT_GONE';
    let status: Exclude<TenantStatus, 'ACTIVE'> = 'SUSPENDED';
    try {
      const body = (await response.json()) as { error?: string; status?: TenantStatus };
      if (body.error) code = body.error;
      if (body.status && body.status !== 'ACTIVE') status = body.status;
    } catch {
      // Body parse failure — fall back to defaults.
    }
    throw new TenantSuspendedError(slug, status, code);
  }

  // 400 INVALID_SLUG_FORMAT, 429, 5xx — treat as transient + don't cache.
  throw new TenantResolveNetworkError(slug, new Error(`Unexpected status ${response.status}`));
}
