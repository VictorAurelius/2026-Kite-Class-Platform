/**
 * MSW handlers for the v2 AI Branding lifecycle (kiteclass-core
 * `InstanceController`, `BrandingPackageController`, `PublicBrandingController`,
 * `InternalWebhookController`).
 *
 * <p>Endpoints mirror the wave plan §7.1 inventory verified against actual
 * Java controllers; lifecycle transitions enforce
 * {@link FrontendInstanceStatus} state-machine rules and stamp matching
 * timestamps + brandingVersion increments on the in-memory store.
 *
 * <p>Tracking: GAP-235 Sub-PR E2.
 */

import { HttpResponse, delay, http } from 'msw';

import {
  aiBrandingState,
  applyTransitionEffects,
  canTransition,
  type MockFrontendInstance,
  type MockBrandingPackage,
} from './ai-branding-state';

const BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

/** ms — fast-forward in tests via NEXT_PUBLIC_MOCK_DELAY_MS=0 */
const TRANSITION_DELAY_MS = Number(process.env.NEXT_PUBLIC_MOCK_DELAY_MS ?? '1500');

const PLACEHOLDER_ASSETS: Record<string, string> = {
  logo: '/mocks/assets/logo-thanglong.png',
  favicon: '/mocks/assets/favicon-thanglong.ico',
  banner: '/mocks/assets/banner-thanglong.svg',
  hero: '/mocks/assets/hero-thanglong.png',
  courseThumbnail: '/mocks/assets/course-thumbnail-thanglong.png',
  socialCover: '/mocks/assets/social-cover-thanglong.png',
};

const DEMO_THEME = {
  primaryColor: '#2563eb',
  secondaryColor: '#f97316',
  accentColor: '#10b981',
  backgroundColor: '#ffffff',
  textColor: '#0f172a',
  fontFamily: 'Inter, "Be Vietnam Pro", sans-serif',
};

function packageFor(instance: MockFrontendInstance): MockBrandingPackage {
  return {
    instanceId: instance.id,
    brandingVersion: instance.brandingVersion,
    theme: DEMO_THEME,
    assets: PLACEHOLDER_ASSETS,
    etag: `"v${instance.brandingVersion}-${instance.updatedAt}"`,
  };
}

function requireInstance(idParam: string | readonly string[] | undefined): MockFrontendInstance | { error: HttpResponse<import('msw').DefaultBodyType> } {
  const id = Number(idParam);
  if (!Number.isFinite(id)) {
    return { error: HttpResponse.json({ success: false, code: 'INVALID_ID', message: `Invalid id: ${idParam}` }, { status: 400 }) };
  }
  const instance = aiBrandingState.get(id);
  if (!instance) {
    return { error: HttpResponse.json({ success: false, code: 'INSTANCE_NOT_FOUND', message: `No instance with id ${id}` }, { status: 404 }) };
  }
  return instance;
}

function transitionOr422(
  instance: MockFrontendInstance,
  target: Parameters<typeof applyTransitionEffects>[1],
): HttpResponse<import('msw').DefaultBodyType> | null {
  if (!canTransition(instance.status, target)) {
    return HttpResponse.json(
      {
        success: false,
        code: 'INVALID_TRANSITION',
        message: `Cannot transition ${instance.status} -> ${target}`,
      },
      { status: 422 },
    );
  }
  applyTransitionEffects(instance, target);
  return null;
}

export const aiBrandingHandlers = [
  // 1. POST /api/v1/instances — create instance (status=NOT_STARTED)
  http.post(`${BASE_URL}/api/v1/instances`, async ({ request }) => {
    const body = (await request.json().catch(() => ({}))) as Partial<MockFrontendInstance>;
    const instance = aiBrandingState.create(body);
    return HttpResponse.json({ success: true, data: instance }, { status: 201 });
  }),

  // 2. GET /api/v1/instances/:id — get single instance
  http.get(`${BASE_URL}/api/v1/instances/:id`, ({ params }) => {
    const r = requireInstance(params.id);
    if ('error' in r) return r.error;
    return HttpResponse.json({ success: true, data: r });
  }),

  // 3. GET /api/v1/instances — list all
  http.get(`${BASE_URL}/api/v1/instances`, () => {
    return HttpResponse.json({ success: true, data: aiBrandingState.list() });
  }),

  // 4. POST /api/v1/instances/:id/infrastructure-ready — NOT_STARTED → INITIALIZING → GENERATING (delayed)
  http.post(`${BASE_URL}/api/v1/instances/:id/infrastructure-ready`, async ({ params }) => {
    const r = requireInstance(params.id);
    if ('error' in r) return r.error;
    const err = transitionOr422(r, 'INITIALIZING');
    if (err) return err;

    void (async () => {
      await delay(TRANSITION_DELAY_MS);
      aiBrandingState.update(r.id, (i) => {
        if (i.status === 'INITIALIZING') applyTransitionEffects(i, 'GENERATING');
      });
    })();

    return HttpResponse.json({ success: true, data: r }, { status: 202 });
  }),

  // 5. POST /api/v1/instances/:id/branding-completed — GENERATING/REGENERATING → DEPLOYED (gate >= 70)
  http.post(`${BASE_URL}/api/v1/instances/:id/branding-completed`, async ({ params, request }) => {
    const r = requireInstance(params.id);
    if ('error' in r) return r.error;

    const body = (await request.json().catch(() => ({}))) as { qualityScore?: number };
    const score = body.qualityScore ?? 85;
    const passed = score >= 70;

    const target = passed ? 'DEPLOYED' : 'FAILED';
    const err = transitionOr422(r, target);
    if (err) return err;

    aiBrandingState.recordQualityReport({
      id: r.id,
      targetInstanceId: r.id,
      brandingVersion: r.brandingVersion,
      score,
      passed,
      contrastScore: Math.max(60, score - 5),
      cssVarsScore: Math.min(100, score + 5),
      assetUrlsScore: score,
      visualRegressionScore: Math.max(50, score - 10),
      logoPlacementScore: Math.min(100, score + 3),
      reviewedAt: new Date().toISOString(),
    });

    if (!passed) {
      r.failureReason = `Quality gate failed: score=${score} (<70)`;
    }
    return HttpResponse.json({ success: true, data: r });
  }),

  // 6. POST /api/v1/instances/:id/rebrand — DEPLOYED → REGENERATING
  http.post(`${BASE_URL}/api/v1/instances/:id/rebrand`, async ({ params }) => {
    const r = requireInstance(params.id);
    if ('error' in r) return r.error;
    const err = transitionOr422(r, 'REGENERATING');
    if (err) return err;
    return HttpResponse.json({ success: true, data: r }, { status: 202 });
  }),

  // 7. POST /api/v1/instances/:id/failed — Any → FAILED
  http.post(`${BASE_URL}/api/v1/instances/:id/failed`, async ({ params, request }) => {
    const r = requireInstance(params.id);
    if ('error' in r) return r.error;
    const body = (await request.json().catch(() => ({}))) as { reason?: string };
    const err = transitionOr422(r, 'FAILED');
    if (err) return err;
    r.failureReason = body.reason ?? 'Unknown failure (mock)';
    return HttpResponse.json({ success: true, data: r });
  }),

  // 8. POST /api/v1/instances/:id/retry — FAILED → INITIALIZING
  http.post(`${BASE_URL}/api/v1/instances/:id/retry`, async ({ params }) => {
    const r = requireInstance(params.id);
    if ('error' in r) return r.error;
    const err = transitionOr422(r, 'INITIALIZING');
    if (err) return err;

    void (async () => {
      await delay(TRANSITION_DELAY_MS);
      aiBrandingState.update(r.id, (i) => {
        if (i.status === 'INITIALIZING') applyTransitionEffects(i, 'GENERATING');
      });
    })();

    return HttpResponse.json({ success: true, data: r }, { status: 202 });
  }),

  // 9. GET /api/v1/branding/:instanceId/package — composite theme + assets (cached, ETag)
  http.get(`${BASE_URL}/api/v1/branding/:instanceId/package`, ({ params, request }) => {
    const r = requireInstance(params.instanceId);
    if ('error' in r) return r.error;

    const pkg = packageFor(r);
    const ifNoneMatch = request.headers.get('If-None-Match');
    if (ifNoneMatch && ifNoneMatch === pkg.etag) {
      return new HttpResponse(null, { status: 304, headers: { ETag: pkg.etag } });
    }
    return HttpResponse.json(
      { success: true, data: pkg },
      { headers: { ETag: pkg.etag } },
    );
  }),

  // 10. GET /api/v1/branding/public — public-facing fetch (no auth)
  http.get(`${BASE_URL}/api/v1/branding/public`, () => {
    const first = aiBrandingState.list().find((i) => i.status === 'DEPLOYED');
    if (!first) {
      return HttpResponse.json(
        { success: true, data: { theme: DEMO_THEME, assets: PLACEHOLDER_ASSETS } },
      );
    }
    return HttpResponse.json({ success: true, data: packageFor(first) });
  }),

  // 11. POST /internal/notify/instance-deployed — internal cross-service webhook
  http.post(`${BASE_URL}/internal/notify/instance-deployed`, async ({ request }) => {
    const body = (await request.json().catch(() => ({}))) as { instanceId?: number };
    if (!body.instanceId) {
      return HttpResponse.json({ success: false, code: 'MISSING_INSTANCE_ID' }, { status: 400 });
    }
    return HttpResponse.json({ success: true, data: { acknowledged: true, instanceId: body.instanceId } });
  }),
];
