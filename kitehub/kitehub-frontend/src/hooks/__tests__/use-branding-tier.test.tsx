import { describe, it, expect, vi } from 'vitest';
import { renderHook } from '@testing-library/react';
import type { PricingTier, Subscription } from '@/types/subscription';

const mockUseActiveSubscription = vi.fn();

vi.mock('../use-subscriptions', () => ({
  useActiveSubscription: (id: string | undefined) => mockUseActiveSubscription(id),
}));

import { useBrandingTier } from '../use-branding-tier';

function fakeSub(tier: PricingTier): Partial<Subscription> {
  return { tier, status: 'ACTIVE' };
}

describe('useBrandingTier', () => {
  it('falls back to FREE when subscription absent (loading or unknown tier)', () => {
    mockUseActiveSubscription.mockReturnValue({ data: undefined, isLoading: true });
    const { result } = renderHook(() => useBrandingTier('inst-x'));
    expect(result.current.tier).toBe('FREE');
    expect(result.current.regenerateQuota).toBe(3);
    expect(result.current.advancedModeEnabled).toBe(false);
    expect(result.current.canUseCustomPrompt).toBe(false);
    expect(result.current.isLoading).toBe(true);
  });

  it.each<[PricingTier, number, boolean]>([
    ['FREE', 3, false],
    ['BASIC', 10, false],
    ['PREMIUM', 30, false],
    ['ENTERPRISE', -1, true],
  ])(
    'maps %s tier → quota %d + advancedMode=%s',
    (tier, quota, isEnterprise) => {
      mockUseActiveSubscription.mockReturnValue({
        data: fakeSub(tier),
        isLoading: false,
      });
      const { result } = renderHook(() => useBrandingTier('inst-x'));
      expect(result.current.tier).toBe(tier);
      expect(result.current.regenerateQuota).toBe(quota);
      expect(result.current.advancedModeEnabled).toBe(isEnterprise);
      expect(result.current.canUseCustomPrompt).toBe(isEnterprise);
    }
  );

  it('returns FREE defaults when instanceId is undefined (mid-wizard)', () => {
    mockUseActiveSubscription.mockReturnValue({ data: undefined, isLoading: false });
    const { result } = renderHook(() => useBrandingTier(undefined));
    expect(result.current.tier).toBe('FREE');
    expect(result.current.advancedModeEnabled).toBe(false);
  });
});
