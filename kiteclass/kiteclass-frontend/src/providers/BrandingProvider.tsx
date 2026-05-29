/**
 * BrandingProvider — applies tenant branding to public (auth) pages.
 *
 * Detects tenant from the URL via {@link useTenantFromUrl}, fetches the public
 * branding payload, and injects CSS variables into {@code <html>} so existing
 * Tailwind/Shadcn components (which key off CSS vars) recolor automatically.
 *
 * Graceful degradation: default palette is applied immediately; real tenant
 * values overwrite once the fetch resolves.
 *
 * @since Wave 4 (GAP-037)
 */

'use client';

import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  type ReactNode,
} from 'react';
import {
  DEFAULT_PUBLIC_BRANDING,
  type PublicBranding,
} from '@/lib/api/public-branding';
import { useTenantFromUrl } from '@/hooks/useTenantFromUrl';
import { usePublicBranding } from '@/hooks/use-public-branding';

interface BrandingContextValue {
  branding: PublicBranding;
  isLoading: boolean;
  tenantId: string | null;
}

const BrandingContext = createContext<BrandingContextValue>({
  branding: DEFAULT_PUBLIC_BRANDING,
  isLoading: false,
  tenantId: null,
});

interface BrandingProviderProps {
  children: ReactNode;
}

/**
 * Hex color (#RRGGBB) -> H S L channels consumable by Shadcn CSS vars.
 * Keeps the math tiny so we can inject without pulling in a color library.
 *
 * Exported so the authenticated dashboard applier (BrandingThemeApplier) can
 * reuse the exact same conversion — keeps the public + authenticated paths in
 * lockstep instead of duplicating the math.
 */
export function hexToHslString(hex: string | null | undefined): string | null {
  if (!hex || typeof hex !== 'string') return null;
  const clean = hex.replace('#', '');
  if (clean.length !== 6) return null;

  const r = parseInt(clean.slice(0, 2), 16) / 255;
  const g = parseInt(clean.slice(2, 4), 16) / 255;
  const b = parseInt(clean.slice(4, 6), 16) / 255;

  const max = Math.max(r, g, b);
  const min = Math.min(r, g, b);
  const l = (max + min) / 2;

  let h = 0;
  let s = 0;

  if (max !== min) {
    const d = max - min;
    s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
    if (max === r) h = (g - b) / d + (g < b ? 6 : 0);
    else if (max === g) h = (b - r) / d + 2;
    else h = (r - g) / d + 4;
    h *= 60;
  }

  return `${Math.round(h)} ${Math.round(s * 100)}% ${Math.round(l * 100)}%`;
}

/**
 * Minimal shape both {@link PublicBranding} (auth pages) and the authenticated
 * {@code Branding} (dashboard) satisfy — only the brand colours are needed to
 * recolour CSS variables.
 */
export interface BrandColorSource {
  primaryColor: string;
  secondaryColor: string;
  accentColor: string;
}

/**
 * Inject tenant brand colours into the document root CSS variables.
 *
 * Sets both raw-hex `--brand-*` vars (direct consumers) and the Shadcn HSL
 * `--primary`/`--accent` channels (Tailwind components recolour automatically).
 * SSR-safe: no-op when document is unavailable.
 *
 * Shared by {@link BrandingProvider} (public auth pages) and the dashboard's
 * BrandingThemeApplier (authenticated) so a single applier governs both paths.
 *
 * NOTE: deliberately does NOT touch `--theme-*` vars (ThemeContext / localStorage
 * brand theme) nor the light/dark mode toggle — those are separate concerns.
 */
export function applyBrandColorVars(branding: BrandColorSource): void {
  if (typeof document === 'undefined') return;
  const root = document.documentElement;

  // Raw hex — for consumers that read `var(--brand-primary)` directly.
  root.style.setProperty('--brand-primary', branding.primaryColor);
  root.style.setProperty('--brand-secondary', branding.secondaryColor);
  root.style.setProperty('--brand-accent', branding.accentColor);

  // Tailwind/Shadcn Hue-Sat-Lightness channels (space-separated triple).
  const primaryHsl = hexToHslString(branding.primaryColor);
  if (primaryHsl) {
    root.style.setProperty('--primary', primaryHsl);
  }
  const accentHsl = hexToHslString(branding.accentColor);
  if (accentHsl) {
    root.style.setProperty('--accent', accentHsl);
  }
}

function applyCssVars(branding: PublicBranding): void {
  applyBrandColorVars(branding);
}

export function BrandingProvider({ children }: BrandingProviderProps) {
  const tenantId = useTenantFromUrl();
  const { branding, isLoading } = usePublicBranding(tenantId);

  useEffect(() => {
    applyCssVars(branding);
  }, [branding]);

  const value = useMemo<BrandingContextValue>(
    () => ({ branding, isLoading, tenantId }),
    [branding, isLoading, tenantId],
  );

  return (
    <BrandingContext.Provider value={value}>
      {children}
    </BrandingContext.Provider>
  );
}

/**
 * Access current tenant branding. Always returns a value — when no tenant
 * context is present, consumers receive {@link DEFAULT_PUBLIC_BRANDING}.
 */
export function useTenantBranding(): BrandingContextValue {
  return useContext(BrandingContext);
}
