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
  // GAP-1374: labels are now associated via htmlFor/id, so we can query by
  // accessible label instead of raw name attribute.
  const emailInput = screen.getByLabelText('Email') as HTMLInputElement;
  const passwordInput = screen.getByLabelText('Mật khẩu') as HTMLInputElement;
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

  // GAP-1374 — login form a11y: label↔input association + autocomplete.
  it('associates labels with inputs and sets autocomplete (WCAG 1.3.1/1.3.5)', () => {
    render(<LoginPage />);
    const emailInput = screen.getByLabelText('Email') as HTMLInputElement;
    const passwordInput = screen.getByLabelText('Mật khẩu') as HTMLInputElement;

    expect(emailInput).toHaveAttribute('id', 'login-email');
    expect(emailInput).toHaveAttribute('autoComplete', 'email');
    expect(passwordInput).toHaveAttribute('id', 'login-password');
    expect(passwordInput).toHaveAttribute('autoComplete', 'current-password');
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
