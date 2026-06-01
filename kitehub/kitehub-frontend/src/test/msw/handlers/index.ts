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
import { cookieConsentHandlers } from './cookie-consent';
import { feedbackHandlers } from './feedback';
import { onboardingHandlers } from './onboarding';
import { staffInvitationsHandlers } from './staff-invitations';
import { supportHandlers } from './support';
import { tenantHandlers } from './tenant';

export const handlers: HttpHandler[] = [
  ...brandingHandlers,
  ...betaAccessHandlers,
  ...betaStatusHandlers,
  ...authHandlers,
  ...feedbackHandlers,
  ...onboardingHandlers,
  ...supportHandlers,
  ...staffInvitationsHandlers,
  ...cookieConsentHandlers,
  ...tenantHandlers,
];

export {
  authHandlers,
  betaAccessHandlers,
  betaStatusHandlers,
  brandingHandlers,
  cookieConsentHandlers,
  feedbackHandlers,
  onboardingHandlers,
  staffInvitationsHandlers,
  supportHandlers,
  tenantHandlers,
};
