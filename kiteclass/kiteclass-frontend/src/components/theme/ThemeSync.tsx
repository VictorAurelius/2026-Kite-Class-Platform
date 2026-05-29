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

function hexToRgb(hex: string | null | undefined): string {
  if (!hex || typeof hex !== 'string') return '59 130 246';
  const clean = hex.replace('#', '');
  const r = parseInt(clean.substring(0, 2), 16);
  const g = parseInt(clean.substring(2, 4), 16);
  const b = parseInt(clean.substring(4, 6), 16);
  if (isNaN(r) || isNaN(g) || isNaN(b)) return '59 130 246';
  return `${r} ${g} ${b}`;
}

// shadcn UI primitives (Button, Card, etc.) read the `--primary`/`--secondary`/`--accent`
// HSL vars, NOT the RGB `--theme-*` vars. Set both so the tenant colour reaches every
// component (GAP-809: previously only --theme-* synced → shadcn buttons stayed default blue).
function hexToHsl(hex: string | null | undefined): string | null {
  if (!hex || typeof hex !== 'string') return null;
  const clean = hex.replace('#', '');
  if (clean.length !== 6) return null;
  const r = parseInt(clean.slice(0, 2), 16) / 255;
  const g = parseInt(clean.slice(2, 4), 16) / 255;
  const b = parseInt(clean.slice(4, 6), 16) / 255;
  if (Number.isNaN(r) || Number.isNaN(g) || Number.isNaN(b)) return null;
  const max = Math.max(r, g, b), min = Math.min(r, g, b);
  let h = 0;
  const l = (max + min) / 2;
  const d = max - min;
  const s = d === 0 ? 0 : d / (1 - Math.abs(2 * l - 1));
  if (d !== 0) {
    if (max === r) h = ((g - b) / d) % 6;
    else if (max === g) h = (b - r) / d + 2;
    else h = (r - g) / d + 4;
    h *= 60;
    if (h < 0) h += 360;
  }
  return `${Math.round(h)} ${Math.round(s * 100)}% ${Math.round(l * 100)}%`;
}

export function ThemeSync({ primaryColor, secondaryColor, accentColor }: ThemeSyncProps) {
  useEffect(() => {
    if (typeof document === 'undefined') return;

    const root = document.documentElement;

    if (primaryColor) {
      root.style.setProperty('--theme-primary', hexToRgb(primaryColor));
      const hsl = hexToHsl(primaryColor);
      if (hsl) root.style.setProperty('--primary', hsl);
    }
    if (secondaryColor) {
      root.style.setProperty('--theme-secondary', hexToRgb(secondaryColor));
      const hsl = hexToHsl(secondaryColor);
      if (hsl) root.style.setProperty('--secondary', hsl);
    }
    if (accentColor) {
      root.style.setProperty('--theme-accent', hexToRgb(accentColor));
      const hsl = hexToHsl(accentColor);
      if (hsl) root.style.setProperty('--accent', hsl);
    }
  }, [primaryColor, secondaryColor, accentColor]);

  return null;
}
