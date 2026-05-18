/**
 * Tests for 2FA Setup page (Wave 72b Bucket B / GAP-516 FE).
 *
 * Consumes MSW handlers from `src/test/msw/handlers/auth.ts` (Bucket 0 Foundation).
 */
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';

const mockPush = vi.fn();
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush, replace: vi.fn() }),
  useSearchParams: () => new URLSearchParams('?token=stub-challenge-enrollment'),
}));

// Mock api client to relay axios calls through MSW endpoints.
// MSW handlers expect plain fetch; apiClient uses axios with baseURL pointing to
// http://localhost:9000 — that's fine, MSW intercepts by path regex (`*/api/auth/...`).
vi.mock('@/lib/api/client', async () => {
  const axios = (await import('axios')).default;
  return {
    default: axios.create({ baseURL: 'http://localhost:9000' }),
  };
});

import TwoFactorSetupPage from '../page';

describe('TwoFactorSetupPage', () => {
  beforeEach(() => {
    mockPush.mockClear();
    // Clear any persisted store state between tests
    localStorage.clear();
  });

  it('shows loader while /enroll-init in flight', () => {
    render(<TwoFactorSetupPage />);
    expect(screen.getByText(/Đang khởi tạo phiên thiết lập 2FA/i)).toBeInTheDocument();
  });

  it('renders QR + recovery codes after /enroll-init success', async () => {
    render(<TwoFactorSetupPage />);
    await waitFor(() => {
      expect(
        screen.getByRole('heading', { name: /Quét mã QR bằng app Authenticator/i })
      ).toBeInTheDocument();
    });
    // Should render the 10 stub codes
    expect(screen.getByText('ab23cd45')).toBeInTheDocument();
    expect(screen.getByText('cd9efgh2')).toBeInTheDocument();
    // Continue button should be disabled until user acknowledges
    const continueBtn = screen.getByRole('button', { name: /Tiếp tục/i }) as HTMLButtonElement;
    expect(continueBtn).toBeDisabled();
  });

  it('enables continue after user checks "đã lưu mã"', async () => {
    render(<TwoFactorSetupPage />);
    await waitFor(() => {
      expect(
        screen.getByRole('heading', { name: /Quét mã QR/i })
      ).toBeInTheDocument();
    });
    const checkbox = screen.getByRole('checkbox', { name: /Tôi đã lưu mã khôi phục/i });
    fireEvent.click(checkbox);
    const continueBtn = screen.getByRole('button', { name: /Tiếp tục/i }) as HTMLButtonElement;
    expect(continueBtn).not.toBeDisabled();
  });

  it('advances to TOTP entry step and confirms on valid code', async () => {
    render(<TwoFactorSetupPage />);
    await waitFor(() => {
      expect(
        screen.getByRole('heading', { name: /Quét mã QR/i })
      ).toBeInTheDocument();
    });
    fireEvent.click(screen.getByRole('checkbox', { name: /Tôi đã lưu mã khôi phục/i }));
    fireEvent.click(screen.getByRole('button', { name: /Tiếp tục/i }));

    await waitFor(() => {
      expect(
        screen.getByRole('heading', { name: /Nhập mã 6 số/i })
      ).toBeInTheDocument();
    });

    const inputs = screen.getAllByRole('textbox');
    // 6 TOTP boxes
    expect(inputs.length).toBeGreaterThanOrEqual(6);
    for (let i = 0; i < 6; i += 1) {
      fireEvent.change(inputs[i]!, { target: { value: String((i + 1) % 10) } });
    }

    fireEvent.click(screen.getByRole('button', { name: /Xác nhận và kích hoạt 2FA/i }));

    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith('/admin');
    });
    // GAP-599 Wave 92 Bucket B: tokens persist in sessionStorage (per-tab isolation).
    expect(sessionStorage.getItem('accessToken')).toBe('stub-access-token-2fa-enrolled');
  });
});
