import { apiClient } from '@/lib/api-client';
import type { ApiResponse, PaginatedResponse } from '@/types/api';
import type {
  Payment,
  CreatePaymentRequest,
  PaymentSearchParams,
} from '@/types/payment';

export const paymentsApi = {
  create: async (data: CreatePaymentRequest): Promise<Payment> => {
    const response = await apiClient.post<ApiResponse<Payment>>(
      '/api/v1/payments',
      data
    );
    return response.data.data!;
  },

  getById: async (id: number): Promise<Payment> => {
    const response = await apiClient.get<ApiResponse<Payment>>(
      `/api/v1/payments/${id}`
    );
    return response.data.data!;
  },

  getByInvoice: async (invoiceId: number): Promise<Payment[]> => {
    const response = await apiClient.get<ApiResponse<Payment[]>>(
      `/api/v1/payments/invoice/${invoiceId}`
    );
    return response.data.data!;
  },

  getPending: async (
    params: PaymentSearchParams
  ): Promise<PaginatedResponse<Payment>> => {
    const response = await apiClient.get<
      ApiResponse<PaginatedResponse<Payment>>
    >('/api/v1/payments/pending', { params });
    return response.data.data!;
  },

  cancel: async (id: number): Promise<void> => {
    await apiClient.put(`/api/v1/payments/${id}/cancel`);
  },

  refund: async (id: number): Promise<void> => {
    await apiClient.post(`/api/v1/payments/${id}/refund`);
  },
};
