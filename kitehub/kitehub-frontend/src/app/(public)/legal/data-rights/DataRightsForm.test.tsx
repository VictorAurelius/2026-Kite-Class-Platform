/**
 * Tests for DataRightsForm (GAP-1438 Phase-3 Bucket A KH-8).
 *
 * Covers the two acceptance criteria:
 *  1. Submit reaches the gateway (NEXT_PUBLIC_API_URL / :9000), not the FE origin.
 *  2. Errors render a status-specific Vietnamese message — never a raw HTML body.
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@/test/test-utils';
import { DataRightsForm } from './DataRightsForm';

function fillValidForm() {
  fireEvent.change(screen.getByLabelText(/Họ và tên/i), {
    target: { value: 'Trần Thị Hồng' },
  });
  fireEvent.change(screen.getByLabelText(/^Email/i), {
    target: { value: 'hong.tran@skyedu.vn' },
  });
  fireEvent.change(screen.getByLabelText(/4 chữ số cuối/i), {
    target: { value: '1234' },
  });
}

describe('DataRightsForm', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('submits to the gateway base URL, not the FE origin (GAP-1438)', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({ ticketId: 'abc', status: 'PENDING', slaDeadline: '2026-07-01' }),
        { status: 201, headers: { 'content-type': 'application/json' } },
      ),
    );
    vi.stubGlobal('fetch', fetchMock);

    render(<DataRightsForm />);
    fillValidForm();
    fireEvent.click(screen.getByRole('button', { name: /Gửi yêu cầu/i }));

    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1));
    const calledUrl = String(fetchMock.mock.calls[0][0]);
    // Must hit the gateway absolute URL, not a bare relative path served by FE origin.
    expect(calledUrl).toMatch(/^https?:\/\//);
    expect(calledUrl).toContain('/api/v1/dsar/request');
    expect(calledUrl).not.toMatch(/^\/api\/v1\/dsar/); // not a bare relative path
  });

  it('renders the success surface with the ticket UUID on 201', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            ticketId: 'd1f2c3b4-0000-0000-0000-000000000000',
            status: 'PENDING',
            slaDeadline: '2026-07-01',
          }),
          { status: 201, headers: { 'content-type': 'application/json' } },
        ),
      ),
    );

    render(<DataRightsForm />);
    fillValidForm();
    fireEvent.click(screen.getByRole('button', { name: /Gửi yêu cầu/i }));

    await waitFor(() =>
      expect(screen.getByText(/Đã ghi nhận yêu cầu DSAR/i)).toBeInTheDocument(),
    );
    expect(
      screen.getByText('d1f2c3b4-0000-0000-0000-000000000000'),
    ).toBeInTheDocument();
  });

  it('shows a friendly Vietnamese 404 message — never the raw HTML body (GAP-1438)', async () => {
    const htmlBody = '<!DOCTYPE html><html><body>404: This page could not be found.</body></html>';
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(htmlBody, { status: 404, headers: { 'content-type': 'text/html' } }),
      ),
    );

    render(<DataRightsForm />);
    fillValidForm();
    fireEvent.click(screen.getByRole('button', { name: /Gửi yêu cầu/i }));

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent(/định tuyến/i); // routing-error message
    expect(alert).not.toHaveTextContent(/DOCTYPE|<html|could not be found/i); // no raw HTML
  });

  it('shows the JSON error message on a 400 validation response', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify({ message: 'Email đã được dùng cho yêu cầu khác' }), {
          status: 400,
          headers: { 'content-type': 'application/json' },
        }),
      ),
    );

    render(<DataRightsForm />);
    fillValidForm();
    fireEvent.click(screen.getByRole('button', { name: /Gửi yêu cầu/i }));

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent(/Email đã được dùng/i);
  });
});
