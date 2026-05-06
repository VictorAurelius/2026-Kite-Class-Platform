/**
 * Dashboard ThemeProvider — typed wrapper around next-themes.
 *
 * Mirror of Wave 30 KC primitive with KH-specific localStorage namespace.
 * The ONLY KC coupling in the source was the storage key string; we
 * re-namespace to `kitehub.dashboard.theme` so KC + KH coexist without
 * stepping on each other's persisted preference.
 *
 * Why a wrapper? next-themes exposes `theme: string | undefined`. Our domain
 * uses `ThemeMode = 'light' | 'dark'` (no 'system' for the dashboard chrome
 * to keep KPI tones predictable). This provider:
 *   1. Forces 2-state mode (light/dark)
 *   2. Persists choice via next-themes (already wired with localStorage in
 *      providers/ThemeProvider).
 *   3. Exposes `useDashboardTheme()` hook with typed `mode` + `toggle()`.
 *
 * Root layout already mounts ThemeProvider (next-themes); this component
 * exports the typed hook + a small client-only `<DashboardThemeBoundary>`
 * for SSR-safe render guards.
 */

'use client';

import { useTheme } from 'next-themes';
import { useCallback, useEffect, useState, type ReactNode } from 'react';
import type { ThemeMode } from './types';

const STORAGE_KEY = 'kitehub.dashboard.theme';

interface DashboardThemeContextValue {
  mode: ThemeMode;
  toggle: () => void;
  setMode: (mode: ThemeMode) => void;
  /** True after hydration completes (matches next-themes pattern). */
  ready: boolean;
}

/**
 * Hook surfacing typed theme controls.
 * Consumers should always check `ready` before rendering theme-aware content
 * to avoid SSR hydration mismatch.
 */
export function useDashboardTheme(): DashboardThemeContextValue {
  const { theme, resolvedTheme, setTheme } = useTheme();
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  const current: ThemeMode = (() => {
    const candidate = theme ?? resolvedTheme;
    return candidate === 'dark' ? 'dark' : 'light';
  })();

  const setMode = useCallback(
    (mode: ThemeMode) => {
      setTheme(mode);
      // Mirror to a stable namespaced key so non-next-themes consumers
      // (e.g. analytics, admin debug panel) can read the choice.
      try {
        if (typeof window !== 'undefined') {
          window.localStorage.setItem(STORAGE_KEY, mode);
        }
      } catch {
        // localStorage may be blocked (private mode) — silent fail acceptable.
      }
    },
    [setTheme],
  );

  const toggle = useCallback(() => {
    setMode(current === 'dark' ? 'light' : 'dark');
  }, [current, setMode]);

  return { mode: current, toggle, setMode, ready: mounted };
}

/**
 * Optional explicit boundary marker — useful when consumers want to ensure
 * a subtree only renders post-hydration. Pass-through children otherwise.
 */
export function DashboardThemeBoundary({
  children,
  fallback = null,
}: {
  children: ReactNode;
  fallback?: ReactNode;
}) {
  const { ready } = useDashboardTheme();
  if (!ready) return <>{fallback}</>;
  return <>{children}</>;
}

export const DASHBOARD_THEME_STORAGE_KEY = STORAGE_KEY;
