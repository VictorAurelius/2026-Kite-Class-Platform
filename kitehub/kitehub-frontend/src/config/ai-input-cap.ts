/**
 * AI Branding token-cap configuration — frontend mirror of backend
 * `AIInputCapConfig` (Java side) per `ai-branding-guidelines.md` §2.5.
 *
 * Wave 50 Bucket B (GAP-272): centralizes per-tier token cap so the wizard,
 * settings panels, and any future surface read a single source of truth
 * instead of duplicating literals.
 *
 * Backend rejects with HTTP 400 + `AI_INPUT_TOO_LONG` once estimated tokens
 * exceed the cap; FE labels here surface the same numbers in UI for user
 * clarity. Estimation heuristic (chars / 4) deliberately matches backend.
 */

import type { PricingTier } from '@/types/subscription';

/**
 * Per-tier estimated-token cap (rounded display values).
 * `-1` = unlimited (currently only ENTERPRISE Advanced Mode).
 */
export const TOKEN_CAPS: Record<PricingTier, number> = {
  FREE: 2000,
  BASIC: 4000,
  PREMIUM: 8000,
  ENTERPRISE: 16000,
};

/** Vietnamese-formatted display label for the wizard UI. */
export const TIER_CAP_LABEL: Record<PricingTier, string> = {
  FREE: '2.000',
  BASIC: '4.000',
  PREMIUM: '8.000',
  ENTERPRISE: '16.000',
};

/**
 * Tier-aware regenerate quota per session per
 * `ai-branding-guidelines.md` §4.3. `-1` = unlimited (ENTERPRISE).
 */
export const REGENERATE_QUOTA: Record<PricingTier, number> = {
  FREE: 3,
  BASIC: 10,
  PREMIUM: 30,
  ENTERPRISE: -1,
};

/**
 * Estimated-token heuristic (chars / 4) — matches backend
 * `PromptTokenEstimator` for parity. Vietnamese over-estimates slightly,
 * which is the desired fail-safe (cost-side, not UX-side).
 */
export function estimateTokens(text: string): number {
  return Math.ceil(text.length / 4);
}

/**
 * Returns true when the supplied text exceeds the tier cap.
 * `-1` cap (ENTERPRISE unlimited) always returns false.
 */
export function exceedsCap(tier: PricingTier, text: string): boolean {
  const cap = TOKEN_CAPS[tier];
  if (cap < 0) return false;
  return estimateTokens(text) > cap;
}
