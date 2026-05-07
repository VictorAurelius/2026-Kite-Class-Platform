/**
 * Tests for BetaSignupForm (GAP-372 Wave 33).
 */
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@/test/test-utils';
import BetaSignupForm from '../BetaSignupForm';

vi.mock('@/lib/api/client', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(() => Promise.resolve({ data: {} })),
  },
}));

import apiClient from '@/lib/api/client';

describe('BetaSignupForm', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('shows error when token query param is missing', async () => {
    render(<BetaSignupForm token={null} />);
    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent(/không hợp lệ/i);
    });
    expect(apiClient.get).not.toHaveBeenCalled();
  });

  it('renders pre-fill data when token is valid', async () => {
    (apiClient.get as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      data: {
        valid: true,
        email: 'inv@example.com',
        name: 'Invitee',
        orgName: 'ABC Center',
        persona: 'P2_CENTER_OWNER',
      },
    });

    render(<BetaSignupForm token="abc-token" />);

    await waitFor(() => {
      expect(screen.getByText('inv@example.com')).toBeInTheDocument();
      expect(screen.getByText('ABC Center')).toBeInTheDocument();
    });
    expect(apiClient.get).toHaveBeenCalledWith(
      '/api/v1/auth/beta-signup/validate',
      expect.objectContaining({ params: { token: 'abc-token' } }),
    );
  });

  it('shows expired-token message when validation returns TOKEN_EXPIRED', async () => {
    (apiClient.get as ReturnType<typeof vi.fn>).mockRejectedValueOnce({
      response: { data: { valid: false, errorCode: 'TOKEN_EXPIRED' } },
    });

    render(<BetaSignupForm token="exp" />);

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent(/hết hạn/i);
    });
  });
});
