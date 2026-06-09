/**
 * Theme Sync Component
 *
 * Renders the tenant landing-page theme as an SSR-inline `<style>` block.
 *
 * Why a `<style>` instead of a post-hydration `useEffect`:
 * the previous version set CSS vars on `:root` only AFTER React hydrated, so on
 * every reload the page first painted the globals.css default blue, then flipped
 * to the tenant colour (FOUC / "theme not applied on reload"). This component is
 * a Server Component — the colours arrive as props from the server-fetched
 * landing payload (see `(public)/page.tsx`), so the `:root{...}` block is part of
 * the initial server HTML. First paint already carries the tenant theme.
 *
 * The emitted vars are contrast-guarded (WCAG AA) via {@link buildThemeStyleCss}:
 * raw `--theme-primary/secondary/accent` pass through, while the derived
 * `--theme-cta` / `--theme-text` are clamped to >= 4.5:1, plus the Shadcn HSL
 * `--primary/--secondary/--accent` channels so first paint is fully themed.
 *
 * @since PR-THEME-1, SSR-inline + contrast guard Wave landing-100 Bucket D
 */

import { buildThemeStyleCss } from '@/lib/theme/contrast';

interface ThemeSyncProps {
  primaryColor?: string;
  secondaryColor?: string;
  accentColor?: string;
  /** Page background; defaults to white when the tenant has no custom bg. */
  backgroundColor?: string;
}

export function ThemeSync({
  primaryColor,
  secondaryColor,
  accentColor,
  backgroundColor,
}: ThemeSyncProps) {
  const css = buildThemeStyleCss({
    primary: primaryColor,
    secondary: secondaryColor,
    accent: accentColor,
    background: backgroundColor,
  });

  // SSR-inline: this <style> is server-rendered into the initial HTML, so the
  // tenant theme is present on first paint (no FOUC, applied on reload).
  return (
    <style
      data-theme-sync=""
      dangerouslySetInnerHTML={{ __html: css }}
    />
  );
}
