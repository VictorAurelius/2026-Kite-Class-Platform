/**
 * Tests for billing-pay page error state.
 * Verifies proper error UI when invoice not found instead of infinite loading.
 *
 * @since 2026-04-11
 */

import { describe, it, expect } from 'vitest';
import { render, screen, waitFor } from '@/test/utils';
import { server } from '@/mocks/server';
import { http, HttpResponse } from 'msw';
import CreatePaymentPage from '../page';

const BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

vi.mock('next/navigation', () => ({
  useParams: () => ({ id: '999' }),
  useRouter: () => ({ push: vi.fn(), back: vi.fn() }),
}));

describe('CreatePaymentPage error state', () => {
  it('shows error message when invoice not found (404)', async () => {
    server.use(
      http.get(`${BASE_URL}/api/v1/invoices/999`, () => {
        return HttpResponse.json({ message: 'Not found' }, { status: 404 });
      })
    );

    render(<CreatePaymentPage />);

    await waitFor(() => {
      expect(screen.getByText(/Không tìm thấy hóa đơn/i)).toBeInTheDocument();
    });

    expect(screen.getByRole('link', { name: /Quay lại danh sách/i })).toBeInTheDocument();
  });

  it('shows loading state initially', () => {
    server.use(
      http.get(`${BASE_URL}/api/v1/invoices/999`, async () => {
        await new Promise(() => {});
        return HttpResponse.json({});
      })
    );

    render(<CreatePaymentPage />);
    expect(screen.getByText(/Đang tải/i)).toBeInTheDocument();
  });
});
