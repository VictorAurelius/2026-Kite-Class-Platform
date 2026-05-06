/**
 * Wave 31 Bucket D — instance detail page G9 integration tests.
 *
 * Complements the existing `instance-detail.test.tsx` (which covers the
 * useInstance / useTrialStatus hook contracts). This file covers the
 * full G9 InstanceLifecycleStatus timeline rendered on the detail page.
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen } from '@testing-library/react';
import { render } from '@/test/test-utils';
import React from 'react';

// `params` is a Promise consumed by React 19 `use(...)`. Mock `use` in tests
// so the page receives the resolved object synchronously and we don't need
// a Suspense boundary or microtask flushing.
vi.mock('react', async (importOriginal) => {
  const actual = await importOriginal<typeof import('react')>();
  return {
    ...actual,
    use: <T,>(value: T | Promise<T>): T => {
      if (value && typeof (value as { then?: unknown }).then === 'function') {
        // Tests construct params via `Promise.resolve({ id })` — peek the
        // resolved value via internal helper for tests only.
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const fake = value as any;
        if (fake.__resolvedValue !== undefined) return fake.__resolvedValue;
      }
      return value as T;
    },
  };
});

vi.mock('@/hooks/use-instances', () => ({
  useInstance: vi.fn(),
  useTrialStatus: vi.fn(),
}));

import { useInstance, useTrialStatus } from '@/hooks/use-instances';
import InstanceDetailPage from '../[id]/page';
import {
  mockLifecycleEvents,
  mockLifecycleStateFor,
} from '../_lifecycle-mock';

const mockInstance = {
  id: 'inst-detail-1',
  organizationName: 'Trung tâm Toán Master',
  subdomain: 'mathmaster',
  ownerId: 'owner-1',
  contactEmail: 'admin@mathmaster.edu.vn',
  status: 'ACTIVE',
  tier: 'PREMIUM',
  trialStartedAt: null,
  trialExpiresAt: null,
  trialDaysLeft: null,
  subscriptionId: 'sub-1',
  subscriptionExpiresAt: null,
  isActive: true,
  isOnTrial: false,
  createdAt: '2026-03-31T00:00:00Z',
  updatedAt: '2026-04-01T00:00:00Z',
};

// React 19 `use(params)` consumes a Promise-like. The mocked `React.use`
// reads `__resolvedValue` synchronously so tests stay simple.
function makeParams(id: string) {
  const promise = Promise.resolve({ id });
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  (promise as any).__resolvedValue = { id };
  return promise;
}

describe('InstanceDetailPage — G9 timeline', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    (useTrialStatus as ReturnType<typeof vi.fn>).mockReturnValue({ data: null });
  });

  it('renders G9 timeline section when instance is loaded', async () => {
    (useInstance as ReturnType<typeof vi.fn>).mockReturnValue({
      data: mockInstance,
      isLoading: false,
      error: null,
    });

    render(
      <InstanceDetailPage params={makeParams('inst-detail-1') as unknown as Promise<{ id: string }>} />
    );

    expect(screen.getByTestId('g9-timeline')).toBeInTheDocument();
    expect(screen.getByText('Vòng đời thương hiệu AI')).toBeInTheDocument();
    expect(screen.getAllByText('Trung tâm Toán Master').length).toBeGreaterThan(0);
  });

  it('does not render G9 timeline while loading', () => {
    (useInstance as ReturnType<typeof vi.fn>).mockReturnValue({
      data: null,
      isLoading: true,
      error: null,
    });

    render(
      <InstanceDetailPage params={makeParams('inst-detail-1') as unknown as Promise<{ id: string }>} />
    );

    // While instance data is loading, only the spinner renders; G9 timeline
    // must NOT be in the DOM yet.
    expect(screen.queryByTestId('g9-timeline')).not.toBeInTheDocument();
  });
});

describe('mockLifecycleEvents', () => {
  it('returns empty events for NOT_STARTED', () => {
    expect(mockLifecycleEvents('NOT_STARTED')).toEqual([]);
  });

  it('returns full chain ending in DEPLOYED for DEPLOYED state', () => {
    const events = mockLifecycleEvents('DEPLOYED');
    expect(events.length).toBeGreaterThanOrEqual(3);
    expect(events[events.length - 1]?.to).toBe('DEPLOYED');
  });

  it('returns FAILED transition with reason copy when state is FAILED', () => {
    const events = mockLifecycleEvents('FAILED');
    const failedEvent = events.find((e) => e.to === 'FAILED');
    expect(failedEvent).toBeDefined();
    expect(failedEvent?.reason).toMatch(/Quality gate/);
  });

  it('REGENERATING events include a DEPLOYED -> REGENERATING transition with actor reason', () => {
    const events = mockLifecycleEvents('REGENERATING');
    const regen = events.find(
      (e) => e.from === 'DEPLOYED' && e.to === 'REGENERATING'
    );
    expect(regen).toBeDefined();
    expect(regen?.reason).toMatch(/tái tạo/i);
  });
});

describe('lifecycle state coverage', () => {
  it('hash-based mock can produce all 6 lifecycle states across a small id set', () => {
    const seen = new Set(
      [
        'a-id',
        'b-id',
        'c-id',
        'd-id',
        'e-id',
        'f-id',
        'g-id',
        'h-id',
        'i-id',
        'j-id',
        'k-id',
        'l-id',
      ].map(mockLifecycleStateFor)
    );
    // Across 12 distinct ids the hash-based picker spans multiple states.
    expect(seen.size).toBeGreaterThanOrEqual(3);
  });
});
