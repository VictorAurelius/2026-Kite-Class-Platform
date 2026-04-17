/**
 * Default Theme Configuration
 *
 * This theme is used when:
 * - Instance has not yet configured AI Branding
 * - AI Branding failed or is being processed
 * - User resets theme to default
 *
 * Design principles:
 * - Professional, neutral colors that work for all education types
 * - Accessible color contrast (WCAG AA compliant)
 * - Matches Tailwind's default color system
 * - Works well in both light and dark modes (future)
 *
 * @since PR-THEME-1
 */

import type { ThemeConfig } from './types';

/**
 * Default theme configuration.
 * Uses professional blue palette with good contrast and readability.
 *
 * Color choices:
 * - Primary (#3B82F6): Trustworthy blue, common in education
 * - Secondary (#8B5CF6): Complementary purple for accents
 * - Accent (#F59E0B): Warm amber for CTAs and highlights
 * - Background (#FFFFFF): Clean white background
 *
 * All colors are from Tailwind's default palette for consistency.
 */
export const DEFAULT_THEME: ThemeConfig = {
  colors: {
    // Primary blue-500 - used for main CTAs, links, active states
    primary: '#3B82F6',

    // Secondary violet-500 - used for secondary actions, hover states
    secondary: '#8B5CF6',

    // Accent amber-500 - used for warnings, highlights, special emphasis
    accent: '#F59E0B',

    // Background white - main page background
    background: '#FFFFFF',
  },

  fonts: {
    // Inter is already loaded in root layout, so we use it for consistency
    heading: 'Inter, system-ui, -apple-system, sans-serif',
    body: 'Inter, system-ui, -apple-system, sans-serif',
  },

  // Moderate border radius - not too sharp, not too rounded
  borderRadius: '8px',

  shadows: {
    // Subtle shadow for slight elevation (e.g., hover states)
    sm: '0 1px 2px 0 rgba(0, 0, 0, 0.05)',

    // Medium shadow for cards and panels
    md: '0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06)',

    // Large shadow for modals, popovers, dropdowns
    lg: '0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05)',
  },
};

/**
 * CSS variable names for theme properties.
 * Used when applying theme to DOM.
 */
export const THEME_CSS_VARS = {
  // Colors
  PRIMARY: '--theme-primary',
  SECONDARY: '--theme-secondary',
  ACCENT: '--theme-accent',
  BACKGROUND: '--theme-background',

  // Fonts
  FONT_HEADING: '--theme-font-heading',
  FONT_BODY: '--theme-font-body',

  // Border radius
  BORDER_RADIUS: '--theme-border-radius',

  // Shadows
  SHADOW_SM: '--theme-shadow-sm',
  SHADOW_MD: '--theme-shadow-md',
  SHADOW_LG: '--theme-shadow-lg',
} as const;

/**
 * localStorage key for storing user's theme configuration.
 */
export const THEME_STORAGE_KEY = 'kiteclass_theme';

/**
 * Validates if a color string is a valid CSS color.
 * Basic validation - checks for hex colors, rgb(), hsl(), etc.
 *
 * @param color - Color string to validate
 * @returns True if valid CSS color format
 */
export function isValidCSSColor(color: string): boolean {
  // Check for hex colors (#RGB, #RRGGBB, #RRGGBBAA)
  if (/^#([0-9A-Fa-f]{3}){1,2}([0-9A-Fa-f]{2})?$/.test(color)) {
    return true;
  }

  // Check for rgb/rgba
  if (/^rgba?\([\d\s,%.]+\)$/.test(color)) {
    return true;
  }

  // Check for hsl/hsla
  if (/^hsla?\([\d\s,%.]+\)$/.test(color)) {
    return true;
  }

  // Check for named colors (basic check - just alphabetic)
  if (/^[a-z]+$/.test(color.toLowerCase())) {
    return true;
  }

  return false;
}
