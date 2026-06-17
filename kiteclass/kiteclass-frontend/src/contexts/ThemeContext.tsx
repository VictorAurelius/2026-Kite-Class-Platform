/**
 * Theme Context and Provider
 *
 * Provides theme state management across the application.
 * Handles theme persistence, updates, and CSS variable application.
 *
 * @since PR-THEME-1
 */

'use client';

import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import type { ThemeConfig } from '@/lib/theme/types';
import { isThemeConfig } from '@/lib/theme/types';
import { DEFAULT_THEME, THEME_STORAGE_KEY } from '@/lib/theme/defaultTheme';
import { applyThemeVariables } from '@/lib/theme/utils';

/**
 * Theme context value interface.
 */
interface ThemeContextValue {
  /** Current theme configuration */
  theme: ThemeConfig;
  /** Update theme (persists to localStorage) */
  setTheme: (theme: ThemeConfig) => void;
  /** Reset to default theme (removes from localStorage) */
  resetTheme: () => void;
  /** True if currently using default theme */
  isDefaultTheme: boolean;
}

/**
 * Theme context.
 * Use useTheme() hook to access this context.
 */
const ThemeContext = createContext<ThemeContextValue | undefined>(undefined);

/**
 * Theme Provider Props
 */
interface ThemeProviderProps {
  /** Child components */
  children: React.ReactNode;
  /** Optional initial theme (for testing) */
  initialTheme?: ThemeConfig;
}

/**
 * Loads theme from localStorage.
 * Returns null if not found or invalid.
 *
 * @returns Stored theme or null
 */
function loadThemeFromStorage(): ThemeConfig | null {
  // SSR safety
  if (typeof window === 'undefined') {
    return null;
  }

  try {
    const stored = localStorage.getItem(THEME_STORAGE_KEY);
    if (!stored) {
      return null;
    }

    const parsed = JSON.parse(stored);

    // Validate structure
    if (!isThemeConfig(parsed)) {
      console.warn('Invalid theme in localStorage, using default');
      return null;
    }

    return parsed;
  } catch (error) {
    console.error('Failed to load theme from localStorage:', error);
    return null;
  }
}

/**
 * Saves theme to localStorage.
 *
 * @param theme - Theme to save
 */
function saveThemeToStorage(theme: ThemeConfig): void {
  // SSR safety
  if (typeof window === 'undefined') {
    return;
  }

  try {
    localStorage.setItem(THEME_STORAGE_KEY, JSON.stringify(theme));
  } catch (error) {
    console.error('Failed to save theme to localStorage:', error);
  }
}

/**
 * Removes theme from localStorage.
 */
function removeThemeFromStorage(): void {
  // SSR safety
  if (typeof window === 'undefined') {
    return;
  }

  try {
    localStorage.removeItem(THEME_STORAGE_KEY);
  } catch (error) {
    console.error('Failed to remove theme from localStorage:', error);
  }
}

/**
 * Checks if two themes are equal.
 * Deep comparison of theme objects.
 *
 * @param a - First theme
 * @param b - Second theme
 * @returns True if themes are equal
 */
function areThemesEqual(a: ThemeConfig, b: ThemeConfig): boolean {
  return JSON.stringify(a) === JSON.stringify(b);
}

/**
 * Theme Provider Component
 *
 * Provides theme context to all child components.
 * Handles theme persistence and CSS variable application.
 *
 * @example
 * ```tsx
 * <ThemeProvider>
 *   <App />
 * </ThemeProvider>
 * ```
 */
export function ThemeProvider({ children, initialTheme }: ThemeProviderProps) {
  // Initialize theme state
  const [theme, setThemeState] = useState<ThemeConfig>(() => {
    // Priority: initialTheme > localStorage > DEFAULT_THEME
    if (initialTheme) {
      return initialTheme;
    }

    const stored = loadThemeFromStorage();
    return stored || DEFAULT_THEME;
  });

  // Check if current theme is default
  const isDefaultTheme = areThemesEqual(theme, DEFAULT_THEME);

  // Apply theme variables when theme changes.
  // Skip the DEFAULT fallback: applying it sets inline `--theme-*` on
  // documentElement, which OVERRIDES the SSR-inline ThemeSync (<style
  // data-theme-sync>:root{...}</style>) per-tenant theme on the public landing —
  // so a tenant's green/blue/gold would flip to DEFAULT purple after hydration
  // (GAP: theme "reset về tím"). globals.css already provides the default and the
  // SSR ThemeSync provides the per-tenant theme; only apply an EXPLICIT theme
  // (localStorage restore or user setTheme / postMessage preview), which != DEFAULT.
  useEffect(() => {
    if (areThemesEqual(theme, DEFAULT_THEME)) return;
    applyThemeVariables(theme);
  }, [theme]);

  /**
   * Updates theme and persists to localStorage.
   */
  const setTheme = useCallback((newTheme: ThemeConfig) => {
    setThemeState(newTheme);
    saveThemeToStorage(newTheme);
  }, []);

  /**
   * Resets to default theme and removes from localStorage.
   */
  const resetTheme = useCallback(() => {
    setThemeState(DEFAULT_THEME);
    removeThemeFromStorage();
  }, []);

  const value: ThemeContextValue = {
    theme,
    setTheme,
    resetTheme,
    isDefaultTheme,
  };

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}

/**
 * Hook to access theme context.
 *
 * Must be used within a ThemeProvider.
 *
 * @returns Theme context value
 * @throws Error if used outside ThemeProvider
 *
 * @example
 * ```tsx
 * function MyComponent() {
 *   const { theme, setTheme } = useTheme();
 *
 *   return (
 *     <div style={{ color: theme.colors.primary }}>
 *       Themed content
 *     </div>
 *   );
 * }
 * ```
 */
export function useTheme(): ThemeContextValue {
  const context = useContext(ThemeContext);

  if (context === undefined) {
    throw new Error('useTheme must be used within a ThemeProvider');
  }

  return context;
}
