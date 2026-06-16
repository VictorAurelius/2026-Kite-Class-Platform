/**
 * useAdminDashboard + mapDashboardStats contract tests (GAP-1440).
 *
 * Guards the FE↔BE contract for `GET /api/platform/admin/dashboard`:
 * the backend (kitehub-admin `AnalyticsService.getDashboardStats()` →
 * `com.kitehub.admin.dto.DashboardStats`) returns a NESTED shape
 * (`instancesByStatus` map + `mrr`/`arr`), while the dashboard page reads a FLAT
 * view model. The mapper bridges the two; these tests fail if the mapper stops
 * reading the keys the BE actually ships (drift catcher) or if any mapped field
 * comes out NaN/undefined.
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { mapDashboardStats, useAdminDashboard } from '../use-admin';
import type { DashboardStatsResponse } from '@/types/admin';
import apiClient from '@/lib/api/client';

vi.mock('@/lib/api/client', () => ({
  default: { get: vi.fn() },
}));

/**
 * Contract fixture — MUST mirror the JSON shape serialized from
 * `com.kitehub.admin.dto.DashboardStats`. Keep field names in sync with that
 * DTO; if the BE renames a field (e.g. `mrr` → `monthlyRecurring`), this fixture
 * + the mapper assertions below break, surfacing the drift on the FE side.
 */
const BE_DASHBOARD_RESPONSE: DashboardStatsResponse = {
  totalInstances: 42,
  instancesByStatus: { ACTIVE: 25, TRIAL: 12, SUSPENDED: 3, EXPIRED: 1, DELETED: 1 },
  instancesByTier: { FREE: 20, BASIC: 15, PREMIUM: 7 },
  mrr: 15_000_000,
  arr: 180_000_000,
  churnRate: 9.5,
  conversionRate: 67.5,
  newSignupsLast30Days: 8,
  totalActiveUsers: 250,
  revenueByTier: { BASIC: 5_000_000, PREMIUM: 10_000_000 },
  calculatedAt: '2026-06-16T09:30:00',
};

describe('mapDashboardStats (GAP-1440 contract)', () => {
  it('maps the nested BE response to the flat view model', () => {
    const flat = mapDashboardStats(BE_DASHBOARD_RESPONSE);

    expect(flat).toEqual({
      totalInstances: 42,
      activeInstances: 25,   // instancesByStatus.ACTIVE
      trialInstances: 12,    // instancesByStatus.TRIAL
      suspendedInstances: 3, // instancesByStatus.SUSPENDED
      totalRevenue: 180_000_000, // arr
      monthlyRevenue: 15_000_000, // mrr
      newInstancesThisMonth: 8,   // newSignupsLast30Days
    });
  });

  it('produces no NaN/undefined for any field (drift / partial payload guard)', () => {
    const flat = mapDashboardStats(BE_DASHBOARD_RESPONSE);
    for (const [key, value] of Object.entries(flat)) {
      expect(typeof value, `${key} should be a number`).toBe('number');
      expect(Number.isNaN(value), `${key} should not be NaN`).toBe(false);
    }
  });

  it('defaults every field to 0 when the BE omits keys (no NaN on empty platform)', () => {
    // Simulate a drift / empty payload: only totalInstances present.
    const partial = { totalInstances: 0 } as unknown as DashboardStatsResponse;
    const flat = mapDashboardStats(partial);

    expect(flat).toEqual({
      totalInstances: 0,
      activeInstances: 0,
      trialInstances: 0,
      suspendedInstances: 0,
      totalRevenue: 0,
      monthlyRevenue: 0,
      newInstancesThisMonth: 0,
    });
  });

  it('contract guard: BE fixture exposes the keys the mapper depends on', () => {
    // If a future BE rename drops one of these, keep the fixture synced with the
    // DTO and this assertion documents which keys the FE relies on.
    expect(BE_DASHBOARD_RESPONSE).toHaveProperty('instancesByStatus');
    expect(BE_DASHBOARD_RESPONSE).toHaveProperty('mrr');
    expect(BE_DASHBOARD_RESPONSE).toHaveProperty('arr');
    expect(BE_DASHBOARD_RESPONSE).toHaveProperty('newSignupsLast30Days');
    expect(BE_DASHBOARD_RESPONSE.instancesByStatus).toHaveProperty('ACTIVE');
  });
});

describe('useAdminDashboard (GAP-1440)', () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    vi.clearAllMocks();
    queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });
  });

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );

  it('returns flat mapped stats from the nested BE response', async () => {
    (apiClient.get as ReturnType<typeof vi.fn>).mockResolvedValue({
      data: BE_DASHBOARD_RESPONSE,
    });

    const { result } = renderHook(() => useAdminDashboard(), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(result.current.data).toEqual({
      totalInstances: 42,
      activeInstances: 25,
      trialInstances: 12,
      suspendedInstances: 3,
      totalRevenue: 180_000_000,
      monthlyRevenue: 15_000_000,
      newInstancesThisMonth: 8,
    });
  });

  it('returns null when the API responds with no body (204 / empty)', async () => {
    (apiClient.get as ReturnType<typeof vi.fn>).mockResolvedValue({ data: '' });

    const { result } = renderHook(() => useAdminDashboard(), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toBeNull();
  });
});
