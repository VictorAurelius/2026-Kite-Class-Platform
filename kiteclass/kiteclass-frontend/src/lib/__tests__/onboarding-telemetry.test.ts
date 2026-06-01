/**
 * Tests for onboarding telemetry util.
 *
 * @author KiteClass Team
 * @since 4.0.0 — GAP-288
 */

import { describe, it, expect, beforeEach, vi } from 'vitest';
import {
  trackOnboarding,
  getOnboardingEvents,
  clearOnboardingEvents,
  summarizeOnboardingFunnel,
} from '../onboarding-telemetry';

let store: Record<string, string> = {};

const localStorageMock = {
  getItem: vi.fn((key: string) => store[key] ?? null),
  setItem: vi.fn((key: string, value: string) => {
    store[key] = value;
  }),
  removeItem: vi.fn((key: string) => {
    delete store[key];
  }),
  clear: vi.fn(() => {
    store = {};
  }),
  get length() {
    return Object.keys(store).length;
  },
  key: vi.fn((_index: number) => null),
};

Object.defineProperty(window, 'localStorage', { value: localStorageMock });

describe('onboarding-telemetry', () => {
  beforeEach(() => {
    store = {};
    vi.clearAllMocks();
  });

  it('records a start event', () => {
    trackOnboarding('onboarding_start');
    const events = getOnboardingEvents();
    expect(events).toHaveLength(1);
    expect(events[0]?.type).toBe('onboarding_start');
    expect(events[0]?.at).toBeTruthy();
  });

  it('records step index for step events', () => {
    trackOnboarding('onboarding_step_view', 3);
    const events = getOnboardingEvents();
    expect(events[0]?.step).toBe(3);
  });

  it('appends multiple events in order', () => {
    trackOnboarding('onboarding_start');
    trackOnboarding('onboarding_step_skip', 2);
    trackOnboarding('onboarding_complete');
    const events = getOnboardingEvents();
    expect(events.map((e) => e.type)).toEqual([
      'onboarding_start',
      'onboarding_step_skip',
      'onboarding_complete',
    ]);
  });

  it('caps the ring buffer at 50 events', () => {
    for (let i = 0; i < 60; i++) {
      trackOnboarding('onboarding_step_view', i);
    }
    const events = getOnboardingEvents();
    expect(events).toHaveLength(50);
    // Oldest dropped → first retained is step 10
    expect(events[0]?.step).toBe(10);
  });

  it('clears events', () => {
    trackOnboarding('onboarding_start');
    clearOnboardingEvents();
    expect(getOnboardingEvents()).toHaveLength(0);
  });

  it('does not throw when storage write fails', () => {
    localStorageMock.setItem.mockImplementationOnce(() => {
      throw new Error('QuotaExceeded');
    });
    expect(() => trackOnboarding('onboarding_start')).not.toThrow();
  });

  it('returns empty array on corrupt storage', () => {
    store['kiteclass-onboarding-events'] = '{not-json';
    expect(getOnboardingEvents()).toEqual([]);
  });

  describe('summarizeOnboardingFunnel', () => {
    it('returns zeros when no events', () => {
      const s = summarizeOnboardingFunnel();
      expect(s).toEqual({
        starts: 0,
        completes: 0,
        skips: 0,
        completionRate: 0,
        skipRate: 0,
      });
    });

    it('computes completion + skip rates', () => {
      trackOnboarding('onboarding_start');
      trackOnboarding('onboarding_step_skip', 1);
      trackOnboarding('onboarding_complete');
      const s = summarizeOnboardingFunnel();
      expect(s.starts).toBe(1);
      expect(s.completes).toBe(1);
      expect(s.skips).toBe(1);
      expect(s.completionRate).toBe(1);
      expect(s.skipRate).toBe(1);
    });

    it('caps skipRate at 1 even with multiple skips per start', () => {
      trackOnboarding('onboarding_start');
      trackOnboarding('onboarding_step_skip', 1);
      trackOnboarding('onboarding_step_skip', 2);
      trackOnboarding('onboarding_step_skip', 3);
      const s = summarizeOnboardingFunnel();
      expect(s.skipRate).toBe(1);
    });
  });
});
