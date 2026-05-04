/**
 * React Query hooks for payroll Phase 1 (read-only).
 *
 * GAP-057 Phase 1 (Wave 18a Bucket C).
 */

'use client';

import { useQuery } from '@tanstack/react-query';
import { payrollApi } from '@/lib/api/payroll';
import type {
  PayrollConfigSearchParams,
  PayrollPeriodSearchParams,
} from '@/types/payroll';

const PAYROLL_QUERY_KEY = 'payroll';

export function usePayrollConfigs(params: PayrollConfigSearchParams = {}) {
  return useQuery({
    queryKey: [PAYROLL_QUERY_KEY, 'configs', params],
    queryFn: () => payrollApi.listConfigs(params),
  });
}

export function usePayrollPeriods(params: PayrollPeriodSearchParams = {}) {
  return useQuery({
    queryKey: [PAYROLL_QUERY_KEY, 'periods', params],
    queryFn: () => payrollApi.listPeriods(params),
  });
}

export function usePayrollPeriod(id: number) {
  return useQuery({
    queryKey: [PAYROLL_QUERY_KEY, 'period', id],
    queryFn: () => payrollApi.getPeriod(id),
    enabled: !!id,
  });
}
