/**
 * BrandingThemeApplier — applies the authenticated tenant's persisted branding
 * colours to the dashboard.
 *
 * The public auth pages get branding via {@link BrandingProvider} (wraps the
 * `(auth)` layout only). The dashboard `(dashboard)` layout had NO branding
 * fetch, so an Owner's saved colours never reached the dashboard — it stayed on
 * the globals.css default blue (GAP-807).
 *
 * This component fetches the authenticated branding (`GET /api/v1/settings/branding`
 * via {@link useBranding}) and injects the brand CSS variables on mount + whenever
 * the branding payload changes. It reuses {@link applyBrandColorVars} so the
 * public + authenticated paths stay in lockstep.
 *
 * Scope discipline:
 * - Touches ONLY the brand-colour vars (`--primary` / `--accent` / `--brand-*`).
 * - Does NOT touch the light/dark mode toggle (NextThemesProvider).
 * - Does NOT touch the `--theme-*` localStorage brand theme (ThemeContext) or the
 *   postMessage preview path (ThemeReceiver) — those remain independent.
 *
 * Renders nothing (side-effect only).
 *
 * @since GAP-807
 */

'use client';

import { useEffect } from 'react';
import { useBranding } from '@/hooks/use-branding';
import { applyBrandColorVars } from '@/providers/BrandingProvider';
import { deriveThemeVars } from '@/lib/theme/contrast';

export function BrandingThemeApplier(): null {
  const { data: branding } = useBranding();

  useEffect(() => {
    if (!branding) return;
    // Shadcn HSL `--primary/--accent` + raw `--brand-*` (shared public path).
    applyBrandColorVars(branding);

    // Contrast-guarded `--theme-*` RGB vars (text/CTA clamped to WCAG AA),
    // matching the SSR-inline landing path (ThemeSync). The dashboard is
    // auth-gated (no server-fetched branding), so these apply post-hydration;
    // SSR-inline for the dashboard is a follow-up (see Đợt 2 note).
    const themeVars = deriveThemeVars({
      primary: branding.primaryColor,
      secondary: branding.secondaryColor,
      accent: branding.accentColor,
    });
    const root = document.documentElement;
    Object.entries(themeVars).forEach(([k, v]) => root.style.setProperty(k, v));
  }, [
    branding?.primaryColor,
    branding?.secondaryColor,
    branding?.accentColor,
    branding,
  ]);

  return null;
}
