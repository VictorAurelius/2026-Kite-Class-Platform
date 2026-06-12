/**
 * Next.js edge middleware — host → tenant resolver (GAP-811).
 *
 * Intercepts every public-page request, extracts the subdomain slug from the
 * `Host` header (or `?tenant=` preview query param), resolves to a tenant UUID
 * via the Public Tenant Resolve endpoint (GAP-813), and injects `x-tenant-id`
 * into the downstream request so server components can read it via
 * `next/headers#headers()`.
 *
 * Behaviour map (per GAP-811 §AC + outside-in findings):
 *
 * | Host shape                       | Action                                                      |
 * |----------------------------------|-------------------------------------------------------------|
 * | `sky.kiteclass.com`              | Resolve `sky` → inject `x-tenant-id`                        |
 * | `kiteclass.com` (apex)           | Pass through — marketing site, no tenant context            |
 * | `www.kiteclass.com`              | Pass through — reserved subdomain                            |
 * | `localhost` / `127.0.0.1` / IP   | Pass through (dev). `?tenant=sky` query param overrides.    |
 * | `unknown.kiteclass.com`          | Inject `x-tenant-not-found: <slug>` → friendly not-found page (GAP-1200) |
 * | `suspended.kiteclass.com`        | 307 redirect → `/suspended?slug=suspended`                  |
 * | BE down / 5xx                    | Pass through with `x-tenant-resolve-error` warning header   |
 *
 * Per `documents/04-quality/gaps/phase-1-beta/GAP-811-*.md` Proposed Fix
 * Approach A (middleware → BE resolve endpoint, NOT direct DB query — keeps FE
 * decoupled from schema).
 *
 * Reserved subdomains list mirrors `src/hooks/useTenantFromUrl.ts`
 * (www / api / admin / staging) plus apex-marketing additions (beta / preview).
 *
 * Ported per GAP-1077 từ kitehub-frontend — host→tenant middleware thuộc về
 * kiteclass-frontend (mỗi tenant = 1 trang học, resolve theo Host).
 *
 * @author KiteClass Team
 */

import { NextRequest, NextResponse } from 'next/server';

import {
  TenantResolveNetworkError,
  TenantSuspendedError,
  resolveTenant,
} from '@/lib/tenant/resolveTenant';

/**
 * Matcher excludes static + API routes — middleware should NOT intercept
 * `/api/*` (handled by gateway), `_next/*` (Next internals), or asset files.
 */
export const config = {
  matcher: ['/((?!api|_next/static|_next/image|favicon.ico|robots.txt|sitemap.xml).*)'],
};

/**
 * Reserved subdomains that map to platform-level concerns, not tenants.
 * Mirrors `src/hooks/useTenantFromUrl.ts` reserved list (www/api/admin/staging)
 * plus apex-marketing additions (beta/preview).
 */
const RESERVED_SUBDOMAINS = new Set([
  'www',
  'api',
  'admin',
  'staging',
  'beta',
  'preview',
]);

/**
 * Extract a subdomain slug from a `Host` header value.
 *
 * Examples:
 * - `sky.kiteclass.com`       → `'sky'`
 * - `sky.kiteclass.com:3000`  → `'sky'`
 * - `www.kiteclass.com`       → `null` (reserved)
 * - `kiteclass.com`           → `null` (apex, only 2 parts)
 * - `localhost:4700`          → `null`
 * - `127.0.0.1`               → `null`
 *
 * Apex domains (2 parts) and IP addresses both yield `null` — there is no
 * subdomain context to resolve.
 */
export function extractSlugFromHost(host: string | null | undefined): string | null {
  if (!host) return null;
  const hostname = host.split(':')[0]?.toLowerCase() ?? '';
  if (!hostname) return null;
  if (hostname === 'localhost' || /^\d+\.\d+\.\d+\.\d+$/.test(hostname)) return null;

  const parts = hostname.split('.');
  if (parts.length < 3) return null; // apex or 2-part host — no subdomain

  const sub = parts[0] ?? '';
  if (!sub || RESERVED_SUBDOMAINS.has(sub)) return null;
  return sub;
}

/**
 * Pull a tenant slug from one of the supported request signals.
 *
 * Priority order:
 * 1. `?tenant=<slug>` query param — dev / preview override (chủ trung tâm xem
 *    landing trước go-live; per outside-in finding persona P1)
 * 2. `Host` header subdomain
 */
function extractSlug(req: NextRequest): string | null {
  const preview = req.nextUrl.searchParams.get('tenant');
  if (preview) return preview.toLowerCase();
  return extractSlugFromHost(req.headers.get('host'));
}

export async function middleware(req: NextRequest): Promise<NextResponse> {
  // G2 walk 2026-06-12 — /preview là draft surface của wizard (GAP-1215): slug có thể
  // CHƯA tồn tại (persona tạo brand mới nhập slug mới). Bỏ qua tenant-gating
  // (not-found header / suspended 307) — page tự fetch base theo ?tenant=, miss →
  // default base + draft params override. KHÔNG ảnh hưởng landing thật (path khác).
  if (req.nextUrl.pathname === '/preview') {
    return NextResponse.next();
  }

  const slug = extractSlug(req);
  if (!slug) {
    // No subdomain (apex marketing, localhost without `?tenant=`, IP probe).
    return NextResponse.next();
  }

  try {
    const tenant = await resolveTenant(slug);

    if (!tenant) {
      // BE confirmed 404 — the host carried a real subdomain slug but no tenant
      // exists for it (mistyped / decommissioned). Mark the request so server
      // components render a friendly "trung tâm không tồn tại" page instead of
      // silently falling back to the env/default tenant landing — which would
      // show a DIFFERENT center's brand + content (confusing, mild content-leak).
      // GAP-1200. (localhost/IP without a subdomain never reach here — `slug` is
      // null and we returned early above, preserving the dev fallback.)
      const requestHeaders = new Headers(req.headers);
      requestHeaders.set('x-tenant-not-found', slug);
      return NextResponse.next({
        request: { headers: requestHeaders },
      });
    }

    // 200 OK + ACTIVE — inject tenant id for server components.
    const requestHeaders = new Headers(req.headers);
    requestHeaders.set('x-tenant-id', tenant.id);
    requestHeaders.set('x-tenant-subdomain', tenant.subdomain);

    return NextResponse.next({
      request: { headers: requestHeaders },
    });
  } catch (err) {
    if (err instanceof TenantSuspendedError) {
      // Already on the status page — pass through, otherwise the redirect
      // below re-fires on every /suspended request (infinite 307 loop,
      // browser shows ERR_TOO_MANY_REDIRECTS). GAP-1199.
      if (req.nextUrl.pathname === '/suspended') {
        return NextResponse.next();
      }

      // Redirect to friendly status page rather than render marketing landing.
      const url = req.nextUrl.clone();
      url.pathname = '/suspended';
      url.searchParams.set('slug', err.slug);
      url.searchParams.set('status', err.status.toLowerCase());
      return NextResponse.redirect(url, 307);
    }

    if (err instanceof TenantResolveNetworkError) {
      // Graceful degradation per GAP-811 §AC — BE down should NOT crash the
      // whole site. Pass through + flag header so downstream can show fallback
      // branding / "experiencing issues" banner.
      const requestHeaders = new Headers(req.headers);
      requestHeaders.set('x-tenant-resolve-error', 'upstream-unavailable');
      // Avoid noisy logs in tests, but in real runtime this is the only signal
      // we get about an upstream incident.
      if (process.env.NODE_ENV !== 'test') {
        console.warn(
          `[middleware] resolveTenant('${slug}') failed; passing through`,
          err.cause,
        );
      }
      return NextResponse.next({
        request: { headers: requestHeaders },
      });
    }

    // Unknown error class — same graceful pass-through.
    if (process.env.NODE_ENV !== 'test') {
      console.error('[middleware] resolveTenant threw unexpected error', err);
    }
    return NextResponse.next();
  }
}
