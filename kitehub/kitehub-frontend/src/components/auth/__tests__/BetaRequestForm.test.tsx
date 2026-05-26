/**
 * Tests for BetaRequestForm (GAP-372 Wave 33 + Wave 35 GAP-385 PDPL consent
 * + Wave 105 Bucket A A1 double-submit debounce).
 */
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@/test/test-utils';
import BetaRequestForm from '../BetaRequestForm';

vi.mock('@/lib/api/client', () => ({
  default: { post: vi.fn(() => Promise.resolve({ data: {} })) },
}));

// Wave beta-prep-1 Bucket F7 — multi-branch filter uses useRouter to redirect
// to /waitlist when branchCount > 1. Mock router for tests.
const pushMock = vi.fn();
vi.mock('next/navigation', () => ({
  useRouter: () => ({
    push: pushMock,
    replace: vi.fn(),
    back: vi.fn(),
    forward: vi.fn(),
    refresh: vi.fn(),
    prefetch: vi.fn(),
  }),
}));

import apiClient from '@/lib/api/client';

describe('BetaRequestForm', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    pushMock.mockClear();
  });

  it('renders all required fields', () => {
    render(<BetaRequestForm />);
    expect(screen.getByLabelText(/Email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Họ và tên/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Tên tổ chức/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Vai trò/i)).toBeInTheDocument();
    expect(screen.getByTestId('beta-consent-checkbox')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Gửi yêu cầu beta/i })).toBeInTheDocument();
  });

  it('renders the honeypot but hidden from accessibility tree', () => {
    render(<BetaRequestForm />);
    const honeypot = screen.getByTestId('beta-honeypot');
    expect(honeypot).toBeInTheDocument();
    expect(honeypot.getAttribute('aria-hidden')).toBe('true');
    expect(honeypot.getAttribute('tabindex')).toBe('-1');
  });

  it('shows validation error when email is empty', async () => {
    render(<BetaRequestForm />);
    const form = screen.getByRole('form', { name: /beta-request-form/i });
    fireEvent.change(screen.getByLabelText(/Họ và tên/i), { target: { value: 'X' } });
    fireEvent.change(screen.getByLabelText(/Tên tổ chức/i), { target: { value: 'Y' } });
    fireEvent.change(screen.getByLabelText(/Email/i), { target: { value: 'not-an-email' } });
    fireEvent.click(screen.getByTestId('beta-consent-checkbox'));
    fireEvent.submit(form);

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent(/Email không hợp lệ/i);
    });
    expect(apiClient.post).not.toHaveBeenCalled();
  });

  // GAP-385 — PDPL 2023 Art 11 consent enforcement
  it('disables submit button until consent checkbox is checked', () => {
    render(<BetaRequestForm />);
    const submit = screen.getByTestId('beta-submit') as HTMLButtonElement;
    expect(submit).toBeDisabled();

    fireEvent.click(screen.getByTestId('beta-consent-checkbox'));
    expect(submit).not.toBeDisabled();
  });

  // GAP-385 — defense in depth: even if button enabled bypassed, validate() catches
  it('blocks submit + shows consent error when checkbox unchecked', async () => {
    render(<BetaRequestForm />);
    fireEvent.change(screen.getByLabelText(/Email/i), {
      target: { value: 'owner@example.com' },
    });
    fireEvent.change(screen.getByLabelText(/Họ và tên/i), { target: { value: 'Owner' } });
    fireEvent.change(screen.getByLabelText(/Tên tổ chức/i), { target: { value: 'ABC' } });
    // consent NOT checked
    const form = screen.getByRole('form', { name: /beta-request-form/i });
    fireEvent.submit(form);

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent(
        /đồng ý.*Chính sách.*Điều khoản/i,
      );
    });
    expect(apiClient.post).not.toHaveBeenCalled();
  });

  it('submits with valid data + consent + sends consentGiven=true', async () => {
    render(<BetaRequestForm />);
    fireEvent.change(screen.getByLabelText(/Email/i), {
      target: { value: 'owner@example.com' },
    });
    fireEvent.change(screen.getByLabelText(/Họ và tên/i), { target: { value: 'Owner' } });
    fireEvent.change(screen.getByLabelText(/Tên tổ chức/i), { target: { value: 'ABC' } });
    fireEvent.click(screen.getByTestId('beta-consent-checkbox'));

    const form = screen.getByRole('form', { name: /beta-request-form/i });
    fireEvent.submit(form);

    await waitFor(() => {
      expect(apiClient.post).toHaveBeenCalledWith(
        '/api/v1/auth/request-beta-access',
        expect.objectContaining({
          email: 'owner@example.com',
          name: 'Owner',
          orgName: 'ABC',
          persona: 'P2_CENTER_OWNER',
          honeypot: '',
          consentGiven: true,
        }),
      );
    });
    await waitFor(() => {
      expect(screen.getByRole('status')).toHaveTextContent(/Đã nhận yêu cầu beta/i);
    });
  });

  // Wave 105 Bucket A — A1 double-submit hardening: FE debounce 1s
  it('debounces double-submit within 1s window (only 1 POST dispatched)', async () => {
    // Hold the POST promise so we can dispatch a 2nd submit while in-flight.
    let resolvePost: ((v: { data: object }) => void) | undefined;
    vi.mocked(apiClient.post).mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          resolvePost = resolve;
        }),
    );

    render(<BetaRequestForm />);
    fireEvent.change(screen.getByLabelText(/Email/i), {
      target: { value: 'race@example.com' },
    });
    fireEvent.change(screen.getByLabelText(/Họ và tên/i), { target: { value: 'Trần Thị Hồng' } });
    fireEvent.change(screen.getByLabelText(/Tên tổ chức/i), { target: { value: 'Trung tâm Sky Education' } });
    fireEvent.click(screen.getByTestId('beta-consent-checkbox'));

    const form = screen.getByRole('form', { name: /beta-request-form/i });
    // 2 rapid submits (double-click pattern)
    fireEvent.submit(form);
    fireEvent.submit(form);

    // Only 1 POST should have fired (loading state OR debounce window blocked #2)
    await waitFor(() => {
      expect(apiClient.post).toHaveBeenCalledTimes(1);
    });

    // Release the in-flight POST + verify success surface renders
    resolvePost?.({ data: {} });
    await waitFor(() => {
      expect(screen.getByRole('status')).toHaveTextContent(/Đã nhận yêu cầu beta/i);
    });
  });

  // Wave beta-prep-1 Bucket F7 — multi-branch filter per ADR-036
  describe('multi-branch filter (Bucket F7)', () => {
    it('renders branchCount field with default value 1', () => {
      render(<BetaRequestForm />);
      const branchInput = screen.getByTestId('beta-branch-count') as HTMLInputElement;
      expect(branchInput).toBeInTheDocument();
      expect(branchInput.value).toBe('1');
    });

    it('submits with branchCount=1 (single-branch path: POST dispatched)', async () => {
      render(<BetaRequestForm />);
      fireEvent.change(screen.getByLabelText(/Email/i), {
        target: { value: 'hong@skyedu.vn' },
      });
      fireEvent.change(screen.getByLabelText(/Họ và tên/i), {
        target: { value: 'Trần Thị Hồng' },
      });
      fireEvent.change(screen.getByLabelText(/Tên tổ chức/i), {
        target: { value: 'Trung tâm Anh ngữ Sky Education' },
      });
      // branchCount defaults to 1 — no change needed
      fireEvent.click(screen.getByTestId('beta-consent-checkbox'));
      fireEvent.submit(screen.getByRole('form', { name: /beta-request-form/i }));

      await waitFor(() => {
        expect(apiClient.post).toHaveBeenCalledWith(
          '/api/v1/auth/request-beta-access',
          expect.objectContaining({
            branchCount: 1,
          }),
        );
      });
      // No redirect happened
      expect(pushMock).not.toHaveBeenCalled();
    });

    it('redirects to /waitlist when branchCount > 1 (multi-branch path: NO POST)', async () => {
      render(<BetaRequestForm />);
      fireEvent.change(screen.getByLabelText(/Email/i), {
        target: { value: 'multi@example.com' },
      });
      fireEvent.change(screen.getByLabelText(/Họ và tên/i), {
        target: { value: 'Nguyễn Văn An' },
      });
      fireEvent.change(screen.getByLabelText(/Tên tổ chức/i), {
        target: { value: 'Trung tâm Đa Chi Nhánh' },
      });
      fireEvent.change(screen.getByTestId('beta-branch-count'), {
        target: { value: '3' },
      });
      fireEvent.click(screen.getByTestId('beta-consent-checkbox'));
      fireEvent.submit(screen.getByRole('form', { name: /beta-request-form/i }));

      await waitFor(() => {
        expect(pushMock).toHaveBeenCalledWith(
          '/waitlist?reason=multi-branch&branches=3',
        );
      });
      // No POST should have been dispatched
      expect(apiClient.post).not.toHaveBeenCalled();
    });
  });
});
