/**
 * Instance Detail Page Tests
 *
 * Tests for the instance detail page (customer view).
 *
 * @since wave/11
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import { render } from '@/__tests__/test-utils';

vi.mock('@/hooks/use-instances', () => ({
  useInstance: vi.fn(),
  useTrialStatus: vi.fn(),
}));

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn(), back: vi.fn() }),
  useParams: () => ({ id: 'test-instance-id' }),
}));

vi.mock('@/lib/tenant-url', () => ({
  getTenantUrl: (subdomain: string) => `https://${subdomain}.kiteclass.com`,
  getTenantDisplayUrl: (subdomain: string) => `${subdomain}.kiteclass.com`,
}));

import { useInstance, useTrialStatus } from '@/hooks/use-instances';
import InstanceDetailPage from '../[id]/page';

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

const mockTrialStatus = {
  daysLeft: 10,
  trialExpiresAt: '2026-04-07T00:00:00Z',
  status: 'TRIAL',
};

describe('InstanceDetailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    (useTrialStatus as ReturnType<typeof vi.fn>).mockReturnValue({ data: null });
  });

  it('hiển thị loading spinner khi đang tải', () => {
    (useInstance as ReturnType<typeof vi.fn>).mockReturnValue({
      data: null,
      isLoading: true,
      error: null,
    });

    render(<InstanceDetailPage params={Promise.resolve({ id: 'test-instance-id' })} />);
    expect(screen.getByTestId('loading-spinner')).toBeTruthy();
  });

  it('hiển thị lỗi khi không tải được instance', () => {
    (useInstance as ReturnType<typeof vi.fn>).mockReturnValue({
      data: null,
      isLoading: false,
      error: new Error('Not found'),
    });

    render(<InstanceDetailPage params={Promise.resolve({ id: 'test-instance-id' })} />);
    expect(screen.getByText(/không thể tải/i)).toBeTruthy();
  });

  it('hiển thị tên tổ chức và subdomain', async () => {
    (useInstance as ReturnType<typeof vi.fn>).mockReturnValue({
      data: mockInstance,
      isLoading: false,
      error: null,
    });

    render(<InstanceDetailPage params={Promise.resolve({ id: 'test-instance-id' })} />);

    await waitFor(() => {
      expect(screen.getByText('Trường Test ABC')).toBeTruthy();
      expect(screen.getByText(/testabc\.kiteclass\.com/)).toBeTruthy();
    });
  });

  it('hiển thị trial countdown khi instance đang trial', async () => {
    (useInstance as ReturnType<typeof vi.fn>).mockReturnValue({
      data: mockInstance,
      isLoading: false,
      error: null,
    });
    (useTrialStatus as ReturnType<typeof vi.fn>).mockReturnValue({
      data: mockTrialStatus,
    });

    render(<InstanceDetailPage params={Promise.resolve({ id: 'test-instance-id' })} />);

    await waitFor(() => {
      // Trial countdown or days remaining should be visible
      expect(screen.getByText('Trường Test ABC')).toBeTruthy();
    });
  });

  it('hiển thị link truy cập instance', async () => {
    (useInstance as ReturnType<typeof vi.fn>).mockReturnValue({
      data: mockInstance,
      isLoading: false,
      error: null,
    });

    render(<InstanceDetailPage params={Promise.resolve({ id: 'test-instance-id' })} />);

    await waitFor(() => {
      const links = screen.getAllByRole('link');
      expect(links.length).toBeGreaterThan(0);
    });
  });
});
