/**
 * Tests for BetaClaimCodeForm (GAP-609 Wave 91).
 *
 * 5 cases:
 *  1. < 6 digits → submit disabled
 *  2. Valid code 123456 → router.push to /beta-signup?token=<UUID>
 *  3. CODE_NOT_FOUND → Vietnamese error message
 *  4. CODE_EXPIRED → Vietnamese error message
 *  5. ALREADY_USED → Vietnamese error message
 */
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@/test/test-utils';
import userEvent from '@testing-library/user-event';
import BetaClaimCodeForm from '../BetaClaimCodeForm';

const pushMock = vi.fn();

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: pushMock }),
  useSearchParams: () => ({ get: () => null }),
}));

const postMock = vi.fn();
vi.mock('@/lib/api/client', () => ({
  default: {
    post: (...args: unknown[]) => postMock(...args),
  },
}));

describe('BetaClaimCodeForm', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('disables submit when code has fewer than 6 digits', async () => {
    const user = userEvent.setup();
    render(<BetaClaimCodeForm />);
    const input = screen.getByLabelText(/Mã invite/i);
    await user.type(input, '123');
    const button = screen.getByRole('button', { name: /Tiếp tục/i });
    expect(button).toBeDisabled();
    expect(postMock).not.toHaveBeenCalled();
  });

  it('redirects to /beta-signup?token=... on valid code', async () => {
    postMock.mockResolvedValueOnce({
      data: {
        valid: true,
        inviteToken: '00000000-0000-0000-0000-000000000001',
      },
    });
    const user = userEvent.setup();
    render(<BetaClaimCodeForm />);
    await user.type(screen.getByLabelText(/Mã invite/i), '123456');
    await user.click(screen.getByRole('button', { name: /Tiếp tục/i }));
    await waitFor(() => {
      expect(pushMock).toHaveBeenCalledWith(
        '/beta-signup?token=00000000-0000-0000-0000-000000000001',
      );
    });
    expect(postMock).toHaveBeenCalledWith(
      '/api/v1/auth/beta-signup/exchange-claim-code',
      { claimCode: '123456' },
    );
  });

  it('shows Vietnamese error message on CODE_NOT_FOUND', async () => {
    postMock.mockRejectedValueOnce({
      response: { data: { valid: false, errorCode: 'CODE_NOT_FOUND' } },
    });
    const user = userEvent.setup();
    render(<BetaClaimCodeForm />);
    await user.type(screen.getByLabelText(/Mã invite/i), '999999');
    await user.click(screen.getByRole('button', { name: /Tiếp tục/i }));
    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent(/không hợp lệ/i);
    });
    expect(pushMock).not.toHaveBeenCalled();
  });

  it('shows Vietnamese error message on CODE_EXPIRED', async () => {
    postMock.mockRejectedValueOnce({
      response: { data: { valid: false, errorCode: 'CODE_EXPIRED' } },
    });
    const user = userEvent.setup();
    render(<BetaClaimCodeForm />);
    await user.type(screen.getByLabelText(/Mã invite/i), '000000');
    await user.click(screen.getByRole('button', { name: /Tiếp tục/i }));
    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent(/hết hạn/i);
    });
  });

  it('shows Vietnamese error message on ALREADY_USED', async () => {
    postMock.mockRejectedValueOnce({
      response: { data: { valid: false, errorCode: 'ALREADY_USED' } },
    });
    const user = userEvent.setup();
    render(<BetaClaimCodeForm />);
    await user.type(screen.getByLabelText(/Mã invite/i), '111111');
    await user.click(screen.getByRole('button', { name: /Tiếp tục/i }));
    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent(/đã được sử dụng/i);
    });
  });
});
