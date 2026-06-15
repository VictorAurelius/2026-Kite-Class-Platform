/**
 * GAP-865 — Owner reports dashboard page tests.
 *
 * Covers:
 *  - Loading state (skeleton placeholders)
 *  - Happy path: 2 KPI cards (VND + percent VN format) + 2 charts render
 *  - Empty state: all-zero series → chart shows "Chưa có dữ liệu" hint
 *  - Error state: query error → user-visible error message
 *  - Role guard: non-admin user → "Không có quyền truy cập" notice
 *
 * Hooks + auth store are mocked (keeps test self-contained, no MSW dependency).
 *
 * @since GAP-865
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@/test/utils';
import { UserType } from '@/types/auth';
import type { RevenueReport, AttendanceReport } from '@/types/report';

// --- Mocks ---------------------------------------------------------------

const mockUseRevenueReport = vi.fn();
const mockUseAttendanceReport = vi.fn();
const mockUseAuthStore = vi.fn();

vi.mock('@/hooks/use-reports', () => ({
  useRevenueReport: () => mockUseRevenueReport(),
  useAttendanceReport: () => mockUseAttendanceReport(),
}));

vi.mock('@/stores/auth-store', () => ({
  // Mirror real zustand: support both `useAuthStore(selector)` (page role-guard)
  // and bare `useAuthStore()` (Header → useAuth destructures whole state).
  useAuthStore: (selector?: (s: unknown) => unknown) => {
    const state = mockUseAuthStore();
    return selector ? selector(state) : state;
  },
}));

import ReportsPage from '../page';

// --- Fixtures ------------------------------------------------------------

const ADMIN_USER = {
  id: 1,
  email: 'owner@skyedu.vn',
  name: 'Trần Thị Hồng',
  userType: UserType.ADMIN,
};

const TEACHER_USER = {
  ...ADMIN_USER,
  userType: UserType.TEACHER,
};

const REVENUE_OK: RevenueReport = {
  period: 'month',
  months: 12,
  totalRevenue: 15_000_000,
  points: [
    { month: '2025-07', amount: 0 },
    { month: '2026-06', amount: 1_500_000 },
  ],
};

const ATTENDANCE_OK: AttendanceReport = {
  period: 'month',
  months: 12,
  overallPresentRate: 92.5,
  points: [
    { month: '2025-07', presentCount: 0, totalCount: 0, presentRate: 0 },
    { month: '2026-06', presentCount: 37, totalCount: 40, presentRate: 92.5 },
  ],
};

const REVENUE_EMPTY: RevenueReport = {
  period: 'month',
  months: 12,
  totalRevenue: 0,
  points: [
    { month: '2025-07', amount: 0 },
    { month: '2026-06', amount: 0 },
  ],
};

const ATTENDANCE_EMPTY: AttendanceReport = {
  period: 'month',
  months: 12,
  overallPresentRate: 0,
  points: [
    { month: '2025-07', presentCount: 0, totalCount: 0, presentRate: 0 },
    { month: '2026-06', presentCount: 0, totalCount: 0, presentRate: 0 },
  ],
};

function ok<T>(data: T) {
  return { data, isLoading: false, error: null };
}
function loading() {
  return { data: undefined, isLoading: true, error: null };
}
function errored() {
  return { data: undefined, isLoading: false, error: new Error('boom') };
}

describe('GAP-865 — Reports dashboard page', () => {
  beforeEach(() => {
    mockUseAuthStore.mockReturnValue({ user: ADMIN_USER });
    mockUseRevenueReport.mockReturnValue(ok(REVENUE_OK));
    mockUseAttendanceReport.mockReturnValue(ok(ATTENDANCE_OK));
  });

  it('renders 2 KPI cards + 2 charts on happy path with VN format', () => {
    render(<ReportsPage />);

    expect(
      screen.getByRole('heading', { name: 'Báo cáo' })
    ).toBeInTheDocument();

    // Revenue KPI — VND format with đ suffix
    expect(screen.getByText(/15\.000\.000đ/)).toBeInTheDocument();
    // Attendance KPI — percent with VN decimal comma. GAP-1378: the same value
    // now also appears in the chart's sr-only data table, so >=1 occurrence.
    expect(screen.getAllByText('92,5%').length).toBeGreaterThanOrEqual(1);

    // Two chart card titles. GAP-1378: each label now also captions the chart's
    // sr-only data table, so assert >=1 occurrence.
    expect(screen.getAllByText('Doanh thu theo tháng').length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText('Tỷ lệ điểm danh theo tháng').length).toBeGreaterThanOrEqual(1);

    // GAP-1378: charts now expose their values via accessible data tables
    // (decorative SVG is aria-hidden) — 2 tables, named by their captions.
    expect(screen.getByRole('table', { name: 'Doanh thu theo tháng' })).toBeInTheDocument();
    expect(screen.getByRole('table', { name: 'Tỷ lệ điểm danh theo tháng' })).toBeInTheDocument();
  });

  it('shows loading skeletons while queries pending', () => {
    mockUseRevenueReport.mockReturnValue(loading());
    mockUseAttendanceReport.mockReturnValue(loading());

    render(<ReportsPage />);
    // Heading still renders; KPI values replaced by pulse placeholders (no SVG yet)
    expect(screen.getByRole('heading', { name: 'Báo cáo' })).toBeInTheDocument();
    expect(screen.queryByText(/15\.000\.000đ/)).not.toBeInTheDocument();
  });

  it('shows empty-state hint when all values are zero', () => {
    mockUseRevenueReport.mockReturnValue(ok(REVENUE_EMPTY));
    mockUseAttendanceReport.mockReturnValue(ok(ATTENDANCE_EMPTY));

    render(<ReportsPage />);
    expect(
      screen.getByText(/Chưa có dữ liệu doanh thu/)
    ).toBeInTheDocument();
    expect(
      screen.getByText(/Chưa có dữ liệu điểm danh/)
    ).toBeInTheDocument();
  });

  it('shows error message when a query fails', () => {
    mockUseRevenueReport.mockReturnValue(errored());
    mockUseAttendanceReport.mockReturnValue(errored());

    render(<ReportsPage />);
    expect(
      screen.getByText(/Không thể tải biểu đồ doanh thu/)
    ).toBeInTheDocument();
    expect(
      screen.getByText(/Không thể tải biểu đồ điểm danh/)
    ).toBeInTheDocument();
  });

  it('blocks non-admin user with a permission notice (FE role guard)', () => {
    mockUseAuthStore.mockReturnValue({ user: TEACHER_USER });

    render(<ReportsPage />);
    expect(
      screen.getByText('Không có quyền truy cập')
    ).toBeInTheDocument();
    // KPI / charts not rendered for non-admin
    expect(screen.queryByText('Doanh thu theo tháng')).not.toBeInTheDocument();
  });
});
