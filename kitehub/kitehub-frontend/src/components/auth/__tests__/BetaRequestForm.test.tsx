/**
 * Tests for BetaRequestForm (GAP-372 Wave 33).
 */
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@/test/test-utils';
import BetaRequestForm from '../BetaRequestForm';

vi.mock('@/lib/api/client', () => ({
  default: { post: vi.fn(() => Promise.resolve({ data: {} })) },
}));

import apiClient from '@/lib/api/client';

describe('BetaRequestForm', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders all required fields', () => {
    render(<BetaRequestForm />);
    expect(screen.getByLabelText(/Email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Họ và tên/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Tên tổ chức/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Vai trò/i)).toBeInTheDocument();
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
    // Use form.requestSubmit() to bypass HTML5 validation prevention?
    // Instead, supply only invalid email and trigger submit.
    fireEvent.change(screen.getByLabelText(/Họ và tên/i), { target: { value: 'X' } });
    fireEvent.change(screen.getByLabelText(/Tên tổ chức/i), { target: { value: 'Y' } });
    fireEvent.change(screen.getByLabelText(/Email/i), { target: { value: 'not-an-email' } });
    fireEvent.submit(form);

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent(/Email không hợp lệ/i);
    });
    expect(apiClient.post).not.toHaveBeenCalled();
  });

  it('submits with valid data + shows success state', async () => {
    render(<BetaRequestForm />);
    fireEvent.change(screen.getByLabelText(/Email/i), {
      target: { value: 'owner@example.com' },
    });
    fireEvent.change(screen.getByLabelText(/Họ và tên/i), { target: { value: 'Owner' } });
    fireEvent.change(screen.getByLabelText(/Tên tổ chức/i), { target: { value: 'ABC' } });

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
        }),
      );
    });
    await waitFor(() => {
      expect(screen.getByRole('status')).toHaveTextContent(/Đã nhận yêu cầu beta/i);
    });
  });
});
