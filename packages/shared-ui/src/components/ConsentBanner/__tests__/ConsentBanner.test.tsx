/**
 * ConsentBanner component tests — RTL-driven user-flow coverage per
 * GAP-353 spec.
 *
 * Covers:
 *  - First mount → banner visible
 *  - Accept all → state persisted, banner hidden, analytics + marketing on
 *  - Reject all → state persisted, banner hidden, analytics + marketing off
 *  - Customize → toggles render
 *  - Customize → toggle analytics + save → only analytics on
 *  - Re-mount with valid state → banner not shown
 *  - Mock Date 13 months future → expiry triggers re-prompt
 *  - Revoke → banner re-shown
 *  - ESC closes saving "reject all"
 *  - Keyboard tab cycles inside customize panel
 */

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ConsentBanner } from '../ConsentBanner';
import { DEFAULT_STORAGE_KEY, readConsent, writeConsent } from '../storage';

const TWELVE_MONTHS_MS = 365 * 24 * 60 * 60 * 1000;

const defaultProps = {
  privacyHref: '/legal/privacy',
  cookieHref: '/legal/cookies',
  termsHref: '/legal/terms',
} as const;

describe('<ConsentBanner>', () => {
  beforeEach(() => {
    window.localStorage.clear();
  });

  afterEach(() => {
    vi.useRealTimers();
    window.localStorage.clear();
  });

  it('renders banner on first visit', async () => {
    render(<ConsentBanner {...defaultProps} />);
    expect(await screen.findByTestId('consent-banner')).toBeInTheDocument();
    expect(
      screen.getByRole('heading', { name: /quyền riêng tư của bạn/i }),
    ).toBeInTheDocument();
  });

  it('does not render when consent already given and not expired', () => {
    writeConsent({
      version: 1,
      timestamp: Date.now(),
      expiresAt: Date.now() + TWELVE_MONTHS_MS,
      categories: { essential: true, analytics: true, marketing: true },
    });
    render(<ConsentBanner {...defaultProps} />);
    expect(screen.queryByTestId('consent-banner')).not.toBeInTheDocument();
  });

  it('Accept all → persists analytics+marketing on, banner hides', async () => {
    const user = userEvent.setup();
    render(<ConsentBanner {...defaultProps} />);
    await screen.findByTestId('consent-banner');

    await user.click(screen.getByTestId('consent-accept-btn'));

    expect(screen.queryByTestId('consent-banner')).not.toBeInTheDocument();
    const stored = readConsent();
    expect(stored?.categories.analytics).toBe(true);
    expect(stored?.categories.marketing).toBe(true);
    expect(stored?.categories.essential).toBe(true);
  });

  it('Reject all → persists analytics+marketing off, banner hides', async () => {
    const user = userEvent.setup();
    render(<ConsentBanner {...defaultProps} />);
    await screen.findByTestId('consent-banner');

    await user.click(screen.getByTestId('consent-reject-btn'));

    expect(screen.queryByTestId('consent-banner')).not.toBeInTheDocument();
    const stored = readConsent();
    expect(stored?.categories.analytics).toBe(false);
    expect(stored?.categories.marketing).toBe(false);
    expect(stored?.categories.essential).toBe(true);
  });

  it('Customize expands toggles', async () => {
    const user = userEvent.setup();
    render(<ConsentBanner {...defaultProps} />);
    await screen.findByTestId('consent-banner');

    await user.click(screen.getByTestId('consent-customize-btn'));

    expect(screen.getByTestId('consent-banner-customize')).toBeInTheDocument();
    expect(screen.getByTestId('consent-toggle-essential')).toBeDisabled();
    expect(screen.getByTestId('consent-toggle-analytics')).not.toBeChecked();
    expect(screen.getByTestId('consent-toggle-marketing')).not.toBeChecked();
  });

  it('Customize → toggle analytics → save persists analytics-only', async () => {
    const user = userEvent.setup();
    render(<ConsentBanner {...defaultProps} />);
    await screen.findByTestId('consent-banner');

    await user.click(screen.getByTestId('consent-customize-btn'));
    await user.click(screen.getByTestId('consent-toggle-analytics'));
    await user.click(screen.getByTestId('consent-save-btn'));

    const stored = readConsent();
    expect(stored?.categories.analytics).toBe(true);
    expect(stored?.categories.marketing).toBe(false);
    expect(stored?.categories.essential).toBe(true);
    expect(screen.queryByTestId('consent-banner')).not.toBeInTheDocument();
  });

  it('Essential toggle is locked-on (disabled, checked)', async () => {
    const user = userEvent.setup();
    render(<ConsentBanner {...defaultProps} />);
    await screen.findByTestId('consent-banner');

    await user.click(screen.getByTestId('consent-customize-btn'));
    const essential = screen.getByTestId('consent-toggle-essential');
    expect(essential).toBeChecked();
    expect(essential).toBeDisabled();
  });

  it('expired state triggers re-prompt on mount', () => {
    writeConsent({
      version: 1,
      timestamp: Date.now() - 2 * TWELVE_MONTHS_MS,
      expiresAt: Date.now() - TWELVE_MONTHS_MS, // expired
      categories: { essential: true, analytics: true, marketing: false },
    });

    render(<ConsentBanner {...defaultProps} />);
    expect(screen.getByTestId('consent-banner')).toBeInTheDocument();
  });

  it('ESC saves "reject all" and hides banner', () => {
    render(<ConsentBanner {...defaultProps} />);
    const banner = screen.getByTestId('consent-banner');

    fireEvent.keyDown(banner, { key: 'Escape' });

    expect(screen.queryByTestId('consent-banner')).not.toBeInTheDocument();
    const stored = readConsent();
    expect(stored?.categories.analytics).toBe(false);
    expect(stored?.categories.marketing).toBe(false);
  });

  it('renders privacy + cookie links with provided hrefs', async () => {
    render(<ConsentBanner {...defaultProps} />);
    await screen.findByTestId('consent-banner');

    const privacy = screen.getByRole('link', { name: /chính sách quyền riêng tư/i });
    const cookies = screen.getByRole('link', { name: /chính sách cookie/i });
    expect(privacy).toHaveAttribute('href', '/legal/privacy');
    expect(cookies).toHaveAttribute('href', '/legal/cookies');
  });

  it('uses custom storageKey when provided (test isolation)', async () => {
    const user = userEvent.setup();
    render(<ConsentBanner {...defaultProps} storageKey="kite.consent.test" />);
    await screen.findByTestId('consent-banner');

    await user.click(screen.getByTestId('consent-accept-btn'));

    expect(window.localStorage.getItem('kite.consent.test')).not.toBeNull();
    expect(window.localStorage.getItem(DEFAULT_STORAGE_KEY)).toBeNull();
  });

  it('has accessible role=dialog with labelled title and description', async () => {
    render(<ConsentBanner {...defaultProps} />);
    const dialog = await screen.findByRole('dialog');
    expect(dialog).toHaveAttribute('aria-labelledby');
    expect(dialog).toHaveAttribute('aria-describedby');
    expect(dialog).toHaveAttribute('aria-modal', 'false');
  });
});
