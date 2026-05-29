/**
 * Theme Utility Functions
 *
 * Utilities for applying, reading, and managing theme CSS variables.
 * All functions are SSR-safe (gracefully handle missing document).
 *
 * @since PR-THEME-1
 */

import type { ThemeConfig } from './types';
import { THEME_CSS_VARS, DEFAULT_THEME } from './defaultTheme';

/**
 * Merge partial theme with defaults.
 * Allows postMessage to send only colors without fonts/shadows.
 */
function mergeWithDefaults(theme: Partial<ThemeConfig> & Pick<ThemeConfig, 'colors'>): ThemeConfig {
  return {
    colors: { ...DEFAULT_THEME.colors, ...(theme.colors || {}) },
    fonts: { ...DEFAULT_THEME.fonts, ...(theme.fonts || {}) },
    borderRadius: theme.borderRadius ?? DEFAULT_THEME.borderRadius,
    shadows: { ...DEFAULT_THEME.shadows, ...(theme.shadows || {}) },
  };
}

/**
 * Convert hex color (#RRGGBB) to space-separated RGB (R G B).
 * Required for Tailwind CSS opacity modifier support.
 * e.g., '#3B82F6' → '59 130 246'
 */
function hexToRgb(hex: string | null | undefined): string {
  if (!hex || typeof hex !== 'string') return '';
  const clean = hex.replace('#', '');
  const r = parseInt(clean.substring(0, 2), 16);
  const g = parseInt(clean.substring(2, 4), 16);
  const b = parseInt(clean.substring(4, 6), 16);
  if (isNaN(r) || isNaN(g) || isNaN(b)) return hex;
  return `${r} ${g} ${b}`;
}

/**
 * Converts a ThemeConfig object to a flat CSS variables object.
 * Colors are converted to RGB format for Tailwind opacity support.
 *
 * @param theme - Theme configuration to convert (can be partial)
 * @returns Object mapping CSS variable names to values
 */
export function themeToCSS(theme: ThemeConfig): Record<string, string> {
  const merged = mergeWithDefaults(theme);
  return {
    [THEME_CSS_VARS.PRIMARY]: hexToRgb(merged.colors.primary),
    [THEME_CSS_VARS.SECONDARY]: hexToRgb(merged.colors.secondary),
    [THEME_CSS_VARS.ACCENT]: hexToRgb(merged.colors.accent),
    [THEME_CSS_VARS.BACKGROUND]: hexToRgb(merged.colors.background),
    [THEME_CSS_VARS.FONT_HEADING]: merged.fonts.heading,
    [THEME_CSS_VARS.FONT_BODY]: merged.fonts.body,
    [THEME_CSS_VARS.BORDER_RADIUS]: merged.borderRadius,
    [THEME_CSS_VARS.SHADOW_SM]: merged.shadows.sm,
    [THEME_CSS_VARS.SHADOW_MD]: merged.shadows.md,
    [THEME_CSS_VARS.SHADOW_LG]: merged.shadows.lg,
  };
}

/**
 * Applies theme variables to the document root.
 * Sets CSS custom properties on :root element.
 *
 * SSR-safe: Does nothing if document is not available.
 *
 * @param theme - Theme configuration to apply
 *
 * @example
 * ```typescript
 * applyThemeVariables(myTheme);
 * // CSS variables are now set on :root
 * // Can use in CSS: color: var(--theme-primary);
 * ```
 */
export function applyThemeVariables(theme: ThemeConfig): void {
  // SSR safety check
  if (typeof document === 'undefined') {
    return;
  }

  const cssVars = themeToCSS(theme);
  const root = document.documentElement;

  // Apply each CSS variable to :root
  Object.entries(cssVars).forEach(([varName, value]) => {
    root.style.setProperty(varName, value);
  });
}

/**
 * Removes all theme variables from the document root.
 * Resets :root element to default (no custom theme variables).
 *
 * SSR-safe: Does nothing if document is not available.
 *
 * @example
 * ```typescript
 * removeThemeVariables();
 * // All --theme-* CSS variables removed from :root
 * ```
 */
export function removeThemeVariables(): void {
  // SSR safety check
  if (typeof document === 'undefined') {
    return;
  }

  const root = document.documentElement;

  // Remove each theme CSS variable
  Object.values(THEME_CSS_VARS).forEach((varName) => {
    root.style.removeProperty(varName);
  });
}

/**
 * Reads the current value of a theme CSS variable.
 *
 * SSR-safe: Returns empty string if document is not available.
 *
 * @param varName - CSS variable name (e.g., '--theme-primary')
 * @returns Current value of the CSS variable, or empty string if not set
 *
 * @example
 * ```typescript
 * const primary = getThemeVariable('--theme-primary');
 * console.log(primary); // '#3B82F6'
 * ```
 */
export function getThemeVariable(varName: string): string {
  // SSR safety check
  if (typeof document === 'undefined') {
    return '';
  }

  const root = document.documentElement;
  const value = root.style.getPropertyValue(varName);

  // Trim whitespace
  return value.trim();
}

/**
 * Checks if theme variables are currently applied to the document.
 *
 * SSR-safe: Returns false if document is not available.
 *
 * @returns True if any theme variable is set
 *
 * @example
 * ```typescript
 * if (hasThemeVariables()) {
 *   console.log('Theme is active');
 * }
 * ```
 */
export function hasThemeVariables(): boolean {
  // SSR safety check
  if (typeof document === 'undefined') {
    return false;
  }

  // Check if primary color is set (indicator that theme is applied)
  const primary = getThemeVariable(THEME_CSS_VARS.PRIMARY);
  return primary.length > 0;
}

/**
 * Gets all current theme variables as an object.
 *
 * SSR-safe: Returns empty object if document is not available.
 *
 * @returns Object mapping CSS variable names to their current values
 *
 * @example
 * ```typescript
 * const current = getCurrentThemeVariables();
 * console.log(current['--theme-primary']); // '#3B82F6'
 * ```
 */
export function getCurrentThemeVariables(): Record<string, string> {
  // SSR safety check
  if (typeof document === 'undefined') {
    return {};
  }

  const current: Record<string, string> = {};

  Object.values(THEME_CSS_VARS).forEach((varName) => {
    const value = getThemeVariable(varName);
    if (value) {
      current[varName] = value;
    }
  });

  return current;
}
