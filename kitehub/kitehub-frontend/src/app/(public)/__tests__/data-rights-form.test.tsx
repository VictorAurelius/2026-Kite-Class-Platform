/**
 * Component tests for DSAR self-service intake form.
 *
 * @since Wave 26 Bucket A — GAP-353c
 */

import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, waitFor } from '@/test/test-utils';
import userEvent from '@testing-library/user-event';

import { DataRightsForm } from '../legal/data-rights/DataRightsForm';

describe('DataRightsForm', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('renders all 6 PDPL Art 14 right options', () => {
    render(<DataRightsForm />);
    expect(screen.getByLabelText(/Quyền truy cập/)).toBeInTheDocument();
    expect(screen.getByLabelText(/Quyền chỉnh sửa/)).toBeInTheDocument();
    expect(screen.getByLabelText(/Quyền xoá/)).toBeInTheDocument();
    expect(screen.getByLabelText(/Quyền chuyển dữ liệu/)).toBeInTheDocument();
    expect(screen.getByLabelText(/Quyền hạn chế xử lý/)).toBeInTheDocument();
    expect(screen.getByLabelText(/Quyền phản đối xử lý/)).toBeInTheDocument();
  });

  it('shows validation error when nationalIdLast4 is invalid', async () => {
    const user = userEvent.setup();
    render(<DataRightsForm />);

    await user.type(screen.getByLabelText(/Họ và tên/), 'Nguyen Test');
    await user.type(screen.getByLabelText(/Email \*/), 'subject@example.com');
    // Skip national_id_last4 — validation should fire
    await user.click(screen.getByRole('button', { name: /Gửi yêu cầu DSAR/ }));

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent(/4 chữ số/);
    });
  });

  it('submits valid form to /api/v1/dsar/request and shows success ticket', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        ticketId: '00000000-0000-0000-0000-000000000123',
        status: 'PENDING',
        slaDeadline: '2026-05-26T00:00:00Z',
      }),
      text: async () => '',
    });
    vi.stubGlobal('fetch', fetchMock);

    const user = userEvent.setup();
    render(<DataRightsForm />);

    await user.type(screen.getByLabelText(/Họ và tên/), 'Nguyen Test');
    await user.type(screen.getByLabelText(/Email \*/), 'subject@example.com');
    await user.type(screen.getByLabelText(/4 chữ số cuối/), '1234');
    await user.click(screen.getByRole('button', { name: /Gửi yêu cầu DSAR/ }));

    await waitFor(() => {
      // GAP-1438: form now posts to the gateway absolute URL (not a bare relative
      // path served by the FE origin) — assert the endpoint suffix.
      expect(fetchMock).toHaveBeenCalledWith(
        expect.stringContaining('/api/v1/dsar/request'),
        expect.objectContaining({ method: 'POST' }),
      );
    });

    await waitFor(() => {
      expect(screen.getByRole('status')).toHaveTextContent(/Đã ghi nhận yêu cầu DSAR/);
      expect(screen.getByRole('status')).toHaveTextContent('00000000-0000-0000-0000-000000000123');
    });

    const body = JSON.parse(fetchMock.mock.calls[0]![1]!.body as string);
    expect(body.rightType).toBe('ACCESS');
    expect(body.companyWebsite).toBe(''); // honeypot empty for legitimate submit
  });
});
