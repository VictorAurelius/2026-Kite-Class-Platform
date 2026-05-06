/**
 * Tests for useConsent hook + storage adapter (the headless half).
 *
 * Pure logic tests — no DOM rendering. Uses fake timers + LocalStorage stub.
 */

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useConsent } from '../useConsent';
import {
  DEFAULT_STORAGE_KEY,
  VISITOR_ID_STORAGE_KEY,
  clearConsent,
  getOrCreateVisitorId,
  readConsent,
  validate,
  writeConsent,
} from '../storage';
import type { ConsentState } from '../types';

const TWELVE_MONTHS_MS = 365 * 24 * 60 * 60 * 1000;

const sampleState = (overrides: Partial<ConsentState> = {}): ConsentState => ({
  version: 1,
  timestamp: Date.now(),
  expiresAt: Date.now() + TWELVE_MONTHS_MS,
  categories: {
    essential: true,
    analytics: false,
    marketing: false,
  },
  ...overrides,
});

describe('storage adapter', () => {
  beforeEach(() => {
    window.localStorage.clear();
  });

  it('returns null when key missing', () => {
    expect(readConsent()).toBeNull();
  });

  it('round-trips a valid state', () => {
    const state = sampleState();
    writeConsent(state);
    const read = readConsent();
    expect(read).toEqual(state);
  });

  it('drops malformed JSON', () => {
    window.localStorage.setItem(DEFAULT_STORAGE_KEY, '{not json');
    expect(readConsent()).toBeNull();
  });

  it('drops state with wrong schema version', () => {
    window.localStorage.setItem(
      DEFAULT_STORAGE_KEY,
      JSON.stringify({ version: 2, timestamp: 0, expiresAt: 0, categories: {} }),
    );
    expect(readConsent()).toBeNull();
  });

  it('forces essential = true even if stored false', () => {
    const corrupt = sampleState();
    corrupt.categories.essential = false as unknown as true; // simulate tamper
    writeConsent(corrupt);
    const read = readConsent();
    expect(read?.categories.essential).toBe(true);
  });

  it('clearConsent removes the key', () => {
    writeConsent(sampleState());
    clearConsent();
    expect(readConsent()).toBeNull();
  });

  it('validate rejects null / non-objects', () => {
    expect(validate(null)).toBeNull();
    expect(validate('string')).toBeNull();
    expect(validate(42)).toBeNull();
  });

  describe('getOrCreateVisitorId', () => {
    it('generates and persists a UUID v4 on first call', () => {
      const id = getOrCreateVisitorId();
      expect(id).toMatch(
        /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[0-9a-f]{4}-[0-9a-f]{12}$/i,
      );
      expect(window.localStorage.getItem(VISITOR_ID_STORAGE_KEY)).toBe(id);
    });

    it('returns the same id on subsequent calls', () => {
      const a = getOrCreateVisitorId();
      const b = getOrCreateVisitorId();
      expect(b).toBe(a);
    });

    it('regenerates when stored value is malformed', () => {
      window.localStorage.setItem(VISITOR_ID_STORAGE_KEY, 'not-a-uuid');
      const id = getOrCreateVisitorId();
      expect(id).not.toBe('not-a-uuid');
      expect(id).toMatch(/^[0-9a-f-]{36}$/);
    });

    it('honors a custom storage key', () => {
      const id = getOrCreateVisitorId('custom.visitor.key');
      expect(window.localStorage.getItem('custom.visitor.key')).toBe(id);
      expect(window.localStorage.getItem(VISITOR_ID_STORAGE_KEY)).toBeNull();
    });
  });
});

describe('useConsent hook', () => {
  // Stub fetch globally so API sync doesn't error in tests not asserting it.
  beforeEach(() => {
    window.localStorage.clear();
    vi.useRealTimers();
    vi.stubGlobal(
      'fetch',
      vi.fn(() => Promise.resolve({ ok: true, json: async () => ({}) } as Response)),
    );
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.unstubAllGlobals();
  });

  it('hydrates with null state when no consent stored', () => {
    const { result } = renderHook(() => useConsent({ syncToServer: false }));
    expect(result.current.hydrated).toBe(true);
    expect(result.current.state).toBeNull();
    expect(result.current.analytics).toBe(false);
    expect(result.current.marketing).toBe(false);
  });

  it('hydrates with stored state when consent exists', () => {
    const stored = sampleState({
      categories: { essential: true, analytics: true, marketing: false },
    });
    writeConsent(stored);
    const { result } = renderHook(() => useConsent({ syncToServer: false }));
    expect(result.current.state).not.toBeNull();
    expect(result.current.analytics).toBe(true);
    expect(result.current.marketing).toBe(false);
  });

  it('drops expired state and re-prompts', () => {
    const expired = sampleState({
      timestamp: Date.now() - 2 * TWELVE_MONTHS_MS,
      expiresAt: Date.now() - TWELVE_MONTHS_MS, // already expired
    });
    writeConsent(expired);
    const { result } = renderHook(() => useConsent({ syncToServer: false }));
    expect(result.current.state).toBeNull();
    expect(window.localStorage.getItem(DEFAULT_STORAGE_KEY)).toBeNull();
  });

  it('give() persists analytics + marketing', () => {
    const { result } = renderHook(() => useConsent({ syncToServer: false }));
    act(() => {
      result.current.give({ analytics: true, marketing: true });
    });
    expect(result.current.analytics).toBe(true);
    expect(result.current.marketing).toBe(true);
    expect(result.current.state?.categories.essential).toBe(true);
  });

  it('reject() persists analytics=false + marketing=false', () => {
    const { result } = renderHook(() => useConsent({ syncToServer: false }));
    act(() => {
      result.current.reject();
    });
    expect(result.current.state?.categories.analytics).toBe(false);
    expect(result.current.state?.categories.marketing).toBe(false);
    expect(result.current.state?.categories.essential).toBe(true);
  });

  it('revoke() clears state and storage', () => {
    const { result } = renderHook(() => useConsent({ syncToServer: false }));
    act(() => {
      result.current.give({ analytics: true });
    });
    expect(result.current.state).not.toBeNull();
    act(() => {
      result.current.revoke();
    });
    expect(result.current.state).toBeNull();
    expect(window.localStorage.getItem(DEFAULT_STORAGE_KEY)).toBeNull();
  });

  it('expiresAt is set ~12 months from timestamp', () => {
    const { result } = renderHook(() => useConsent({ syncToServer: false }));
    act(() => {
      result.current.give({ analytics: true });
    });
    const state = result.current.state!;
    expect(state.expiresAt - state.timestamp).toBe(TWELVE_MONTHS_MS);
  });

  it('isolates per storageKey', () => {
    const { result: first } = renderHook(() =>
      useConsent({ storageKey: 'test.key.a', syncToServer: false }),
    );
    const { result: second } = renderHook(() =>
      useConsent({ storageKey: 'test.key.b', syncToServer: false }),
    );
    act(() => {
      first.current.give({ analytics: true });
    });
    expect(first.current.analytics).toBe(true);
    expect(second.current.analytics).toBe(false);
  });

  it('hydrates a stable visitorId (UUID v4)', () => {
    const { result, rerender } = renderHook(() =>
      useConsent({ syncToServer: false, visitorIdKey: 'test.visitor' }),
    );
    expect(result.current.visitorId).toMatch(
      /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[0-9a-f]{4}-[0-9a-f]{12}$/i,
    );
    const first = result.current.visitorId;
    rerender();
    expect(result.current.visitorId).toBe(first);
  });

  it('give() triggers API record call when syncToServer=true', async () => {
    const fetchMock = vi.fn((..._args: unknown[]) =>
      Promise.resolve({ ok: true, json: async () => ({}) } as Response),
    );
    vi.stubGlobal('fetch', fetchMock);
    const { result } = renderHook(() =>
      useConsent({ syncToServer: true, visitorIdKey: 'test.api.visitor' }),
    );
    await act(async () => {
      result.current.give({ analytics: true, marketing: false });
      // Allow microtasks (the fetch call) to resolve.
      await Promise.resolve();
    });
    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, init] = fetchMock.mock.calls[0]!;
    expect(url).toContain('/api/v1/consent/record');
    expect((init as RequestInit).method).toBe('POST');
    const body = JSON.parse((init as RequestInit).body as string);
    expect(body.analyticsConsented).toBe(true);
    expect(body.marketingConsented).toBe(false);
    expect(body.essentialConsented).toBe(true);
    expect(body.visitorId).toMatch(/^[0-9a-f-]+$/);
  });

  it('reject() triggers API record call when syncToServer=true', async () => {
    const fetchMock = vi.fn((..._args: unknown[]) =>
      Promise.resolve({ ok: true, json: async () => ({}) } as Response),
    );
    vi.stubGlobal('fetch', fetchMock);
    const { result } = renderHook(() =>
      useConsent({ syncToServer: true, visitorIdKey: 'test.api.visitor.b' }),
    );
    await act(async () => {
      result.current.reject();
      await Promise.resolve();
    });
    expect(fetchMock).toHaveBeenCalledTimes(1);
    const body = JSON.parse((fetchMock.mock.calls[0]![1] as RequestInit).body as string);
    expect(body.analyticsConsented).toBe(false);
    expect(body.marketingConsented).toBe(false);
  });

  it('revoke() triggers API revoke call when syncToServer=true', async () => {
    const fetchMock = vi.fn((..._args: unknown[]) =>
      Promise.resolve({ ok: true, json: async () => ({}) } as Response),
    );
    vi.stubGlobal('fetch', fetchMock);
    const { result } = renderHook(() =>
      useConsent({ syncToServer: true, visitorIdKey: 'test.api.visitor.c' }),
    );
    await act(async () => {
      result.current.give({ analytics: true });
      await Promise.resolve();
    });
    fetchMock.mockClear();
    await act(async () => {
      result.current.revoke();
      await Promise.resolve();
    });
    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, init] = fetchMock.mock.calls[0]!;
    expect(url).toMatch(/\/api\/v1\/consent\/[0-9a-f-]+\/revoke$/);
    expect((init as RequestInit).method).toBe('POST');
  });

  it('API failure does not throw (LocalStorage primary stays intact)', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(() => Promise.reject(new Error('network down'))),
    );
    const { result } = renderHook(() =>
      useConsent({ syncToServer: true, visitorIdKey: 'test.api.visitor.d' }),
    );
    await act(async () => {
      result.current.give({ analytics: true });
      await Promise.resolve();
    });
    expect(result.current.analytics).toBe(true);
    expect(result.current.state?.categories.analytics).toBe(true);
  });
});
