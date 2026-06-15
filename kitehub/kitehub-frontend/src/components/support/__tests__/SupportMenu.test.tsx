/**
 * SupportMenu component tests — Wave 98 B6 GAP-660 Zalo OA fast-path.
 *
 * Verifies trigger render + accessibility. Dropdown content uses Radix
 * Portal so it is not part of the trigger DOM tree at render time — full
 * interaction coverage deferred to Playwright E2E (B7 scope).
 *
 * @since Wave 98 — GAP-660
 */

import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { SupportMenu, helpRouteFor } from '../SupportMenu';

describe('SupportMenu — Wave 98 B6 GAP-660 Zalo OA fast-path', () => {
  it('renders floating trigger button with aria-label', () => {
    render(<SupportMenu />);
    const trigger = screen.getByTestId('support-menu-trigger');
    expect(trigger).toBeInTheDocument();
    expect(trigger).toHaveAttribute('aria-label', 'Mở menu hỗ trợ');
  });

  it('trigger button has accessible touch target (min 44px per WCAG 2.5.5)', () => {
    render(<SupportMenu />);
    const trigger = screen.getByTestId('support-menu-trigger');
    // h-14 w-14 = 56px (above 44px minimum)
    expect(trigger.className).toMatch(/h-14/);
    expect(trigger.className).toMatch(/w-14/);
  });

  it('trigger has focus ring for WCAG 2.4.7 keyboard navigation', () => {
    render(<SupportMenu />);
    const trigger = screen.getByTestId('support-menu-trigger');
    expect(trigger.className).toMatch(/focus:ring-2/);
  });
});

describe('helpRouteFor — GAP-1394 role-based help routing', () => {
  it('routes anonymous phase to public help regardless of role', () => {
    expect(helpRouteFor('anonymous', undefined)).toBe('/help/anonymous');
    expect(helpRouteFor('anonymous', 'OWNER')).toBe('/help/anonymous');
  });

  it('routes unauthenticated (no role) to public help', () => {
    expect(helpRouteFor('steady', undefined)).toBe('/help/anonymous');
    expect(helpRouteFor(undefined, undefined)).toBe('/help/anonymous');
  });

  it('routes OWNER to the center-owner help page', () => {
    expect(helpRouteFor('steady', 'OWNER')).toBe('/help/p2-owner');
  });

  it('routes STAFF to the manager help page', () => {
    expect(helpRouteFor('day-1', 'STAFF')).toBe('/help/p3-manager');
  });

  it('routes PLATFORM_ADMIN and legacy ADMIN to the platform-admin help page', () => {
    expect(helpRouteFor('steady', 'PLATFORM_ADMIN')).toBe('/help/platform-admin');
    expect(helpRouteFor('steady', 'ADMIN')).toBe('/help/platform-admin');
  });

  it('is case/whitespace tolerant and falls back to owner help for unknown roles', () => {
    expect(helpRouteFor('steady', ' owner ')).toBe('/help/p2-owner');
    expect(helpRouteFor('steady', 'SOME_FUTURE_ROLE')).toBe('/help/p2-owner');
  });
});
