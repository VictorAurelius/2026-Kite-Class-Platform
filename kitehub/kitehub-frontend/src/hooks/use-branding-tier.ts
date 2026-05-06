'use client';

/**
 * useBrandingTier — Wave 32 Bucket D (GAP-272 ai-branding-wizard v2)
 *
 * Consolidates tier-gating for AI Branding:
 *   - Regenerate quotas per ai-branding-guidelines.md §4.3
 *   - Advanced Mode eligibility (ENTERPRISE only — §2.4)
 *   - Custom prompt eligibility
 *
 * Auth-store does NOT expose subscription tier directly (User has id/email/name/role).
 * TODO: replace mock with real subscription API call via useActiveSubscription once
 *       instanceId is available in the branding wizard context. Use FREE as safe fallback
 *       (per ai-branding-guidelines.md §2.5 "unknown tier → fallback FREE cap").
 */

import type { PricingTier } from '@/types/subscription';

/** Regenerate quota per tier — ai-branding-guidelines.md §4.3 */
const TIER_QUOTAS: Record<PricingTier, number> = {
  FREE: 3,
  BASIC: 10,        // maps to PRO in guidelines; BASIC is the project's tier enum
  PREMIUM: 30,
  ENTERPRISE: -1,   // -1 = unlimited
};

/** Input token cap per tier — ai-branding-guidelines.md §2.5 */
const TIER_TOKEN_CAPS: Record<PricingTier, number> = {
  FREE: 2000,
  BASIC: 4000,
  PREMIUM: 8000,
  ENTERPRISE: 16000,
};

export interface BrandingTierInfo {
  /** Current subscription tier. Defaults to FREE when unavailable. */
  tier: PricingTier;
  /** Total regenerates allowed this session (-1 = unlimited). */
  regenerateQuota: number;
  /** How many regenerates have been used this session. */
  regenerateUsed: number;
  /** How many remain. -1 if unlimited. */
  regenerateRemaining: number;
  /** Whether user has explicitly opted-in to Advanced Mode (ENTERPRISE only). */
  advancedModeEnabled: boolean;
  /** Whether user can submit free-form prompts (ENTERPRISE + advancedModeEnabled). */
  canUseCustomPrompt: boolean;
  /** Input token cap for this tier. */
  inputTokenCap: number;
  /** Enable / disable advanced mode toggle (ENTERPRISE only). */
  setAdvancedModeEnabled: (enabled: boolean) => void;
  /** Increment used count (call after each regenerate action). */
  incrementRegenerate: () => void;
  /** True when quota is exhausted. Always false for ENTERPRISE (-1). */
  isQuotaExhausted: boolean;
}

/**
 * Hook state stored in React state — scoped to the wizard session.
 * If the wizard unmounts and remounts, quota resets (per spec: "reset each session").
 */
import { useState, useCallback } from 'react';

/**
 * Resolve tier from env or API.
 * TODO(GAP-272): replace with `useActiveSubscription(instanceId).data?.tier`
 *               when branding wizard passes instanceId through context.
 */
function useResolveTier(): PricingTier {
  // During development, NEXT_PUBLIC_DEV_TIER can override for manual testing.
  const devTier = process.env.NEXT_PUBLIC_DEV_TIER as PricingTier | undefined;
  if (devTier && devTier in TIER_QUOTAS) return devTier;
  // Fallback: FREE (fail-safe per guidelines §2.5)
  return 'FREE';
}

export function useBrandingTier(): BrandingTierInfo {
  const tier = useResolveTier();
  const quota = TIER_QUOTAS[tier];

  const [regenerateUsed, setRegenerateUsed] = useState(0);
  const [advancedModeEnabled, setAdvancedModeEnabledState] = useState(false);

  const regenerateRemaining = quota === -1 ? -1 : Math.max(0, quota - regenerateUsed);
  const isQuotaExhausted = quota !== -1 && regenerateUsed >= quota;

  const setAdvancedModeEnabled = useCallback(
    (enabled: boolean) => {
      // Only ENTERPRISE may enable advanced mode — silent no-op for other tiers
      if (tier === 'ENTERPRISE') {
        setAdvancedModeEnabledState(enabled);
      }
    },
    [tier]
  );

  const incrementRegenerate = useCallback(() => {
    if (quota === -1) return; // unlimited — no tracking needed
    setRegenerateUsed((prev) => prev + 1);
  }, [quota]);

  return {
    tier,
    regenerateQuota: quota,
    regenerateUsed,
    regenerateRemaining,
    advancedModeEnabled,
    canUseCustomPrompt: tier === 'ENTERPRISE' && advancedModeEnabled,
    inputTokenCap: TIER_TOKEN_CAPS[tier],
    setAdvancedModeEnabled,
    incrementRegenerate,
    isQuotaExhausted,
  };
}
