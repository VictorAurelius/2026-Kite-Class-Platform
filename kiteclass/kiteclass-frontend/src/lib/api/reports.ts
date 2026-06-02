/**
 * Reports API functions — Owner dashboard analytics (GAP-865).
 *
 * Consumes GAP-775 ReportController:
 *   - GET /api/v1/reports/revenue?months=N
 *   - GET /api/v1/reports/attendance?months=N
 *
 * Both endpoints are `hasRole('ADMIN')` (OWASP A01 role gate) and
 * tenant-scoped (X-Tenant-Id header attached by apiClient interceptor).
 */

import { apiClient } from '@/lib/api-client';
import type { ApiResponse } from '@/types/api';
import type { RevenueReport, AttendanceReport } from '@/types/report';

export const reportsApi = {
  /** Monthly revenue report over a trailing window (default 12 months). */
  getRevenue: async (months = 12): Promise<RevenueReport> => {
    const response = await apiClient.get<ApiResponse<RevenueReport>>(
      '/api/v1/reports/revenue',
      { params: { months } }
    );
    return response.data.data!;
  },

  /** Monthly attendance present-rate report over a trailing window (default 12). */
  getAttendance: async (months = 12): Promise<AttendanceReport> => {
    const response = await apiClient.get<ApiResponse<AttendanceReport>>(
      '/api/v1/reports/attendance',
      { params: { months } }
    );
    return response.data.data!;
  },
};
