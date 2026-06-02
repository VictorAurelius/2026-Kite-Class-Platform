/**
 * React Query hooks for Owner dashboard analytics reports (GAP-865).
 */

'use client';

import { useQuery } from '@tanstack/react-query';
import { reportsApi } from '@/lib/api/reports';

const REPORTS_QUERY_KEY = 'reports';

export function useRevenueReport(months = 12) {
  return useQuery({
    queryKey: [REPORTS_QUERY_KEY, 'revenue', months],
    queryFn: () => reportsApi.getRevenue(months),
  });
}

export function useAttendanceReport(months = 12) {
  return useQuery({
    queryKey: [REPORTS_QUERY_KEY, 'attendance', months],
    queryFn: () => reportsApi.getAttendance(months),
  });
}
