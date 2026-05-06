/**
 * Wave 31 Bucket D — instances list page tests.
 *
 * Verifies:
 *  - Empty state renders when there are no instances.
 *  - Multiple-instance list renders one row per instance.
 *  - G9 InstanceLifecycleStatus pill is wired per row.
 *  - Lifecycle state is deterministic (hash-based mock) so the list reflects
 *    the full set of lifecycle states across a small fixture.
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen } from '@testing-library/react';
import { render } from '@/test/test-utils';

// Auth store + instances hook are mocked so the component renders deterministically.
vi.mock('@/stores/auth-store', () => ({
  useAuthStore: vi.fn((selector: (state: { user: { id: string } }) => unknown) =>
    selector({ user: { id: 'owner-1' } })
  ),
}));

vi.mock('@/hooks/use-instances', () => ({
  useOwnerInstances: vi.fn(),
}));

import { useOwnerInstances } from '@/hooks/use-instances';
import InstancesPage from '../page';
import { mockLifecycleStateFor } from '../_lifecycle-mock';

const baseInstance = {
  id: 'inst-1',
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

describe('InstancesPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders empty state when there are no instances', () => {
    (useOwnerInstances as ReturnType<typeof vi.fn>).mockReturnValue({
      data: [],
      isLoading: false,
      error: null,
    });

    render(<InstancesPage />);

    expect(screen.getByText(/Chưa có phiên bản nào/)).toBeInTheDocument();
  });

  it('renders one row per instance with G9 lifecycle pill', () => {
    const instances = [
      baseInstance,
      { ...baseInstance, id: 'inst-2', organizationName: 'Trung tâm Anh ngữ Sao Mai', subdomain: 'saomai' },
      { ...baseInstance, id: 'inst-3', organizationName: 'Trường mầm non Hoa Sen', subdomain: 'hoasen' },
    ];
    (useOwnerInstances as ReturnType<typeof vi.fn>).mockReturnValue({
      data: instances,
      isLoading: false,
      error: null,
    });

    render(<InstancesPage />);

    // Org names appear in both the row header and inside the G9 component;
    // verify presence with `getAllByText` rather than exact-one match.
    expect(screen.getAllByText('Trung tâm Toán Master').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Trung tâm Anh ngữ Sao Mai').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Trường mầm non Hoa Sen').length).toBeGreaterThan(0);

    // G9 pills present per row.
    expect(screen.getByTestId('g9-pill-inst-1')).toBeInTheDocument();
    expect(screen.getByTestId('g9-pill-inst-2')).toBeInTheDocument();
    expect(screen.getByTestId('g9-pill-inst-3')).toBeInTheDocument();
  });

  it('shows error alert when hook returns error', () => {
    (useOwnerInstances as ReturnType<typeof vi.fn>).mockReturnValue({
      data: null,
      isLoading: false,
      error: new Error('boom'),
    });

    render(<InstancesPage />);

    expect(
      screen.getByText(/Không thể tải danh sách phiên bản/)
    ).toBeInTheDocument();
  });
});

describe('mockLifecycleStateFor', () => {
  it('is deterministic for the same instance id', () => {
    expect(mockLifecycleStateFor('inst-1')).toBe(mockLifecycleStateFor('inst-1'));
  });

  it('returns NOT_STARTED when given undefined', () => {
    expect(mockLifecycleStateFor(undefined)).toBe('NOT_STARTED');
  });

  it('produces a valid lifecycle state for arbitrary ids', () => {
    const validStates = new Set([
      'NOT_STARTED',
      'INITIALIZING',
      'GENERATING',
      'DEPLOYED',
      'REGENERATING',
      'FAILED',
    ]);
    for (const id of ['a', 'inst-1', 'inst-2', 'inst-3', 'inst-4', 'inst-5', 'inst-6']) {
      expect(validStates.has(mockLifecycleStateFor(id))).toBe(true);
    }
  });
});
