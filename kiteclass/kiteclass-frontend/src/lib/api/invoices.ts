import { apiClient } from '@/lib/api-client';
import type { ApiResponse, PaginatedResponse } from '@/types/api';
import type {
  Invoice,
  CreateInvoiceRequest,
  ApplyAdjustmentRequest,
  InvoiceSearchParams,
  BatchInvoicePreviewResponse,
  BatchInvoiceConfirmResponse,
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

  /**
   * Preview batch hóa đơn học phí hàng tháng — KHÔNG persist (GAP-297).
   * Per api-contract.md §3.11: POST /api/v1/invoices/batch-generate?month=yyyy-MM
   */
  batchGenerate: async (
    month: string
  ): Promise<BatchInvoicePreviewResponse> => {
    const response = await apiClient.post<
      ApiResponse<BatchInvoicePreviewResponse>
    >('/api/v1/invoices/batch-generate', null, { params: { month } });
    return response.data.data!;
  },

  /**
   * Tạo (persist) batch hóa đơn học phí hàng tháng + phát InvoiceCreated event (GAP-297).
   * Per api-contract.md §3.12: POST /api/v1/invoices/batch-confirm?month=yyyy-MM
   */
  batchConfirm: async (
    month: string
  ): Promise<BatchInvoiceConfirmResponse> => {
    const response = await apiClient.post<
      ApiResponse<BatchInvoiceConfirmResponse>
    >('/api/v1/invoices/batch-confirm', null, { params: { month } });
    return response.data.data!;
  },
};
