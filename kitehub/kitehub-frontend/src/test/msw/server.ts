/**
 * MSW Node server for Vitest. Wired into vitest.config.ts via setupFiles.
 *
 * Lifecycle hooks (listen / resetHandlers / close) are registered in
 * `src/test/setup.ts` so existing tests pick them up automatically.
 *
 * @author KiteHub Team
 * @since Wave 34 Bucket 0
 */

import { setupServer } from 'msw/node';

import { handlers } from './handlers';

export const server = setupServer(...handlers);
