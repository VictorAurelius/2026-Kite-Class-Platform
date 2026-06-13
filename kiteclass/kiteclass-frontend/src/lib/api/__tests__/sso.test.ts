/**
 * Tests for the cross-product SSO exchange (KiteClass side) — GAP-1138.
 */

import { describe, it, expect, beforeEach, vi } from 'vitest';

// Bare SSO client (axios.create, no interceptor) used by exchangeSsoCode — see
// sso.ts. Hoisted so the module-load `axios.create(...)` returns this mock.
const { ssoPost } = vi.hoisted(() => ({ ssoPost: vi.fn() }));
vi.mock('axios', () => ({
  default: { create: () => ({ post: ssoPost }) },
}));

import { exchangeSsoCode } from '../sso';

describe('exchangeSsoCode', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    ssoPost.mockReset();
  });

  it('POSTs the one-time code to the exchange endpoint and returns the KH session', async () => {
    ssoPost.mockResolvedValueOnce({
      data: {
        accessToken: 'kh-access',
        refreshToken: 'kh-refresh',
        user: { id: 'u-1', email: 'hong.tran@skyedu.vn', name: 'Trần Thị Hồng', role: 'OWNER' },
      },
    });

    const res = await exchangeSsoCode('ONE-TIME-CODE');

    expect(ssoPost).toHaveBeenCalledWith('/api/v1/auth/sso/exchange', { code: 'ONE-TIME-CODE' });
    expect(res.accessToken).toBe('kh-access');
    expect(res.refreshToken).toBe('kh-refresh');
    expect(res.user.role).toBe('OWNER');
    expect(res.user.name).toBe('Trần Thị Hồng');
  });

  it('propagates a rejection (invalid / expired / replayed code → BE 401) to the caller', async () => {
    ssoPost.mockRejectedValueOnce(new Error('Request failed with status code 401'));

    await expect(exchangeSsoCode('expired-or-used')).rejects.toThrow();
  });
});
