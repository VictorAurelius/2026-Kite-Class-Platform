/**
 * PaymentRecord API client — Wave beta-readiness-4 Bucket C / GAP-292b.
 *
 * Endpoints under /api/v1/invoices/{invoiceId}/...:
 *   POST /record-payment   — record CASH | BANK_TRANSFER | VIETQR | MOMO
 *   GET  /payment-records  — list all payments for invoice
 *
 * Idempotency-Key header recommended on POST to prevent FE double-submit.
 */

import { apiClient } from '@/lib/api-client';
import type { ApiResponse } from '@/types/api';
import type {
  PaymentRecord,
  RecordPaymentRequest,
} from '@/types/payment-record';

export const paymentRecordsApi = {
  /**
   * Record a manual payment received from parent/student.
   *
   * @param invoiceId Invoice being paid
   * @param data Payment details (method, amount, paidAt, note)
   * @param idempotencyKey Optional UUID v4 — prevent FE retry duplicate (BR-PAYMENT-METHOD-004)
   */
  record: async (
    invoiceId: number,
    data: RecordPaymentRequest,
    idempotencyKey?: string
  ): Promise<PaymentRecord> => {
    const headers: Record<string, string> = {};
    if (idempotencyKey) {
      headers['Idempotency-Key'] = idempotencyKey;
    }
    const response = await apiClient.post<ApiResponse<PaymentRecord>>(
      `/api/v1/invoices/${invoiceId}/record-payment`,
      data,
      { headers }
    );
    return response.data.data!;
  },

  /**
   * List all manual payment records for an invoice (current tenant scope).
   */
  list: async (invoiceId: number): Promise<PaymentRecord[]> => {
    const response = await apiClient.get<ApiResponse<PaymentRecord[]>>(
      `/api/v1/invoices/${invoiceId}/payment-records`
    );
    return response.data.data!;
  },
};
