/**
 * Theme Sync Component
 *
 * Syncs backend landing page colors with theme system.
 * Directly sets CSS variables on :root for immediate visual effect.
 *
 * @since PR-THEME-1, fixed PR-THEME-3
 */

'use client';

import { useEffect } from 'react';

interface ThemeSyncProps {
  primaryColor?: string;
  secondaryColor?: string;
  accentColor?: string;
}

function hexToRgb(hex: string): string {
  const clean = hex.replace('#', '');
  const r = parseInt(clean.substring(0, 2), 16);
  const g = parseInt(clean.substring(2, 4), 16);
  const b = parseInt(clean.substring(4, 6), 16);
  if (isNaN(r) || isNaN(g) || isNaN(b)) return '59 130 246';
  return `${r} ${g} ${b}`;
}

export function ThemeSync({ primaryColor, secondaryColor, accentColor }: ThemeSyncProps) {
  useEffect(() => {
    if (typeof document === 'undefined') return;

    const root = document.documentElement;

    if (primaryColor) {
      root.style.setProperty('--theme-primary', hexToRgb(primaryColor));
    }
    if (secondaryColor) {
      root.style.setProperty('--theme-secondary', hexToRgb(secondaryColor));
    }
    if (accentColor) {
      root.style.setProperty('--theme-accent', hexToRgb(accentColor));
    }
  }, [primaryColor, secondaryColor, accentColor]);

  return null;
}
