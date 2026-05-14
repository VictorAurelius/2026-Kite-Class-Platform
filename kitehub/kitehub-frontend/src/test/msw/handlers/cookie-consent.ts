/**
 * MSW handlers for Cookie Consent endpoints (Wave 79 Bucket 0 Foundation).
 *
 * Schema: `documents/01-business/cookie-consent/api-contract.md`
 *
 * Cross-layer foundation per `.claude/rules/contract-first-for-cross-layer.md`:
 * Bucket B (GAP-558) FE cookie consent banner component will consume these
 * handlers in unit tests before BE module lands.
 *
 * Endpoints covered:
 *   - POST   /api/v1/consent/cookie               — Record consent (anonymous + authenticated)
 *   - GET    /api/v1/consent/cookie/{cookieId}    — Read current consent state
 *   - PUT    /api/v1/consent/cookie/{cookieId}    — Partial update (toggle category)
 *   - DELETE /api/v1/consent/cookie/{cookieId}    — Full withdraw
 *
 * Per-test overrides via `server.use(http.X(...))` in individual specs.
 *
 * @author KiteHub Team
 * @since Wave 79 Bucket 0
 */

import { http, HttpResponse } from 'msw';
import type { HttpHandler } from 'msw';

const UUID_V4_RE =
  /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

const ALLOWED_CATEGORIES = new Set(['essential', 'functional', 'analytics']);

interface ConsentCategories {
  essential: boolean;
  functional: boolean;
  analytics: boolean;
}

interface ConsentRequest {
  cookieId?: string;
  categories?: Partial<ConsentCategories>;
  userAgent?: string;
  language?: string;
}

interface ConsentUpdateRequest {
  categories?: Partial<ConsentCategories>;
}

interface ConsentRecord {
  cookieId: string;
  userId: string | null;
  tenantId: string | null;
  categoriesAccepted: ConsentCategories;
  createdAt: string;
  updatedAt: string | null;
  expiresAt: string;
  withdrawnAt: string | null;
  status: 'ACTIVE' | 'WITHDRAWN' | 'EXPIRED';
}

const consentStore = new Map<string, ConsentRecord>();

function errorBody(code: string, message: string) {
  return HttpResponse.json({ error: code, message }, { status: errorStatus(code) });
}

function errorStatus(code: string): number {
  switch (code) {
    case 'CONSENT_ALREADY_RECORDED':
      return 409;
    case 'CONSENT_NOT_FOUND':
      return 404;
    case 'CONSENT_ALREADY_WITHDRAWN':
    case 'CONSENT_EXPIRED':
      return 410;
    case 'INVALID_CATEGORY':
    case 'INVALID_ESSENTIAL_CONSENT':
    case 'INVALID_COOKIE_ID':
      return 400;
    case 'RATE_LIMITED':
      return 429;
    default:
      return 500;
  }
}

function validateCategories(
  cats: Partial<ConsentCategories> | undefined
): { ok: true; categories: ConsentCategories } | { ok: false; code: string; message: string } {
  if (!cats || typeof cats !== 'object') {
    return { ok: false, code: 'INVALID_CATEGORY', message: 'categories required' };
  }
  for (const key of Object.keys(cats)) {
    if (!ALLOWED_CATEGORIES.has(key)) {
      return { ok: false, code: 'INVALID_CATEGORY', message: `Unknown category: ${key}` };
    }
  }
  if (cats.essential !== true) {
    return {
      ok: false,
      code: 'INVALID_ESSENTIAL_CONSENT',
      message: 'essential MUST be true',
    };
  }
  return {
    ok: true,
    categories: {
      essential: true,
      functional: cats.functional === true,
      analytics: cats.analytics === true,
    },
  };
}

function toResponse(rec: ConsentRecord) {
  return {
    cookieId: rec.cookieId,
    categoriesAccepted: rec.categoriesAccepted,
    createdAt: rec.createdAt,
    expiresAt: rec.expiresAt,
    status: rec.status,
  };
}

export const cookieConsentHandlers: HttpHandler[] = [
  /** POST /api/v1/consent/cookie — Record consent */
  http.post('/api/v1/consent/cookie', async ({ request }) => {
    const body = (await request.json().catch(() => ({}))) as ConsentRequest;
    if (!body.cookieId || !UUID_V4_RE.test(body.cookieId)) {
      return errorBody('INVALID_COOKIE_ID', 'cookieId must be UUID v4');
    }
    if (consentStore.has(body.cookieId)) {
      const existing = consentStore.get(body.cookieId)!;
      if (existing.status === 'ACTIVE') {
        return errorBody('CONSENT_ALREADY_RECORDED', 'Active consent already exists');
      }
    }
    const validation = validateCategories(body.categories);
    if (!validation.ok) {
      return errorBody(validation.code, validation.message);
    }

    const now = new Date();
    const expires = new Date(now.getTime() + 12 * 30 * 24 * 60 * 60 * 1000); // ~12 months
    const userId = (() => {
      const auth = request.headers.get('Authorization');
      if (auth?.startsWith('Bearer ')) return 'user-fixture-from-jwt';
      return null;
    })();

    const record: ConsentRecord = {
      cookieId: body.cookieId,
      userId,
      tenantId: userId ? 'tenant-fixture-from-jwt' : null,
      categoriesAccepted: validation.categories,
      createdAt: now.toISOString(),
      updatedAt: null,
      expiresAt: expires.toISOString(),
      withdrawnAt: null,
      status: 'ACTIVE',
    };
    consentStore.set(body.cookieId, record);

    return HttpResponse.json(toResponse(record), { status: 201 });
  }),

  /** GET /api/v1/consent/cookie/{cookieId} — Read consent state */
  http.get('/api/v1/consent/cookie/:cookieId', ({ params }) => {
    const cookieId = params.cookieId as string;
    if (!UUID_V4_RE.test(cookieId)) {
      return errorBody('INVALID_COOKIE_ID', 'cookieId must be UUID v4');
    }
    const rec = consentStore.get(cookieId);
    if (!rec) return errorBody('CONSENT_NOT_FOUND', 'Consent not found');

    if (new Date(rec.expiresAt) < new Date()) {
      rec.status = 'EXPIRED';
      return errorBody('CONSENT_EXPIRED', 'Consent expired');
    }
    return HttpResponse.json(toResponse(rec));
  }),

  /** PUT /api/v1/consent/cookie/{cookieId} — Partial update */
  http.put('/api/v1/consent/cookie/:cookieId', async ({ params, request }) => {
    const cookieId = params.cookieId as string;
    if (!UUID_V4_RE.test(cookieId)) {
      return errorBody('INVALID_COOKIE_ID', 'cookieId must be UUID v4');
    }
    const rec = consentStore.get(cookieId);
    if (!rec) return errorBody('CONSENT_NOT_FOUND', 'Consent not found');
    if (rec.status === 'WITHDRAWN') {
      return errorBody('CONSENT_ALREADY_WITHDRAWN', 'Consent already withdrawn');
    }
    if (new Date(rec.expiresAt) < new Date()) {
      return errorBody('CONSENT_EXPIRED', 'Consent expired');
    }

    const body = (await request.json().catch(() => ({}))) as ConsentUpdateRequest;
    const validation = validateCategories(body.categories);
    if (!validation.ok) {
      return errorBody(validation.code, validation.message);
    }

    rec.categoriesAccepted = validation.categories;
    rec.updatedAt = new Date().toISOString();
    consentStore.set(cookieId, rec);
    return HttpResponse.json(toResponse(rec));
  }),

  /** DELETE /api/v1/consent/cookie/{cookieId} — Full withdraw */
  http.delete('/api/v1/consent/cookie/:cookieId', ({ params }) => {
    const cookieId = params.cookieId as string;
    if (!UUID_V4_RE.test(cookieId)) {
      return errorBody('INVALID_COOKIE_ID', 'cookieId must be UUID v4');
    }
    const rec = consentStore.get(cookieId);
    if (!rec) return errorBody('CONSENT_NOT_FOUND', 'Consent not found');
    if (rec.status === 'WITHDRAWN') {
      return errorBody('CONSENT_ALREADY_WITHDRAWN', 'Consent already withdrawn');
    }

    rec.status = 'WITHDRAWN';
    rec.withdrawnAt = new Date().toISOString();
    consentStore.set(cookieId, rec);
    return new HttpResponse(null, { status: 204 });
  }),
];

/** Reset store between tests — call in `beforeEach`. */
export function resetCookieConsentStore(): void {
  consentStore.clear();
}

export const COOKIE_CONSENT_FIXTURE_UUID = '550e8400-e29b-41d4-a716-446655440000';
