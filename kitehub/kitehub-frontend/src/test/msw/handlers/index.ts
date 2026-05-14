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
import { brandingHandlers } from './branding';

export const handlers: HttpHandler[] = [
  ...brandingHandlers,
  ...betaAccessHandlers,
  ...authHandlers,
];

export { authHandlers, betaAccessHandlers, brandingHandlers };
