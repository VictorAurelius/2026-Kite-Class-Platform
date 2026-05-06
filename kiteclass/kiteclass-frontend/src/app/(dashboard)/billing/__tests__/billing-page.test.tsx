/**
 * Billing list page tests — Wave 30 Bucket D.
 *
 * Verifies token-styled summary tiles, VN currency rendering via @kite/shared-ui
 * G6 `formatVNCurrency`, and table render with sample invoices.
 *
 * @since Wave 30 (GAP-266)
 */

import { describe, it, expect, vi } from 'vitest';
import { http, HttpResponse } from 'msw';
import { render, screen, waitFor } from '@/test/utils';
import { server } from '@/mocks/server';
import BillingPage from '../page';

const BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn(), back: vi.fn() }),
  usePathname: () => '/billing',
}));

const sampleInvoices = {
  content: [
    {
      id: 1,
      invoiceNumber: 'KC-2026-05-001',
      studentId: 101,
      classId: 11,
      enrollmentId: 1,
      status: 'PENDING',
      issueDate: '2026-05-01',
      dueDate: '2026-05-15',
      periodStart: '2026-05-01',
      periodEnd: '2026-05-31',
      subtotal: 5000000,
      discount: 0,
      total: 5000000,
      amountPaid: 0,
      balanceDue: 5000000,
      items: [],
      adjustments: [],
      createdAt: '2026-05-01T00:00:00Z',
      updatedAt: '2026-05-01T00:00:00Z',
    },
    {
      id: 2,
      invoiceNumber: 'KC-2026-05-002',
      studentId: 102,
      classId: 12,
      enrollmentId: 2,
      status: 'OVERDUE',
      issueDate: '2026-04-01',
      dueDate: '2026-04-15',
      periodStart: '2026-04-01',
      periodEnd: '2026-04-30',
      subtotal: 3000000,
      discount: 0,
      total: 3000000,
      amountPaid: 1000000,
      balanceDue: 2000000,
      items: [],
      adjustments: [],
      createdAt: '2026-04-01T00:00:00Z',
      updatedAt: '2026-04-15T00:00:00Z',
    },
  ],
  totalElements: 2,
  totalPages: 1,
  number: 0,
  size: 20,
};

describe('BillingPage (Wave 30 token-styled list)', () => {
  it('renders header + sample invoices and shows summary tiles', async () => {
    server.use(
      http.get(`${BASE_URL}/api/v1/invoices`, () =>
        HttpResponse.json({ data: sampleInvoices }),
      ),
    );

    render(<BillingPage />);

    expect(screen.getByRole('heading', { level: 1, name: 'Hóa đơn' })).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText('KC-2026-05-001')).toBeInTheDocument();
    });
    expect(screen.getByText('KC-2026-05-002')).toBeInTheDocument();

    // Summary tiles render — overdue count derived from rows
    const summary = screen.getByTestId('billing-summary');
    expect(summary).toBeInTheDocument();
    expect(summary).toHaveTextContent('Còn phải thu');
    expect(summary).toHaveTextContent('Đã thanh toán');
    expect(summary).toHaveTextContent('Quá hạn');
  });

  it('formats currency via @kite/shared-ui formatVNCurrency (VND)', async () => {
    server.use(
      http.get(`${BASE_URL}/api/v1/invoices`, () =>
        HttpResponse.json({ data: sampleInvoices }),
      ),
    );

    render(<BillingPage />);

    // 5,000,000 + 3,000,000 = 8,000,000 outstanding-eligible? No: balance = 5M + 2M = 7M.
    await waitFor(() => {
      // Currency strings include "đ" or non-breaking space; assert a VND-shape token.
      const summary = screen.getByTestId('billing-summary');
      expect(summary.textContent).toMatch(/7\D000\D000/); // 7,000,000 outstanding
    });
  });
});
