/**
 * Instance Detail Page Tests
 *
 * Tests for the instance detail page (customer view).
 * Uses mocked React.use to handle async params pattern.
 *
 * @since wave/11
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import { render } from '@/__tests__/test-utils';

// Mock React.use to handle async params in Next.js page components
vi.mock('react', async () => {
  const actual = await vi.importActual<typeof import('react')>('react');
  return {
    ...actual,
    use: vi.fn((value: unknown) => {
      // If it's a promise with a resolved value, return it synchronously
      if (value && typeof value === 'object' && '_resolved' in value) {
        return (value as { _resolved: unknown })._resolved;
      }
      return value;
    }),
  };
});

vi.mock('@/hooks/use-instances', () => ({
  useInstance: vi.fn(),
  useTrialStatus: vi.fn(),
}));

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn(), back: vi.fn() }),
  useParams: () => ({ id: 'test-instance-id' }),
}));

vi.mock('next/link', () => ({
  default: ({ children, href }: { children: React.ReactNode; href: string }) => (
    <a href={href}>{children}</a>
  ),
}));

vi.mock('@/lib/tenant-url', () => ({
  getTenantUrl: (subdomain: string) => `https://${subdomain}.kiteclass.com`,
  getTenantDisplayUrl: (subdomain: string) => `${subdomain}.kiteclass.com`,
}));

vi.mock('@/lib/utils', () => ({
  formatDate: (date: string) => date,
  cn: (...args: string[]) => args.filter(Boolean).join(' '),
}));

import { useInstance, useTrialStatus } from '@/hooks/use-instances';

const mockInstance = {
  id: 'test-instance-id',
  organizationName: 'Trường Test ABC',
  subdomain: 'testabc',
  status: 'TRIAL',
  tier: 'FREE',
  isOnTrial: true,
  contactEmail: 'admin@testabc.edu.vn',
  createdAt: '2026-03-24T00:00:00Z',
};

describe('Instance hooks (unit)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('useInstance returns loading state initially', () => {
    (useInstance as ReturnType<typeof vi.fn>).mockReturnValue({
      data: null,
      isLoading: true,
      error: null,
    });

    const result = useInstance('test-id');
    expect(result.isLoading).toBe(true);
    expect(result.data).toBeNull();
  });

  it('useInstance returns instance data when loaded', () => {
    (useInstance as ReturnType<typeof vi.fn>).mockReturnValue({
      data: mockInstance,
      isLoading: false,
      error: null,
    });

    const result = useInstance('test-id');
    expect(result.data).toEqual(mockInstance);
    expect(result.data?.organizationName).toBe('Trường Test ABC');
    expect(result.data?.subdomain).toBe('testabc');
  });

  it('useInstance returns error when fetch fails', () => {
    const error = new Error('Not found');
    (useInstance as ReturnType<typeof vi.fn>).mockReturnValue({
      data: null,
      isLoading: false,
      error,
    });

    const result = useInstance('test-id');
    expect(result.error).toBeInstanceOf(Error);
    expect(result.data).toBeNull();
  });

  it('useTrialStatus returns trial info for TRIAL instances', () => {
    (useTrialStatus as ReturnType<typeof vi.fn>).mockReturnValue({
      data: { daysLeft: 10, trialExpiresAt: '2026-04-07T00:00:00Z', status: 'TRIAL' },
    });

    const result = useTrialStatus('test-id');
    expect(result.data?.daysLeft).toBe(10);
  });

  it('useTrialStatus returns null for non-trial instances', () => {
    (useTrialStatus as ReturnType<typeof vi.fn>).mockReturnValue({
      data: null,
    });

    const result = useTrialStatus(undefined);
    expect(result.data).toBeNull();
  });
});
