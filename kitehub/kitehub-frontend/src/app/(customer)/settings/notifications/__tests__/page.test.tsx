/**
 * Notification preferences page tests (Wave 18a Bucket B — GAP-063 Phase 1).
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen } from '@testing-library/react';
import { render } from '@/__tests__/test-utils';
import NotificationPreferencesPage from '../page';
import type { NotificationPreferenceListResponse } from '@/types/notification-preference';

vi.mock('@/hooks/use-notification-preferences', () => ({
  useNotificationPreferences: vi.fn(),
  useUpdateNotificationPreference: vi.fn(),
}));

import {
  useNotificationPreferences,
  useUpdateNotificationPreference,
} from '@/hooks/use-notification-preferences';

const mockData: NotificationPreferenceListResponse = {
  preferences: [
    { notificationType: 'ABSENCE', enabledChannels: ['EMAIL'], mandatory: false },
    { notificationType: 'BILLING_INVOICE', enabledChannels: ['EMAIL'], mandatory: true },
  ],
};

describe('NotificationPreferencesPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    (useUpdateNotificationPreference as ReturnType<typeof vi.fn>).mockReturnValue({
      mutateAsync: vi.fn(),
      isPending: false,
    });
  });

  it('renders loading state', () => {
    (useNotificationPreferences as ReturnType<typeof vi.fn>).mockReturnValue({
      data: undefined,
      isLoading: true,
      error: null,
    });

    render(<NotificationPreferencesPage />);

    expect(screen.getByTestId('loading-spinner')).toBeInTheDocument();
  });

  it('renders error state', () => {
    (useNotificationPreferences as ReturnType<typeof vi.fn>).mockReturnValue({
      data: undefined,
      isLoading: false,
      error: new Error('boom'),
    });

    render(<NotificationPreferencesPage />);

    expect(
      screen.getByText('Không thể tải tùy chọn thông báo. Vui lòng thử lại.')
    ).toBeInTheDocument();
  });

  it('renders preferences and marks mandatory rows', () => {
    (useNotificationPreferences as ReturnType<typeof vi.fn>).mockReturnValue({
      data: mockData,
      isLoading: false,
      error: null,
    });

    render(<NotificationPreferencesPage />);

    expect(screen.getByText('Tùy chọn thông báo')).toBeInTheDocument();
    expect(screen.getByText('Vắng mặt')).toBeInTheDocument();
    expect(screen.getByText('Hóa đơn thanh toán')).toBeInTheDocument();
    expect(screen.getByText('Bắt buộc')).toBeInTheDocument();
  });

  it('renders SMS / Zalo / Push as Sắp có (Phase 1 placeholders)', () => {
    (useNotificationPreferences as ReturnType<typeof vi.fn>).mockReturnValue({
      data: mockData,
      isLoading: false,
      error: null,
    });

    render(<NotificationPreferencesPage />);

    // BR-NOTIF-002: future channels visible but disabled with "Sắp có"
    const comingSoonLabels = screen.getAllByText('(Sắp có)');
    expect(comingSoonLabels.length).toBeGreaterThanOrEqual(3); // 3 future channels × N rows
  });
});
