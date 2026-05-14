/**
 * MSW handlers barrel — aggregates all per-domain handler arrays.
 *
 * Add new domain handler arrays here as they ship.
 *
 * @author KiteHub Team
 * @since Wave 34 Bucket 0
 */

import type { HttpHandler } from 'msw';

import { authHandlers } from './auth';
import { betaAccessHandlers } from './beta-access';
import { betaStatusHandlers } from './beta-status';
import { brandingHandlers } from './branding';
import { feedbackHandlers } from './feedback';
import { onboardingHandlers } from './onboarding';
import { supportHandlers } from './support';

export const handlers: HttpHandler[] = [
  ...brandingHandlers,
  ...betaAccessHandlers,
  ...betaStatusHandlers,
  ...authHandlers,
  ...feedbackHandlers,
  ...onboardingHandlers,
  ...supportHandlers,
];

export {
  authHandlers,
  betaAccessHandlers,
  betaStatusHandlers,
  brandingHandlers,
  feedbackHandlers,
  onboardingHandlers,
  supportHandlers,
};
