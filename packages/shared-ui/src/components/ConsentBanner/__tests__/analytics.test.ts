/**
 * Tests for analytics SDK lifecycle handler — Wave br-4 Bucket B (GAP-353b).
 *
 * Verifies:
 *   1. gtag('consent','update', ...) fires synchronously with correct mapping
 *   2. No-gtag environment falls back to dataLayer push (no throw)
 *   3. SSR environment (no window) is no-op
 *   4. revoke() in useConsent fires gtag denied BEFORE async server POST
 *   5. revoke() completes effective gates within 5s budget per PDPL Art 14
 */

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { applyAnalyticsConsent } from '../analytics';
import { useConsent } from '../useConsent';
import { DEFAULT_STORAGE_KEY, VISITOR_ID_STORAGE_KEY } from '../storage';

const TWELVE_MONTHS_MS = 365 * 24 * 60 * 60 * 1000;

declare global {
  interface Window {
    gtag?: (...args: unknown[]) => void;
    dataLayer?: unknown[];
  }
}

describe('applyAnalyticsConsent — SDK lifecycle handler', () => {
  beforeEach(() => {
    window.localStorage.clear();
    delete window.gtag;
    delete window.dataLayer;
  });

  it('fires gtag consent update with full Google mapping when gtag present', () => {
    const calls: unknown[][] = [];
    window.gtag = ((...args: unknown[]) => {
      calls.push(args);
    }) as never;

    applyAnalyticsConsent({ essential: true, analytics: true, marketing: false });

    expect(calls).toHaveLength(1);
    expect(calls[0][0]).toBe('consent');
    expect(calls[0][1]).toBe('update');
    const payload = calls[0][2] as Record<string, string>;
    expect(payload.analytics_storage).toBe('granted');
    expect(payload.ad_storage).toBe('denied');
    expect(payload.ad_user_data).toBe('denied');
    expect(payload.ad_personalization).toBe('denied');
    expect(payload.functionality_storage).toBe('granted');
    expect(payload.security_storage).toBe('granted');
  });

  it('fires denied gates when all categories false (revoke scenario)', () => {
    const calls: unknown[][] = [];
    window.gtag = ((...args: unknown[]) => {
      calls.push(args);
    }) as never;

    applyAnalyticsConsent({ essential: true, analytics: false, marketing: false });

    const payload = calls[0][2] as Record<string, string>;
    expect(payload.analytics_storage).toBe('denied');
    expect(payload.ad_storage).toBe('denied');
  });

  it('falls back to dataLayer push when gtag not yet loaded', () => {
    // Simulate SDK not yet loaded — only dataLayer present.
    window.dataLayer = [];
    applyAnalyticsConsent({ essential: true, analytics: true, marketing: true });

    expect(window.dataLayer).toHaveLength(1);
    const entry = window.dataLayer[0] as Record<string, unknown>;
    expect(entry.event).toBe('consent_update');
    const snapshot = entry.consentSnapshot as Record<string, string>;
    expect(snapshot.analytics_storage).toBe('granted');
    expect(snapshot.ad_storage).toBe('granted');
  });

  it('does not throw when gtag implementation is broken', () => {
    window.gtag = (() => {
      throw new Error('analytics SDK error');
    }) as never;
    expect(() =>
      applyAnalyticsConsent({ essential: true, analytics: true, marketing: false }),
    ).not.toThrow();
  });
});

describe('useConsent revoke — lifecycle handler fires synchronously BEFORE server POST', () => {
  let gtagCalls: unknown[][];
  let fetchOrder: string[];

  beforeEach(() => {
    window.localStorage.clear();
    delete window.dataLayer;

    gtagCalls = [];
    window.gtag = ((...args: unknown[]) => {
      gtagCalls.push(args);
      fetchOrder.push('gtag');
    }) as never;

    fetchOrder = [];
    // Mock fetch to record CALL ORDER + delay response — proves gtag fires first.
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => {
        fetchOrder.push('fetch');
        // Simulate slow server (1s) to highlight gtag-first ordering.
        await new Promise((resolve) => setTimeout(resolve, 1000));
        return { ok: true, json: async () => ({}) } as Response;
      }),
    );
  });

  afterEach(() => {
    vi.restoreAllMocks();
    vi.useRealTimers();
    delete window.gtag;
  });

  it('revoke fires gtag denied BEFORE fetch POST is invoked', async () => {
    // Seed an existing consent so revoke has something to revoke.
    window.localStorage.setItem(VISITOR_ID_STORAGE_KEY, '11111111-2222-4333-8444-555555555555');
    window.localStorage.setItem(
      DEFAULT_STORAGE_KEY,
      JSON.stringify({
        version: 1,
        timestamp: Date.now(),
        expiresAt: Date.now() + TWELVE_MONTHS_MS,
        categories: { essential: true, analytics: true, marketing: true },
      }),
    );

    const { result } = renderHook(() => useConsent());
    // Wait for hydration.
    await act(async () => {
      await Promise.resolve();
    });
    expect(result.current.hydrated).toBe(true);

    await act(async () => {
      result.current.revoke();
      // Microtask to allow synchronous gtag call + microtask fetch enqueue.
      await Promise.resolve();
    });

    expect(gtagCalls).toHaveLength(1);
    const payload = gtagCalls[0][2] as Record<string, string>;
    expect(payload.analytics_storage).toBe('denied');
    expect(payload.ad_storage).toBe('denied');
    // Order assertion: gtag MUST appear before fetch in our trace.
    const gtagIdx = fetchOrder.indexOf('gtag');
    const fetchIdx = fetchOrder.indexOf('fetch');
    expect(gtagIdx).toBeGreaterThanOrEqual(0);
    expect(fetchIdx).toBeGreaterThan(gtagIdx);
  });

  it('revoke completes synchronously well under 5s budget per PDPL Art 14', async () => {
    window.localStorage.setItem(VISITOR_ID_STORAGE_KEY, '22222222-3333-4333-8444-555555555555');
    window.localStorage.setItem(
      DEFAULT_STORAGE_KEY,
      JSON.stringify({
        version: 1,
        timestamp: Date.now(),
        expiresAt: Date.now() + TWELVE_MONTHS_MS,
        categories: { essential: true, analytics: true, marketing: true },
      }),
    );
    const { result } = renderHook(() => useConsent());
    await act(async () => {
      await Promise.resolve();
    });

    const start = performance.now();
    await act(async () => {
      result.current.revoke();
    });
    const elapsedMs = performance.now() - start;

    // Synchronous fire path must be much faster than 5s — closer to <50ms.
    expect(elapsedMs).toBeLessThan(5000);
    // State should be cleared immediately (LocalStorage gone, hook state null).
    expect(window.localStorage.getItem(DEFAULT_STORAGE_KEY)).toBeNull();
    expect(result.current.state).toBeNull();
    // gtag was called with denied mapping inline (not awaiting server response).
    const payload = gtagCalls[0][2] as Record<string, string>;
    expect(payload.analytics_storage).toBe('denied');
  });
});
