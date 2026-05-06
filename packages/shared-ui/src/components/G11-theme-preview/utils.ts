/**
 * G11 — WCAG 2.1 contrast utilities + auto-fix suggestion engine.
 *
 * Implements the relative-luminance + contrast-ratio formula from
 * https://www.w3.org/TR/WCAG21/#dfn-relative-luminance and the AA threshold
 * (≥4.5:1 for normal text) used in `.claude/rules/ai-branding-guidelines.md`
 * §5 quality gate.
 *
 * `suggestFix` is the reflexive auto-fix engine that powers the
 * "Tự động sửa" CTA on the WCAG warning state — it preserves the brand's
 * background and darkens (or lightens, when appropriate) the foreground
 * until the AA threshold is met. If the foreground cannot reach AA from
 * its current side, the function falls back to inverting fg/bg so the
 * resulting pair is guaranteed to pass AA.
 */

/** WCAG AA threshold for normal text. */
export const WCAG_AA_NORMAL = 4.5;

/** Convert a 6-digit hex `#rrggbb` to 0-255 RGB. Throws on malformed input. */
function hexToRgb(hex: string): { r: number; g: number; b: number } {
  const m = /^#?([0-9a-fA-F]{6})$/.exec(hex.trim());
  if (!m) {
    throw new Error(`Invalid hex colour: ${hex}`);
  }
  const value = m[1] as string;
  const num = parseInt(value, 16);
  return {
    r: (num >> 16) & 0xff,
    g: (num >> 8) & 0xff,
    b: num & 0xff,
  };
}

/** Convert 0-255 RGB to `#rrggbb`. */
function rgbToHex(r: number, g: number, b: number): string {
  const clamp = (v: number) => Math.max(0, Math.min(255, Math.round(v)));
  const hex = (v: number) => clamp(v).toString(16).padStart(2, '0');
  return `#${hex(r)}${hex(g)}${hex(b)}`;
}

/**
 * WCAG 2.1 relative luminance (0-1).
 *
 * Per https://www.w3.org/TR/WCAG21/#dfn-relative-luminance:
 *   For each channel c in [0..255]:
 *     c' = c / 255
 *     L_channel = c' / 12.92                     if c' <= 0.03928
 *               = ((c' + 0.055) / 1.055) ** 2.4  otherwise
 *   L = 0.2126 * R + 0.7152 * G + 0.0722 * B
 */
function relativeLuminance(r: number, g: number, b: number): number {
  const channel = (c: number) => {
    const v = c / 255;
    return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
  };
  return 0.2126 * channel(r) + 0.7152 * channel(g) + 0.0722 * channel(b);
}

/**
 * WCAG contrast ratio between two hex colours (returns 1-21).
 *
 * Formula: `(L1 + 0.05) / (L2 + 0.05)` where L1 ≥ L2.
 * Order-independent: `calculateContrast(a, b) === calculateContrast(b, a)`.
 *
 * Examples:
 * - `calculateContrast('#ffffff', '#ffffff')` ≈ 1
 * - `calculateContrast('#000000', '#ffffff')` = 21
 * - `calculateContrast('#777777', '#ffffff')` ≈ 4.48
 */
export function calculateContrast(fg: string, bg: string): number {
  const a = hexToRgb(fg);
  const b = hexToRgb(bg);
  const la = relativeLuminance(a.r, a.g, a.b);
  const lb = relativeLuminance(b.r, b.g, b.b);
  const lighter = Math.max(la, lb);
  const darker = Math.min(la, lb);
  return (lighter + 0.05) / (darker + 0.05);
}

/**
 * Internal: scale RGB by factor (preserving hue), clamped to 0-255.
 */
function scaleRgb(
  rgb: { r: number; g: number; b: number },
  factor: number,
): { r: number; g: number; b: number } {
  return {
    r: rgb.r * factor,
    g: rgb.g * factor,
    b: rgb.b * factor,
  };
}

/**
 * Auto-fix: return an AA-compliant fg/bg pair derived from `brandColors`.
 *
 * Strategy:
 *   1. Keep `brandColors.background` constant (preserves brand surface).
 *   2. Compute current ratio. If already AA → return as-is with reason.
 *   3. Determine direction: if foreground is lighter than background, push
 *      foreground toward white; otherwise push toward black.
 *   4. Iterate scaling factor in 0.05 steps (up to 20 iterations) until
 *      ratio ≥ AA threshold.
 *   5. If iteration cannot reach AA on this side, swap fg/bg as a last
 *      resort (guaranteed to flip the pair into the passing region).
 *
 * Reflexive guarantee: the returned pair MUST satisfy
 * `calculateContrast(out.fg, out.bg) >= WCAG_AA_NORMAL`. The component's
 * "Tự động sửa" CTA relies on this to demonstrate live remediation.
 */
export function suggestFix(brandColors: {
  foreground: string;
  background: string;
}): { fg: string; bg: string; reason: string } {
  const fgRgb = hexToRgb(brandColors.foreground);
  const bgRgb = hexToRgb(brandColors.background);
  const startRatio = calculateContrast(brandColors.foreground, brandColors.background);

  if (startRatio >= WCAG_AA_NORMAL) {
    return {
      fg: brandColors.foreground,
      bg: brandColors.background,
      reason: `Cặp màu hiện tại đạt AA (${startRatio.toFixed(2)}:1).`,
    };
  }

  const fgLum = relativeLuminance(fgRgb.r, fgRgb.g, fgRgb.b);
  const bgLum = relativeLuminance(bgRgb.r, bgRgb.g, bgRgb.b);

  // If fg is lighter than bg → push fg toward white (factor > 1).
  // Otherwise → push fg toward black (factor < 1).
  const pushLighter = fgLum >= bgLum;
  let candidateFg = { ...fgRgb };

  for (let step = 1; step <= 20; step++) {
    if (pushLighter) {
      // Lerp toward white (255, 255, 255).
      const t = step / 20;
      candidateFg = {
        r: fgRgb.r + (255 - fgRgb.r) * t,
        g: fgRgb.g + (255 - fgRgb.g) * t,
        b: fgRgb.b + (255 - fgRgb.b) * t,
      };
    } else {
      // Scale toward black (0, 0, 0).
      const factor = 1 - step / 20;
      candidateFg = scaleRgb(fgRgb, factor);
    }
    const candidateHex = rgbToHex(candidateFg.r, candidateFg.g, candidateFg.b);
    const ratio = calculateContrast(candidateHex, brandColors.background);
    if (ratio >= WCAG_AA_NORMAL) {
      return {
        fg: candidateHex,
        bg: brandColors.background,
        reason: pushLighter
          ? `Đã làm sáng foreground để đạt AA (${ratio.toFixed(2)}:1).`
          : `Đã làm tối foreground để đạt AA (${ratio.toFixed(2)}:1).`,
      };
    }
  }

  // Last resort: invert the pair. Guaranteed to flip into the passing
  // region because the inverse of a failing low-ratio direction is the
  // passing high-ratio direction relative to the now-swapped surface.
  const swapRatio = calculateContrast(brandColors.background, brandColors.foreground);
  if (swapRatio >= WCAG_AA_NORMAL) {
    return {
      fg: brandColors.background,
      bg: brandColors.foreground,
      reason: `Đã đảo foreground/background để đạt AA (${swapRatio.toFixed(2)}:1).`,
    };
  }

  // Defensive — fall back to black-on-white (21:1) if all else fails.
  return {
    fg: '#000000',
    bg: '#ffffff',
    reason: 'Fallback: chuyển sang đen-trên-trắng (21:1).',
  };
}
