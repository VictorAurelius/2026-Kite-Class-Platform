/**
 * MSW handlers for Public Tenant Resolve endpoint (Wave tenant-domain-1 Bucket 0 Foundation).
 *
 * Schema: `documents/01-business/kitehub/marketing/api-contract.md` §9
 *
 * Cross-layer foundation per `.claude/rules/contract-first-for-cross-layer.md`:
 * Wave tenant-domain-1 Bucket C (GAP-811) FE middleware `resolveTenant.ts` will consume
 * these handlers in unit tests trước BE Bucket B (GAP-813 `PublicTenantController`) lands.
 *
 * Endpoints covered:
 *   - GET /api/v1/public/tenants/by-subdomain/{slug}  (anonymous tenant lookup)
 *
 * Scenarios stubbed (per api-contract.md §9.1.3 / §9.1.4):
 *   - `sky` / `pioneer` → 200 OK với tenant ACTIVE
 *   - `suspended` → 410 GONE với status=SUSPENDED
 *   - any other slug → 404 NOT_FOUND
 *
 * Per-test overrides via `server.use(http.get(...))` trong individual specs.
 *
 * @author KiteHub Team
 * @since Wave tenant-domain-1 Bucket 0
 */

import { http, HttpResponse } from 'msw';
import type { HttpHandler } from 'msw';

// ---------------------------------------------------------------
// Test fixtures (deterministic UUIDs cho snapshot stability)
// ---------------------------------------------------------------

interface TenantFixture {
  id: string;
  subdomain: string;
  name: string;
  status: 'ACTIVE' | 'SUSPENDED' | 'ARCHIVED' | 'DELETED';
}

const tenants: Record<string, TenantFixture> = {
  sky: {
    id: '11111111-1111-1111-1111-111111111111',
    subdomain: 'sky',
    name: 'Trung tâm Anh ngữ Sky Education',
    status: 'ACTIVE',
  },
  pioneer: {
    id: '22222222-2222-2222-2222-222222222222',
    subdomain: 'pioneer',
    name: 'Trung tâm Pioneer',
    status: 'ACTIVE',
  },
  suspended: {
    id: '33333333-3333-3333-3333-333333333333',
    subdomain: 'suspended',
    name: 'Suspended Center',
    status: 'SUSPENDED',
  },
};

// Slug format regex per api-contract.md §9.1.1 (lowercase-kebab, length 1-50)
const SLUG_RE = /^[a-z0-9]([a-z0-9-]{0,48}[a-z0-9])?$/;

export const tenantHandlers: HttpHandler[] = [
  // ---------------------------------------------------------------
  // GET /api/v1/public/tenants/by-subdomain/{slug}
  // ---------------------------------------------------------------
  http.get('*/api/v1/public/tenants/by-subdomain/:slug', ({ params }) => {
    const slug = String(params.slug ?? '');

    if (!slug || !SLUG_RE.test(slug)) {
      return HttpResponse.json(
        {
          error: 'INVALID_SLUG_FORMAT',
          message: 'Slug must be lowercase-kebab-case, length 1-50, no leading/trailing hyphen.',
        },
        { status: 400 }
      );
    }

    const tenant = tenants[slug];

    if (!tenant) {
      return HttpResponse.json(
        {
          error: 'TENANT_NOT_FOUND',
          message: `No tenant found for subdomain '${slug}'.`,
        },
        { status: 404 }
      );
    }

    if (tenant.status !== 'ACTIVE') {
      return HttpResponse.json(
        {
          error: `TENANT_${tenant.status}`,
          message: `Tenant '${slug}' is currently ${tenant.status.toLowerCase()}.`,
          status: tenant.status,
        },
        { status: 410 }
      );
    }

    return HttpResponse.json({
      id: tenant.id,
      subdomain: tenant.subdomain,
      name: tenant.name,
      status: tenant.status,
    });
  }),
];
