/**
 * Tests for 2FA Challenge page (Wave 72b Bucket B / GAP-516 FE).
 *
 * Consumes MSW handlers from `src/test/msw/handlers/auth.ts` (Bucket 0 Foundation).
 */
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';

const mockPush = vi.fn();
let searchParamsString = '?token=stub-challenge-token';

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush, replace: vi.fn() }),
  useSearchParams: () => new URLSearchParams(searchParamsString),
}));

vi.mock('@/lib/api/client', async () => {
  const axios = (await import('axios')).default;
  return {
    default: axios.create({ baseURL: 'http://localhost:9000' }),
  };
});

import TwoFactorChallengePage from '../page';

describe('TwoFactorChallengePage', () => {
  beforeEach(() => {
    mockPush.mockClear();
    localStorage.clear();
    sessionStorage.clear();
    searchParamsString = '?token=stub-challenge-token';
  });

  it('renders 6-digit TOTP input by default', () => {
    render(<TwoFactorChallengePage />);
    expect(screen.getByText(/Xác thực 2 lớp/i)).toBeInTheDocument();
    expect(screen.getByText(/Nhập mã 6 số từ app Authenticator/i)).toBeInTheDocument();
    expect(screen.getAllByRole('textbox').length).toBeGreaterThanOrEqual(6);
  });

  it('shows invalid-session view when token missing', () => {
    searchParamsString = '';
    render(<TwoFactorChallengePage />);
    expect(screen.getByText(/Phiên không hợp lệ/i)).toBeInTheDocument();
  });

  it('submits valid TOTP and redirects to /admin', async () => {
    render(<TwoFactorChallengePage />);
    const inputs = screen.getAllByRole('textbox') as HTMLInputElement[];
    for (let i = 0; i < 6; i += 1) {
      fireEvent.change(inputs[i]!, { target: { value: String((i + 1) % 10) } });
    }
    fireEvent.click(screen.getByRole('button', { name: /^Xác thực$/i }));

    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith('/admin');
    });
    // GAP-599 Wave 92 Bucket B: tokens persist in sessionStorage (per-tab isolation).
    expect(sessionStorage.getItem('accessToken')).toBe('stub-access-token-2fa-verified');
  });

  it('shows error on wrong TOTP (000000) and clears input', async () => {
    render(<TwoFactorChallengePage />);
    const inputs = screen.getAllByRole('textbox') as HTMLInputElement[];
    for (let i = 0; i < 6; i += 1) {
      fireEvent.change(inputs[i]!, { target: { value: '0' } });
    }
    fireEvent.click(screen.getByRole('button', { name: /^Xác thực$/i }));

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent(/Mã TOTP không đúng/i);
    });
    // Inputs cleared
    inputs.forEach((i) => expect(i.value).toBe(''));
    expect(mockPush).not.toHaveBeenCalled();
  });

  it('toggles to recovery-code mode and accepts valid recovery code', async () => {
    render(<TwoFactorChallengePage />);
    fireEvent.click(screen.getByRole('button', { name: /Dùng mã khôi phục thay TOTP/i }));

    await waitFor(() => {
      expect(screen.getByLabelText(/Mã khôi phục/i)).toBeInTheDocument();
    });

    const input = screen.getByLabelText(/Mã khôi phục/i) as HTMLInputElement;
    fireEvent.change(input, { target: { value: 'ab23cd45' } });
    fireEvent.click(screen.getByRole('button', { name: /^Xác thực$/i }));

    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith('/admin');
    });
    // GAP-599 Wave 92 Bucket B: tokens persist in sessionStorage (per-tab isolation).
    expect(sessionStorage.getItem('accessToken')).toBe('stub-access-token-recovery-used');
    // Recovery used → handler sets regenerate_recommended=true; we stored in sessionStorage
    expect(sessionStorage.getItem('recovery_codes_remaining')).toBe('9');
  });
});
