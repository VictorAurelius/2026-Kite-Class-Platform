/**
 * Barrel-export smoke test — Wave 31 Bucket A foundation contract.
 *
 * Buckets B/C/D rely on `@/_shared/dashboard-foundation` exporting these
 * named symbols. This test guards against accidental rename / deletion in
 * follow-up commits.
 */

import { describe, it, expect } from 'vitest';
import * as foundation from '../index';

describe('dashboard-foundation barrel', () => {
  it('exports the public API expected by Bucket B/C/D consumers', () => {
    // Components
    expect(typeof foundation.Sparkline).toBe('function');
    expect(typeof foundation.KPICard).toBe('function');
    expect(typeof foundation.CommandPalette).toBe('function');
    expect(typeof foundation.SuccessConfetti).toBe('function');
    expect(typeof foundation.DashboardThemeBoundary).toBe('function');

    // Hooks
    expect(typeof foundation.useCommandPalette).toBe('function');
    expect(typeof foundation.useDashboardTheme).toBe('function');

    // Imperative
    expect(typeof foundation.fireConfetti).toBe('function');

    // Constants + datasets
    expect(typeof foundation.DASHBOARD_THEME_STORAGE_KEY).toBe('string');
    expect(foundation.DASHBOARD_THEME_STORAGE_KEY).toBe('kitehub.dashboard.theme');
    expect(Array.isArray(foundation.KH_DASHBOARD_COMMANDS)).toBe(true);
    expect(foundation.KH_DASHBOARD_COMMANDS.length).toBeGreaterThan(0);
  });

  it('uses kitehub-namespaced storage key (not kiteclass)', () => {
    // Decision B (duplicate) — KH foundation must NOT collide with KC's
    // 'kiteclass.dashboard.theme' localStorage key. Re-namespacing was the
    // only behavioral delta vs Wave 30 KC primitives.
    expect(foundation.DASHBOARD_THEME_STORAGE_KEY).not.toContain('kiteclass');
    expect(foundation.DASHBOARD_THEME_STORAGE_KEY.startsWith('kitehub.')).toBe(true);
  });
});
