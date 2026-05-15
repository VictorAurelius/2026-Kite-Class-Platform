/**
 * Unit tests for ConsentGatedAnalytics — PDPL Art 11 gate (GAP-558).
 *
 * Verifies:
 * - Renders nothing when not hydrated (SSR safety contract)
 * - Renders nothing when gaId is undefined
 * - Renders nothing when consent state is null (banner not yet dismissed)
 * - Renders nothing when consent rejected analytics
 * - Mounts <GoogleAnalytics> only when (hydrated && gaId && analytics === true)
 *
 * @since Wave 83 — GAP-558
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render } from '@/test/test-utils';
import { ConsentGatedAnalytics } from '../ConsentGatedAnalytics';

// Mock @next/third-parties — captures the gaId arg so we can assert mount.
let gaMountCalls: Array<{ gaId: string }> = [];
vi.mock('@next/third-parties/google', () => ({
  GoogleAnalytics: ({ gaId }: { gaId: string }) => {
    gaMountCalls.push({ gaId });
    return <div data-testid="ga-script-stub">{gaId}</div>;
  },
}));

// Mock @kite/shared-ui useConsent — return controllable state per test.
let consentMock: { hydrated: boolean; analytics: boolean };
vi.mock('@kite/shared-ui', () => ({
  useConsent: () => consentMock,
}));

describe('ConsentGatedAnalytics — PDPL Art 11 gate', () => {
  beforeEach(() => {
    gaMountCalls = [];
    consentMock = { hydrated: false, analytics: false };
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('renders nothing pre-hydration (SSR contract)', () => {
    consentMock = { hydrated: false, analytics: false };
    const { container } = render(<ConsentGatedAnalytics gaId="G-TEST123" />);
    expect(container.firstChild).toBeNull();
    expect(gaMountCalls).toHaveLength(0);
  });

  it('renders nothing when gaId is undefined', () => {
    consentMock = { hydrated: true, analytics: true };
    const { container } = render(<ConsentGatedAnalytics gaId={undefined} />);
    expect(container.firstChild).toBeNull();
    expect(gaMountCalls).toHaveLength(0);
  });

  it('renders nothing when analytics consent is false (banner pending OR rejected)', () => {
    consentMock = { hydrated: true, analytics: false };
    const { container } = render(<ConsentGatedAnalytics gaId="G-TEST123" />);
    expect(container.firstChild).toBeNull();
    expect(gaMountCalls).toHaveLength(0);
  });

  it('mounts GoogleAnalytics only when hydrated + gaId + analytics consented', () => {
    consentMock = { hydrated: true, analytics: true };
    const { getByTestId } = render(<ConsentGatedAnalytics gaId="G-TEST123" />);
    expect(getByTestId('ga-script-stub')).toBeInTheDocument();
    expect(gaMountCalls).toEqual([{ gaId: 'G-TEST123' }]);
  });
});
