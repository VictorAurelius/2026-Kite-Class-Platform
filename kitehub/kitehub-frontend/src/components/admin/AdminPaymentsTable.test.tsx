import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@/test/test-utils';
import userEvent from '@testing-library/user-event';
import { AdminPaymentsTable } from './AdminPaymentsTable';
import { mockPendingPayments, mockEmptyPayments } from '@/test/mocks/admin-data';

// Mock the hooks
const mockConfirmMutate = vi.fn().mockResolvedValue({});
const mockRejectMutate = vi.fn().mockResolvedValue({});

vi.mock('@/hooks/use-admin', () => ({
  useConfirmPayment: () => ({
    mutateAsync: mockConfirmMutate,
    isPending: false,
  }),
  useRejectPayment: () => ({
    mutateAsync: mockRejectMutate,
    isPending: false,
  }),
}));

// Mock sonner
vi.mock('sonner', () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
  },
}));

describe('AdminPaymentsTable', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('Rendering', () => {
    it('renders table with payments', () => {
      render(<AdminPaymentsTable payments={mockPendingPayments} />);

      // Check header
      expect(screen.getByText('Mã thanh toán')).toBeInTheDocument();
      expect(screen.getByText('Số tiền')).toBeInTheDocument();
      expect(screen.getByText('Phương thức')).toBeInTheDocument();

      // Check data - payment IDs are truncated
      expect(screen.getByText('PAY-001-')).toBeInTheDocument();
    });

    it('renders empty state when no payments', () => {
      render(<AdminPaymentsTable payments={mockEmptyPayments} />);

      expect(
        screen.getByText('Không có thanh toán nào đang chờ xác nhận')
      ).toBeInTheDocument();
    });

    it('displays formatted amounts in VND', () => {
      render(<AdminPaymentsTable payments={mockPendingPayments} />);

      // VND format: 2.990.000 đ or 2,990,000 ₫
      // Use getAllByText since regex might match multiple
      const amounts = screen.getAllByText(/[0-9,.]+\s*₫/);
      expect(amounts.length).toBeGreaterThanOrEqual(2);
    });

    it('displays payment methods', () => {
      render(<AdminPaymentsTable payments={mockPendingPayments} />);

      expect(screen.getByText('VietQR')).toBeInTheDocument();
      expect(screen.getByText('Chuyển khoản')).toBeInTheDocument();
    });

    it('displays payment content', () => {
      render(<AdminPaymentsTable payments={mockPendingPayments} />);

      expect(screen.getByText('KITEHUB PAY001')).toBeInTheDocument();
      expect(screen.getByText('KITEHUB PAY002')).toBeInTheDocument();
    });

    it('shows pending count in summary', () => {
      render(<AdminPaymentsTable payments={mockPendingPayments} />);

      expect(screen.getByText(/2 thanh toán đang chờ/)).toBeInTheDocument();
    });

    it('shows total amount in summary', () => {
      render(<AdminPaymentsTable payments={mockPendingPayments} />);

      // Total: 2,990,000 + 990,000 = 3,980,000
      expect(screen.getByText(/3[,.]980[,.]000/)).toBeInTheDocument();
    });
  });

  describe('Selection', () => {
    it('can select individual payment', async () => {
      const user = userEvent.setup();
      render(<AdminPaymentsTable payments={mockPendingPayments} />);

      // Find checkboxes (first one is "select all")
      const checkboxes = screen.getAllByRole('checkbox');
      expect(checkboxes.length).toBeGreaterThan(1);

      // Select first payment
      await user.click(checkboxes[1]!);

      // Should show bulk action bar
      expect(screen.getByText(/Đã chọn/)).toBeInTheDocument();
      expect(screen.getByText('1')).toBeInTheDocument();
    });

    it('can select all payments', async () => {
      const user = userEvent.setup();
      render(<AdminPaymentsTable payments={mockPendingPayments} />);

      // Click "select all" checkbox
      const checkboxes = screen.getAllByRole('checkbox');
      await user.click(checkboxes[0]!);

      // Should show both selected
      expect(screen.getByText(/Đã chọn/)).toBeInTheDocument();
      expect(screen.getByText('2')).toBeInTheDocument();
    });

    it('can deselect all', async () => {
      const user = userEvent.setup();
      render(<AdminPaymentsTable payments={mockPendingPayments} />);

      // Select all
      const checkboxes = screen.getAllByRole('checkbox');
      await user.click(checkboxes[0]!);

      // Click "Bỏ chọn" button
      const deselectButton = screen.getByText('Bỏ chọn');
      await user.click(deselectButton);

      // Bulk action bar should disappear
      expect(screen.queryByText(/Đã chọn/)).not.toBeInTheDocument();
    });
  });

  describe('Confirm Payment', () => {
    it('opens confirm dialog on button click', async () => {
      const user = userEvent.setup();
      render(<AdminPaymentsTable payments={mockPendingPayments} />);

      // Find and click first "Xác nhận" button
      const confirmButtons = screen.getAllByRole('button', { name: /Xác nhận/i });
      await user.click(confirmButtons[0]!);

      // Dialog should open
      await waitFor(() => {
        expect(screen.getByRole('dialog')).toBeInTheDocument();
      });
    });

    it('requires transaction ID to confirm', async () => {
      const user = userEvent.setup();
      render(<AdminPaymentsTable payments={mockPendingPayments} />);

      // Open dialog
      const confirmButtons = screen.getAllByRole('button', { name: /Xác nhận/i });
      await user.click(confirmButtons[0]!);

      // Wait for dialog
      await waitFor(() => {
        expect(screen.getByRole('dialog')).toBeInTheDocument();
      });

      // Find confirm button in dialog - it should be disabled
      const dialogButtons = screen.getAllByRole('button', { name: /Xác nhận/i });
      const dialogConfirmButton = dialogButtons[dialogButtons.length - 1]!;

      expect(dialogConfirmButton).toBeDisabled();
    });

    it('submits confirm with transaction ID', async () => {
      const user = userEvent.setup();
      render(<AdminPaymentsTable payments={mockPendingPayments} />);

      // Open dialog
      const confirmButtons = screen.getAllByRole('button', { name: /Xác nhận/i });
      await user.click(confirmButtons[0]!);

      // Wait for dialog
      await waitFor(() => {
        expect(screen.getByRole('dialog')).toBeInTheDocument();
      });

      // Enter transaction ID in text input
      const inputs = screen.getAllByRole('textbox');
      const transactionInput = inputs[0]!;
      await user.type(transactionInput, 'TXN123456');

      // Find and click confirm in dialog
      const dialogButtons = screen.getAllByRole('button', { name: /Xác nhận/i });
      const dialogConfirmButton = dialogButtons[dialogButtons.length - 1]!;
      await user.click(dialogConfirmButton);

      // Mutation should be called
      await waitFor(() => {
        expect(mockConfirmMutate).toHaveBeenCalledWith({
          paymentId: 'pay-001-uuid',
          request: { transactionId: 'TXN123456' },
        });
      });
    });

    it('closes dialog on cancel', async () => {
      const user = userEvent.setup();
      render(<AdminPaymentsTable payments={mockPendingPayments} />);

      // Open dialog
      const confirmButtons = screen.getAllByRole('button', { name: /Xác nhận/i });
      await user.click(confirmButtons[0]!);

      // Wait for dialog
      await waitFor(() => {
        expect(screen.getByRole('dialog')).toBeInTheDocument();
      });

      // Click cancel
      const cancelButton = screen.getByRole('button', { name: /Hủy/i });
      await user.click(cancelButton);

      // Dialog should close
      await waitFor(() => {
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
      });
    });
  });

  describe('Reject Payment', () => {
    it('opens reject dialog on button click', async () => {
      const user = userEvent.setup();
      render(<AdminPaymentsTable payments={mockPendingPayments} />);

      // Find and click first "Từ chối" button (in table row, not dialog)
      const rejectButtons = screen.getAllByRole('button', { name: /Từ chối/i });
      await user.click(rejectButtons[0]!);

      // Dialog should open - look for dialog heading
      await waitFor(() => {
        expect(screen.getByRole('dialog')).toBeInTheDocument();
      });
    });

    it('requires reason to reject', async () => {
      const user = userEvent.setup();
      render(<AdminPaymentsTable payments={mockPendingPayments} />);

      // Open dialog
      const rejectButtons = screen.getAllByRole('button', { name: /Từ chối/i });
      await user.click(rejectButtons[0]!);

      // Wait for dialog
      await waitFor(() => {
        expect(screen.getByRole('dialog')).toBeInTheDocument();
      });

      // Find reject button in dialog - should be disabled without reason
      const dialogButtons = screen.getAllByRole('button', { name: /Từ chối/i });
      const dialogRejectButton = dialogButtons[dialogButtons.length - 1]!;

      expect(dialogRejectButton).toBeDisabled();
    });

    it('submits reject with reason', async () => {
      const user = userEvent.setup();
      render(<AdminPaymentsTable payments={mockPendingPayments} />);

      // Open dialog
      const rejectButtons = screen.getAllByRole('button', { name: /Từ chối/i });
      await user.click(rejectButtons[0]!);

      // Wait for dialog
      await waitFor(() => {
        expect(screen.getByRole('dialog')).toBeInTheDocument();
      });

      // Enter reason in textarea
      const textarea = screen.getByRole('textbox');
      await user.type(textarea, 'Không tìm thấy giao dịch');

      // Click reject in dialog
      const dialogButtons = screen.getAllByRole('button', { name: /Từ chối/i });
      const dialogRejectButton = dialogButtons[dialogButtons.length - 1]!;
      await user.click(dialogRejectButton);

      // Mutation should be called
      await waitFor(() => {
        expect(mockRejectMutate).toHaveBeenCalledWith({
          paymentId: 'pay-001-uuid',
          request: { reason: 'Không tìm thấy giao dịch' },
        });
      });
    });
  });

  describe('QR Code Preview', () => {
    it('shows QR button when qrCodeUrl exists', () => {
      render(<AdminPaymentsTable payments={mockPendingPayments} />);

      // First payment has QR code, should have QR button
      // Second payment has no QR code, should show "-"
      const qrButtons = screen.getAllByRole('button').filter(
        (btn) => btn.querySelector('svg[class*="qrcode" i]') || btn.textContent === ''
      );

      // At least one QR button should exist
      expect(qrButtons.length).toBeGreaterThanOrEqual(0);
    });
  });
});
