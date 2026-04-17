import { apiClient } from '@/lib/api-client';
import type { ApiResponse, PaginatedResponse } from '@/types/api';
import type {
  Invoice,
  CreateInvoiceRequest,
  ApplyAdjustmentRequest,
  InvoiceSearchParams,
} from '@/types/invoice';

export const invoicesApi = {
  getById: async (id: number): Promise<Invoice> => {
    const response = await apiClient.get<ApiResponse<Invoice>>(
      `/api/v1/invoices/${id}`
    );
    return response.data.data!;
  },

  getByStudent: async (
    params: InvoiceSearchParams
  ): Promise<PaginatedResponse<Invoice>> => {
    const response = await apiClient.get<
      ApiResponse<PaginatedResponse<Invoice>>
    >('/api/v1/invoices', { params });
    return response.data.data!;
  },

  create: async (data: CreateInvoiceRequest): Promise<Invoice> => {
    const response = await apiClient.post<ApiResponse<Invoice>>(
      '/api/v1/invoices',
      data
    );
    return response.data.data!;
  },

  applyAdjustment: async (
    id: number,
    data: ApplyAdjustmentRequest
  ): Promise<Invoice> => {
    const response = await apiClient.post<ApiResponse<Invoice>>(
      `/api/v1/invoices/${id}/adjustments`,
      data
    );
    return response.data.data!;
  },

  applyLateFees: async (id: number): Promise<Invoice> => {
    const response = await apiClient.post<ApiResponse<Invoice>>(
      `/api/v1/invoices/${id}/late-fees`
    );
    return response.data.data!;
  },

  getOverdue: async (
    params: InvoiceSearchParams
  ): Promise<PaginatedResponse<Invoice>> => {
    const response = await apiClient.get<
      ApiResponse<PaginatedResponse<Invoice>>
    >('/api/v1/invoices/overdue', { params });
    return response.data.data!;
  },

  cancel: async (id: number): Promise<Invoice> => {
    const response = await apiClient.put<ApiResponse<Invoice>>(
      `/api/v1/invoices/${id}/cancel`
    );
    return response.data.data!;
  },
};
