/**
 * MSW browser worker for Storybook / dev runtime stub.
 *
 * Not wired into the Next.js app entrypoint by default — opt-in. Useful
 * when Storybook or a dev-only flow needs to short-circuit network
 * without the real backend running.
 *
 * @author KiteHub Team
 * @since Wave 34 Bucket 0
 */

import { setupWorker } from 'msw/browser';

import { handlers } from './handlers';

export const worker = setupWorker(...handlers);
