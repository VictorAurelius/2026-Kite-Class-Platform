/**
 * MSW handlers for AI Branding wizard endpoints (Wave 34 Bucket D).
 *
 * Schema: `documents/01-business/kitehub/ai-branding/api-contract.md`
 * Wave 34 endpoints. Each handler ships happy-path defaults that match
 * what Bucket A/B/C controllers return today; per-test overrides via
 * `server.use(http.X(...))` in individual specs.
 *
 * Lifecycle gotcha (per Bucket 0 setup.ts):
 *   `setupFiles` activates MSW server only when `handlers.length > 0`.
 *   Wave 34 Bucket D populates handlers for the FIRST time → MSW
 *   intercepts route-matched requests. Existing tests that mock
 *   `global.fetch = vi.fn()` (e.g. `use-theme-generation.test.ts`)
 *   continue to work because the listener uses
 *   `onUnhandledRequest: 'bypass'` — MSW only matches the routes we
 *   declare here; everything else falls through.
 *
 * Endpoints covered:
 *   - GET    /api/v1/branding/slug-availability
 *   - GET    /api/v1/branding/regenerate-quota
 *   - POST   /api/v1/branding/jobs/:jobId/regenerate
 *   - GET    /api/v1/branding/jobs/:jobId/quality-score
 *   - GET    /api/v1/branding/jobs/:jobId/preview        (HTML)
 *   - GET    /api/v1/branding/instances/:instanceId/lifecycle/events
 *   - GET    /api/v1/branding/jobs/:jobId                (extended w/ brandColors)
 *
 * Deploy-stream SSE (`/jobs/:jobId/deploy-stream`) is intentionally NOT
 * mocked here — `useDeployStream` opens an `EventSource`, which jsdom
 * does not provide. Tests for that hook stub `window.EventSource`
 * directly; see `useDeployStream.test.ts`.
 *
 * @author KiteHub Team
 * @since Wave 34 Bucket D
 */

import { http, HttpResponse } from 'msw';
import type { HttpHandler } from 'msw';

const SLUG_TAKEN = new Set(['toan-master', 'edu-center', 'kite-demo']);

const RFC4122_V4 =
  /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

export const brandingHandlers: HttpHandler[] = [
  // ---------------------------------------------------------------
  // POST /api/v1/branding/jobs  (GAP-1021 — create wizard job)
  // ---------------------------------------------------------------
  http.post('*/api/v1/branding/jobs', () => {
    return HttpResponse.json(
      {
        jobId: '11111111-1111-4111-8111-111111111111',
        instanceId: '22222222-2222-4222-8222-222222222222',
        status: 'INITIALIZING',
        regenerateCount: 0,
        brandingVersion: 1,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        brandColors: {
          primary: '#2563eb',
          secondary: '#f59e0b',
          accent: '#22c55e',
          neutral: '#0f172a',
          background: '#ffffff',
          source: 'TEMPLATE',
        },
        previewUrl: '/api/v1/branding/jobs/11111111-1111-4111-8111-111111111111/preview',
      },
      { status: 201 }
    );
  }),

  // ---------------------------------------------------------------
  // POST /api/v1/branding/jobs/:jobId/approve  (GAP-1021 — deploy trigger)
  // ---------------------------------------------------------------
  http.post('*/api/v1/branding/jobs/:jobId/approve', async ({ request, params }) => {
    const { jobId } = params as { jobId: string };
    const body = (await request.json().catch(() => ({}))) as { slug?: string };
    const slug = body.slug || 'tenant';
    return HttpResponse.json(
      {
        jobId,
        status: 'INITIALIZING',
        frontendUrl: `https://${slug}.kitehub.me`,
        message: 'Đang triển khai (mock provisioning)',
      },
      { status: 202 }
    );
  }),

  // ---------------------------------------------------------------
  // GET /api/v1/branding/slug-availability
  // ---------------------------------------------------------------
  http.get('*/api/v1/branding/slug-availability', ({ request }) => {
    const slug = new URL(request.url).searchParams.get('slug') ?? '';
    if (!slug || !/^[a-z0-9](?:[a-z0-9-]*[a-z0-9])?$/.test(slug) || slug.length < 3) {
      return HttpResponse.json(
        {
          error: 'INVALID_SLUG_FORMAT',
          message: 'Slug must be lowercase alphanumeric with hyphens, 3–63 chars.',
          slug,
        },
        { status: 400 }
      );
    }
    if (SLUG_TAKEN.has(slug)) {
      return HttpResponse.json({
        available: false,
        suggestions: [`${slug}-2026`, `${slug}-edu`, `tt-${slug}`, `${slug}-vn`],
      });
    }
    return HttpResponse.json({ available: true, suggestions: [] });
  }),

  // ---------------------------------------------------------------
  // GET /api/v1/branding/regenerate-quota
  // ---------------------------------------------------------------
  http.get('*/api/v1/branding/regenerate-quota', ({ request }) => {
    const tier = request.headers.get('X-Subscription-Tier') ?? 'FREE';
    if (tier === 'ENTERPRISE') {
      return HttpResponse.json({
        tier: 'ENTERPRISE',
        used: 0,
        limit: -1,
        resetAt: null,
      });
    }
    const limits: Record<string, number> = {
      FREE: 3,
      BASIC: 10,
      PRO: 10,
      PREMIUM: 30,
    };
    const limit = limits[tier] ?? 3;
    return HttpResponse.json({
      tier,
      used: 0,
      limit,
      resetAt: '2026-05-08T00:00:00Z',
    });
  }),

  // ---------------------------------------------------------------
  // POST /api/v1/branding/jobs/:jobId/regenerate
  // ---------------------------------------------------------------
  http.post('*/api/v1/branding/jobs/:jobId/regenerate', ({ request, params }) => {
    const { jobId } = params as { jobId: string };
    const idempotencyKey = request.headers.get('Idempotency-Key');
    if (!idempotencyKey || !RFC4122_V4.test(idempotencyKey)) {
      return HttpResponse.json(
        {
          error: 'MISSING_IDEMPOTENCY_KEY',
          message: 'Idempotency-Key header (UUID v4) required.',
        },
        { status: 400 }
      );
    }
    if (jobId === 'job-quota-exceeded') {
      return HttpResponse.json(
        {
          error: 'AI_REGENERATE_QUOTA_EXCEEDED',
          tier: 'FREE',
          used: 3,
          limit: 3,
          resetAt: '2026-05-08T00:00:00Z',
        },
        { status: 403 }
      );
    }
    return HttpResponse.json({
      jobId,
      instanceId: 12345,
      status: 'REGENERATING',
      regenerateCount: 2,
      brandingVersion: 2,
      createdAt: '2026-05-07T09:00:00Z',
      updatedAt: new Date().toISOString(),
      templateId: 'tpl-edu-modern-001',
      audience: 'K-12',
      tone: 'FRIENDLY',
      previewUrl: `/api/v1/branding/jobs/${jobId}/preview`,
    });
  }),

  // ---------------------------------------------------------------
  // GET /api/v1/branding/jobs/:jobId/quality-score
  // ---------------------------------------------------------------
  http.get('*/api/v1/branding/jobs/:jobId/quality-score', ({ params }) => {
    const { jobId } = params as { jobId: string };
    if (jobId === 'job-not-found') {
      return HttpResponse.json(
        { error: 'JOB_NOT_FOUND', jobId },
        { status: 404 }
      );
    }
    return HttpResponse.json({
      jobId,
      score: 82,
      passed: true,
      threshold: 70,
      subscores: {
        contrast: 90,
        brokenLinks: 100,
        cssVarsApplied: 85,
        visualRegression: 75,
        logoPlacement: 80,
      },
      issues: [],
      computedAt: '2026-05-07T10:23:00Z',
    });
  }),

  // ---------------------------------------------------------------
  // GET /api/v1/branding/jobs/:jobId/preview  (HTML)
  // ---------------------------------------------------------------
  http.get('*/api/v1/branding/jobs/:jobId/preview', ({ params }) => {
    const { jobId } = params as { jobId: string };
    if (jobId === 'job-not-found') {
      return new HttpResponse('Not found', { status: 404 });
    }
    const html = `<!doctype html><html lang="vi"><head><meta charset="utf-8"/><title>Preview</title></head><body><div data-testid="preview-hero">Hero</div><div data-testid="preview-card">CTA</div></body></html>`;
    return new HttpResponse(html, {
      status: 200,
      headers: {
        'Content-Type': 'text/html; charset=utf-8',
        'X-Frame-Options': 'SAMEORIGIN',
        'Cache-Control': 'private, max-age=60',
      },
    });
  }),

  // ---------------------------------------------------------------
  // GET /api/v1/branding/instances/:instanceId/lifecycle/events
  // ---------------------------------------------------------------
  http.get(
    '*/api/v1/branding/instances/:instanceId/lifecycle/events',
    ({ params }) => {
      const { instanceId } = params as { instanceId: string };
      if (instanceId === 'inst-not-found') {
        return HttpResponse.json(
          { error: 'INSTANCE_NOT_FOUND', instanceId },
          { status: 404 }
        );
      }
      return HttpResponse.json({
        instanceId,
        events: [
          {
            id: 'evt-2026-0507-001',
            ts: '2026-05-07T10:00:00Z',
            type: 'state-change',
            fromState: 'INITIALIZING',
            toState: 'GENERATING',
            actor: { kind: 'system', id: 'saga:provisioning' },
            metadata: { brandingVersion: 1 },
          },
          {
            id: 'evt-2026-0507-002',
            ts: '2026-05-07T10:05:00Z',
            type: 'state-change',
            fromState: 'GENERATING',
            toState: 'DEPLOYED',
            actor: { kind: 'system', id: 'saga:provisioning' },
            metadata: { brandingVersion: 1 },
          },
        ],
        nextCursor: null,
      });
    }
  ),

  // ---------------------------------------------------------------
  // GET /api/v1/branding/jobs/:jobId  (extended w/ brandColors — Bucket B wrapper)
  // ---------------------------------------------------------------
  http.get('*/api/v1/branding/jobs/:jobId', ({ params }) => {
    const { jobId } = params as { jobId: string };
    if (jobId === 'job-not-found') {
      return HttpResponse.json(
        { error: 'JOB_NOT_FOUND', jobId },
        { status: 404 }
      );
    }
    return HttpResponse.json({
      jobId,
      instanceId: 12345,
      tenantId: 'kitehub-tenant-uuid',
      status: 'DEPLOYED',
      regenerateCount: 1,
      brandingVersion: 2,
      createdAt: '2026-05-07T09:00:00Z',
      updatedAt: '2026-05-07T10:05:00Z',
      brandColors: {
        primary: '#1a73e8',
        secondary: '#fbbc04',
        accent: '#10B981',
        neutral: '#1f2937',
        background: '#ffffff',
        source: 'TEMPLATE',
      },
      templateId: 'tpl-edu-modern-001',
      audience: 'K-12',
      tone: 'FRIENDLY',
      previewUrl: `/api/v1/branding/jobs/${jobId}/preview`,
    });
  }),
];
