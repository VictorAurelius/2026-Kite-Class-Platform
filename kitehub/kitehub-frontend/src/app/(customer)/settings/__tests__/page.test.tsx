/**
 * Settings Page Tests
 *
 * Tests for settings page with tabs.
 *
 * @since PR-Q4
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen } from '@testing-library/react';
import { render } from '@/__tests__/test-utils';
import { mockUser, mockInstances } from '@/__tests__/mocks/data';
import SettingsPage from '../page';

// Mock stores and hooks
vi.mock('@/stores/auth-store', () => ({
  useAuthStore: vi.fn((selector) => selector({ user: mockUser })),
}));

vi.mock('@/hooks/use-instances', () => ({
  useOwnerInstances: vi.fn(),
}));

// Mock child components
vi.mock('../components/AccountTab', () => ({
  AccountTab: ({ user, organizationName }: { user?: typeof mockUser; organizationName?: string }) => (
    <div data-testid="account-tab">
      Account for {user?.email || 'no-email'} - {organizationName}
    </div>
  ),
}));

vi.mock('../components/InstanceTab', () => ({
  InstanceTab: ({ instance }: { instance?: typeof mockInstances[0] }) => (
    <div data-testid="instance-tab">
      Instance: {instance?.organizationName}
    </div>
  ),
}));

vi.mock('../components/DangerZone', () => ({
  DangerZone: ({ instance }: { instance?: typeof mockInstances[0] }) => (
    <div data-testid="danger-zone">
      Danger Zone for {instance?.id}
    </div>
  ),
}));

vi.mock('../components/CustomDomainTab', () => ({
  CustomDomainTab: ({ instance }: { instance?: typeof mockInstances[0] }) => (
    <div data-testid="custom-domain-tab">
      Custom Domain for {instance?.id}
    </div>
  ),
}));

// Import mocks
import { useOwnerInstances } from '@/hooks/use-instances';

describe('SettingsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders loading state', () => {
    (useOwnerInstances as ReturnType<typeof vi.fn>).mockReturnValue({
      data: undefined,
      isLoading: true,
      error: null,
    });

    render(<SettingsPage />);

    expect(screen.getByTestId('loading-spinner')).toBeInTheDocument();
  });

  it('renders error state', () => {
    (useOwnerInstances as ReturnType<typeof vi.fn>).mockReturnValue({
      data: undefined,
      isLoading: false,
      error: new Error('Network error'),
    });

    render(<SettingsPage />);

    expect(screen.getByText('Không thể tải thông tin cài đặt. Vui lòng thử lại.')).toBeInTheDocument();
  });

  it('renders settings page with tabs', () => {
    (useOwnerInstances as ReturnType<typeof vi.fn>).mockReturnValue({
      data: mockInstances,
      isLoading: false,
      error: null,
    });

    render(<SettingsPage />);

    expect(screen.getByText('Cài đặt')).toBeInTheDocument();
    expect(screen.getByText('Quản lý tài khoản và cấu hình instance của bạn')).toBeInTheDocument();
  });

  it('renders all tab triggers', () => {
    (useOwnerInstances as ReturnType<typeof vi.fn>).mockReturnValue({
      data: mockInstances,
      isLoading: false,
      error: null,
    });

    render(<SettingsPage />);

    // Check tab triggers exist (using role=tab from radix-ui)
    const tabs = screen.getAllByRole('tab');
    expect(tabs).toHaveLength(4);

    // Check for tab labels (some might be hidden on mobile)
    expect(screen.getByText('Tài khoản')).toBeInTheDocument();
    expect(screen.getByText('Instance')).toBeInTheDocument();
    expect(screen.getByText('Tên miền')).toBeInTheDocument();
    expect(screen.getByText('Nguy hiểm')).toBeInTheDocument();
  });

  it('renders account tab by default', () => {
    (useOwnerInstances as ReturnType<typeof vi.fn>).mockReturnValue({
      data: mockInstances,
      isLoading: false,
      error: null,
    });

    render(<SettingsPage />);

    // Account tab should be visible by default
    expect(screen.getByTestId('account-tab')).toBeInTheDocument();
    expect(screen.getByTestId('account-tab')).toHaveTextContent(mockUser.email);
  });

  it('passes correct props to AccountTab', () => {
    (useOwnerInstances as ReturnType<typeof vi.fn>).mockReturnValue({
      data: mockInstances,
      isLoading: false,
      error: null,
    });

    render(<SettingsPage />);

    const accountTab = screen.getByTestId('account-tab');
    expect(accountTab).toHaveTextContent(mockUser.email);
    expect(accountTab).toHaveTextContent(mockInstances[0]!.organizationName);
  });
});
