/**
 * MSW handlers for Beta Access endpoints (Wave 35 Bucket 0 Foundation).
 *
 * Schema: `documents/01-business/kitehub/beta-access/api-contract.md`
 *
 * Cross-layer foundation per `.claude/rules/contract-first-for-cross-layer.md`:
 * Bucket B (GAP-385) FE form will consume these handlers in unit tests
 * before BE Bucket B Java migration lands. Bucket A (GAP-384) admin guard
 * tests can use the admin-list handler stub.
 *
 * Endpoints covered:
 *   - POST /api/v1/auth/request-beta-access (PDPL consent flow — primary)
 *   - GET  /api/v1/auth/beta-signup/validate (token pre-fill stub)
 *   - GET  /api/v1/admin/beta-requests (admin list stub)
 *   - POST /api/v1/admin/beta-requests/:id/approve (admin action stub)
 *   - POST /api/v1/admin/beta-requests/:id/reject (admin action stub)
 *
 * Per-test overrides via `server.use(http.X(...))` in individual specs.
 *
 * @author KiteHub Team
 * @since Wave 35 Bucket 0
 */

import { http, HttpResponse } from 'msw';
import type { HttpHandler } from 'msw';

// Email seeded as duplicate-active for 409 path tests.
const DUPLICATE_EMAILS = new Set(['duplicate@example.edu.vn']);

const ALLOWED_PERSONAS = new Set(['P1_SOLO_TEACHER', 'P2_CENTER_OWNER']);

// RFC-5321 simplified email check (mirrors @Email loosely; BE is authoritative).
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

interface BetaRequestPayload {
  email?: string;
  name?: string;
  orgName?: string;
  persona?: string;
  referralSource?: string;
  honeypot?: string;
  consentGiven?: boolean;
}

export const betaAccessHandlers: HttpHandler[] = [
  // ---------------------------------------------------------------
  // POST /api/v1/auth/request-beta-access (PDPL consent flow)
  // ---------------------------------------------------------------
  http.post('*/api/v1/auth/request-beta-access', async ({ request }) => {
    let body: BetaRequestPayload;
    try {
      body = (await request.json()) as BetaRequestPayload;
    } catch {
      return HttpResponse.json(
        { error: 'BETA_INVALID_PAYLOAD', message: 'Malformed JSON' },
        { status: 400 }
      );
    }

    // Order of precedence mirrors Bucket B BE validation:
    // 1. consent (PDPL — BR-BETA-001) — most specific user-blocking error
    if (body.consentGiven !== true) {
      return HttpResponse.json(
        {
          error: 'BETA_CONSENT_REQUIRED',
          message: 'PDPL 2023 Art 11 — explicit consent is required to submit a beta request.',
          field: 'consentGiven',
        },
        { status: 400 }
      );
    }

    // 2. honeypot (bot trap — silent on the real frontend, surfaced for tests)
    if (body.honeypot && body.honeypot.length > 0) {
      return HttpResponse.json(
        { error: 'BETA_HONEYPOT_FILLED', message: 'Honeypot must be empty.' },
        { status: 400 }
      );
    }

    // 3. email format
    if (!body.email || !EMAIL_RE.test(body.email)) {
      return HttpResponse.json(
        { error: 'BETA_INVALID_EMAIL', message: 'email must be a valid email address.', field: 'email' },
        { status: 400 }
      );
    }

    // 4. persona enum
    if (!body.persona || !ALLOWED_PERSONAS.has(body.persona)) {
      return HttpResponse.json(
        {
          error: 'BETA_INVALID_PERSONA',
          message: 'persona must be P1_SOLO_TEACHER or P2_CENTER_OWNER.',
          field: 'persona',
        },
        { status: 400 }
      );
    }

    // 5. duplicate
    if (DUPLICATE_EMAILS.has(body.email)) {
      return HttpResponse.json(
        {
          error: 'BETA_DUPLICATE_EMAIL',
          message: 'An active beta request already exists for this email.',
        },
        { status: 409 }
      );
    }

    // Happy path
    return HttpResponse.json(
      {
        id: 12345,
        email: body.email,
        name: body.name ?? '',
        orgName: body.orgName ?? '',
        persona: body.persona,
        referralSource: body.referralSource ?? null,
        status: 'PENDING',
        createdAt: '2026-05-08T10:23:00Z',
        approvedAt: null,
        rejectedAt: null,
        rejectionReason: null,
      },
      { status: 201 }
    );
  }),

  // ---------------------------------------------------------------
  // GET /api/v1/auth/beta-signup/validate (token pre-fill stub)
  // ---------------------------------------------------------------
  http.get('*/api/v1/auth/beta-signup/validate', ({ request }) => {
    const token = new URL(request.url).searchParams.get('token') ?? '';
    if (!token || token === 'invalid') {
      return HttpResponse.json({ valid: false }, { status: 404 });
    }
    return HttpResponse.json({
      valid: true,
      email: 'owner@example.edu.vn',
      name: 'Nguyễn Văn A',
      persona: 'P2_CENTER_OWNER',
      expiresAt: '2026-05-09T10:23:00Z',
    });
  }),

  // ---------------------------------------------------------------
  // POST /api/v1/auth/beta-signup/exchange-claim-code (GAP-609 Wave 91)
  // ---------------------------------------------------------------
  // Stub behavior keyed by claim code value for deterministic test cases:
  //   "123456" → valid → returns inviteToken UUID
  //   "000000" → CODE_EXPIRED
  //   "111111" → ALREADY_USED
  //   anything else → CODE_NOT_FOUND
  http.post('*/api/v1/auth/beta-signup/exchange-claim-code', async ({ request }) => {
    let body: { claimCode?: string };
    try {
      body = (await request.json()) as { claimCode?: string };
    } catch {
      return HttpResponse.json(
        { valid: false, errorCode: 'CODE_NOT_FOUND' },
        { status: 400 },
      );
    }
    const code = body.claimCode ?? '';
    if (code === '123456') {
      return HttpResponse.json({
        valid: true,
        inviteToken: '00000000-0000-0000-0000-000000000001',
        email: 'owner@example.edu.vn',
        name: 'Trần Thị Hồng',
        orgName: 'Trung tâm Anh ngữ Sky Education',
        persona: 'P2_CENTER_OWNER',
      });
    }
    if (code === '000000') {
      return HttpResponse.json(
        { valid: false, errorCode: 'CODE_EXPIRED' },
        { status: 404 },
      );
    }
    if (code === '111111') {
      return HttpResponse.json(
        { valid: false, errorCode: 'ALREADY_USED' },
        { status: 404 },
      );
    }
    return HttpResponse.json(
      { valid: false, errorCode: 'CODE_NOT_FOUND' },
      { status: 404 },
    );
  }),

  // ---------------------------------------------------------------
  // GET /api/v1/admin/beta-requests (admin list stub — Bucket A guard target)
  // ---------------------------------------------------------------
  http.get('*/api/v1/admin/beta-requests', ({ request }) => {
    const auth = request.headers.get('Authorization');
    if (!auth) {
      return HttpResponse.json({ error: 'UNAUTHORIZED' }, { status: 401 });
    }
    // Stub: any token containing "admin" passes; otherwise 403. Real check is in BE.
    if (!auth.includes('admin')) {
      return HttpResponse.json({ error: 'FORBIDDEN' }, { status: 403 });
    }
    return HttpResponse.json({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    });
  }),

  // ---------------------------------------------------------------
  // POST /api/v1/admin/beta-requests/:id/approve (admin action stub)
  // ---------------------------------------------------------------
  http.post('*/api/v1/admin/beta-requests/:id/approve', ({ request, params }) => {
    const auth = request.headers.get('Authorization');
    if (!auth) return HttpResponse.json({ error: 'UNAUTHORIZED' }, { status: 401 });
    if (!auth.includes('admin')) return HttpResponse.json({ error: 'FORBIDDEN' }, { status: 403 });
    const { id } = params as { id: string };
    return HttpResponse.json({
      id: Number(id) || 12345,
      email: 'owner@example.edu.vn',
      name: 'Nguyễn Văn A',
      orgName: 'Trung tâm Anh ngữ ABC',
      persona: 'P2_CENTER_OWNER',
      referralSource: null,
      status: 'APPROVED',
      createdAt: '2026-05-08T10:23:00Z',
      approvedAt: '2026-05-08T11:00:00Z',
      rejectedAt: null,
      rejectionReason: null,
    });
  }),

  // ---------------------------------------------------------------
  // POST /api/v1/admin/beta-requests/:id/reject (admin action stub)
  // ---------------------------------------------------------------
  http.post('*/api/v1/admin/beta-requests/:id/reject', async ({ request, params }) => {
    const auth = request.headers.get('Authorization');
    if (!auth) return HttpResponse.json({ error: 'UNAUTHORIZED' }, { status: 401 });
    if (!auth.includes('admin')) return HttpResponse.json({ error: 'FORBIDDEN' }, { status: 403 });
    const { id } = params as { id: string };
    let reason = '';
    try {
      const body = (await request.json()) as { rejectionReason?: string };
      reason = body.rejectionReason ?? '';
    } catch {
      // ignore — empty reason acceptable for stub
    }
    return HttpResponse.json({
      id: Number(id) || 12345,
      email: 'owner@example.edu.vn',
      name: 'Nguyễn Văn A',
      orgName: 'Trung tâm Anh ngữ ABC',
      persona: 'P2_CENTER_OWNER',
      referralSource: null,
      status: 'REJECTED',
      createdAt: '2026-05-08T10:23:00Z',
      approvedAt: null,
      rejectedAt: '2026-05-08T11:00:00Z',
      rejectionReason: reason,
    });
  }),
];
