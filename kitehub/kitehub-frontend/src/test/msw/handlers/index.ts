/**
 * MSW handlers barrel — aggregates all per-domain handler arrays.
 *
 * Add new domain handler arrays here as they ship.
 *
 * @author KiteHub Team
 * @since Wave 34 Bucket 0
 */

import type { HttpHandler } from 'msw';

import { brandingHandlers } from './branding';

export const handlers: HttpHandler[] = [...brandingHandlers];

export { brandingHandlers };
