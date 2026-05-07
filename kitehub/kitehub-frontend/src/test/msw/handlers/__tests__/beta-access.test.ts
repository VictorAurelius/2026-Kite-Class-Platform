/**
 * Unit tests for beta-access MSW handlers (Wave 35 Bucket 0 Foundation).
 *
 * Verifies the contract documented in
 * `documents/01-business/kitehub/beta-access/api-contract.md`:
 *   - POST /api/v1/auth/request-beta-access — consent gate (BR-BETA-001)
 *   - duplicate email (BR-BETA-002)
 *   - admin endpoints surface 401/403 (BR-BETA-003 — Bucket A guard target)
 *
 * The MSW server is started by `src/test/setup.ts` via `setupFiles` once
 * `handlers.length > 0`. This file therefore relies on the global MSW
 * server being live — no manual setup needed.
 *
 * @author KiteHub Team
 * @since Wave 35 Bucket 0
 */

import { describe, expect, it } from 'vitest';

const ENDPOINT_SUBMIT = 'http://localhost/api/v1/auth/request-beta-access';
const ENDPOINT_ADMIN_LIST = 'http://localhost/api/v1/admin/beta-requests';

const validPayload = {
  email: 'owner@example.edu.vn',
  name: 'Nguyễn Văn A',
  orgName: 'Trung tâm Anh ngữ ABC',
  persona: 'P2_CENTER_OWNER',
  referralSource: 'google',
  honeypot: '',
  consentGiven: true,
};

describe('beta-access MSW handlers — POST /request-beta-access', () => {
  it('returns 201 + BetaRequestResponse when consentGiven=true + valid payload', async () => {
    const res = await fetch(ENDPOINT_SUBMIT, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(validPayload),
    });
    expect(res.status).toBe(201);
    const body = await res.json();
    expect(body.status).toBe('PENDING');
    expect(body.email).toBe(validPayload.email);
    expect(body.persona).toBe('P2_CENTER_OWNER');
  });

  it('returns 400 BETA_CONSENT_REQUIRED when consentGiven is missing', async () => {
    const { consentGiven, ...without } = validPayload;
    void consentGiven;
    const res = await fetch(ENDPOINT_SUBMIT, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(without),
    });
    expect(res.status).toBe(400);
    const body = await res.json();
    expect(body.error).toBe('BETA_CONSENT_REQUIRED');
    expect(body.field).toBe('consentGiven');
  });

  it('returns 400 BETA_CONSENT_REQUIRED when consentGiven=false', async () => {
    const res = await fetch(ENDPOINT_SUBMIT, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ ...validPayload, consentGiven: false }),
    });
    expect(res.status).toBe(400);
    const body = await res.json();
    expect(body.error).toBe('BETA_CONSENT_REQUIRED');
  });

  it('returns 409 BETA_DUPLICATE_EMAIL for the seeded duplicate address', async () => {
    const res = await fetch(ENDPOINT_SUBMIT, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ ...validPayload, email: 'duplicate@example.edu.vn' }),
    });
    expect(res.status).toBe(409);
    const body = await res.json();
    expect(body.error).toBe('BETA_DUPLICATE_EMAIL');
  });

  it('returns 400 BETA_INVALID_PERSONA for unsupported persona', async () => {
    const res = await fetch(ENDPOINT_SUBMIT, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ ...validPayload, persona: 'P5_K12_PRINCIPAL' }),
    });
    expect(res.status).toBe(400);
    const body = await res.json();
    expect(body.error).toBe('BETA_INVALID_PERSONA');
  });
});

describe('beta-access MSW handlers — admin endpoints (Bucket A guard target)', () => {
  it('returns 401 when no Authorization header is sent', async () => {
    const res = await fetch(ENDPOINT_ADMIN_LIST);
    expect(res.status).toBe(401);
  });

  it('returns 403 when authenticated but lacks admin role', async () => {
    const res = await fetch(ENDPOINT_ADMIN_LIST, {
      headers: { Authorization: 'Bearer user-token' },
    });
    expect(res.status).toBe(403);
  });

  it('returns 200 + paged body when admin token is provided', async () => {
    const res = await fetch(ENDPOINT_ADMIN_LIST, {
      headers: { Authorization: 'Bearer admin-token' },
    });
    expect(res.status).toBe(200);
    const body = await res.json();
    expect(body).toHaveProperty('content');
    expect(body).toHaveProperty('totalElements');
  });
});
