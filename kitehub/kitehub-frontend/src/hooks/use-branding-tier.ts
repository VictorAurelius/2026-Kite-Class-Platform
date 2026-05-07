/**
 * useBrandingTier — consolidate tier-gating logic for AI Branding wizard v2.
 *
 * Wave 32 Bucket D — Direction C 6-step refactor (per
 * `documents/03-planning/waves/wave-2026-05-06-32-ai-branding-wizard-v2.md` §3
 * Bucket D + rework brief §4 Bucket D deltas).
 *
 * Returns the active tier + tier-derived flags consumed by:
 *   - `RegenerateCounter` (Step 6)   — tier comparison + quota
 *   - `TemplateStep`     (Step 5)    — Enterprise free-form prompt gate
 *   - `(customer)/settings/branding/advanced/page.tsx` — Advanced Mode opt-in gate
 *
 * Tier quotas codified per `ai-branding-guidelines.md` §4.3:
 *   FREE     → 3   regenerates / session
 *   BASIC    → 10  (treated as PRO equivalent)
 *   PREMIUM  → 30
 *   ENTERPRISE → unlimited (sentinel: -1)
 *
 * Real-tier sourcing:
 *   - Calls `useActiveSubscription(instanceId)` from `use-subscriptions.ts`.
 *   - Falls back to FREE only when subscription is loading OR endpoint absent
 *     (fail-safe per `ai-branding-guidelines.md` §2.5 "Unknown tier → fallback FREE cap").
 *   - Hook accepts `instanceId` so call sites can be explicit; nullable when wizard
 *     is mid-flow (no instance yet) → fallback FREE.
 *
 * Note (per rework brief §4 Bucket D mandate): Tier MUST be sourced from the
 * real `useActiveSubscription` hook — NOT hardcoded — to avoid recurrence of
 * v1 violation. If the hook is later split (e.g. a global tier context), the
 * call site here is the single point of change.
 */

import { useActiveSubscription } from './use-subscriptions';
import type { PricingTier } from '@/types/subscription';

export interface BrandingTierInfo {
  /** Active tier for the instance — falls back to FREE while loading or on error. */
  tier: PricingTier;
  /**
   * Regenerates allowed per session per tier (per `ai-branding-guidelines.md` §4.3).
   * `-1` sentinel = unlimited (ENTERPRISE).
   */
  regenerateQuota: number;
  /**
   * Whether ENTERPRISE Advanced Mode toggle is even visible.
   * Per `ai-branding-guidelines.md` §2.4: opt-in via Settings, NOT inline default.
   * The toggle itself starts OFF — this flag controls visibility, not state.
   */
  advancedModeEnabled: boolean;
  /**
   * Whether Step 5 may render the free-form custom-prompt input.
   * BANNED for FREE/BASIC/PRO/PREMIUM per §2.1; ENTERPRISE-only per §2.4.
   * Note: even ENTERPRISE only sees the input when `advancedModeEnabled` toggle is ON;
   * this flag here gates VISIBILITY of the toggle path, not the toggle's runtime state.
   */
  canUseCustomPrompt: boolean;
  /** True while subscription query is loading. */
  isLoading: boolean;
}

/**
 * Tier → regenerate quota lookup table.
 * Sentinel `-1` for ENTERPRISE = unlimited; consumers must guard `quota === -1`.
 */
const TIER_QUOTA: Record<PricingTier, number> = Object.freeze({
  FREE: 3,
  BASIC: 10,
  PREMIUM: 30,
  ENTERPRISE: -1,
}) as Record<PricingTier, number>;

/**
 * Returns tier-gating info for the active instance.
 *
 * @param instanceId  Active instance UUID. When null/undefined (e.g. mid-wizard
 *                    before instance creation), the hook returns FREE defaults
 *                    so consumers can always render guarded UI safely.
 */
export function useBrandingTier(instanceId: string | undefined): BrandingTierInfo {
  const { data: subscription, isLoading } = useActiveSubscription(instanceId);

  // Fail-safe per `ai-branding-guidelines.md` §2.5: unknown tier → FREE.
  // While loading we also assume FREE — the strictest possible gate so the UI
  // never accidentally exposes ENTERPRISE-only controls to a non-Enterprise tenant.
  const tier: PricingTier = subscription?.tier ?? 'FREE';

  const regenerateQuota = TIER_QUOTA[tier];
  const isEnterprise = tier === 'ENTERPRISE';

  return {
    tier,
    regenerateQuota,
    advancedModeEnabled: isEnterprise,
    canUseCustomPrompt: isEnterprise,
    isLoading,
  };
}
