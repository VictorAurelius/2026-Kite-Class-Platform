import '@testing-library/jest-dom';
import { afterAll, afterEach, beforeAll } from 'vitest';

import { handlers } from './msw/handlers';
import { server } from './msw/server';

// MSW Node server lifecycle (Wave 34 Bucket 0 Foundation).
//
// Gated on handlers.length > 0 so Bucket 0 ships infrastructure WITHOUT
// disrupting existing tests that mock `global.fetch` directly via
// `vi.fn()` (e.g. use-theme-generation.test.ts). Once Bucket D populates
// `brandingHandlers`, the gate flips on and MSW intercepts route-matched
// requests; non-matched requests still pass through to vi-mocked fetch
// thanks to `onUnhandledRequest: 'bypass'`.
//
// When migrating an existing test from vi.fn-fetch to MSW, drop the
// `global.fetch = vi.fn()` line and add a `server.use(...)` per-test override.
if (handlers.length > 0) {
  beforeAll(() => server.listen({ onUnhandledRequest: 'bypass' }));
  afterEach(() => server.resetHandlers());
  afterAll(() => server.close());
}

// Mock pointer capture methods for Radix UI components
// See: https://github.com/radix-ui/primitives/issues/1822
if (typeof Element !== 'undefined') {
  Element.prototype.hasPointerCapture = () => false;
  Element.prototype.setPointerCapture = () => {};
  Element.prototype.releasePointerCapture = () => {};
}

// Mock ResizeObserver
global.ResizeObserver = class ResizeObserver {
  observe() {}
  unobserve() {}
  disconnect() {}
};

// Mock IntersectionObserver (required by framer-motion)
global.IntersectionObserver = class IntersectionObserver {
  readonly root: Element | null = null;
  readonly rootMargin: string = '';
  readonly thresholds: ReadonlyArray<number> = [];

  constructor() {}
  disconnect() {}
  observe() {}
  takeRecords(): IntersectionObserverEntry[] {
    return [];
  }
  unobserve() {}
} as any;

// Mock scrollIntoView
Element.prototype.scrollIntoView = () => {};
