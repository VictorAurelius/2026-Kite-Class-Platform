'use client';

/**
 * OnboardingCoordinator — sequences first-login UI reveal (Wave 98 GAP-656).
 *
 * Per GAP-656 §Proposed Fix Step 3: stagger reveal so only ONE of [banner,
 * onboarding modal, support menu] is the user's primary focus at a time.
 *
 * Priority order:
 *  1. Beta disclaimer banner (always-visible top of dashboard, dismissible)
 *  2. Onboarding modal (first-login phase, after banner dismissed OR
 *     parallel — banner stays as ambient bar, modal demands focus)
 *  3. SupportMenu `?` floating button (always-visible after onboarding closed)
 *
 * Outside-in audit F-NEW-4 (cognitive overload at first-login): banner +
 * onboarding modal + 2 floating widgets cùng mount → ~40% bounce. Solution:
 * SupportMenu (merges support + feedback) renders ONLY when onboarding modal
 * not active. Banner remains because it's slim + dismissible.
 *
 * @since Wave 98 — GAP-656
 */

import { useOnboardingPhase } from '@/hooks/useOnboardingPhase';
import { BetaDisclaimerBanner } from '@/components/beta-disclaimer/BetaDisclaimerBanner';
import { SupportMenu } from '@/components/support/SupportMenu';

export interface OnboardingCoordinatorProps {
  /** Whether onboarding modal is currently open (passed from parent layout). */
  onboardingModalOpen?: boolean;
  /** Callback when user opens feedback từ SupportMenu — B5 will wire to FeedbackForm. */
  onFeedbackClick?: () => void;
  /** Force-show banner for tests. */
  forceShowBanner?: boolean;
}

/**
 * Sequences UI surface reveal:
 *  - BetaDisclaimerBanner: always renders (dismissed state managed internally
 *    via cookie); slim top bar không block content
 *  - SupportMenu: renders ONLY when onboarding modal is closed → avoid
 *    floating button overlap với modal dismiss UX
 *
 * Onboarding modal itself is rendered by parent layout (OnboardingWizard
 * already exists per Wave 4); this coordinator only orchestrates
 * banner + support-menu visibility relative to modal state.
 */
export function OnboardingCoordinator({
  onboardingModalOpen = false,
  onFeedbackClick,
  forceShowBanner,
}: OnboardingCoordinatorProps) {
  const { phase } = useOnboardingPhase();

  return (
    <>
      {/* Banner — always-rendered (internal dismissal state via cookie) */}
      <BetaDisclaimerBanner forceShow={forceShowBanner} />

      {/* SupportMenu — only mount when onboarding modal closed (avoid focus collision) */}
      {!onboardingModalOpen && (
        <SupportMenu phase={phase} onFeedbackClick={onFeedbackClick} />
      )}
    </>
  );
}
