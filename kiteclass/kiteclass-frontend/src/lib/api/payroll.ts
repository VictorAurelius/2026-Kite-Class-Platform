/**
 * Payroll API functions — Phase 1 (read-only).
 *
 * GAP-057 Phase 1 (Wave 18a Bucket C). Phase 2 (GAP-057b) will add
 * run/approve/pay/payslip-PDF/bank-export endpoints.
 */

import { apiClient } from '@/lib/api-client';
import type { ApiResponse, PaginatedResponse } from '@/types/api';
import type {
  PayrollConfig,
  PayrollConfigSearchParams,
  PayrollPeriod,
  PayrollPeriodSearchParams,
} from '@/types/payroll';

export const payrollApi = {
  /** Paged list of teacher payroll configs (admin view). */
  listConfigs: async (
    params: PayrollConfigSearchParams = {}
  ): Promise<PaginatedResponse<PayrollConfig>> => {
    const response = await apiClient.get<
      ApiResponse<PaginatedResponse<PayrollConfig>>
    >('/api/v1/admin/payroll/configs', { params });
    return response.data.data!;
  },

  /** Paged list of payroll periods, filterable by teacher + date range. */
  listPeriods: async (
    params: PayrollPeriodSearchParams = {}
  ): Promise<PaginatedResponse<PayrollPeriod>> => {
    const response = await apiClient.get<
      ApiResponse<PaginatedResponse<PayrollPeriod>>
    >('/api/v1/admin/payroll/periods', { params });
    return response.data.data!;
  },

  /** Single period detail. */
  getPeriod: async (id: number): Promise<PayrollPeriod> => {
    const response = await apiClient.get<ApiResponse<PayrollPeriod>>(
      `/api/v1/admin/payroll/periods/${id}`
    );
    return response.data.data!;
  },
};
