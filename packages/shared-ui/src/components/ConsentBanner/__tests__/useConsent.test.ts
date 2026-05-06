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
  clearConsent,
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
});

describe('useConsent hook', () => {
  beforeEach(() => {
    window.localStorage.clear();
    vi.useRealTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('hydrates with null state when no consent stored', () => {
    const { result } = renderHook(() => useConsent());
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
    const { result } = renderHook(() => useConsent());
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
    const { result } = renderHook(() => useConsent());
    expect(result.current.state).toBeNull();
    expect(window.localStorage.getItem(DEFAULT_STORAGE_KEY)).toBeNull();
  });

  it('give() persists analytics + marketing', () => {
    const { result } = renderHook(() => useConsent());
    act(() => {
      result.current.give({ analytics: true, marketing: true });
    });
    expect(result.current.analytics).toBe(true);
    expect(result.current.marketing).toBe(true);
    expect(result.current.state?.categories.essential).toBe(true);
  });

  it('reject() persists analytics=false + marketing=false', () => {
    const { result } = renderHook(() => useConsent());
    act(() => {
      result.current.reject();
    });
    expect(result.current.state?.categories.analytics).toBe(false);
    expect(result.current.state?.categories.marketing).toBe(false);
    expect(result.current.state?.categories.essential).toBe(true);
  });

  it('revoke() clears state and storage', () => {
    const { result } = renderHook(() => useConsent());
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
    const { result } = renderHook(() => useConsent());
    act(() => {
      result.current.give({ analytics: true });
    });
    const state = result.current.state!;
    expect(state.expiresAt - state.timestamp).toBe(TWELVE_MONTHS_MS);
  });

  it('isolates per storageKey', () => {
    const { result: first } = renderHook(() => useConsent('test.key.a'));
    const { result: second } = renderHook(() => useConsent('test.key.b'));
    act(() => {
      first.current.give({ analytics: true });
    });
    expect(first.current.analytics).toBe(true);
    expect(second.current.analytics).toBe(false);
  });
});
