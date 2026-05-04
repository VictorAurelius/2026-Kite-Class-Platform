/**
 * Payroll types — Phase 1 (GAP-057 Phase 1, Wave 18a Bucket C).
 *
 * Phase 2 (GAP-057b) will extend with run/approve/pay request shapes.
 */

export type PayrollType = 'SALARY' | 'HOURLY' | 'COMMISSION' | 'HYBRID';

export type PayrollStatus = 'DRAFT' | 'APPROVED' | 'PAID';

export interface PayrollConfig {
  id: number;
  teacherId: number;
  type: PayrollType;
  hourlyRate: number | null;
  baseSalary: number | null;
  commissionPercent: number | null;
  gvcnAllowance: number | null;
}

export interface PayrollPeriod {
  id: number;
  teacherId: number;
  startDate: string; // ISO yyyy-MM-dd
  endDate: string;   // ISO yyyy-MM-dd
  hoursWorked: number | null;
  grossAmount: number;
  deductions: number;
  netAmount: number;
  status: PayrollStatus;
}

export interface PayrollPeriodSearchParams {
  teacherId?: number;
  startDate?: string;
  endDate?: string;
  page?: number;
  size?: number;
  sort?: string;
}

export interface PayrollConfigSearchParams {
  page?: number;
  size?: number;
  sort?: string;
}
