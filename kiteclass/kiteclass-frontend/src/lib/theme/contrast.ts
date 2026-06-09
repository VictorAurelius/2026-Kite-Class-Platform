/**
 * WCAG AA contrast guard + theme-var derivation.
 *
 * Tenant brand palettes (AI Branding / Owner picker) can produce low-contrast
 * combos (pale text on white, pale CTA with white label). These pure helpers
 * clamp the lightness of derived text/CTA colours so the rendered theme always
 * meets WCAG AA (contrast ratio >= 4.5:1 normal text, >= 3:1 large text / UI).
 *
 * Everything here is pure + SSR-safe (no `document` access) so the same code can
 * run server-side (SSR-inline <style>) and client-side (BrandingThemeApplier).
 *
 * @since Wave landing-100 Bucket D
 */

/** WCAG AA contrast target for normal-size text. */
export const WCAG_AA_TEXT = 4.5;
/** WCAG AA contrast target for large text / UI components. */
export const WCAG_AA_LARGE = 3.0;

export type Rgb = readonly [number, number, number];
export type Hsl = readonly [number, number, number]; // h:0-360, s:0-100, l:0-100

const DEFAULT_PRIMARY = '#3B82F6';
const DEFAULT_SECONDARY = '#8B5CF6';
const DEFAULT_ACCENT = '#F59E0B';
const DEFAULT_BACKGROUND = '#FFFFFF';

/** Parse `#RGB` / `#RRGGBB` → [r,g,b] (0-255); null when invalid. */
export function hexToRgbTuple(hex: string | null | undefined): Rgb | null {
  if (!hex || typeof hex !== 'string') return null;
  let clean = hex.trim().replace('#', '');
  if (clean.length === 3) {
    clean = clean
      .split('')
      .map((c) => c + c)
      .join('');
  }
  if (clean.length !== 6) return null;
  const r = parseInt(clean.slice(0, 2), 16);
  const g = parseInt(clean.slice(2, 4), 16);
  const b = parseInt(clean.slice(4, 6), 16);
  if (Number.isNaN(r) || Number.isNaN(g) || Number.isNaN(b)) return null;
  return [r, g, b];
}

/** [r,g,b] (0-255) → `#RRGGBB`. */
export function rgbToHex([r, g, b]: Rgb): string {
  const h = (n: number) =>
    Math.max(0, Math.min(255, Math.round(n))).toString(16).padStart(2, '0');
  return `#${h(r)}${h(g)}${h(b)}`.toUpperCase();
}

/** [r,g,b] → space-separated RGB triple for Tailwind `rgb(var(--x) / a)`. */
export function rgbToVar([r, g, b]: Rgb): string {
  return `${Math.round(r)} ${Math.round(g)} ${Math.round(b)}`;
}

/** WCAG relative luminance of an sRGB colour (0-1). */
export function relativeLuminance([r, g, b]: Rgb): number {
  const ch = (c: number) => {
    const s = c / 255;
    return s <= 0.03928 ? s / 12.92 : ((s + 0.055) / 1.055) ** 2.4;
  };
  return 0.2126 * ch(r) + 0.7152 * ch(g) + 0.0722 * ch(b);
}

/** WCAG contrast ratio between two colours (1..21). */
export function contrastRatio(a: Rgb, b: Rgb): number {
  const la = relativeLuminance(a);
  const lb = relativeLuminance(b);
  const [hi, lo] = la >= lb ? [la, lb] : [lb, la];
  return (hi + 0.05) / (lo + 0.05);
}

/** [r,g,b] (0-255) → [h,s,l]. */
export function rgbToHsl([r, g, b]: Rgb): Hsl {
  const rr = r / 255;
  const gg = g / 255;
  const bb = b / 255;
  const max = Math.max(rr, gg, bb);
  const min = Math.min(rr, gg, bb);
  const l = (max + min) / 2;
  let h = 0;
  let s = 0;
  if (max !== min) {
    const d = max - min;
    s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
    if (max === rr) h = (gg - bb) / d + (gg < bb ? 6 : 0);
    else if (max === gg) h = (bb - rr) / d + 2;
    else h = (rr - gg) / d + 4;
    h *= 60;
  }
  return [h, s * 100, l * 100];
}

/** [h,s,l] → [r,g,b] (0-255). */
export function hslToRgb([h, s, l]: Hsl): Rgb {
  const hh = ((h % 360) + 360) % 360;
  const ss = Math.max(0, Math.min(100, s)) / 100;
  const ll = Math.max(0, Math.min(100, l)) / 100;
  const c = (1 - Math.abs(2 * ll - 1)) * ss;
  const x = c * (1 - Math.abs(((hh / 60) % 2) - 1));
  const m = ll - c / 2;
  let r = 0;
  let g = 0;
  let b = 0;
  if (hh < 60) [r, g, b] = [c, x, 0];
  else if (hh < 120) [r, g, b] = [x, c, 0];
  else if (hh < 180) [r, g, b] = [0, c, x];
  else if (hh < 240) [r, g, b] = [0, x, c];
  else if (hh < 300) [r, g, b] = [x, 0, c];
  else [r, g, b] = [c, 0, x];
  return [(r + m) * 255, (g + m) * 255, (b + m) * 255];
}

/** Hex → Shadcn HSL channel string `"H S% L%"` (null when invalid). */
export function hexToHslChannels(hex: string | null | undefined): string | null {
  const rgb = hexToRgbTuple(hex);
  if (!rgb) return null;
  const [h, s, l] = rgbToHsl(rgb);
  return `${Math.round(h)} ${Math.round(s)}% ${Math.round(l)}%`;
}

/** Black/white — whichever reads better on `bg`. */
export function readableOn(bg: Rgb): Rgb {
  const white: Rgb = [255, 255, 255];
  const black: Rgb = [0, 0, 0];
  return contrastRatio(white, bg) >= contrastRatio(black, bg) ? white : black;
}

/**
 * Clamp `fg` lightness until it meets `target` contrast against `bg`, keeping
 * hue + saturation. Contrast ratio is symmetric, so this also makes `fg` usable
 * as a *background* that a colour-equal-to-`bg` label reads on. Falls back to
 * pure black/white when no in-gamut lightness reaches the target.
 */
export function ensureContrast(fg: Rgb, bg: Rgb, target = WCAG_AA_TEXT): Rgb {
  if (contrastRatio(fg, bg) >= target) return fg;
  const [h, s] = rgbToHsl(fg);
  const bgLum = relativeLuminance(bg);
  // On a light bg, darken first; on a dark bg, lighten first.
  const dirs = bgLum > 0.5 ? [-1, 1] : [1, -1];
  for (const dir of dirs) {
    const [, , startL] = rgbToHsl(fg);
    let l = startL;
    for (let i = 0; i < 100; i++) {
      l += dir;
      if (l < 0 || l > 100) break;
      const cand = hslToRgb([h, s, l]);
      if (contrastRatio(cand, bg) >= target) return cand;
    }
  }
  return readableOn(bg);
}

export interface ThemePaletteInput {
  primary?: string | null;
  secondary?: string | null;
  accent?: string | null;
  background?: string | null;
}

/**
 * Derive the full set of contrast-guarded `--theme-*` CSS variable values
 * (RGB triples) from a tenant palette. Raw brand colours pass through verbatim
 * (they are decorative); the *text* + *CTA* colours are clamped to WCAG AA.
 */
export function deriveThemeVars(
  input: ThemePaletteInput,
): Record<string, string> {
  const primary = hexToRgbTuple(input.primary) ?? hexToRgbTuple(DEFAULT_PRIMARY)!;
  const secondary =
    hexToRgbTuple(input.secondary) ?? hexToRgbTuple(DEFAULT_SECONDARY)!;
  const accent = hexToRgbTuple(input.accent) ?? hexToRgbTuple(DEFAULT_ACCENT)!;
  const background =
    hexToRgbTuple(input.background) ?? hexToRgbTuple(DEFAULT_BACKGROUND)!;

  // CTA: derive a bold, high-emphasis fill from accent (fallback primary) that a
  // WHITE label reads on (contrast symmetric → ensureContrast vs white works).
  const ctaBase = hexToRgbTuple(input.accent) ?? primary;
  const cta = ensureContrast(ctaBase, [255, 255, 255], WCAG_AA_TEXT);
  const ctaForeground = readableOn(cta);

  // Body/link text tinted from primary but clamped readable on the theme bg.
  const text = ensureContrast(primary, background, WCAG_AA_TEXT);
  // Label colour that reads on a primary-coloured fill (e.g. hero button).
  const onPrimary = readableOn(primary);

  return {
    '--theme-primary': rgbToVar(primary),
    '--theme-secondary': rgbToVar(secondary),
    '--theme-accent': rgbToVar(accent),
    '--theme-background': rgbToVar(background),
    '--theme-cta': rgbToVar(cta),
    '--theme-cta-foreground': rgbToVar(ctaForeground),
    '--theme-text': rgbToVar(text),
    '--theme-on-primary': rgbToVar(onPrimary),
  };
}

/**
 * Build a `:root { ... }` CSS string for SSR-inline injection. Emits the
 * contrast-guarded `--theme-*` RGB vars AND the Shadcn `--primary/--secondary/
 * --accent` HSL channels so first paint (server HTML) already carries the
 * tenant theme — no FOUC, applied immediately on reload.
 */
export function buildThemeStyleCss(input: ThemePaletteInput): string {
  const vars = deriveThemeVars(input);

  const primaryHsl = hexToHslChannels(input.primary);
  if (primaryHsl) vars['--primary'] = primaryHsl;
  const secondaryHsl = hexToHslChannels(input.secondary);
  if (secondaryHsl) vars['--secondary'] = secondaryHsl;
  const accentHsl = hexToHslChannels(input.accent);
  if (accentHsl) vars['--accent'] = accentHsl;

  const body = Object.entries(vars)
    .map(([k, v]) => `${k}:${v};`)
    .join('');
  return `:root{${body}}`;
}
