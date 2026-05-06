/**
 * Invoice detail page integration tests — Wave 30 Bucket D.
 *
 * Verifies G6 `InvoiceDetail` + G10 `PaymentStatusTimeline` integration alongside
 * the existing controls (Pay / Apply late fee / Cancel).
 *
 * @since Wave 30 (GAP-266)
 */

import { describe, it, expect, vi } from 'vitest';
import { http, HttpResponse } from 'msw';
import { render, screen, waitFor } from '@/test/utils';
import { server } from '@/mocks/server';
import InvoiceDetailPage from '../page';

const BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

vi.mock('next/navigation', () => ({
  useParams: () => ({ id: '42' }),
  useRouter: () => ({ push: vi.fn(), back: vi.fn() }),
  usePathname: () => '/billing/42',
}));

const sampleInvoice = {
  id: 42,
  invoiceNumber: 'KC-2026-05-042',
  studentId: 101,
  classId: 11,
  enrollmentId: 1,
  status: 'PENDING',
  issueDate: '2026-05-01',
  dueDate: '2026-05-15',
  periodStart: '2026-05-01',
  periodEnd: '2026-05-31',
  subtotal: 5000000,
  discount: 200000,
  total: 4800000,
  amountPaid: 1000000,
  balanceDue: 3800000,
  items: [
    {
      id: 1,
      type: 'TUITION',
      description: 'Học phí tháng 5/2026',
      quantity: 1,
      unitPrice: 5000000,
      amount: 5000000,
    },
  ],
  adjustments: [
    {
      id: 1,
      type: 'DISCOUNT',
      description: 'Giảm giá anh chị em',
      amount: 200000,
    },
  ],
  createdAt: '2026-05-01T00:00:00Z',
  updatedAt: '2026-05-01T00:00:00Z',
};

const samplePayments = [
  {
    id: 1,
    paymentNumber: 'PMT-001',
    transactionId: 'tx-1',
    invoiceId: 42,
    amount: 1000000,
    paymentMethod: 'CASH',
    paymentStatus: 'COMPLETED',
    initiatedAt: '2026-05-02T10:00:00Z',
    completedAt: '2026-05-02T10:05:00Z',
  },
];

function mockEndpoints() {
  server.use(
    http.get(`${BASE_URL}/api/v1/invoices/42`, () =>
      HttpResponse.json({ data: sampleInvoice }),
    ),
    http.get(`${BASE_URL}/api/v1/payments/invoice/42`, () =>
      HttpResponse.json({ data: samplePayments }),
    ),
  );
}

describe('InvoiceDetailPage — G6 + G10 integration', () => {
  it('renders invoice header and integrates G6 InvoiceDetail panel', async () => {
    mockEndpoints();
    render(<InvoiceDetailPage />);

    await waitFor(() => {
      expect(
        screen.getByRole('heading', { level: 1, name: /KC-2026-05-042/ }),
      ).toBeInTheDocument();
    });

    // G6 InvoiceDetail mounted (test-id wrapper)
    expect(screen.getByTestId('invoice-detail-g6')).toBeInTheDocument();
  });

  it('integrates G10 PaymentStatusTimeline with derived events', async () => {
    mockEndpoints();
    render(<InvoiceDetailPage />);

    await waitFor(() => {
      expect(screen.getByTestId('invoice-payment-timeline')).toBeInTheDocument();
    });

    // Timeline section is labelled and present
    const timeline = screen.getByLabelText('Lịch sử thanh toán');
    expect(timeline).toBeInTheDocument();
  });

  it('preserves Pay action for pending invoices', async () => {
    mockEndpoints();
    render(<InvoiceDetailPage />);

    await waitFor(() => {
      expect(screen.getByRole('link', { name: /Thanh toán/ })).toBeInTheDocument();
    });
  });
});
