/**
 * Theme Sync Component
 *
 * Syncs backend landing page colors with theme system.
 * Applies AI-generated colors from backend to theme CSS variables.
 *
 * @since PR-THEME-1 (Task #9)
 */

'use client';

import { useEffect } from 'react';
import { useTheme } from '@/contexts/ThemeContext';

interface ThemeSyncProps {
  /** Primary color from backend AI Branding */
  primaryColor?: string;
  /** Secondary color from backend AI Branding */
  secondaryColor?: string;
  /** Accent color from backend AI Branding (optional) */
  accentColor?: string;
}

/**
 * ThemeSync Component
 *
 * Applies backend AI-generated colors to the theme system.
 * Should be placed near the top of pages that fetch branding data.
 *
 * @example
 * ```tsx
 * <ThemeSync
 *   primaryColor={landingData.primaryColor}
 *   secondaryColor={landingData.secondaryColor}
 * />
 * ```
 */
export function ThemeSync({
  primaryColor,
  secondaryColor,
  accentColor,
}: ThemeSyncProps) {
  const { theme, setTheme } = useTheme();

  useEffect(() => {
    // Only update if backend has provided colors different from current theme
    if (!primaryColor && !secondaryColor && !accentColor) {
      return;
    }

    // Check if colors have changed
    const hasColorChanges =
      (primaryColor && primaryColor !== theme.colors.primary) ||
      (secondaryColor && secondaryColor !== theme.colors.secondary) ||
      (accentColor && accentColor !== theme.colors.accent);

    if (!hasColorChanges) {
      return;
    }

    // Apply backend colors to theme
    setTheme({
      ...theme,
      colors: {
        ...theme.colors,
        ...(primaryColor && { primary: primaryColor }),
        ...(secondaryColor && { secondary: secondaryColor }),
        ...(accentColor && { accent: accentColor }),
      },
    });
  }, [primaryColor, secondaryColor, accentColor, theme, setTheme]);

  // This component doesn't render anything
  return null;
}
