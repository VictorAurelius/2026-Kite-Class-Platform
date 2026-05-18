/**
 * Tests for Login page 2FA branching (Wave 72b Bucket B / GAP-516 FE).
 *
 * Focuses on the new 2FA-branching logic added by Bucket B:
 *   - requires2fa: true → redirect to /2fa-challenge
 *   - requires2fa_enrollment: true → redirect to /2fa-setup
 *   - existing non-2FA login path still works
 *
 * Consumes MSW login handler from `src/test/msw/handlers/auth.ts`.
 */
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';

const mockPush = vi.fn();
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush, replace: vi.fn() }),
}));

vi.mock('@/lib/api/client', async () => {
  const axios = (await import('axios')).default;
  return {
    default: axios.create({ baseURL: 'http://localhost:9000' }),
  };
});

import LoginPage from '../page';

async function fillAndSubmit(email: string, password: string = 'Passw0rd!') {
  // Login form labels lack htmlFor; query by input name attribute via querySelector.
  const emailInput = document.querySelector('input[name="email"]') as HTMLInputElement;
  const passwordInput = document.querySelector('input[name="password"]') as HTMLInputElement;
  if (!emailInput || !passwordInput) throw new Error('Login inputs not found in DOM');
  fireEvent.change(emailInput, { target: { value: email } });
  fireEvent.change(passwordInput, { target: { value: password } });
  fireEvent.click(screen.getByRole('button', { name: /Đăng nhập/i }));
}

describe('LoginPage — 2FA branching (Wave 72b Bucket B)', () => {
  beforeEach(() => {
    mockPush.mockClear();
    localStorage.clear();
    sessionStorage.clear();
  });

  it('redirects to /2fa-challenge when response is requires2fa', async () => {
    render(<LoginPage />);
    await fillAndSubmit('admin@example.com');

    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith(
        expect.stringMatching(/^\/2fa-challenge\?token=stub-challenge-token-2fa-required$/)
      );
    });
    // GAP-599 Wave 92 Bucket B: tokens persist in sessionStorage (per-tab isolation).
    expect(sessionStorage.getItem('accessToken')).toBeNull();
  });

  it('redirects to /2fa-setup when response is requires2fa_enrollment', async () => {
    render(<LoginPage />);
    await fillAndSubmit('admin-first@example.com');

    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith(
        expect.stringMatching(/^\/2fa-setup\?token=stub-challenge-token-enrollment-required$/)
      );
    });
  });

  it('completes login without 2FA when no flags present', async () => {
    render(<LoginPage />);
    await fillAndSubmit('owner@example.com');

    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith('/dashboard');
    });
    // GAP-599 Wave 92 Bucket B: tokens persist in sessionStorage.
    expect(sessionStorage.getItem('accessToken')).toBe('stub-access-token-no-2fa');
  });

  it('shows lockout message on 423', async () => {
    render(<LoginPage />);
    await fillAndSubmit('locked@example.com');

    await waitFor(() => {
      expect(screen.getByText(/Tài khoản đã bị khóa tạm thời/i)).toBeInTheDocument();
    });
    expect(mockPush).not.toHaveBeenCalled();
  });

  // GAP-515 Wave 78 Bucket C — Retry-After UX: countdown timer + submit disabled.
  it('parses Retry-After header on 423 and renders countdown + disables submit', async () => {
    render(<LoginPage />);
    await fillAndSubmit('locked@example.com');

    // MSW handler stubs Retry-After: 900 (15 minutes). Initial countdown shows "15:00".
    const countdown = await screen.findByTestId('login-retry-countdown');
    expect(countdown).toBeInTheDocument();
    expect(countdown.textContent).toMatch(/15:00/);

    const submit = screen.getByTestId('login-submit');
    expect(submit).toBeDisabled();
    expect(submit.textContent).toMatch(/Tạm khóa/);
    expect(submit.textContent).toMatch(/15:00/);

    // Inline error message includes the countdown.
    const errorMsg = screen.getByTestId('login-error-message');
    expect(errorMsg.textContent).toMatch(/Thử lại sau 15:00/);
  });
});
