/**
 * Tests for the GAP-353b consent API wrappers.
 *
 * Best-effort semantics: every wrapper must SWALLOW network/server errors and
 * NEVER throw to callers (LocalStorage primary in useConsent).
 */

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  buildPayload,
  getConsent,
  recordConsent,
  revokeConsent,
} from '../api';

const visitorId = '11111111-2222-4333-8444-555555555555';

beforeEach(() => {
  vi.unstubAllGlobals();
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('buildPayload', () => {
  it('produces the canonical request shape', () => {
    const payload = buildPayload(visitorId, {
      essential: true,
      analytics: true,
      marketing: false,
    });
    expect(payload).toMatchObject({
      visitorId,
      analyticsConsented: true,
      marketingConsented: false,
      essentialConsented: true,
      consentVersion: 1,
      userId: null,
      tenantId: null,
    });
  });

  it('respects extras + defaults', () => {
    const payload = buildPayload(
      visitorId,
      { essential: true, analytics: false, marketing: false },
      { userId: 42, tenantId: 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa' },
    );
    expect(payload.userId).toBe(42);
    expect(payload.tenantId).toBe('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa');
  });
});

describe('recordConsent', () => {
  it('returns true on 2xx', async () => {
    const fetchMock = vi.fn((..._args: unknown[]) =>
      Promise.resolve({ ok: true, status: 201, json: async () => ({}) } as Response),
    );
    vi.stubGlobal('fetch', fetchMock);
    const ok = await recordConsent(
      buildPayload(visitorId, { essential: true, analytics: true, marketing: false }),
    );
    expect(ok).toBe(true);
    expect(fetchMock).toHaveBeenCalledOnce();
    expect(fetchMock.mock.calls[0]![0]).toMatch(/\/api\/v1\/consent\/record$/);
  });

  it('returns false on non-2xx', async () => {
    const fetchMock = vi.fn(() =>
      Promise.resolve({ ok: false, status: 500 } as Response),
    );
    vi.stubGlobal('fetch', fetchMock);
    const ok = await recordConsent(
      buildPayload(visitorId, { essential: true, analytics: false, marketing: false }),
    );
    expect(ok).toBe(false);
  });

  it('returns false on network error (does not throw)', async () => {
    vi.stubGlobal('fetch', vi.fn(() => Promise.reject(new Error('boom'))));
    const ok = await recordConsent(
      buildPayload(visitorId, { essential: true, analytics: false, marketing: false }),
    );
    expect(ok).toBe(false);
  });
});

describe('getConsent', () => {
  it('returns the parsed record on success', async () => {
    const sample = {
      id: 1,
      visitorId,
      analyticsConsented: true,
      marketingConsented: false,
      essentialConsented: true,
      consentVersion: 1,
      createdAt: '2026-05-06T00:00:00Z',
      updatedAt: '2026-05-06T00:00:00Z',
      expiresAt: '2027-05-06T00:00:00Z',
      revokedAt: null,
      revoked: false,
      userId: null,
      tenantId: null,
    };
    vi.stubGlobal(
      'fetch',
      vi.fn(() =>
        Promise.resolve({ ok: true, json: async () => sample } as Response),
      ),
    );
    const got = await getConsent(visitorId);
    expect(got).toEqual(sample);
  });

  it('returns null on 404', async () => {
    vi.stubGlobal('fetch', vi.fn(() => Promise.resolve({ ok: false, status: 404 } as Response)));
    const got = await getConsent(visitorId);
    expect(got).toBeNull();
  });

  it('returns null on network failure', async () => {
    vi.stubGlobal('fetch', vi.fn(() => Promise.reject(new Error('offline'))));
    const got = await getConsent(visitorId);
    expect(got).toBeNull();
  });
});

describe('revokeConsent', () => {
  it('returns true on success', async () => {
    const fetchMock = vi.fn((..._args: unknown[]) =>
      Promise.resolve({ ok: true, json: async () => ({}) } as Response),
    );
    vi.stubGlobal('fetch', fetchMock);
    const ok = await revokeConsent(visitorId);
    expect(ok).toBe(true);
    expect(fetchMock.mock.calls[0]![0]).toMatch(/\/api\/v1\/consent\/[0-9a-f-]+\/revoke$/);
  });

  it('returns false on 404', async () => {
    vi.stubGlobal('fetch', vi.fn(() => Promise.resolve({ ok: false, status: 404 } as Response)));
    const ok = await revokeConsent(visitorId);
    expect(ok).toBe(false);
  });

  it('returns false on network error', async () => {
    vi.stubGlobal('fetch', vi.fn(() => Promise.reject(new Error('offline'))));
    const ok = await revokeConsent(visitorId);
    expect(ok).toBe(false);
  });
});
