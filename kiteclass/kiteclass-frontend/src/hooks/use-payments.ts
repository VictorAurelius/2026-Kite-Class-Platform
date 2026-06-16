import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useRouter } from 'next/navigation';
import { useToast } from '@/hooks/use-toast';
import { paymentsApi } from '@/lib/api/payments';
import { paymentRecordsApi } from '@/lib/api/payment-records';
import type {
  CreatePaymentRequest,
  PaymentSearchParams,
} from '@/types/payment';
import type { AxiosError } from 'axios';

const PAYMENTS_KEY = 'payments';
const PAYMENT_RECORDS_KEY = 'payment-records';

export function usePayment(id: number) {
  return useQuery({
    queryKey: [PAYMENTS_KEY, id],
    queryFn: () => paymentsApi.getById(id),
    enabled: !!id,
  });
}

export function useInvoicePayments(invoiceId: number) {
  return useQuery({
    queryKey: [PAYMENTS_KEY, 'invoice', invoiceId],
    queryFn: () => paymentsApi.getByInvoice(invoiceId),
    enabled: !!invoiceId,
  });
}

/**
 * GAP-1433 — Manual payment records cho 1 hóa đơn.
 *
 * Khác `useInvoicePayments` (gateway/SePay payments qua `/payments/invoice/{id}`):
 * hook này đọc `/api/v1/invoices/{id}/payment-records` — nơi lưu phiếu thu thủ công
 * (tiền mặt / chuyển khoản / VietQR / MoMo) tạo qua `record-payment`. Billing
 * timeline merge cả hai nguồn để phiếu thu thủ công cũng hiển thị.
 */
export function useInvoicePaymentRecords(invoiceId: number) {
  return useQuery({
    queryKey: [PAYMENT_RECORDS_KEY, 'invoice', invoiceId],
    queryFn: () => paymentRecordsApi.list(invoiceId),
    enabled: !!invoiceId,
  });
}

export function usePendingPayments(params: PaymentSearchParams = {}) {
  return useQuery({
    queryKey: [PAYMENTS_KEY, 'pending', params],
    queryFn: () => paymentsApi.getPending(params),
  });
}

export function useCreatePayment() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const { toast } = useToast();

  return useMutation({
    mutationFn: (data: CreatePaymentRequest) => paymentsApi.create(data),
    onSuccess: (payment) => {
      queryClient.invalidateQueries({ queryKey: [PAYMENTS_KEY] });
      queryClient.invalidateQueries({ queryKey: ['invoices'] });

      // Redirect to payment detail or payment URL (online)
      if (payment.paymentUrl) {
        window.location.href = payment.paymentUrl; // Redirect to gateway
      } else {
        toast({ title: 'Thành công', description: 'Đã tạo thanh toán' });
        router.push(`/billing/payments/${payment.id}`);
      }
    },
    onError: (error: AxiosError<{ message?: string }>) => {
      toast({
        title: 'Lỗi',
        description:
          error.response?.data?.message || 'Không thể tạo thanh toán',
        variant: 'destructive',
      });
    },
  });
}
