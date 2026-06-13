/**
 * Tests for the cross-product SSO client (KiteHub side) — GAP-1138.
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { issueSsoCode, buildKiteClassSsoCallbackUrl } from '../sso';
import { apiClient } from '@/lib/api/client';

vi.mock('@/lib/api/client', () => ({
  apiClient: { post: vi.fn() },
}));

describe('issueSsoCode', () => {
  beforeEach(() => vi.clearAllMocks());

  it('POSTs to the SSO issue-code endpoint and returns the code payload', async () => {
    (apiClient.post as unknown as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      data: { code: 'ONE-TIME-CODE', expiresIn: 60 },
    });

    const res = await issueSsoCode();

    expect(apiClient.post).toHaveBeenCalledWith('/api/v1/auth/sso/issue-code');
    expect(res).toEqual({ code: 'ONE-TIME-CODE', expiresIn: 60 });
  });
});

describe('buildKiteClassSsoCallbackUrl', () => {
  afterEach(() => vi.unstubAllEnvs());

  it('builds the callback URL on the default local KiteClass port with URL-encoded code', () => {
    const url = buildKiteClassSsoCallbackUrl('a b/c+d');
    expect(url).toBe('http://localhost:3000/sso/callback?code=a%20b%2Fc%2Bd');
  });

  it('uses NEXT_PUBLIC_KITECLASS_URL and strips a trailing slash', () => {
    vi.stubEnv('NEXT_PUBLIC_KITECLASS_URL', 'https://truong.kitehub.me/');
    expect(buildKiteClassSsoCallbackUrl('XYZ')).toBe(
      'https://truong.kitehub.me/sso/callback?code=XYZ',
    );
  });
});
